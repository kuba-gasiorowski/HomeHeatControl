package com.sasieczno.homeheat.manager.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
public class ControllerConfig {
    Double extMinTemp;
    Double extMaxTemp;
    Double extStartThreshold;
    Double tempBaseLevel;
    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime nightStartTime;
    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime nightEndTime;
    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime dayStartTime;
    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime dayEndTime;
    ArrayList<OffHomeElement> offHome;
    String logConfig;
    String logLevel;
    String managementServer;
    ArrayList<Circuit> circuits;

    public void copy(ControllerConfig cfg) {
        if (cfg.getExtMinTemp() != null)
            extMinTemp = cfg.getExtMinTemp();
        if (cfg.getExtMaxTemp() != null)
            extMaxTemp = cfg.getExtMaxTemp();
        if (cfg.getExtStartThreshold() != null)
            extStartThreshold = cfg.getExtStartThreshold();
        if (cfg.getTempBaseLevel() != null)
            tempBaseLevel = cfg.getTempBaseLevel();
        if (cfg.getNightStartTime() != null)
            nightStartTime = cfg.getNightStartTime();
        if (cfg.getNightEndTime() != null)
            nightEndTime = cfg.getNightEndTime();
        if (cfg.getDayStartTime() != null)
            dayStartTime = cfg.getDayStartTime();
        if (cfg.getDayEndTime() != null)
            dayEndTime = cfg.getDayEndTime();
        if (cfg.getOffHome() != null) {
            offHome = new ArrayList<>();
            offHome.addAll(cfg.getOffHome());
        }
        if (cfg.getLogConfig() != null)
            logConfig = cfg.getLogConfig();
        if (cfg.getLogLevel() != null)
            logLevel = cfg.getLogLevel();
        if (cfg.getManagementServer() != null)
            managementServer = cfg.getManagementServer();
        if (cfg.getCircuits() != null) {
            for (Circuit circuit: cfg.getCircuits()) {
                for (Circuit dstCircuit: circuits) {
                    if (dstCircuit.getIndex() == circuit.getIndex())
                        dstCircuit.copy(circuit);
                }
            }
        }
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public static class OffHomeElement {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime decreaseFrom;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime decreaseTo;
        Double decreaseTemp;

        public void copy(OffHomeElement src) {
            if (src.getDecreaseFrom() != null)
                decreaseFrom = src.getDecreaseFrom();
            if (src.getDecreaseTo() != null)
                decreaseTo = src.getDecreaseTo();
            if (src.getDecreaseTemp() != null)
                decreaseTemp = src.getDecreaseTemp();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Circuit {
        int index;
        String description;
        CircuitMode active;
        Double maxTemp;
        Double tempBaseLevel;
        Double nightAdjust;
        Double dayAdjust;
        ArrayList<HeatCharacteristics> heatCharacteristics;

        public void copy(Circuit src) {
            if (src.getIndex() != index)
                return;
            if (src.getDescription() != null)
                description = src.getDescription();
            if (src.getActive() != null)
                active = src.getActive();
            if (src.getMaxTemp() != null)
                maxTemp = src.getMaxTemp();
            if (src.getTempBaseLevel() != null)
                tempBaseLevel = src.getTempBaseLevel();
            if (src.getNightAdjust() != null)
                nightAdjust = src.getNightAdjust();
            if (src.getDayAdjust() != null)
                dayAdjust = src.getDayAdjust();
            if (src.getHeatCharacteristics() != null) {
                heatCharacteristics = new ArrayList<>();
                heatCharacteristics.addAll(src.getHeatCharacteristics());
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class HeatCharacteristics {
        Double tempMax;
        Double heatFactor;

    }
}
