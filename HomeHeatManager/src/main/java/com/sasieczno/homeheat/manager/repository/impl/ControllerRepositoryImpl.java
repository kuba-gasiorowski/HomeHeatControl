package com.sasieczno.homeheat.manager.repository.impl;

import com.sasieczno.homeheat.manager.model.ControllerProcessData;
import com.sasieczno.homeheat.manager.model.HeatStatus;
import com.sasieczno.homeheat.manager.repository.ControllerRepository;
import de.thjom.java.systemd.Manager;
import de.thjom.java.systemd.Service;
import de.thjom.java.systemd.Systemd;
import de.thjom.java.systemd.Unit;
import org.freedesktop.dbus.exceptions.DBusException;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Repository("ControllerRepository")
public class ControllerRepositoryImpl implements ControllerRepository {
    HeatStatus heatStatus;
    Systemd systemd;
    Manager manager;
    Service controllerService;
    Thread monitoringThread;
    boolean running = false;

    String state;
    long activeSince;
    long inactiveSince;
    int pid;
    long restarts;


    @PostConstruct
    void init() throws DBusException {
        heatStatus = new HeatStatus();
        systemd = Systemd.get();
        manager = systemd.getManager();
        controllerService = manager.getService("HomeHeat.service");
        running = true;
        monitoringThread = new Thread(() -> {
            while (running) {
                monitorControllerProcess();
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {}
            }
        }, "ControllerCheck");
        monitoringThread.start();
    }

    @PreDestroy
    void destroy() {
        Systemd.disconnect();
    }

    public HeatStatus getHeatStatus() {
        return heatStatus;
    }

    void monitorControllerProcess() {
        state = controllerService.getActiveState();
        activeSince = controllerService.getActiveEnterTimestamp();
        inactiveSince = controllerService.getInactiveEnterTimestamp();
        pid = controllerService.getMainPID();
        restarts = controllerService.getNRestarts();
    }

    @Override
    public ControllerProcessData getControllerProcessData() {
        ControllerProcessData data = new ControllerProcessData();
        data.setStatus(state);
        data.setActiveStateTimestamp(activeSince);
        data.setInactiveStateTimestamp(inactiveSince);
        data.setPid(pid);
        data.setnRestarts(restarts);
        return data;
    }

    @Override
    public void restartService() {
        controllerService.stop(Unit.Mode.FAIL);
        controllerService.start(Unit.Mode.FAIL);
    }
}
