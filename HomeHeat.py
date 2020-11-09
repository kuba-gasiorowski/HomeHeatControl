#/usr/bin/env python3

import atexit
import math
import yaml
import RPi.GPIO as GPIO
import time
import board
import busio
import adafruit_ads1x15.ads1115 as ADS
from adafruit_ads1x15.analog_in import AnalogIn
import datetime
import logging
import logging.config

# Reference voltage for thermistor resistance measurements
REF_VOLTAGE=3.3
# Factors for Steinhart-Hart equation
NTC_A_FACTOR=0.0012797372847331715
NTC_B_FACTOR=0.00026335882652276787
NTC_C_FACTOR=1.6897560208446226e-7

DAY_SECONDS=86400

TEMP_MIN=-20.0
TEMP_MIN_EXT=-50.0

TEMP_MAX=100.0
TEMP_MAX_EXT=60.0

def get_temperature(sensor_input, sensor_map_tab):
    """
    Gets the temperature reported by the sensor. Using
    Steinhart-Hart equation to convert the resistance of the
    thermistor to the temperature value (in celsius grades)
    :param: sensor_input: the sensor id
    :param: sensor_map_tab: The sensor table each with ADS channel, GPIO pin to provide supply for measurement, the
        reference resistor value
    :return: the temperature in celsius grades (float)
    """
    sensor_obj = sensor_map_tab[sensor_input]
    GPIO.output(sensor_obj[1], GPIO.HIGH)
    time.sleep(2)
    v0_0 = sensor_obj[0].voltage
    v0_1 = sensor_obj[0].voltage
    v0_2 = sensor_obj[0].voltage
    v0 = (v0_0 + v0_1 + v0_2)/3
    GPIO.output(sensor_obj[1], GPIO.LOW)
    r1 = sensor_obj[2]
    if v0 == REF_VOLTAGE:
        v0 = REF_VOLTAGE - 1e-4
    thermistor_resistance = (r1*v0)/(REF_VOLTAGE - v0)
    log_tr = math.log(thermistor_resistance)
    return (1.0 / (NTC_A_FACTOR + NTC_B_FACTOR * log_tr + NTC_C_FACTOR * log_tr * log_tr * log_tr)) - 273.15


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
    return math.fsum(hist_temp_array)/len(hist_temp_array)

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
    return ((ext_max_temp - ext_start_threshold*ext_min_temp)/(1 - ext_start_threshold) - avg_temp)/((ext_max_temp - ext_start_threshold*ext_min_temp)/(1 - ext_start_threshold) - ext_min_temp)

def get_heating_period(heat_level, current_temp, max_temp, base_level, heat_characteristics):
    """
    Calculates the length in seconds to heat the specific circuit
    :param heat_level: The heat level (0-1) from the external temperature
    :param current_temp: The current circuit temperature
    :param max_temp: The max temperature for circuit (to be reached for heat_level=1)
    :param base_level: The base temperature for circuit (to be reached for heat_level=0)
    :param heat_characteristics: The heating characteristics of the circuit - table of dictionary elements
         each with 'tempMax' and 'heatFactor' defining the heating function in the specific segment
    :return: Heating length in seconds to reach desired temperature, desired temperature
    """
    desired_temp = base_level + (max_temp - base_level) * heat_level
    diff = desired_temp - current_temp
    if diff <= 0:
        return 0, 0.0
    heat_period = 0
    n = len(heat_characteristics) - 2
    while n >= 0:
        elem_a = heat_characteristics[n]
        elem_b = heat_characteristics[n+1]
        temp_max_a = elem_a['tempMax']
        temp_max_b = elem_b['tempMax']
        heat_factor = elem_b['heatFactor']
        if desired_temp >= temp_max_a:
            tb = min(desired_temp, temp_max_b)
            ta = max(current_temp, temp_max_a)
            heat_period += (tb - ta) / heat_factor
        n -= 1
    temp_max_b = heat_characteristics[0]['tempMax']
    heat_factor = heat_characteristics[0]['heatFactor']
    if current_temp < heat_characteristics[0]['tempMax']:
        heat_period += (temp_max_b - current_temp) / heat_factor
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
    return ts.second + 60*ts.minute + 3600*ts.hour

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
    :param: circuit_table: The table with circuits containing table element with gpio PIN and its state
    """
    for elem in circuit_table:
        GPIO.output(elem[0], GPIO.LOW)
        elem[1] = GPIO.LOW
        time.sleep(1.0)

@atexit.register
def cleanup():
    reset_circuits(circuits)
    GPIO.cleanup()

with open('config/HomeHeat_logging.yml','r') as f:
    logging_config = yaml.load(f, Loader=yaml.FullLoader)
    logging.config.dictConfig(logging_config)
logger = logging.getLogger()

GPIO.setmode(GPIO.BCM)
GPIO.setup([14, 15, 4, 17, 11, 8, 7, 9, 10, 23, 22], GPIO.OUT)
gpio_circuit = [26, 19, 5, 6, 13, 25, 12, 16, 20, 21]
GPIO.setup(gpio_circuit, GPIO.OUT)

i2c = busio.I2C(board.SCL, board.SDA)
time.sleep(1.0)
ads0 = ADS.ADS1115(i2c, address=0x48)
ads1 = ADS.ADS1115(i2c, address=0x49)
ads2 = ADS.ADS1115(i2c, address=0x4A)

"""
  Sensor monitoring the room - assignment to ADC1115 module/input
  The values are: ads channel instance, related output on this ads, gpio to provide
  the voltage for resistance measurement, the resistor value in ohm
"""
sensor_map = [
    [AnalogIn(ads0, ADS.P3), 14, 5600.0], # index 0 is external temperature sensor! -> uses PIN TxD
    [AnalogIn(ads0, ADS.P2), 15, 2200.0], # -> uses PIN RxD
    [AnalogIn(ads0, ADS.P1), 4, 2200.0], # -> uses PIN GP4
    [AnalogIn(ads0, ADS.P0), 17, 2200.0], # -> uses PIN GP17
    [AnalogIn(ads2, ADS.P3), 11, 2200.0], # -> uses PIN SCLK
    [AnalogIn(ads2, ADS.P2), 8, 2200.0], # -> uses PIN CE0
    [AnalogIn(ads2, ADS.P1), 7, 2200.0], # -> uses PIN CE1
    # ads2, P0 -> not used!
    [AnalogIn(ads1, ADS.P3), 9, 2200.0], # -> uses PIN MISO
    [AnalogIn(ads1, ADS.P2), 10, 2200.0], # -> uses PIN MOSI
    [AnalogIn(ads1, ADS.P1), 23, 2200.0], # -> uses PIN GP23
    [AnalogIn(ads1, ADS.P0), 22, 2200.0] # -> uses PIN GP22
]

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

cfg = []
curr_day_period = 0
while True:
    curr_timestamp = datetime.datetime.now()
    with open('config/HomeHeat.yml', 'r') as cfg_file:
        cfg = yaml.load(cfg_file, Loader=yaml.FullLoader)
    logger.setLevel(cfg['logLevel'])
    day_period, time_to_end, period_length = get_day_period(curr_timestamp, cfg)
    if day_period == 0:
        if curr_day_period > 0:
            logger.info("Heating period finished %d", curr_day_period)
            reset_circuits(circuits)
    curr_day_period = day_period
    ext_temp = get_temperature(0, sensor_map)
    logger.info("External temperature: %f", ext_temp)
    ext_temp_array = []
    try:
        with open('config/ExtTemperatures.txt','r') as ext_temp_file:
            for line in ext_temp_file:
                ext_temp_array.append(float(line))
    except Exception:
        pass
    ext_temp_avg = calculate_avg_ext_temperature(ext_temp, ext_temp_array)
    if ext_temp <= TEMP_MIN_EXT or ext_temp >= TEMP_MAX_EXT:
        logger.warning("External temperature: %f outside of reasonable level, check sensor connection!!!", ext_temp)
        reset_circuits(circuits)
        continue
    try:
        with open('config/ExtTemperatures.txt','w') as ext_temp_file:
            for i in ext_temp_array:
                ext_temp_file.write('%f\n' % i)
    except Exception:
        pass
    heating_level = get_ext_temperature_level(ext_temp_avg, cfg['extMaxTemp'], cfg['extMinTemp'], cfg['extStartThreshold'])
    logger.debug("Avg external temperature: %f, level: %f", ext_temp_avg, heating_level)
    if day_period == 0:
        logger.debug("Outside of heating period")
    circuit_temp = []
    for index, sensor in enumerate(sensor_map[1:]):
        temp = get_temperature(index+1, sensor_map)
        circuit_temp.append(temp)
        heat_status_string = ""
        if day_period > 0:
            heat_status_string = " (" + ("ON" if circuits[index][1] == GPIO.HIGH else "OFF") + ")"
        logger.debug("Temperature in room '%s'(%d): %f%s",
                    cfg['circuits'][index]['description'], index, temp, heat_status_string)
        if temp <= TEMP_MIN or temp >= TEMP_MAX:
            logger.warning("Temperature in room '%s'(%d): %f outside of reasonable level, check sensor connection!!!",
                           cfg['circuits'][index]['description'], index, temp)
            circuits[index][1] = GPIO.LOW
            GPIO.output(circuits[index][0], GPIO.LOW)
            continue
        if day_period == 0:
            continue
        if not cfg['circuits'][index]['active']:
            log_str = "Circuit %d isn't active"
            if circuits[index][1] != GPIO.LOW:
                circuits[index][1] = GPIO.LOW
                GPIO.output(circuits[index][0], GPIO.LOW)
                log_str = log_str + ", heating switching off"
            logger.info(log_str, index)
            continue
        heating_period, desired_temp =\
            get_heating_period(heating_level, temp,
                               cfg['circuits'][index]['maxTemp'],
                               cfg['circuits'][index]['tempBaseLevel'],
                               cfg['circuits'][index]['heatCharacteristics'])
        if heating_period <= 0 and circuits[index][1] != GPIO.LOW:
            circuits[index][1] = GPIO.LOW
            GPIO.output(circuits[index][0], GPIO.LOW)
            logger.info("Desired temp reached for circuit %s [%d] = %f" % (cfg['circuits'][index]['description'], index, temp))
        elif heating_period > 0 and heating_period >= time_to_end and circuits[index][1] == GPIO.LOW:
            circuits[index][1] = GPIO.HIGH
            GPIO.output(circuits[index][0], GPIO.HIGH)
            logger.info(
                "Starting heating circuit %s [%d] = %f, %d to reach %f" % (cfg['circuits'][index]['description'], index, temp, heating_period, desired_temp))
    time_to_sleep = 60.0 - datetime.datetime.now().timestamp() + curr_timestamp.timestamp()
    if time_to_sleep < 0:
        logger.warning("No time left after cycle: %f" % time_to_sleep)
        time_to_sleep = 0.0
    time.sleep(time_to_sleep)