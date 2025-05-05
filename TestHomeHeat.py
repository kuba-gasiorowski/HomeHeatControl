import unittest
import atexit
import yaml
import datetime
from unittest.mock import Mock, patch


GPIO = Mock()
ads1115 = Mock()
ads1x15 = Mock()
board = Mock()
busio = Mock()
analog_in = Mock()

orig_import = __import__


def mock_import(name, *args):
    if name == 'RPi.GPIO':
        return GPIO
    elif name == 'adafruit_ads1x15.ads1115':
        return ads1115
    elif name == 'adafruit_ads1x15.analog_in':
        return analog_in
    elif name == 'ads1x15':
        return ads1x15
    elif name == 'board':
        return board
    elif name == 'busio':
        return busio
    return orig_import(name, *args)


with patch('builtins.__import__', side_effect=mock_import):
    import HomeHeat


class TestHomeHeat(unittest.TestCase):

    def test_calculate_avg_ext_temperature(self):
        """
        The average external temperature calculation
        """
        hist_temp_array = [0.5, 1.0, 1.5]
        current_temperature = 2.0
        self.assertEqual(HomeHeat.calculate_avg_ext_temperature(current_temperature, hist_temp_array), 1.25)
        self.assertEqual(len(hist_temp_array), 4)
        self.assertEqual(hist_temp_array[3], current_temperature)

    def test_calculate_avg_ext_temperature_full(self):
        """
        The average external temperature calculation with full array
        """
        hist_temp_array = [0] * 1440
        current_temperature = 9
        self.assertEqual(HomeHeat.calculate_avg_ext_temperature(current_temperature, hist_temp_array), 0.00625)
        self.assertEqual(len(hist_temp_array), 1440)
        self.assertEqual(hist_temp_array[1439], current_temperature)

    def test_get_heating_period_no_adjust(self):
        cfg = HomeHeat.open_config_file('test/HomeHeat.yml')
        self.assertEqual(HomeHeat.get_heating_period(0.5, 1, 20.0, 20.0, cfg['circuits'][0]), (5000.0, 25.0))

    def test_get_heating_period_day_adjust(self):
        cfg = HomeHeat.open_config_file('test/HomeHeat.yml')
        self.assertEqual(HomeHeat.get_heating_period(0.5, 2, 20.0, 20.0, cfg['circuits'][1]), (4500.0, 24.5))

    def test_get_heating_period_night_adjust(self):
        cfg = HomeHeat.open_config_file('test/HomeHeat.yml')
        self.assertEqual(HomeHeat.get_heating_period(0.5, 1, 20.0, 20.0, cfg['circuits'][1]), (5500.0, 25.5))

    def test_get_heating_period_zero_adjust(self):
        cfg = HomeHeat.open_config_file('test/HomeHeat.yml')
        self.assertEqual(HomeHeat.get_heating_period(0.5, 1, 20.0, 20.0, cfg['circuits'][2]), (5000.0, 25.0))

    def test_get_heating_period_decrease_temp_1(self):
        cfg = HomeHeat.open_config_file('test/HomeHeat1.yml')
        self.assertEqual(HomeHeat.get_heating_period(0.5, 1, 20.0, 20.0, cfg['circuits'][0], HomeHeat.check_decrease(cfg, datetime.datetime(2024, 2, 11))), (3000.0, 23.0))

    def test_get_heating_period_decrease_temp_2(self):
        cfg = HomeHeat.open_config_file('test/HomeHeat1.yml')
        self.assertEqual(HomeHeat.get_heating_period(0.5, 1, 20.0, 20.0, cfg['circuits'][0], HomeHeat.check_decrease(cfg, datetime.datetime(2024, 1, 21))), (4000.0, 24.0))

    def test_get_heating_period_decrease_temp_outofperiod_no_adjust(self):
        cfg = HomeHeat.open_config_file('test/HomeHeat1.yml')
        self.assertEqual(HomeHeat.get_heating_period(0.5, 1, 20.0, 20.0, cfg['circuits'][0], HomeHeat.check_decrease(cfg, datetime.datetime(2024, 2, 18))), (5000.0, 25.0))

    def test_convert_time(self):
        self.assertEqual(datetime.time(0, 0), HomeHeat.convert_time('00:00'))
        self.assertEqual(datetime.time(0,1), HomeHeat.convert_time('00:01'))
        self.assertEqual(datetime.time(0, 0, 1), HomeHeat.convert_time('00:00:01'))
        self.assertEqual(datetime.time(1,0), HomeHeat.convert_time('01'))
        self.assertRaises(ValueError, lambda: HomeHeat.convert_time('00:00:00:00'))
        self.assertRaises(ValueError, lambda: HomeHeat.convert_time('25:00'))


    def test_convert_date(self):
        self.assertEqual(datetime.datetime(2024, 1, 1, 0, 0), HomeHeat.convert_date('2024-01-01'))
        self.assertEqual(datetime.datetime(1975, 3, 19, 9, 30), HomeHeat.convert_date('1975-03-19 09:30'))
        self.assertEqual(datetime.datetime(2025, 12, 31, 23, 59, 59), HomeHeat.convert_date('2025-12-31 23:59:59'))
        self.assertEqual(datetime.datetime(2025, 12, 31, 23, 0), HomeHeat.convert_date('2025-12-31 23'))
        self.assertRaises(ValueError, lambda: HomeHeat.convert_date('2024-01-01-01'))
        self.assertRaises(ValueError, lambda: HomeHeat.convert_date('2024-13-01'))
        self.assertRaises(ValueError, lambda: HomeHeat.convert_date('2024-01-32'))
        self.assertRaises(ValueError, lambda: HomeHeat.convert_date('2024-01-31 24'))


    def test_get_day_period(self):
        config = {
            'nightStartTime': '22:00:00',
            'nightEndTime': '06:00:00',
            'dayStartTime': '13:00:00',
            'dayEndTime': '16:00:00'
        }
        current_timestamp = datetime.datetime(2023, 10, 10, 12, 0, 0)
        result = HomeHeat.get_day_period(current_timestamp, config)
        self.assertEqual(result, (0, 0, 0))
        current_timestamp = datetime.datetime(2023, 10, 10, 23, 30, 0)
        result = HomeHeat.get_day_period(current_timestamp, config)
        self.assertEqual(result, (1, 23400, 28800))
        current_timestamp = datetime.datetime(2023, 10, 10, 4, 30, 45)
        result = HomeHeat.get_day_period(current_timestamp, config)
        self.assertEqual(result, (1, 5355, 28800))
        current_timestamp = datetime.datetime(2023, 10, 10, 14, 30, 50)
        result = HomeHeat.get_day_period(current_timestamp, config)
        self.assertEqual(result, (2, 5350, 10800))
        current_timestamp = datetime.datetime(2023, 10, 10, 16, 30, 50)
        result = HomeHeat.get_day_period(current_timestamp, config)
        self.assertEqual(result, (0, 0, 0))


    def test_get_temperature_ext_0(self):
        sensor_map_tab = [[analog_in, 14, 5600]]
        analog_in.voltage = 1.64
        self.assertAlmostEqual(HomeHeat.get_temperature(0, sensor_map_tab, 0), 0.25, delta=0.001)


    def test_get_temperature_ext_30(self):
        sensor_map_tab = [[analog_in, 14, 5600]]
        analog_in.voltage = 0.75
        self.assertAlmostEqual(HomeHeat.get_temperature(0, sensor_map_tab, 0), 29.97, delta=0.001)


    def test_get_temperature_ext_m16(self):
        sensor_map_tab = [[analog_in, 14, 5600]]
        analog_in.voltage = 2.25
        self.assertAlmostEqual(HomeHeat.get_temperature(0, sensor_map_tab, 0), -16.304, delta=0.001)


    def test_get_temperature_int_23(self):
        sensor_map_tab = [[analog_in, 14, 2200]]
        analog_in.voltage = 1.64
        self.assertAlmostEqual(HomeHeat.get_temperature(0, sensor_map_tab, 0), 22.701, delta=0.001)


    def test_get_temperature_int_36(self):
        sensor_map_tab = [[analog_in, 14, 2200]]
        analog_in.voltage = 1.23
        self.assertAlmostEqual(HomeHeat.get_temperature(0, sensor_map_tab, 0), 36.26, delta=0.001)


    def test_get_temperature_int_18(self):
        sensor_map_tab = [[analog_in, 14, 2200]]
        analog_in.voltage = 1.84
        self.assertAlmostEqual(HomeHeat.get_temperature(0, sensor_map_tab, 0), 16.561, delta=0.001)


    def test_get_heating_period(self):
        heat_level = 0.5
        day_period = 1
        current_temp = 15.0
        base_level = 10.0
        circuit_data = {
            'maxTemp': 30.0,
            'tempBaseLevel': 20.0,
            'heatCharacteristics': [
                {'tempMax': 20.0, 'heatFactor': 0.5},
                {'tempMax': 25.0, 'heatFactor': 0.2}
            ]
        }
        result, desired_temp = HomeHeat.get_heating_period(heat_level, day_period, current_temp, base_level, circuit_data)
        self.assertAlmostEqual(result, 35)
        self.assertAlmostEqual(desired_temp, 25)

if __name__ == '__main__':
    atexit.unregister(HomeHeat.cleanup)
    unittest.main()
