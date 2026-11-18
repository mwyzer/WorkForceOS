package com.workforceos.organization;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.EmployeeService;

@Service
public class SubscriptionService {

    public static final String PLAN_STARTER = "STARTER";
    public static final String PLAN_PROFESSIONAL = "PROFESSIONAL";
    public static final String PLAN_ENTERPRISE = "ENTERPRISE";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_TRIAL = "TRIAL";
    public static final String STATUS_SUSPENDED = "SUSPENDED";

    private static final String FEATURE_SCHEDULING = "SCHEDULING";
    private static final String FEATURE_ATTENDANCE = "ATTENDANCE";
    private static final String FEATURE_LEAVE = "LEAVE_OVERTIME";
    private static final String FEATURE_ANALYTICS = "ANALYTICS";
    private static final String FEATURE_AUTO_SCHEDULE = "AUTO_SCHEDULE";
    private static final String FEATURE_AI_ASSISTANT = "AI_ASSISTANT";
    private static final String FEATURE_RISK_INTELLIGENCE = "RISK_INTELLIGENCE";

    private static final List<SubscriptionPlan> CATALOG = List.of(
            new SubscriptionPlan(
                    PLAN_STARTER,
                    "Starter",
                    25,
                    49,
                    List.of(FEATURE_SCHEDULING, FEATURE_ATTENDANCE, FEATURE_LEAVE)),
            new SubscriptionPlan(
                    PLAN_PROFESSIONAL,
                    "Professional",
                    100,
                    149,
                    List.of(FEATURE_SCHEDULING, FEATURE_ATTENDANCE, FEATURE_LEAVE, FEATURE_ANALYTICS,
                            FEATURE_AUTO_SCHEDULE)),
            new SubscriptionPlan(
                    PLAN_ENTERPRISE,
                    "Enterprise",
                    10000,
                    499,
                    List.of(FEATURE_SCHEDULING, FEATURE_ATTENDANCE, FEATURE_LEAVE, FEATURE_ANALYTICS,
                            FEATURE_AUTO_SCHEDULE, FEATURE_AI_ASSISTANT, FEATURE_RISK_INTELLIGENCE)));

    private record Assignment(String planCode, String billingStatus) {
    }

    private final EmployeeService employeeService;
    private final Map<UUID, Assignment> assignments = new ConcurrentHashMap<>();

    public SubscriptionService(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    public List<SubscriptionPlan> catalog() {
        return CATALOG;
    }

    public TenantSubscription currentSubscription() {
        return subscriptionFor(TenantContext.require());
    }

    public TenantSubscription subscriptionFor(UUID organizationId) {
        Assignment assignment = assignments.getOrDefault(organizationId,
                new Assignment(PLAN_ENTERPRISE, STATUS_ACTIVE));
        SubscriptionPlan plan = findPlan(assignment.planCode());
        long seatsUsed = countActiveSeats(organizationId);
        boolean compliant = seatsUsed <= plan.maxSeats() && !STATUS_SUSPENDED.equals(assignment.billingStatus());
        return new TenantSubscription(
                organizationId,
                plan.code(),
                plan.name(),
                assignment.billingStatus(),
                plan.maxSeats(),
                seatsUsed,
                compliant,
                plan.features());
    }

    public TenantSubscription updateSubscription(UUID organizationId, SubscriptionUpdateRequest request) {
        if (request == null || ValidationUtils.isBlank(request.planCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan code is required");
        }
        SubscriptionPlan plan = findPlan(request.planCode().trim().toUpperCase(Locale.ROOT));
        String status = ValidationUtils.isBlank(request.billingStatus())
                ? STATUS_ACTIVE
                : normalizeStatus(request.billingStatus());
        assignments.put(organizationId, new Assignment(plan.code(), status));
        return subscriptionFor(organizationId);
    }

    public void assertSeatAvailable(UUID organizationId) {
        TenantSubscription sub = subscriptionFor(organizationId);
        if (STATUS_SUSPENDED.equals(sub.billingStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization subscription is suspended");
        }
        if (sub.seatsUsed() >= sub.seatLimit()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seat limit reached for plan " + sub.planCode() + " (" + sub.seatLimit() + " seats)");
        }
    }

    private SubscriptionPlan findPlan(String code) {
        return CATALOG.stream()
                .filter(p -> p.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown plan code: " + code));
    }

    private String normalizeStatus(String status) {
        String upper = status.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case STATUS_ACTIVE, STATUS_TRIAL, STATUS_SUSPENDED -> upper;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported billing status: " + status);
        };
    }

    private long countActiveSeats(UUID organizationId) {
        UUID previous = TenantContext.current().orElse(null);
        try {
            TenantContext.set(organizationId);
            return employeeService.findAll().stream().filter(Employee::active).count();
        } finally {
            if (previous != null) {
                TenantContext.set(previous);
            } else {
                TenantContext.clear();
            }
        }
    }
}
