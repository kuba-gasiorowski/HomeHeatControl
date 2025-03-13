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
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The controller of the manager REST interface implementation.
 */
@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
public class ManagementController {

    private final ControllerRepository controllerRepository;

    private final ControllerConfigRepository controllerConfigRepository;

    private final ControllerStatusService controllerStatusService;

    private final AppConfig appConfig;


    private HeatStatus getStatus(boolean wait) {
        HeatingData heatingData;
        if (wait) {
            heatingData = controllerStatusService.waitForHeatingDataChange();
        } else {
            heatingData = controllerStatusService.getHeatStatus();
        }
        HeatStatus heatStatus = new HeatStatus();
        ControllerConfig controllerConfig = controllerConfigRepository.getConfig();
        transportData(controllerConfig, heatStatus);
        transportData(heatingData, heatStatus);
        transportData(controllerRepository.getControllerProcessData(), heatStatus);
        return heatStatus;

    }

    @Operation(summary = "Get the current status of the heating system")
    @GetMapping(value = "status", produces = MediaType.APPLICATION_JSON_VALUE)
    public HeatStatus getStatus() {
        return getStatus(false);
    }

    @Operation(summary = "Get status of the heating system in a streaming mode")
    @GetMapping(value = "streamStatus", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStatus() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ExecutorService ssExecutor = Executors.newSingleThreadExecutor();
        ssExecutor.execute(() -> {
            try {
                int i = 0;
                while (true) {
                    HeatStatus heatStatus = getStatus(true);
                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                            .id(String.valueOf(i++))
                            .data(heatStatus)
                            .name("HeatStatus");
                    emitter.send(event);
                }
            } catch (ClientAbortException e) {
                log.debug("Client disconnected", e);
            } catch (Exception e) {
                log.error("Error during SSE stream", e);
            }
        });
        return emitter;
    }

    @Operation(summary = "Get the configuration of the heating system")
    @GetMapping(value = "config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ControllerConfig getConfig() {
        return controllerConfigRepository.getConfig();
    }

    @Operation(summary = "Update the configuration of the heating system")
    @PostMapping(value = "config", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ControllerConfig updateConfig(@RequestBody ControllerConfig config) {
        if (controllerConfigRepository.updateConfig(config)) {
            return controllerConfigRepository.getConfig();
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the config");
        }
    }

    @Operation(summary = "Get the configuration of the particular circuit")
    @GetMapping(value = "config/circuit/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ControllerConfig.Circuit getCircuit(@PathVariable("id") Integer id) {
        for (ControllerConfig.Circuit circuit : controllerConfigRepository.getConfig().getCircuits()) {
            if (circuit.getIndex() == id)
                return circuit;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit does not exist in the config: " + id);
    }

    @Operation(summary = "Update the configuration of the particular circuit")
    @PostMapping(value = "config/circuit/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ControllerConfig.Circuit updateCircuit(@PathVariable("id") Integer id, @RequestBody ControllerConfig.Circuit circuit) {
        if (circuit.getIndex() != id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circuit id mismatch: " + id + " != " + circuit.getIndex());
        }
        if (controllerConfigRepository.updateCircuitConfig(circuit)) {
            return getCircuit(id);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Circuit does not exist in the config: " + id);
        }
    }

    @EventListener
    void onStartup(ApplicationReadyEvent event) {
        log.info("HomeHeatManager version {} started", appConfig.applicationVersion);
    }

    void transportData(HeatingData heatingData, HeatStatus heatStatus) {
        if (heatingData.getLastMessageTime() != null)
            heatStatus.setLastMessageTime(Instant.from(heatingData.getLastMessageTime()));
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
                circuitStatus.setCircuitStatus(circuit.getActive());
            }
        }
    }

    void transportData(ControllerProcessData processData, HeatStatus status) {
        log.debug("ProcessData: status={}, lastActive={}, lastInactive={}",
                processData.getStatus(), processData.getActiveStateTimestamp(), processData.getInactiveStateTimestamp());
        status.setControllerStatus("active".equalsIgnoreCase(processData.getStatus()));
        long timestamp;
        if (status.isControllerStatus()) {
            timestamp = processData.getActiveStateTimestamp();
        } else {
            timestamp = processData.getInactiveStateTimestamp();
        }
        status.setLastStatusChangeTime(Instant.ofEpochMilli(timestamp));
    }
}
