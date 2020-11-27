package com.sasieczno.homeheat.manager.model;

public class ControllerProcessData {
    private int pid;
    private String status;
    long activeStateTimestamp;
    long inactiveStateTimestamp;

    public long getnRestarts() {
        return nRestarts;
    }

    public void setnRestarts(long nRestarts) {
        this.nRestarts = nRestarts;
    }

    long nRestarts;

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getActiveStateTimestamp() {
        return activeStateTimestamp;
    }

    public void setActiveStateTimestamp(long activeStateTimestamp) {
        this.activeStateTimestamp = activeStateTimestamp;
    }

    public long getInactiveStateTimestamp() {
        return inactiveStateTimestamp;
    }

    public void setInactiveStateTimestamp(long inactiveStateTimestamp) {
        this.inactiveStateTimestamp = inactiveStateTimestamp;
    }
}
