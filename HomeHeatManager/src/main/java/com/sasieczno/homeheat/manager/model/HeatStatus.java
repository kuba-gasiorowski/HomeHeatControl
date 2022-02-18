package com.sasieczno.homeheat.manager.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Calendar;
import java.util.LinkedList;

@NoArgsConstructor
@Setter
@Getter
public class HeatStatus {
    private boolean controllerStatus;
    private Calendar lastStatusChangeTime;

    private Calendar lastMessageTime;
    private HeatingPeriod heatingPeriod;
    private double externalTemperature;
    private double avgExternalTemperature;

    LinkedList<CircuitStatus> circuitStatuses;
}
