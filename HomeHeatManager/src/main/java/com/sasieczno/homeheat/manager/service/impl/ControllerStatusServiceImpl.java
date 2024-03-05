package com.sasieczno.homeheat.manager.service.impl;

import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.HeatingData;
import com.sasieczno.homeheat.manager.model.HeatingPeriod;
import com.sasieczno.homeheat.manager.service.ControllerStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.LinkedList;

/**
 * The service sets up the UDP server which listens to the datagrams containing
 * the controller status payload. The payload format is (bytes):
 * Mandatory part:
 * <pre>
 * |--0--|--1--|--2--|--3--|--4--|--5--|--6--|--7--|--8--|
 * | <-------------------- TS -------------------> | HP  |
 * </pre>
 * Non-mandatory:
 * <pre>
 * |--9--|--10-|--11-|--12-|--13-|--14-|--15-|--16-|
 * | <-------------------- TC -------------------> |
 * |--17-|--18-|--19-|--20-|--21-|--22-|--23-|--24-|
 * | <-------------------- AT -------------------> |
 * </pre>
 * For each circuit:
 * <pre>
 * |--n--|-n+1-|-n+2-|-n+3-|-n+4-|-n+5-|-n+6-|-n+7-|-n+8-|-n+9-|
 * | CI  | <-------------------- CT -------------------> | CS  |
 * </pre>
 * <ul>
 *     <li>TS (8 bytes): Message timestamp (unix style + fractional part)</li>
 *     <li>HP (1 byte): Heating period: 0 (no heating), 1 (night), 2 (day)</li>
 *     <li>TC (8 bytes): Current external temperature (double value)</li>
 *     <li>AT (8 bytes): Average external temperature (double value)</li>
 *     <li>CI (1 byte): Circuit index</li>
 *     <li>CT (8 bytes): Circuit temperature (double value)</li>
 *     <li>CS (8 bytes): Circuit status: 0 (off), 1 (on)</li>
 * </ul>
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ControllerStatusServiceImpl implements ControllerStatusService {

    private final AppConfig appConfig;

    private HeatingData heatingData;
    private DatagramSocket managementUdpServer;
    private boolean running = false;
    byte[] buf = new byte[1024];
    private Thread managementServerThread;


    @PostConstruct
    private void init() throws SocketException {
        log.info("Initializing Controller Manager Service");
        heatingData = new HeatingData();
        managementUdpServer = new DatagramSocket(appConfig.udpManagerPort);
        managementUdpServer.setSoTimeout(appConfig.udpManagerTimeout);
        running = true;
        managementServerThread = new Thread(this::processManagementMessage, "ManagementUdpServer");
        managementServerThread.start();

    }

    @PreDestroy
    private void stopService() {
        log.info("Stopping Controller Manager Service");
        running = false;
        try {
            managementUdpServer.close();
            managementServerThread.interrupt();
            managementServerThread.join();
        } catch (InterruptedException e) {}
        log.info("Stopped Controller Manager Service");
    }

    @Override
    public HeatingData getHeatStatus() {
        return heatingData;
    }

    private void processManagementMessage() {
        while (running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                managementUdpServer.receive(packet);
                log.debug("Received packet from: {}", packet.getAddress().getCanonicalHostName());
                heatingData = decodeManagementMessage(packet.getLength());
            } catch (SocketTimeoutException e) {
                log.info("Timeout on Management UDP Socket");
            } catch (Exception e) {
                if (running)
                    log.warn("Exception on Management UDP Socket", e);
            }
        }
    }

    HeatingData decodeManagementMessage(int length) {
        HeatingData heatStatus = new HeatingData();
        int offset = 0;
        if (length >= offset + ManagementMessageDecoder.MESSAGE_TIMESTAMP.getFieldLength()) {
            double timestamp = ManagementMessageDecoder.MESSAGE_TIMESTAMP.decodeValue(buf, offset);
            heatStatus.setLastMessageTime(Instant.ofEpochMilli((long)(timestamp*1000)));
            offset += ManagementMessageDecoder.MESSAGE_TIMESTAMP.getFieldLength();
        } else {
            return null;
        }

        if (length >= offset + ManagementMessageDecoder.HEATING_PERIOD.getFieldLength()) {
            heatStatus.setHeatingPeriod(HeatingPeriod.fromInt(ManagementMessageDecoder.HEATING_PERIOD.decodeValue(buf, offset)));
            offset += ManagementMessageDecoder.HEATING_PERIOD.getFieldLength();
        } else {
            return null;
        }
        heatStatus.setExternalTemperature(Double.MIN_VALUE);
        if (length >= offset + ManagementMessageDecoder.EXTERNAL_TEMPERATURE.getFieldLength()) {
            heatStatus.setExternalTemperature(ManagementMessageDecoder.EXTERNAL_TEMPERATURE.decodeValue(buf, offset));
            offset += ManagementMessageDecoder.EXTERNAL_TEMPERATURE.getFieldLength();
        } else {
            return heatStatus;
        }
        heatStatus.setAverageExternalTemperature(Double.MIN_VALUE);
        if (length >= offset + ManagementMessageDecoder.AVG_EXTERNAL_TEMPERATURE.getFieldLength()) {
            heatStatus.setAverageExternalTemperature(ManagementMessageDecoder.AVG_EXTERNAL_TEMPERATURE.decodeValue(buf, offset));
            offset += ManagementMessageDecoder.AVG_EXTERNAL_TEMPERATURE.getFieldLength();
        } else {
            return heatStatus;
        }
        LinkedList<HeatingData.CircuitData> circuitStatusList = new LinkedList<>();
        heatStatus.setCircuits(circuitStatusList);
        while (length > offset) {
            HeatingData.CircuitData circuitData = new HeatingData.CircuitData();
            if (length >= offset + ManagementMessageDecoder.CIRCUIT_INDEX.getFieldLength()) {
                circuitData.setIndex(ManagementMessageDecoder.CIRCUIT_INDEX.decodeValue(buf, offset));
                offset += ManagementMessageDecoder.CIRCUIT_INDEX.getFieldLength();
            } else {
                return heatStatus;
            }
            if (length >= offset + ManagementMessageDecoder.CIRCUIT_TEMPERATURE.getFieldLength()) {
                circuitData.setTemperature(ManagementMessageDecoder.CIRCUIT_TEMPERATURE.decodeValue(buf, offset));
                offset += ManagementMessageDecoder.CIRCUIT_TEMPERATURE.getFieldLength();
            } else {
                return heatStatus;
            }
            if (length >= offset + ManagementMessageDecoder.CIRCUIT_STATUS.getFieldLength()) {
                circuitData.setHeating(ManagementMessageDecoder.CIRCUIT_STATUS.decodeValue(buf, offset));
                offset += ManagementMessageDecoder.CIRCUIT_STATUS.getFieldLength();
            } else {
                return heatStatus;
            }
            circuitStatusList.add(circuitData);
        }
        return heatStatus;
    }

}
