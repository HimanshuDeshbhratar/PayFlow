package com.payflow.analytics.controller;

import com.payflow.analytics.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return dashboardService.dashboard();
    }

    @GetMapping("/reports")
    public Map<String, Object> reports() {
        return dashboardService.reports();
    }
}
