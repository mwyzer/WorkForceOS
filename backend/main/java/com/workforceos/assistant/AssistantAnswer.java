package com.workforceos.assistant;

import java.util.List;

public record AssistantAnswer(
        String question,
        String answer,
        List<AssistantTopic> topics,
        String mode) {
}