#!/usr/bin/env bash

### Check dependent python packages

check_python_dependencies() {
  fault=0
  for py_package
  do
    if ! pip3 show "${py_package}"
    then
      echo "Python dependencies problem found, install ${py_package} (pip3 install ${py_package})" >&2
      fault=1
    fi
  done
  if [ ${fault} -eq 1 ]
  then
    exit 1
  fi
}

check_python_dependencies RPi.GPIO PyYAML adafruit-circuitpython-ads1x15
