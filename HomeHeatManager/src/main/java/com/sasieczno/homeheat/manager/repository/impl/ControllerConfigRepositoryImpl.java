package com.sasieczno.homeheat.manager.repository.impl;

import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.ControllerConfig;
import com.sasieczno.homeheat.manager.repository.ControllerConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Repository("ControllerConfigRepository")
public class ControllerConfigRepositoryImpl implements ControllerConfigRepository {

    public static final Logger LOGGER = LoggerFactory.getLogger(ControllerConfigRepositoryImpl.class);

    @Autowired
    AppConfig appConfig;

    @Autowired
    JacksonConfiguration jacksonConfiguration;

    @Override
    public void updateCircuitConfig(int circuitId, boolean active, float dayAdjust, float nightAdjust) {
        ControllerConfig config = getConfig();
        Iterator<ControllerConfig.Circuit> it = config.getCircuits().iterator();
        while (it.hasNext()) {
            ControllerConfig.Circuit circuit = it.next();
            if (circuit.getIndex() == circuitId) {
                LOGGER.debug("updateCircuitConfig: id={}, active={}, dayAdjust={}, nightAdjust={}",
                        circuitId, active, dayAdjust, nightAdjust);
                circuit.setActive(active);
                circuit.setDayAdjust(dayAdjust);
                circuit.setNightAdjust(nightAdjust);
                try {
                    jacksonConfiguration.getObjectMapper().writeValue(new File(appConfig.controllerConfigFile), config);
                } catch (IOException e) {
                    LOGGER.warn("updateCircuitConfig: could not save the file", e);
                }
                break;
            }
        }


    }

    @Override
    public ControllerConfig getConfig() {
        ControllerConfig controllerConfig = null;
        try {
            controllerConfig = jacksonConfiguration.getObjectMapper().readValue(new File(appConfig.controllerConfigFile), ControllerConfig.class);
        } catch (IOException e) {
            LOGGER.warn("getConfig: Could not read the controller config file", e);
        } finally {
            return controllerConfig;
        }
    }
}
