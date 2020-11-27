package com.sasieczno.homeheat.manager.controller;

import com.sasieczno.homeheat.manager.model.HeatStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagementController {
    @GetMapping("/status")
    public HeatStatus getStatus() {
        return null;
    }
}
