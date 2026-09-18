package com.workforceos.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.workforceos.bootstrap.WorkforceOsApplication;

/**
 * Automated performance smoke gate. Measures sequential p95 latency across representative
 * read endpoints and verifies a concurrent burst completes without errors. Budgets are
 * deliberately generous because the suite runs in-JVM against in-memory stores; it exists to
 * catch gross regressions. Full latency/concurrency figures require the load harness described
 * in TEST-PLAN.md §5 run against the Docker Compose stack.
 */
@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.security.enabled=false")
class PerformanceSmokeTests {

    private static final long P95_BUDGET_MS = 1_000;
    private static final long SINGLE_REQUEST_BUDGET_MS = 2_000;
    private static final long CONCURRENT_BUDGET_MS = 5_000;
    private static final int CONCURRENT_REQUESTS = 64;

    private static final List<String> ENDPOINTS = List.of(
            "/api/v1/health",
            "/api/v1/employees",
            "/api/v1/reports/leave-requests",
            "/api/v1/reports/attendance",
            "/api/v1/reports/audit-summary",
            "/api/v1/reports/attendance-by-employee",
            "/api/v1/reports/department-staffing",
            "/api/v1/dashboard/summary",
            "/api/v1/dashboard/operations");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sequentialReadLatencyMeetsBudget() throws Exception {
        List<Long> latencies = new ArrayList<>();
        long worst = Long.MIN_VALUE;

        for (String endpoint : ENDPOINTS) {
            long start = System.nanoTime();
            mockMvc.perform(get(endpoint)).andExpect(status().isOk());
            long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            latencies.add(millis);
            worst = Math.max(worst, millis);
        }

        assertTrue(worst <= SINGLE_REQUEST_BUDGET_MS,
                "A request exceeded the " + SINGLE_REQUEST_BUDGET_MS + " ms budget: " + worst + " ms");
        long p95 = percentile(latencies, 0.95);
        assertTrue(p95 <= P95_BUDGET_MS,
                "p95 latency exceeded the " + P95_BUDGET_MS + " ms budget: " + p95 + " ms");
    }

    @Test
    void concurrentReadsAllSucceedWithinBudget() throws Exception {
        long start = System.nanoTime();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<MvcResult>> futures = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> performWithStatus("/api/v1/reports/leave-requests"), executor));
            }
            for (CompletableFuture<MvcResult> future : futures) {
                assertEquals(200, future.get().getResponse().getStatus(),
                        "Concurrent request should succeed");
            }
        }

        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsed <= CONCURRENT_BUDGET_MS,
                "Concurrent burst (" + CONCURRENT_REQUESTS + " requests) took " + elapsed
                        + " ms, over the " + CONCURRENT_BUDGET_MS + " ms budget");
    }

    private MvcResult performWithStatus(String endpoint) {
        try {
            return mockMvc.perform(get(endpoint)).andReturn();
        } catch (Exception ex) {
            throw new IllegalStateException("Request to " + endpoint + " failed", ex);
        }
    }

    private static long percentile(List<Long> values, double percentile) {
        List<Long> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}