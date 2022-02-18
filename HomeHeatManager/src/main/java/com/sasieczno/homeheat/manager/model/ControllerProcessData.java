package com.sasieczno.homeheat.manager.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The heating controller process data.
 */
@Getter
@Setter
@NoArgsConstructor
public class ControllerProcessData {
    private int pid;
    private String status;
    private long activeStateTimestamp;
    private long inactiveStateTimestamp;
    long nRestarts;
}
