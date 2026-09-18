package com.workforceos.assistant;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforceos.shared.ValidationUtils;

@Service
public class AssistantService {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantService.class);

    private final AssistantContextProvider contextProvider;
    private final HeuristicAssistant heuristic;
    private final LlmAssistant llm;

    public AssistantService(AssistantContextProvider contextProvider, HeuristicAssistant heuristic,
            LlmAssistant llm) {
        this.contextProvider = contextProvider;
        this.heuristic = heuristic;
        this.llm = llm;
    }

    public AssistantAnswer ask(AssistantRequest request) {
        if (request == null || ValidationUtils.isBlank(request.question())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A question is required");
        }

        String question = request.question().trim();
        List<AssistantTopic> topics = AssistantIntentClassifier.classify(question);
        AssistantSnapshot snapshot = contextProvider.snapshot();

        if (llm.available()) {
            try {
                return llm.answer(question, snapshot, topics);
            } catch (RuntimeException ex) {
                LOG.warn("LLM assistant failed, falling back to heuristic answer", ex);
            }
        }
        return heuristic.answer(question, snapshot, topics);
    }

    public String mode() {
        return llm.available() ? "llm" : "heuristic";
    }
}