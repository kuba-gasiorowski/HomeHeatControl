package com.sasieczno.homeheat.manager.model;


import java.util.Calendar;
import java.util.LinkedList;

public class HeatStatus {
    private boolean controllerStatus;
    private Calendar lastStatusChangeTime;

    private Calendar lastMessageTime;
    private HeatingPeriod heatingPeriod;
    private double externalTemperature;
    private double avgExternalTemperature;

    LinkedList<CircuitStatus> circuitStatuses;

    public boolean isControllerStatus() {
        return controllerStatus;
    }

    public void setControllerStatus(boolean controllerStatus) {
        this.controllerStatus = controllerStatus;
    }

    public Calendar getLastStatusChangeTime() {
        return lastStatusChangeTime;
    }

    public void setLastStatusChangeTime(Calendar lastStatusChangeTime) {
        this.lastStatusChangeTime = lastStatusChangeTime;
    }

    public Calendar getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Calendar lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public HeatingPeriod getHeatingPeriod() {
        return heatingPeriod;
    }

    public void setHeatingPeriod(HeatingPeriod heatingPeriod) {
        this.heatingPeriod = heatingPeriod;
    }

    public double getExternalTemperature() {
        return externalTemperature;
    }

    public void setExternalTemperature(double externalTemperature) {
        this.externalTemperature = externalTemperature;
    }

    public double getAvgExternalTemperature() {
        return avgExternalTemperature;
    }

    public void setAvgExternalTemperature(double avgExternalTemperature) {
        this.avgExternalTemperature = avgExternalTemperature;
    }

    public LinkedList<CircuitStatus> getCircuitStatuses() {
        return circuitStatuses;
    }

    public void setCircuitStatuses(LinkedList<CircuitStatus> circuitStatuses) {
        this.circuitStatuses = circuitStatuses;
    }
}
