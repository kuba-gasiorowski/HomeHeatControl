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

@Repository("ControllerConfigRepository")
public class ControllerConfigRepositoryImpl implements ControllerConfigRepository {

    public static final Logger LOGGER = LoggerFactory.getLogger(ControllerConfigRepositoryImpl.class);

    @Autowired
    AppConfig appConfig;

    @Autowired
    JacksonConfiguration jacksonConfiguration;

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
                        updatedCircuit = controllerConfig.getCircuits().get(i);
                        LOGGER.debug("updateCircuitConfig: circuit id={)", circuit.getIndex());
                        result = true;
                        updatedCircuit.copy(circuit);
                        updateConfig(controllerConfig);
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

    @Override
    public boolean updateConfig(ControllerConfig controllerConfig) {
        File controllerConfigFile = new File(appConfig.controllerConfigFile);
        Path controllerConfigPath = controllerConfigFile.toPath();
        try {
            Path tmpConfigPath = java.nio.file.Files.createTempFile(controllerConfigPath.getFileName().toString(), ".tmp");
            jacksonConfiguration.getHeatControllerObjectMapper().writeValue(tmpConfigPath.toFile(), controllerConfig);
            Files.move(tmpConfigPath, controllerConfigPath, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not update config file", e);
            return false;
        }
    }
}
