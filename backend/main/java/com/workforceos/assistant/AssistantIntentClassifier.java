package com.workforceos.assistant;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Maps a free-text question to the workforce topics it touches. Offline and deterministic so the
 * assistant works without an LLM; the same topics scope the context sent to an LLM when configured.
 */
public final class AssistantIntentClassifier {

    private AssistantIntentClassifier() {
    }

    public static List<AssistantTopic> classify(String question) {
        if (question == null || question.isBlank()) {
            return List.of(AssistantTopic.GENERAL);
        }
        String text = question.toLowerCase(Locale.ROOT);
        Set<AssistantTopic> topics = new LinkedHashSet<>();

        if (containsAny(text, "headcount", "how many employee", "how many staff", "employee count",
                "staff count", "active employee", "number of employee", "number of staff", "workforce size")) {
            topics.add(AssistantTopic.HEADCOUNT);
        }
        if (containsAny(text, "approval", "approve", "pending", "awaiting", "sign-off", "sign off")) {
            topics.add(AssistantTopic.APPROVALS);
        }
        if (containsAny(text, "attendance", "clock", "late", "absent", "present", "no-show", "no show")) {
            topics.add(AssistantTopic.ATTENDANCE);
        }
        if (containsAny(text, "overtime", "over time", "extra hours")) {
            topics.add(AssistantTopic.OVERTIME);
        }
        if (containsAny(text, "leave", "time off", "vacation", "holiday", "absence request")) {
            topics.add(AssistantTopic.LEAVE);
        }
        if (containsAny(text, "risk", "alert", "coverage", "shortfall", "single point", "burnout")) {
            topics.add(AssistantTopic.RISK);
        }
        if (containsAny(text, "roster", "schedule", "shift", "conflict", "publish")) {
            topics.add(AssistantTopic.ROSTER);
        }
        if (topics.isEmpty()) {
            topics.add(AssistantTopic.GENERAL);
        }
        return List.copyOf(topics);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}