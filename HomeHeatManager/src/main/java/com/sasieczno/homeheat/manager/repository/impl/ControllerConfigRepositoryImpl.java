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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
                    saveConfig(config);
                } catch (IOException e) {
                    LOGGER.warn("updateCircuitConfig: could not save the file", e);
                }
                break;
            }
        }
    }

    @Override
    public boolean updateCircuitConfig(ControllerConfig.Circuit circuit) {
        boolean result = false;
        try {
            File controllerConfigFile = new File(appConfig.controllerConfigFile);
            ControllerConfig controllerConfig = jacksonConfiguration.getHeatControllerObjectMapper().readValue(controllerConfigFile, ControllerConfig.class);
            if (controllerConfig != null && controllerConfig.getCircuits() != null) {
                ControllerConfig.Circuit updatedCircuit = null;
                for (int i = 0; i <= controllerConfig.getCircuits().size(); i++) {
                    if (controllerConfig.getCircuits().get(i).getIndex() == circuit.getIndex()) {
                        LOGGER.debug("updateCircuitConfig: circuit id={)", circuit.getIndex());
                        result = true;
                        controllerConfig.getCircuits().set(i, circuit);
                        saveConfig(controllerConfig);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("getConfig: Could not read/write the controller config file", e);
        }
        return result;
    }

    @Override
    public ControllerConfig getConfig() {
        ControllerConfig controllerConfig = null;
        try {
            controllerConfig = jacksonConfiguration.getHeatControllerObjectMapper().readValue(new File(appConfig.controllerConfigFile), ControllerConfig.class);
        } catch (IOException e) {
            LOGGER.warn("getConfig: Could not read the controller config file", e);
        } finally {
            return controllerConfig;
        }
    }

    void saveConfig(ControllerConfig controllerConfig) throws IOException {
        File controllerConfigFile = new File(appConfig.controllerConfigFile);
        Path controllerConfigPath = controllerConfigFile.toPath();
        Path tmpConfigPath = java.nio.file.Files.createTempFile(appConfig.controllerConfigFile, "tmp");
        jacksonConfiguration.getHeatControllerObjectMapper().writeValue(tmpConfigPath.toFile(), controllerConfig);
        Files.move(tmpConfigPath, controllerConfigPath, StandardCopyOption.ATOMIC_MOVE);
    }
}
