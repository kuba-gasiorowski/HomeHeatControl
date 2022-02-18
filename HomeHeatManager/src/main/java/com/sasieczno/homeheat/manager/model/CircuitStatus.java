package com.sasieczno.homeheat.manager.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class CircuitStatus {
    private int circuitIndex;
    private boolean circuitStatus;
    private boolean heatingOn;
    private String circuitName;
    private double circuitTemperature;
}
