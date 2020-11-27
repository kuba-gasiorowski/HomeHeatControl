package com.sasieczno.homeheat.manager.model;

public class CircuitStatus {
    private int circuitIndex;
    private boolean circuitStatus;
    private boolean heatingOn;
    private String circuitName;
    private double circuitTemperature;

    public int getCircuitIndex() {
        return circuitIndex;
    }

    public void setCircuitIndex(int circuitIndex) {
        this.circuitIndex = circuitIndex;
    }

    public boolean isCircuitStatus() {
        return circuitStatus;
    }

    public void setCircuitStatus(boolean circuitStatus) {
        this.circuitStatus = circuitStatus;
    }

    public boolean isHeatingOn() {
        return heatingOn;
    }

    public void setHeatingOn(boolean heatingOn) {
        this.heatingOn = heatingOn;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public void setCircuitName(String circuitName) {
        this.circuitName = circuitName;
    }

    public double getCircuitTemperature() {
        return circuitTemperature;
    }

    public void setCircuitTemperature(double circuitTemperature) {
        this.circuitTemperature = circuitTemperature;
    }
}
