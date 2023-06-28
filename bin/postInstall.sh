#!/usr/bin/env bash

mkdir -p /var/log/homeheat

systemctl daemon-reload
systemctl enable HomeHeat.service
systemctl enable HomeHeatManager.service
systemctl start HomeHeat.service
systemctl start HomeHeatManager.service
