package com.sasieczno.homeheat.manager.repository;

import com.sasieczno.homeheat.manager.model.ControllerProcessData;

/**
 * The heating controller process repository object.
 */
public interface ControllerRepository {

    /**
     * Provides the heating controller process details.
     * @return
     */
    ControllerProcessData getControllerProcessData();

    /**
     * Restart the heating controller process.
     */
    void restartService();
}
