package com.sasieczno.homeheat.manager.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    LocalTime nightStartTime;
    LocalTime nightEndTime;
    LocalTime dayStartTime;
    LocalTime dayEndTime;
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

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Circuit {
        int index;
        String description;
        Boolean active;
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
                for (HeatCharacteristics hc : src.getHeatCharacteristics()) {
                    heatCharacteristics.add(hc);
                }
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class HeatCharacteristics {
        double tempMax;
        double heatFactor;

    }
}
