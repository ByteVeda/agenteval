package org.byteveda.agenteval.spring.ai;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.TokenUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.Objects;

/**
 * Converts Spring AI types to AgentEval {@link AgentTestCase}.
 */
public final class SpringAiTestCaseBuilder {

    private SpringAiTestCaseBuilder() {}

    /**
     * Creates an AgentTestCase from a Spring AI ChatResponse.
     *
     * @param input the user's input prompt
     * @param response the Spring AI chat response
     * @return a populated AgentTestCase
     */
    public static AgentTestCase fromChatResponse(String input, ChatResponse response) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(response, "response must not be null");

        var builder = AgentTestCase.builder().input(input);

        if (response.getResult() != null) {
            Generation result = response.getResult();
            if (result.getOutput() != null) {
                builder.actualOutput(result.getOutput().getText());
            }
        }

        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            var usage = response.getMetadata().getUsage();
            builder.tokenUsage(new TokenUsage(
                    (int) usage.getPromptTokens(),
                    (int) usage.getCompletionTokens(),
                    (int) usage.getTotalTokens()));
        }

        return builder.build();
    }
}
