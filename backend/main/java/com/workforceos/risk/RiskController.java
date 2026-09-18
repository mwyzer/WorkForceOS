package com.workforceos.risk;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {

    private final RiskAnalysisService riskAnalysisService;
    private final RiskAlertService riskAlertService;

    public RiskController(RiskAnalysisService riskAnalysisService, RiskAlertService riskAlertService) {
        this.riskAnalysisService = riskAnalysisService;
        this.riskAlertService = riskAlertService;
    }

    @PostMapping("/analyze")
    public WorkforceRiskSummary analyze() {
        return riskAnalysisService.analyze();
    }

    @GetMapping("/summary")
    public WorkforceRiskSummary summary() {
        return riskAnalysisService.summary();
    }

    @GetMapping("/assessments")
    public List<RiskAssessment> assessments() {
        return riskAnalysisService.assessments();
    }

    @GetMapping("/assessments/{id}/recommendations")
    public List<RiskRecommendation> recommendations(@PathVariable UUID id) {
        return riskAnalysisService.recommendations(id);
    }

    @GetMapping("/trend")
    public List<RiskTrendPoint> trend() {
        return riskAnalysisService.trend();
    }

    @GetMapping("/alerts")
    public List<RiskAlert> alerts() {
        return riskAlertService.findAll();
    }

    @PostMapping("/alerts/{id}/resolve")
    public RiskAlert resolve(@PathVariable UUID id) {
        return riskAlertService.resolve(id);
    }
}