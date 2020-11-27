package com.sasieczno.homeheat.manager.model;

public enum HeatingPeriod {
    NO_HEATING(0),
    NIGHT(1),
    DAY(2);
    private int periodId;
    HeatingPeriod(int periodId) { this.periodId = periodId; }
    public static HeatingPeriod fromInt(int periodId) {
        switch (periodId) {
            case 1:
                return NIGHT;
            case 2:
                return DAY;
            default:
                return NO_HEATING;
        }
    }
}
