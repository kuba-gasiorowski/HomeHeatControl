package com.sasieczno.homeheat.manager.model;

import java.time.LocalTime;
import java.util.List;

public class ControllerConfig {
    double extMinTemp;
    double extMaxTemp;
    double extStartThreshold;
    double tempBaseLevel;
    LocalTime nightStartTime;
    LocalTime nightEndTime;
    LocalTime dayStartTime;
    LocalTime dayEndTime;
    String logLevel;
    String managementServer;
    List<Circuit> circuits;

    public double getExtMinTemp() {
        return extMinTemp;
    }

    public void setExtMinTemp(double extMinTemp) {
        this.extMinTemp = extMinTemp;
    }

    public double getExtMaxTemp() {
        return extMaxTemp;
    }

    public void setExtMaxTemp(double extMaxTemp) {
        this.extMaxTemp = extMaxTemp;
    }

    public double getExtStartThreshold() {
        return extStartThreshold;
    }

    public void setExtStartThreshold(double extStartThreshold) {
        this.extStartThreshold = extStartThreshold;
    }

    public double getTempBaseLevel() {
        return tempBaseLevel;
    }

    public void setTempBaseLevel(double tempBaseLevel) {
        this.tempBaseLevel = tempBaseLevel;
    }

    public LocalTime getNightStartTime() {
        return nightStartTime;
    }

    public void setNightStartTime(LocalTime nightStartTime) {
        this.nightStartTime = nightStartTime;
    }

    public LocalTime getNightEndTime() {
        return nightEndTime;
    }

    public void setNightEndTime(LocalTime nightEndTime) {
        this.nightEndTime = nightEndTime;
    }

    public LocalTime getDayStartTime() {
        return dayStartTime;
    }

    public void setDayStartTime(LocalTime dayStartTime) {
        this.dayStartTime = dayStartTime;
    }

    public LocalTime getDayEndTime() {
        return dayEndTime;
    }

    public void setDayEndTime(LocalTime dayEndTime) {
        this.dayEndTime = dayEndTime;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getManagementServer() {
        return managementServer;
    }

    public void setManagementServer(String managementServer) {
        this.managementServer = managementServer;
    }

    public List<Circuit> getCircuits() {
        return circuits;
    }

    public void setCircuits(List<Circuit> circuits) {
        this.circuits = circuits;
    }

    public static class Circuit {
        int index;
        String description;
        boolean active;
        double maxTemp;
        double tempBaseLevel;
        double nightAdjust;
        double dayAdjust;
        List<HeatCharacteristics> heatCharacteristics;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public double getMaxTemp() {
            return maxTemp;
        }

        public void setMaxTemp(double maxTemp) {
            this.maxTemp = maxTemp;
        }

        public double getTempBaseLevel() {
            return tempBaseLevel;
        }

        public void setTempBaseLevel(double tempBaseLevel) {
            this.tempBaseLevel = tempBaseLevel;
        }

        public double getNightAdjust() {
            return nightAdjust;
        }

        public void setNightAdjust(double nightAdjust) {
            this.nightAdjust = nightAdjust;
        }

        public double getDayAdjust() {
            return dayAdjust;
        }

        public void setDayAdjust(double dayAdjust) {
            this.dayAdjust = dayAdjust;
        }

        public List<HeatCharacteristics> getHeatCharacteristics() {
            return heatCharacteristics;
        }

        public void setHeatCharacteristics(List<HeatCharacteristics> heatCharacteristics) {
            this.heatCharacteristics = heatCharacteristics;
        }
    }
    public static class HeatCharacteristics {
        double tempMax;
        double heatFactor;

        public double getTempMax() {
            return tempMax;
        }

        public void setTempMax(double tempMax) {
            this.tempMax = tempMax;
        }

        public double getHeatFactor() {
            return heatFactor;
        }

        public void setHeatFactor(double heatFactor) {
            this.heatFactor = heatFactor;
        }
    }
}
