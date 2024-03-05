package com.sasieczno.homeheat.manager.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedList;

@NoArgsConstructor
@Setter
@Getter
public class HeatStatus {
    private boolean controllerStatus;
    @JsonFormat(without = {JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS})
    private Instant lastStatusChangeTime;

    @JsonFormat(without = {JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS})
    private Instant lastMessageTime;
    private HeatingPeriod heatingPeriod;
    private double externalTemperature;
    private double avgExternalTemperature;

    private LinkedList<CircuitStatus> circuitStatuses;
}
