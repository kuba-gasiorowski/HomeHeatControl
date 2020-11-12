#!/usr/bin/env bash

# Ensure the time is synchronized
until timedatectl status | grep -q ' synchronized: yes'; do sleep 1; done