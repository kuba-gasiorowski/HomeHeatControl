package com.sasieczno.homeheat.manager.repository;

import com.sasieczno.homeheat.manager.model.ControllerConfig;

public interface ControllerConfigRepository {
    void updateCircuitConfig(int circuitId, boolean active, float dayAdjust, float nightAdjust);
    ControllerConfig getConfig();
}
