package com.workforceos.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AssistantIntentClassifierTests {

    @Test
    void classifiesHeadcountQuestions() {
        assertTrue(AssistantIntentClassifier.classify("How many employees do we have?")
                .contains(AssistantTopic.HEADCOUNT));
        assertTrue(AssistantIntentClassifier.classify("What is our staff count?")
                .contains(AssistantTopic.HEADCOUNT));
    }

    @Test
    void classifiesApprovalQuestions() {
        assertEquals(List.of(AssistantTopic.APPROVALS),
                AssistantIntentClassifier.classify("What is pending approval?"));
    }

    @Test
    void classifiesRiskAndCoverageQuestions() {
        assertTrue(AssistantIntentClassifier.classify("Are there any coverage alerts?")
                .contains(AssistantTopic.RISK));
    }

    @Test
    void classifiesMultipleTopics() {
        List<AssistantTopic> topics = AssistantIntentClassifier.classify(
                "How much overtime is pending approval and what is our risk?");
        assertTrue(topics.contains(AssistantTopic.OVERTIME));
        assertTrue(topics.contains(AssistantTopic.APPROVALS));
        assertTrue(topics.contains(AssistantTopic.RISK));
    }

    @Test
    void fallsBackToGeneralForUnrecognizedQuestions() {
        assertEquals(List.of(AssistantTopic.GENERAL),
                AssistantIntentClassifier.classify("Tell me something interesting"));
        assertEquals(List.of(AssistantTopic.GENERAL),
                AssistantIntentClassifier.classify(null));
    }
}