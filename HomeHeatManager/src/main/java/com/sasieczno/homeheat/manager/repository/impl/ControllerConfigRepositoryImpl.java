package com.sasieczno.homeheat.manager.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.ControllerConfig;
import com.sasieczno.homeheat.manager.repository.ControllerConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The heating controller configuration - YAML file read and modification.
 */
@Slf4j
@Repository("ControllerConfigRepository")
public class ControllerConfigRepositoryImpl implements ControllerConfigRepository {

    @Autowired
    AppConfig appConfig;

    @Qualifier("yamlConfigObjectMapper")
    @Autowired
    ObjectMapper objectMapper;

    /**
     * Updates the specific heating circuit configuration.
     * @param circuit The heating circuit configuration data.
     * @return True if the circuit is identified (by the index) and updated, false otherwise.
     */
    @Override
    public boolean updateCircuitConfig(ControllerConfig.Circuit circuit) {
        boolean result = false;
        try {
            File controllerConfigFile = new File(appConfig.controllerConfigFile);
            ControllerConfig controllerConfig = objectMapper.readValue(controllerConfigFile, ControllerConfig.class);
            if (controllerConfig != null && controllerConfig.getCircuits() != null) {
                ControllerConfig.Circuit updatedCircuit = null;
                for (int i = 0; i <= controllerConfig.getCircuits().size(); i++) {
                    if (controllerConfig.getCircuits().get(i).getIndex() == circuit.getIndex()) {
                        updatedCircuit = controllerConfig.getCircuits().get(i);
                        log.debug("updateCircuitConfig: circuit id={}", circuit.getIndex());
                        updatedCircuit.copy(circuit);
                        updateConfig(controllerConfig);
                        result = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("getConfig: Could not read/write the controller config file", e);
        }
        return result;
    }

    @Override
    public ControllerConfig getConfig() {
        ControllerConfig controllerConfig = null;
        try {
            controllerConfig = objectMapper.readValue(new File(appConfig.controllerConfigFile), ControllerConfig.class);
        } catch (IOException e) {
            log.warn("getConfig: Could not read the controller config file", e);
        } finally {
            return controllerConfig;
        }
    }

    @Override
    public boolean updateConfig(ControllerConfig controllerConfig) {
        File controllerConfigFile = new File(appConfig.controllerConfigFile);
        Path controllerConfigPath = controllerConfigFile.toPath();
        Path tmpConfigPath = null;
        try {
            ControllerConfig config = objectMapper.readValue(controllerConfigFile, ControllerConfig.class);
            config.copy(controllerConfig);
            tmpConfigPath = java.nio.file.Files.createTempFile(controllerConfigPath.getFileName().toString(), ".tmp");
            objectMapper.writeValue(tmpConfigPath.toFile(), config);
            try {
                Files.move(tmpConfigPath, controllerConfigPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                log.info("Atomic move not supported: {}, falling back to regular move", e.getMessage());
                Files.move(tmpConfigPath, controllerConfigPath, StandardCopyOption.REPLACE_EXISTING);
            }
            tmpConfigPath = null;
            return true;
        } catch (IOException e) {
            log.error("Could not update config file", e);
            return false;
        } finally {
            try {
                if (tmpConfigPath != null)
                    Files.delete(tmpConfigPath);
            } catch (Exception e) {
                log.warn("Could not delete temporary file: ", e);
            }
        }
    }

}
