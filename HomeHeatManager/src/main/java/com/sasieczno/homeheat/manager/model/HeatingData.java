package com.sasieczno.homeheat.manager.model;

import java.util.Calendar;
import java.util.LinkedList;

/**
 * The class provides information delivered via management interface
 * by the heat controller process.
 */
public class HeatingData {
    Calendar lastMessageTime;
    HeatingPeriod heatingPeriod;
    double externalTemperature;
    double averageExternalTemperature;

    LinkedList<CircuitData> circuits;

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

    public double getAverageExternalTemperature() {
        return averageExternalTemperature;
    }

    public void setAverageExternalTemperature(double averageExternalTemperature) {
        this.averageExternalTemperature = averageExternalTemperature;
    }

    public LinkedList<CircuitData> getCircuits() {
        return circuits;
    }

    public void setCircuits(LinkedList<CircuitData> circuits) {
        this.circuits = circuits;
    }


    public static class CircuitData {
        int index;
        double temperature;
        boolean heating;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public boolean isHeating() {
            return heating;
        }

        public void setHeating(boolean heating) {
            this.heating = heating;
        }
    }
}
