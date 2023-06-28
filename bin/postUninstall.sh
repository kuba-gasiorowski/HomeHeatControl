#!/usr/bin/env bash

systemctl daemon-reload

if [ -d /var/log/homeheat ]
then
  echo "============================================================================="
  echo " Folder /var/log/homeheat remains. It may contain old log and working files."
  echo " Please check the content and remove manually, if not needed anymore."
  echo "============================================================================="
fi