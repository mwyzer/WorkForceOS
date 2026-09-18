package com.workforceos.analytics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AbsenteeismAnalyticsService absenteeismAnalyticsService;
    private final DemandForecastService demandForecastService;

    public AnalyticsController(AbsenteeismAnalyticsService absenteeismAnalyticsService,
            DemandForecastService demandForecastService) {
        this.absenteeismAnalyticsService = absenteeismAnalyticsService;
        this.demandForecastService = demandForecastService;
    }

    @GetMapping("/absenteeism")
    public AbsenteeismReport absenteeism(@RequestParam(name = "windowDays", required = false) Integer windowDays) {
        return absenteeismAnalyticsService.report(windowDays);
    }

    @GetMapping("/forecast")
    public DemandForecast forecast(@RequestParam(name = "horizonDays", required = false) Integer horizonDays) {
        return demandForecastService.forecast(horizonDays);
    }
}