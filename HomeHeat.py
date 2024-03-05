#!/usr/bin/env python3

import RPi.GPIO as GPIO
import adafruit_ads1x15.ads1115 as ADS
import argparse
import atexit
import board
import busio
import datetime
import logging
import logging.config
import math
import socket
import struct
import sys
import time
import yaml
from adafruit_ads1x15.analog_in import AnalogIn
from enum import Enum

VERSION = '1.0.0'

# Reference voltage for thermistor resistance measurements
REF_VOLTAGE = 3.3
# Factors for Steinhart-Hart equation
NTC_A_FACTOR = 0.0012797372847331715
NTC_B_FACTOR = 0.00026335882652276787
NTC_C_FACTOR = 1.6897560208446226e-7

DAY_SECONDS = 86400

TEMP_MIN = -20.0
TEMP_MIN_EXT = -50.0

TEMP_MAX = 100.0
TEMP_MAX_EXT = 60.0


class CircuitMode(Enum):
    OFF = 0
    NIGHT = 1
    DAY = 2
    ALL = 3

    @classmethod
    def from_name(cls, name):
        try:
            return cls[name.upper()]
        except:
            return cls.OFF


class MeasurementException(Exception):
    pass


def get_temperature(sensor_input, sensor_map_tab):
    """
    Gets the temperature reported by the sensor. Using
    Steinhart-Hart equation to convert the resistance of the
    thermistor to the temperature value (in celsius grades)
    :param sensor_input: the sensor id
    :param sensor_map_tab: The sensor table each with ADS channel, GPIO pin to provide supply for measurement, the
        reference resistor value
    :return: the temperature in celsius grades (float)
    """
    sensor_obj = sensor_map_tab[sensor_input]
    GPIO.output(sensor_obj[1], GPIO.HIGH)
    time.sleep(2)
    try:
        v0_0 = sensor_obj[0].voltage
        v0_1 = sensor_obj[0].voltage
        v0_2 = sensor_obj[0].voltage
        v0 = (v0_0 + v0_1 + v0_2) / 3
        r1 = sensor_obj[2]
        if v0 == REF_VOLTAGE:
            v0 = REF_VOLTAGE - 1e-4
        thermistor_resistance = (r1 * v0) / (REF_VOLTAGE - v0)
        log_tr = math.log(thermistor_resistance)
        return (1.0 / (NTC_A_FACTOR + NTC_B_FACTOR * log_tr + NTC_C_FACTOR * log_tr * log_tr * log_tr)) - 273.15
    finally:
        GPIO.output(sensor_obj[1], GPIO.LOW)


def calculate_avg_ext_temperature(current_temperature, hist_temp_array):
    """
    Calculates the average external temperature removing the oldest
    measure from list (if full 1440 measurements are available) and adds current one on top of the list
    :param current_temperature: the current measured external temperature
    :param hist_temp_array: the list with the previous external temperatures to compute avg
    :return: The average temperature
    """
    if len(hist_temp_array) >= 1440:
        del hist_temp_array[0]
    hist_temp_array.append(current_temperature)
    return math.fsum(hist_temp_array) / len(hist_temp_array)


def get_ext_temperature_level(avg_temp, ext_max_temp, ext_min_temp, ext_start_threshold):
    """
    Calculates the level of the external temperature for controlling the heating period
    :param avg_temp: The average temperature
    :param ext_max_temp: The maximum external temperature to start heating
    :param ext_min_temp: The minimum external temperature to max heating
    :param ext_start_threshold: The initial threshold (0-1) of the level for ext_max_temp
    :return: The heating level
    """
    if avg_temp > ext_max_temp:
        return 0.0
    if avg_temp < ext_min_temp:
        return 1.0
    return ((ext_max_temp - ext_start_threshold * ext_min_temp) / (1 - ext_start_threshold) - avg_temp) / (
                (ext_max_temp - ext_start_threshold * ext_min_temp) / (1 - ext_start_threshold) - ext_min_temp)


def get_heating_period(heat_level, day_period, current_temp, base_level, circuit_data, decrease_temp = 0.0):
    """
    Calculates the length in seconds to heat the specific circuit
    :param heat_level: The heat level (0-1) from the external temperature
    :param day_period: The heating period - 1 (night) or 2 (day)
    :param current_temp: The current circuit temperature
    :param base_level: The base temperature for circuit (to be reached for heat_level=0)
    :param circuit_data: The structure with circuit definition, containing maxTemp, the heating characteristics
           of the circuit - table of dictionary elements each with 'tempMax' and 'heatFactor' defining the heating
           function in the specific segment
    :param decrease_temp: The base/top temperatures values to be decreased by that value (default 0.0)
    :return: Heating length in seconds to reach desired temperature, desired temperature
    """
    max_temp = circuit_data['maxTemp'] - decrease_temp
    try:
        base_level = circuit_data['tempBaseLevel']
    except KeyError:
        pass
    base_level = base_level - decrease_temp
    adjust_value = 1
    try:
        if day_period == 1:
            adjust_value += float(circuit_data['nightAdjust'] or 0)
        elif day_period == 2:
            adjust_value += float(circuit_data['dayAdjust'] or 0)
    except KeyError:
        pass
    heat_level *= adjust_value
    if heat_level > 1:
        heat_level = 1
    elif heat_level < 0:
        heat_level = 0
    desired_temp = base_level + (max_temp - base_level) * heat_level
    diff = desired_temp - current_temp
    if diff <= 0:
        return 0, 0.0
    heat_period = 0
    heat_characteristics = circuit_data['heatCharacteristics']
    n = len(heat_characteristics) - 2
    while n >= 0:
        elem_a = heat_characteristics[n]
        elem_b = heat_characteristics[n + 1]
        temp_max_a = elem_a['tempMax']
        temp_max_b = elem_b['tempMax']
        heat_factor = elem_b['heatFactor']
        if desired_temp >= temp_max_a and current_temp < temp_max_b:
            tb = min(desired_temp, temp_max_b)
            ta = max(current_temp, temp_max_a)
            heat_period += (tb - ta) / heat_factor
        n -= 1
    temp_max_b = heat_characteristics[0]['tempMax']
    tb = min(desired_temp, temp_max_b)
    heat_factor = heat_characteristics[0]['heatFactor']
    if current_temp < heat_characteristics[0]['tempMax']:
        heat_period += (tb - current_temp) / heat_factor
    return heat_period, desired_temp


def parse_time(time_str):
    """
    Parses the time from HH:mm:ss format to time object
    :param time_str: the time string in HH:mm:ss format (seconds and minutes can be omitted)
    :return: The parsed time object
    """
    p = time_str.split(':')
    hour = int(p[0])
    if len(p) > 1:
        min = int(p[1])
    else:
        min = 0
    if len(p) > 2:
        sec = int(p[2])
    else:
        sec = 0
    return datetime.time(hour, min, sec)


def get_seconds(ts):
    """
    Converts the time object to number of seconds from beginning of the day
    :param ts: the time object
    :return: the number of seconds from beginning of the day
    """
    return ts.second + 60 * ts.minute + 3600 * ts.hour


def get_day_period(current_timestamp, config):
    """
    Returns the day period
    :param current_timestamp: current time (time object)
    :param config: config object containing start/end times
    :return: The tuple with day period (0 - no heating, 1 - night heating, 2 - day heating)
        time to end of current period (if other than 0)
        length in second of the period (if other than 0)
    """
    night_start_time = config['nightStartTime']
    night_end_time = config['nightEndTime']
    day_start_time = config['dayStartTime']
    day_end_time = config['dayEndTime']
    current_time = get_seconds(current_timestamp.time())

    if current_time > night_start_time:
        return 1, DAY_SECONDS - current_time + night_end_time, night_end_time + DAY_SECONDS - night_start_time
    if current_time < night_end_time:
        return 1, night_end_time - current_time, night_end_time + DAY_SECONDS - night_start_time
    if day_start_time < current_time < day_end_time:
        return 2, day_end_time - current_time, day_end_time - day_start_time
    else:
        return 0, 0, 0


def reset_circuits(circuit_table):
    """
    Resets all heating circuits
    :param circuit_table: The table with circuits containing table element with gpio PIN and its state
    """
    for elem in circuit_table:
        GPIO.output(elem[0], GPIO.LOW)
        elem[1] = GPIO.LOW
        time.sleep(1.0)


def initialize_ads(ads_id, i2c_object, ads_list, sensor_setup_list, sensor_map_list):
    """
    Initialize the given ADS1115 module and fills in all related AnalogIn objects in
    the sensor_map_data table
    :param ads_id: The ADS1115 ID. The I2C address of ADS module is 0x48+ads_id
    :param i2c_object: The I2C object
    :param ads_list: The ADS module list. The list element at index ads_id receives newly created ADS1115 object
    :param sensor_setup_list: The sensor setup data table - each element contains inner table with ADS module Id,
                              the ADS input id of the sensor, the GPIO PIN providing the voltage for measurement of
                              this sensor, the value of the reference resistor
    :param sensor_map_list: The sensor map list - the element of this list contains inner list with the ADS1115 AnalogIn
                            object (created by this function), GPIO PIN providing the voltage for this sensor, the
                            value of the reference resitor
    """
    try:
        ads_list[ads_id] = ADS.ADS1115(i2c_object, address=0x48 + ads_id)
        for index, sensor_data in enumerate(sensor_setup_list):
            if sensor_data[0] == ads_id:
                sensor_map_list[index][0] = AnalogIn(ads_list[ads_id], sensor_setup_list[index][1])
    except (IOError, ValueError):
        logger.error("Could not initialize ADS",  ads_id, " address ", (0x48 + ads_id), exc_info=True)


def check_decrease(cfg, timestamp):
    """
    Returns the value to decrease base/max temperatures levels for the circuits to
    temporarily lower the desired temperature. As this is defined for the temporarily
    decrease only both from/to datetimes have to be defined, otherwise settings are ignored
    :param cfg: The config object, where offHome array may be defined with decreaseFrom, decreaseTo
                (as date-time ISO-8601 strings) and decreaseTemp (double) can be defined
    :param timestamp: The timestamp to check the decrease level
    :return: The double value defining the temperature decrease, 0.0 if nothing defined for
             the timestamp
    """
    decrease_temp = 0.0
    off_home = cfg.get("offHome", [])
    for decrease_rec in off_home:
        decrease_temp = decrease_rec.get("decreaseTemp")
        if decrease_temp is None:
            continue
        decrease_from = decrease_rec.get("decreaseFrom")
        if decrease_from is None or decrease_from >= timestamp:
            continue
        decrease_to = decrease_rec.get("decreaseTo")
        if decrease_to is None or decrease_to < timestamp:
            continue
        return decrease_temp
    return 0.0


circuits = [
    [26, GPIO.LOW],
    [19, GPIO.LOW],
    [5, GPIO.LOW],
    [6, GPIO.LOW],
    [13, GPIO.LOW],
    [25, GPIO.LOW],
    [12, GPIO.LOW],
    [16, GPIO.LOW],
    [20, GPIO.LOW],
    [21, GPIO.LOW]
]


@atexit.register
def cleanup():
    reset_circuits(circuits)
    GPIO.cleanup()

def main():
    parser = argparse.ArgumentParser(description='Home Heat Controller process', prog='HomeHeatController')
    parser.add_argument('-c', '--config', default='config/HomeHeat.yml', help='Config file path')
    parser.add_argument('--version', action='version', version='%(prog)s ' + VERSION)
    cmd_args = parser.parse_args()

    with open(cmd_args.config, 'r') as cfg_file:
        cfg = yaml.load(cfg_file, Loader=yaml.FullLoader)

    with open(cfg.get('logConfig', 'config/HomeHeat_logging.yml'), 'r') as f:
        logging_config = yaml.load(f, Loader=yaml.FullLoader)
        logging.config.dictConfig(logging_config)
    logger = logging.getLogger()

    GPIO.setmode(GPIO.BCM)
    sensor_setup = [
        [0, ADS.P3, 14, 5600.0],  # external temperature sensor -> PIN TxD
        [0, ADS.P2, 15, 2200.0],  # -> uses PIN RxD
        [0, ADS.P1, 4, 2200.0],  # -> uses PIN GP4
        [0, ADS.P0, 17, 2200.0],  # -> uses PIN GP17
        [2, ADS.P3, 11, 2200.0],  # -> uses PIN SCLK
        [2, ADS.P2, 8, 2200.0],  # -> uses PIN CE0
        [2, ADS.P1, 7, 2200.0],  # -> uses PIN CE1
        [1, ADS.P3, 9, 2200.0],  # -> uses PIN MISO
        [1, ADS.P2, 10, 2200.0],  # -> uses PIN MOSI
        [1, ADS.P1, 23, 2200.0],  # -> uses PIN GP23
        [1, ADS.P0, 22, 2200.0]  # -> uses PIN GP22
    ]

    GPIO.setup([el[2] for el in sensor_setup], GPIO.OUT)


    GPIO.setup([el[0] for el in circuits], GPIO.OUT)

    i2c = busio.I2C(board.SCL, board.SDA)
    time.sleep(1.0)

    """
      Sensor monitoring the room - assignment to ADC1115 module/input
      The values are: ads channel instance, related output on this ads, gpio to provide
      the voltage for resistance measurement, the resistor value in ohm
    """
    ads = [None, None, None]
    sensor_map = []
    for i in sensor_setup:
        sensor_map.append([None, i[2], i[3]])
    for i in 0, 1, 2:
        initialize_ads(i, i2c, ads, sensor_setup, sensor_map)

    # sensor_map = [
    #     [AnalogIn(ads0, ADS.P3), 14, 5600.0], # index 0 is external temperature sensor! -> uses PIN TxD
    #     [AnalogIn(ads0, ADS.P2), 15, 2200.0], # -> uses PIN RxD
    #     [AnalogIn(ads0, ADS.P1), 4, 2200.0], # -> uses PIN GP4
    #     [AnalogIn(ads0, ADS.P0), 17, 2200.0], # -> uses PIN GP17
    #     [AnalogIn(ads2, ADS.P3), 11, 2200.0], # -> uses PIN SCLK
    #     [AnalogIn(ads2, ADS.P2), 8, 2200.0], # -> uses PIN CE0
    #     [AnalogIn(ads2, ADS.P1), 7, 2200.0], # -> uses PIN CE1
    #     # ads2, P0 -> not used!
    #     [AnalogIn(ads1, ADS.P3), 9, 2200.0], # -> uses PIN MISO
    #     [AnalogIn(ads1, ADS.P2), 10, 2200.0], # -> uses PIN MOSI
    #     [AnalogIn(ads1, ADS.P1), 23, 2200.0], # -> uses PIN GP23
    #     [AnalogIn(ads1, ADS.P0), 22, 2200.0] # -> uses PIN GP22
    # ]

    management_interface = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)
    cfg = []
    curr_day_period = 0
    while True:
        curr_timestamp = datetime.datetime.now()
        management_data = struct.pack('!d', curr_timestamp.timestamp())
        with open(cmd_args.config, 'r') as cfg_file:
            cfg = yaml.load(cfg_file, Loader=yaml.FullLoader)
        if 'logLevel' in cfg:
            logger.setLevel(cfg.get('logLevel'))
        day_period, time_to_end, period_length = get_day_period(curr_timestamp, cfg)
        if day_period == 0:
            if curr_day_period > 0:
                logger.info("Heating period finished %d", curr_day_period)
                reset_circuits(circuits)
        curr_day_period = day_period
        management_data += struct.pack('c', day_period.to_bytes(1, byteorder='little'))
        try:
            if ads[sensor_setup[0][0]] is None:
                initialize_ads(sensor_setup[0][0], i2c, ads, sensor_setup, sensor_map)
            if ads[sensor_setup[0][0]] is not None:
                try:
                    ext_temp = get_temperature(0, sensor_map)
                except (IOError, ValueError) as e:
                    logger.error("Could not measure external temperature", exc_info=True)
                    ads[sensor_setup[0][0]] = None
                    raise MeasurementException("ADS (ext) reading error", e)
            else:
                raise MeasurementException("ADS (ext) not initiliazed")

            logger.info("External temperature: %f", ext_temp)
            if ext_temp <= TEMP_MIN_EXT or ext_temp >= TEMP_MAX_EXT:
                logger.warning("External temperature: %f outside of reasonable level, check sensor connection!!!", ext_temp)
                reset_circuits(circuits)
                continue
            ext_temp_array = []
            try:
                with open('ExtTemperatures.txt', 'r') as ext_temp_file:
                    for line in ext_temp_file:
                        ext_temp_array.append(float(line))
            except IOError:
                logger.warning("Could not read external temperature history from file", exc_info=True)
            ext_temp_avg = calculate_avg_ext_temperature(ext_temp, ext_temp_array)
            management_data += struct.pack('!2d', ext_temp, ext_temp_avg)
            try:
                with open('ExtTemperatures.txt', 'w') as ext_temp_file:
                    for i in ext_temp_array:
                        ext_temp_file.write('%f\n' % i)
            except IOError:
                logger.warning("Could not re-write external temperature history to file", exc_info=True)
            heating_level = get_ext_temperature_level(ext_temp_avg, cfg['extMaxTemp'], cfg['extMinTemp'],
                                                      cfg['extStartThreshold'])
            logger.debug("Avg external temperature: %f, level: %f", ext_temp_avg, heating_level)
            if day_period == 0:
                logger.debug("Outside of heating period")
            circuit_temp = []
            for index, sensor in enumerate(sensor_map[1:]):
                if ads[sensor_setup[index + 1][0]] is None:
                    initialize_ads(sensor_setup[index + 1][0], i2c, ads, sensor_setup, sensor_map)
                if ads[sensor_setup[index + 1][0]] is not None:
                    try:
                        temp = get_temperature(index + 1, sensor_map)
                    except (IOError, ValueError):
                        logger.error("Could not measure temperature circuit: %d", index, exc_info=True)
                        ads[sensor_setup[index + 1][0]] = None
                        circuits[index][1] = GPIO.LOW
                        GPIO.output(circuits[index][0], GPIO.LOW)
                        continue
                else:
                    circuits[index][1] = GPIO.LOW
                    GPIO.output(circuits[index][0], GPIO.LOW)
                    continue
                circuit_temp.append(temp)
                heat_status_string = ""
                if day_period > 0:
                    heat_status_string = " (" + ("ON" if circuits[index][1] == GPIO.HIGH else "OFF") + ")"
                logger.debug("Temperature in room '%s'(%d): %f%s",
                             cfg['circuits'][index]['description'], index, temp, heat_status_string)
                if temp <= TEMP_MIN or temp >= TEMP_MAX:
                    logger.warning("Temperature in room '%s'(%d): %f outside of reasonable level, check sensor connection!",
                                   cfg['circuits'][index]['description'], index, temp)
                    circuits[index][1] = GPIO.LOW
                    GPIO.output(circuits[index][0], GPIO.LOW)
                    continue
                management_data += struct.pack('c', index.to_bytes(1, byteorder='little'))
                management_data += struct.pack('!d', temp)
                management_data += struct.pack('?', (circuits[index][1] == GPIO.HIGH))
                if day_period == 0:
                    continue
                circuit_mode = CircuitMode.from_name(cfg['circuits'][index]['active'])
                if circuit_mode.value & day_period == 0:
                    log_str = "Circuit %d isn't active"
                    if circuits[index][1] != GPIO.LOW:
                        circuits[index][1] = GPIO.LOW
                        GPIO.output(circuits[index][0], GPIO.LOW)
                        log_str = log_str + ", heating switching off"
                    logger.info(log_str, index)
                    continue
                heating_period, desired_temp = \
                    get_heating_period(heating_level, day_period, temp, cfg['tempBaseLevel'], cfg['circuits'][index], check_decrease(cfg, curr_timestamp))
                if heating_period <= 0 and circuits[index][1] != GPIO.LOW:
                    circuits[index][1] = GPIO.LOW
                    GPIO.output(circuits[index][0], GPIO.LOW)
                    logger.info(
                        "Desired temp reached for circuit %s [%d] = %f" % (cfg['circuits'][index]['description'], index, temp))
                elif heating_period > 0 and heating_period >= time_to_end and circuits[index][1] == GPIO.LOW:
                    circuits[index][1] = GPIO.HIGH
                    GPIO.output(circuits[index][0], GPIO.HIGH)
                    logger.info(
                        "Starting heating circuit %s [%d] = %f, %d to reach %f" % (
                            cfg['circuits'][index]['description'], index, temp, heating_period, desired_temp))
        except MeasurementException:
            pass
        if cfg['managementServer'] is not None:
            management_server_address = cfg['managementServer'].split(':')
            management_interface.sendto(management_data, (management_server_address[0], int(management_server_address[1])))
        time_to_sleep = 60.0 - datetime.datetime.now().timestamp() + curr_timestamp.timestamp()
        if time_to_sleep < 0:
            logger.warning("No time left after cycle: %f" % time_to_sleep)
            time_to_sleep = 0.0
        time.sleep(time_to_sleep)


if __name__ == '__main__':
    main()