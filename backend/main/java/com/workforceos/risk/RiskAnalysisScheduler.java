package com.workforceos.risk;

import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.workforceos.event.DomainEvent;
import com.workforceos.event.EventHandler;
import com.workforceos.event.EventTypes;
import com.workforceos.organization.TenantContext;
import com.workforceos.shared.TenantScope;

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
        UUID tenantId = event.organizationId() != null ? event.organizationId() : TenantScope.require();
        runInTenantContext(tenantId, this::analyze);
    }

    @Scheduled(cron = "${workforce.risk.recompute-cron:0 0 */6 * * *}")
    public void scheduledAnalysis() {
        runInTenantContext(TenantScope.require(), this::analyze);
    }

    private void analyze() {
        riskAnalysisService.analyze();
    }

    private void runInTenantContext(UUID tenantId, Runnable runnable) {
        TenantContext.set(tenantId);
        try {
            runnable.run();
        } finally {
            TenantContext.clear();
        }
    }
}