package com.sasieczno.homeheat.manager.controller;

import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.CircuitStatus;
import com.sasieczno.homeheat.manager.model.ControllerConfig;
import com.sasieczno.homeheat.manager.model.ControllerProcessData;
import com.sasieczno.homeheat.manager.model.HeatStatus;
import com.sasieczno.homeheat.manager.model.HeatingData;
import com.sasieczno.homeheat.manager.repository.ControllerConfigRepository;
import com.sasieczno.homeheat.manager.repository.ControllerRepository;
import com.sasieczno.homeheat.manager.service.ControllerStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Calendar;
import java.util.LinkedList;

@RestController
public class ManagementController {
    public static final Logger LOGGER = LoggerFactory.getLogger(ManagementController.class);

    @Autowired
    ControllerRepository controllerRepository;

    @Autowired
    ControllerConfigRepository controllerConfigRepository;

    @Autowired
    ControllerStatusService controllerStatusService;

    @Autowired
    AppConfig appConfig;

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public HeatStatus getStatus() {
        HeatStatus heatStatus = new HeatStatus();
        ControllerConfig controllerConfig = controllerConfigRepository.getConfig();
        HeatingData heatingData = controllerStatusService.getHeatStatus();
        transportData(controllerConfig, heatStatus);
        transportData(heatingData, heatStatus);
        transportData(controllerRepository.getControllerProcessData(), heatStatus);
        return heatStatus;
    }

    @PostMapping(value = "/circuit/${id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ControllerConfig.Circuit updateCircuit(@PathVariable("id") Integer id, ControllerConfig.Circuit circuit) {
        if (controllerConfigRepository.updateCircuitConfig(circuit)) {
            return circuit;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit does not exist in the config: " + id);
        }
    }

    @EventListener
    void onStartup(ApplicationReadyEvent event) {
        LOGGER.info("HomeHeatManager version {} started", appConfig.applicationVersion);
    }

    void transportData(HeatingData heatingData, HeatStatus heatStatus) {
        if (heatingData.getLastMessageTime() != null)
            heatStatus.setLastMessageTime((Calendar) heatingData.getLastMessageTime().clone());
        heatStatus.setHeatingPeriod(heatingData.getHeatingPeriod());
        heatStatus.setExternalTemperature(heatingData.getExternalTemperature());
        heatStatus.setAvgExternalTemperature(heatingData.getAverageExternalTemperature());
        if (heatStatus.getCircuitStatuses() == null) {
            heatStatus.setCircuitStatuses(new LinkedList<>());
        }
        if (heatingData.getCircuits() != null) {
            for (HeatingData.CircuitData circuitData : heatingData.getCircuits()) {
                boolean found = false;
                CircuitStatus circuitStatus = null;
                for (CircuitStatus circuitStatusElem : heatStatus.getCircuitStatuses()) {
                    if (circuitStatusElem.getCircuitIndex() == circuitData.getIndex()) {
                        found = true;
                        circuitStatus = circuitStatusElem;
                        break;
                    }
                }
                if (!found) {
                    circuitStatus = new CircuitStatus();
                    circuitStatus.setCircuitIndex(circuitData.getIndex());
                    heatStatus.getCircuitStatuses().add(circuitStatus);
                }
                circuitStatus.setCircuitTemperature(circuitData.getTemperature());
                circuitStatus.setHeatingOn(circuitData.isHeating());
            }
        }

    }

    void transportData(ControllerConfig controllerConfig, HeatStatus heatStatus) {
        if (heatStatus.getCircuitStatuses() == null) {
            heatStatus.setCircuitStatuses(new LinkedList<>());
        }
        if (controllerConfig.getCircuits() != null) {
            for (ControllerConfig.Circuit circuit : controllerConfig.getCircuits()) {
                boolean found = false;
                CircuitStatus circuitStatus = null;
                for (CircuitStatus circuitStatusElem : heatStatus.getCircuitStatuses()) {
                    if (circuitStatusElem.getCircuitIndex() == circuit.getIndex()) {
                        found = true;
                        circuitStatus = circuitStatusElem;
                        break;
                    }
                }
                if (!found) {
                    circuitStatus = new CircuitStatus();
                    circuitStatus.setCircuitIndex(circuit.getIndex());
                    heatStatus.getCircuitStatuses().add(circuitStatus);
                }
                circuitStatus.setCircuitName(circuit.getDescription());
                circuitStatus.setCircuitStatus(circuit.isActive());
            }
        }
    }

    void transportData(ControllerProcessData processData, HeatStatus status) {
        LOGGER.info("ProcessData: status={}, lastActive={}, lastInactive={}",
                processData.getStatus(), processData.getActiveStateTimestamp(), processData.getInactiveStateTimestamp());
        status.setControllerStatus("active".equalsIgnoreCase(processData.getStatus()));
        Calendar statusChangeDate = Calendar.getInstance();
        if (status.isControllerStatus()) {
            statusChangeDate.setTimeInMillis(processData.getActiveStateTimestamp());
        } else {
            statusChangeDate.setTimeInMillis(processData.getInactiveStateTimestamp());
        }
        status.setLastStatusChangeTime(statusChangeDate);
    }
}
