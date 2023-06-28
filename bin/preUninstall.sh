#!/usr/bin/env bash

systemctl stop HomeHeat.service
systemctl stop HomeHeatManager.service
systemctl disable HomeHeat.service
systemctl disable HomeHeatManager.service
