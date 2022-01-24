package com.sasieczno.homeheat.manager.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Calendar;
import java.util.LinkedList;

/**
 * The class provides information delivered via management interface
 * by the heat controller process.
 */
@Getter
@Setter
@NoArgsConstructor
public class HeatingData {
    Calendar lastMessageTime;
    HeatingPeriod heatingPeriod;
    double externalTemperature;
    double averageExternalTemperature;

    LinkedList<CircuitData> circuits;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CircuitData {
        int index;
        double temperature;
        boolean heating;
    }
}
