package com.workforceos.risk;

import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.workforceos.event.DomainEvent;
import com.workforceos.event.EventHandler;
import com.workforceos.event.EventTypes;

@Component
public class RiskAnalysisScheduler implements EventHandler {

    private static final Set<String> TRIGGER_EVENTS = Set.of(
            EventTypes.ROSTER_PUBLISHED,
            EventTypes.LEAVE_APPROVED,
            EventTypes.OVERTIME_APPROVED,
            EventTypes.EMPLOYEE_DEACTIVATED);

    private final RiskAnalysisService riskAnalysisService;

    public RiskAnalysisScheduler(@Lazy RiskAnalysisService riskAnalysisService) {
        this.riskAnalysisService = riskAnalysisService;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return TRIGGER_EVENTS.contains(event.eventType());
    }

    @Override
    public void onEvent(DomainEvent event) {
        riskAnalysisService.analyze();
    }

    @Scheduled(cron = "${workforce.risk.recompute-cron:0 0 */6 * * *}")
    public void scheduledAnalysis() {
        riskAnalysisService.analyze();
    }
}