package com.sasieczno.homeheat.manager.service.impl;

import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.CircuitStatus;
import com.sasieczno.homeheat.manager.model.HeatStatus;
import com.sasieczno.homeheat.manager.model.HeatingPeriod;
import com.sasieczno.homeheat.manager.service.ControllerStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.LinkedList;

@Service
public class ControllerStatusServiceImpl implements ControllerStatusService {

    public static final Logger LOGGER = LoggerFactory.getLogger(ControllerStatusServiceImpl.class);

    @Autowired
    AppConfig appConfig;

    private HeatStatus heatStatus;
    private DatagramSocket managementUdpServer;
    private boolean running = false;
    byte[] buf = new byte[1024];
    private Thread managementServerThread;


    @PostConstruct
    private void init() throws SocketException {
        LOGGER.info("Initializing Controller Manager Service");
        heatStatus = new HeatStatus();
        managementUdpServer = new DatagramSocket(new InetSocketAddress("localhost", appConfig.udpManagerPort));
        managementUdpServer.setSoTimeout(10000);
        running = true;
        managementServerThread = new Thread(() -> {
            processManagementMessage();
        }, "ManagementUdpServer");
        managementServerThread.start();

    }

    @PreDestroy
    private void stopService() {
        LOGGER.info("Stopping Controller Manager Service");
        running = false;
        try {
            managementServerThread.join();
        } catch (InterruptedException e) {}
        LOGGER.info("Stopped Controller Manager Service");
    }

    @Override
    public HeatStatus getHeatStatus() {
        return heatStatus;
    }

    private void processManagementMessage() {
        while (running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                managementUdpServer.receive(packet);
                if (LOGGER.isDebugEnabled())
                    LOGGER.info("Received packet from: " + packet.getAddress().getCanonicalHostName());
                decodeManagementMessage(packet.getLength());
            } catch (SocketTimeoutException e) {
                LOGGER.info("Timeout on Management UDP Socket");
            } catch (Exception e) {

            }
        }
    }

    HeatStatus decodeManagementMessage(int length) {
        HeatStatus heatStatus = new HeatStatus();
        int offset = 0;
        if (length >= offset + ManagementMessageDecoder.MESSAGE_TIMESTAMP.getFieldLength()) {
            double timestamp = ManagementMessageDecoder.MESSAGE_TIMESTAMP.decodeValue(buf, offset);
            Calendar lastMessageTime = Calendar.getInstance();
            lastMessageTime.setTimeInMillis((long)(timestamp*1000));
            heatStatus.setLastMessageTime(lastMessageTime);
            offset += ManagementMessageDecoder.MESSAGE_TIMESTAMP.getFieldLength();
        } else {
            return null;
        }
        HeatingPeriod heatingPeriod = null;
        if (length >= offset + ManagementMessageDecoder.HEATING_PERIOD.getFieldLength()) {
            heatStatus.setHeatingPeriod(HeatingPeriod.fromInt(ManagementMessageDecoder.HEATING_PERIOD.decodeValue(buf, offset)));
            offset += ManagementMessageDecoder.HEATING_PERIOD.getFieldLength();
        } else {
            return null;
        }
        double externalTemperature = Double.MIN_VALUE;
        if (length >= offset + ManagementMessageDecoder.EXTERNAL_TEMPERATURE.getFieldLength()) {
            externalTemperature = ManagementMessageDecoder.EXTERNAL_TEMPERATURE.decodeValue(buf, offset);
            offset += ManagementMessageDecoder.EXTERNAL_TEMPERATURE.getFieldLength();
        } else {
            return heatStatus;
        }
        double avgExternalTemperature = Double.MIN_VALUE;
        if (length >= offset + ManagementMessageDecoder.AVG_EXTERNAL_TEMPERATURE.getFieldLength()) {
            avgExternalTemperature = ManagementMessageDecoder.AVG_EXTERNAL_TEMPERATURE.decodeValue(buf, offset);
            offset += ManagementMessageDecoder.AVG_EXTERNAL_TEMPERATURE.getFieldLength();
        } else {
            return heatStatus;
        }
        LinkedList<CircuitStatus> circuitStatusList = new LinkedList<>();
        while (length > offset) {
            CircuitStatus circuitStatus = new CircuitStatus();
            if (length >= offset + ManagementMessageDecoder.CIRCUIT_INDEX.getFieldLength()) {
                circuitStatus.setCircuitIndex(ManagementMessageDecoder.CIRCUIT_INDEX.decodeValue(buf, offset));
                offset += ManagementMessageDecoder.CIRCUIT_INDEX.getFieldLength();
            } else {
                return heatStatus;
            }
            if (length >= offset + ManagementMessageDecoder.CIRCUIT_TEMPERATURE.getFieldLength()) {
                circuitStatus.setCircuitTemperature(ManagementMessageDecoder.CIRCUIT_TEMPERATURE.decodeValue(buf, offset));
                offset += ManagementMessageDecoder.CIRCUIT_TEMPERATURE.getFieldLength();
            } else {
                return heatStatus;
            }
            if (length >= offset + ManagementMessageDecoder.CIRCUIT_STATUS.getFieldLength()) {
                circuitStatus.setCircuitStatus(ManagementMessageDecoder.CIRCUIT_STATUS.decodeValue(buf, offset));
                offset += ManagementMessageDecoder.CIRCUIT_STATUS.getFieldLength();
            } else {
                return heatStatus;
            }
            circuitStatusList.add(circuitStatus);
        }
        return heatStatus;
    }

}
