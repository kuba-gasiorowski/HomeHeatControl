import unittest
import atexit
import yaml
from unittest.mock import Mock, patch


GPIO = Mock()
ads1115 = Mock()
ads1x15 = Mock()
board = Mock()

orig_import = __import__


def mock_import(name, *args):
    if name == 'RPi.GPIO':
        return GPIO
    elif name == 'adafruit_ads1x15.ads1115':
        return ads1115
    elif name == 'ads1x15':
        return ads1x15
    elif name == 'board':
        return board
    return orig_import(name, *args)


with patch('builtins.__import__', side_effect=mock_import):
    import HomeHeat


class TestHomeHeat(unittest.TestCase):

    def test_calculate_avg_ext_temperature(self):
        hist_temp_array = [0.5, 1.0, 1.5]
        current_temperature = 2.0
        self.assertEqual(HomeHeat.calculate_avg_ext_temperature(current_temperature, hist_temp_array), 1.25)
        self.assertEqual(len(hist_temp_array), 4)
        self.assertEqual(hist_temp_array[3], current_temperature)

    def test_calculate_avg_ext_temperature_full(self):
        hist_temp_array = [0] * 1440
        current_temperature = 9
        self.assertEqual(HomeHeat.calculate_avg_ext_temperature(current_temperature, hist_temp_array), 0.00625)
        self.assertEqual(len(hist_temp_array), 1440)
        self.assertEqual(hist_temp_array[1439], current_temperature)

    def test_get_heating_period_no_adjust(self):
        with open('test/HomeHeat.yml', 'r') as cfg_file:
            cfg = yaml.load(cfg_file, Loader=yaml.FullLoader)
        self.assertEqual(HomeHeat.get_heating_period(0.5, 1, 20.0, 20.0, cfg['circuits'][0]), (5000.0, 25.0))

    def test_get_heating_period_day_adjust(self):
        with open('test/HomeHeat.yml', 'r') as cfg_file:
            cfg = yaml.load(cfg_file, Loader=yaml.FullLoader)
        self.assertEqual(HomeHeat.get_heating_period(0.5, 2, 20.0, 20.0, cfg['circuits'][1]), (4500.0, 24.5))

    def test_get_heating_period_night_adjust(self):
        with open('test/HomeHeat.yml', 'r') as cfg_file:
            cfg = yaml.load(cfg_file, Loader=yaml.FullLoader)
        self.assertEqual(HomeHeat.get_heating_period(0.5, 1, 20.0, 20.0, cfg['circuits'][1]), (5500.0, 25.5))

    def test_get_heating_period_zero_adjust(self):
        with open('test/HomeHeat.yml', 'r') as cfg_file:
            cfg = yaml.load(cfg_file, Loader=yaml.FullLoader)
        self.assertEqual(HomeHeat.get_heating_period(0.5, 1, 20.0, 20.0, cfg['circuits'][2]), (5000.0, 25.0))


if __name__ == '__main__':
    atexit.unregister(HomeHeat.cleanup)
    unittest.main()
