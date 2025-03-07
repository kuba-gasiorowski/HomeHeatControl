#!/usr/bin/env bash

usage() {
  echo "Usage: $0 [-m <m2_home>] [-g <goal>]" 1>&2
  exit 1
}

while getopts ":m:g:" o; do
  case "${o}" in
    m)
      m2_home=${OPTARG}
      ;;
    g)
      goal=${OPTARG}
      ;;
    *)
      usage
      ;;
  esac
done

if [ -z "${goal}" ]; then
  goal="build"
fi

if [ -n "${m2_home}" ]; then
  M2_HOME_OPT="-v ${m2_home}:/m2_home -e M2_HOME=/m2_home"
fi

PROJECT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )/.." > /dev/null 2>&1 && pwd )
docker run --rm -v ${PROJECT_DIR}:/app ${M2_HOME_OPT} kubagasiorowski/homeheat-build:1.0 /bin/bash -c "cd /app && ./gradlew ${goal}"
