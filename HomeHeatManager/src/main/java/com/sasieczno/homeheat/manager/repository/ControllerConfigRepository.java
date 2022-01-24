package com.sasieczno.homeheat.manager.repository;

import com.sasieczno.homeheat.manager.model.ControllerConfig;

public interface ControllerConfigRepository {
    boolean updateCircuitConfig(ControllerConfig.Circuit circuit);
    ControllerConfig getConfig();
    boolean updateConfig(ControllerConfig controllerConfig);
}
