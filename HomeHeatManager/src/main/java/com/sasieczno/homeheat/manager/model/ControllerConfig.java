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
                for (HeatCharacteristics hc : heatCharacteristics) {
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
