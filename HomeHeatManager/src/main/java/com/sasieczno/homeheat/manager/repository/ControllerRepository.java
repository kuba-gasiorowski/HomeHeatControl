package com.sasieczno.homeheat.manager.repository;

import com.sasieczno.homeheat.manager.model.ControllerProcessData;

public interface ControllerRepository {
    ControllerProcessData getControllerProcessData();
    void restartService();
}
