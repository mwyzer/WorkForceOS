package com.workforceos.shared;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforceos.dashboard.DashboardSummary;
import com.workforceos.scheduling.AttendanceReport;
import com.workforceos.scheduling.AuditLogSummary;
import com.workforceos.scheduling.LeaveRequestReport;
import com.workforceos.scheduling.OvertimeRequestReport;
import com.workforceos.workforce.Department;
import com.workforceos.workforce.Employee;
import com.workforceos.workforce.LeaveRequest;
import com.workforceos.workforce.OvertimeRequest;
import com.workforceos.workforce.Team;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Binds each cache name to a Jackson serializer for its exact concrete type instead of relying on
 * Jackson's polymorphic "default typing" (embedded {@code @class} metadata). Default typing breaks
 * for top-level {@code List<T>} cache values: {@code Stream.toList()} returns a JDK-internal
 * immutable list, and a bare JSON array of typed elements is ambiguous with the wrapper-array format
 * used for polymorphic values, so round-tripping through Redis throws a Jackson MismatchedInputException.
 * Binding a concrete {@link JavaType} per cache sidesteps that entirely.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
class RedisCacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("departments", listConfig(objectMapper, Department.class));
        perCache.put("department", objectConfig(objectMapper, Department.class));
        perCache.put("teams", listConfig(objectMapper, Team.class));
        perCache.put("team", objectConfig(objectMapper, Team.class));
        perCache.put("employees", listConfig(objectMapper, Employee.class));
        perCache.put("employee", objectConfig(objectMapper, Employee.class));
        perCache.put("leaveRequests", listConfig(objectMapper, LeaveRequest.class));
        perCache.put("overtimeRequests", listConfig(objectMapper, OvertimeRequest.class));
        perCache.put("leave-request-report", objectConfig(objectMapper, LeaveRequestReport.class));
        perCache.put("overtime-request-report", objectConfig(objectMapper, OvertimeRequestReport.class));
        perCache.put("attendance-report", objectConfig(objectMapper, AttendanceReport.class));
        perCache.put("audit-log-summary", objectConfig(objectMapper, AuditLogSummary.class));
        perCache.put("dashboard-summary", objectConfig(objectMapper, DashboardSummary.class));

        return builder -> builder
                .cacheDefaults(baseConfig())
                .withInitialCacheConfigurations(perCache);
    }

    private static RedisCacheConfiguration objectConfig(ObjectMapper mapper, Class<?> type) {
        return baseConfig().serializeValuesWith(pair(new Jackson2JsonRedisSerializer<>(mapper, type)));
    }

    private static RedisCacheConfiguration listConfig(ObjectMapper mapper, Class<?> elementType) {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return baseConfig().serializeValuesWith(pair(new Jackson2JsonRedisSerializer<>(mapper, listType)));
    }

    private static RedisCacheConfiguration baseConfig() {
        return RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1));
    }

    @SuppressWarnings("unchecked")
    private static RedisSerializationContext.SerializationPair<Object> pair(Jackson2JsonRedisSerializer<?> serializer) {
        return RedisSerializationContext.SerializationPair.fromSerializer((RedisSerializer<Object>) serializer);
    }
}
