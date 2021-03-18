package com.sasieczno.homeheat.manager.service.impl;

import com.sasieczno.homeheat.manager.config.AppConfig;
import com.sasieczno.homeheat.manager.model.CircuitStatus;
import com.sasieczno.homeheat.manager.model.HeatingData;
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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.LinkedList;

@Service
public class ControllerStatusServiceImpl implements ControllerStatusService {

    public static final Logger LOGGER = LoggerFactory.getLogger(ControllerStatusServiceImpl.class);

    @Autowired
    AppConfig appConfig;

    private HeatingData heatingData;
    private DatagramSocket managementUdpServer;
    private boolean running = false;
    byte[] buf = new byte[1024];
    private Thread managementServerThread;


    @PostConstruct
    private void init() throws SocketException {
        LOGGER.info("Initializing Controller Manager Service");
        heatingData = new HeatingData();
        managementUdpServer = new DatagramSocket(appConfig.udpManagerPort);
        managementUdpServer.setSoTimeout(60000);
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
    public HeatingData getHeatStatus() {
        return heatingData;
    }

    private void processManagementMessage() {
        while (running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                managementUdpServer.receive(packet);
                LOGGER.info("Received packet from: " + packet.getAddress().getCanonicalHostName());
                heatingData = decodeManagementMessage(packet.getLength());
            } catch (SocketTimeoutException e) {
                LOGGER.info("Timeout on Management UDP Socket");
            } catch (Exception e) {

            }
        }
    }

    HeatingData decodeManagementMessage(int length) {
        HeatingData heatStatus = new HeatingData();
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
