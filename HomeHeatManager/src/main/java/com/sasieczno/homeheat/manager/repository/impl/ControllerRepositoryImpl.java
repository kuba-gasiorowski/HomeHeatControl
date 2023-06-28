package com.sasieczno.homeheat.manager.repository.impl;

import com.sasieczno.homeheat.manager.model.ControllerProcessData;
import com.sasieczno.homeheat.manager.repository.ControllerRepository;
import de.thjom.java.systemd.Manager;
import de.thjom.java.systemd.Service;
import de.thjom.java.systemd.Systemd;
import de.thjom.java.systemd.Unit;
import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.exceptions.DBusException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * The heating controller process repository implementation
 * with Systemd DBus access.
 */

@Slf4j
@Repository("ControllerRepository")
public class ControllerRepositoryImpl implements ControllerRepository {
    private Systemd systemd;
    private Manager manager;
    private Service controllerService;
    private String state;
    private long activeSince;
    private long inactiveSince;
    private int pid;
    private long restarts;


    @PostConstruct
    public void init() throws DBusException {
        systemd = Systemd.get();
        manager = systemd.getManager();
        controllerService = manager.getService("HomeHeat.service");
    }

    @PreDestroy
    public void destroy() {
        Systemd.disconnect();
    }

    @Scheduled(fixedRate = 60000)
    public void monitorControllerProcess() {
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
        data.setNRestarts(restarts);
        return data;
    }

    @Override
    public void restartService() {
        controllerService.stop(Unit.Mode.FAIL);
        controllerService.start(Unit.Mode.FAIL);
    }
}
