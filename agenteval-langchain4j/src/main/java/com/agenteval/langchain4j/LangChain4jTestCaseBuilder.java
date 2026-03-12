package com.agenteval.langchain4j;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.TokenUsage;
import com.agenteval.core.model.ToolCall;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts LangChain4j types to AgentEval {@link AgentTestCase}.
 */
public final class LangChain4jTestCaseBuilder {

    private LangChain4jTestCaseBuilder() {}

    /**
     * Creates an AgentTestCase from a LangChain4j AiMessage response.
     *
     * @param input the user's input
     * @param response the LangChain4j response
     * @return a populated AgentTestCase
     */
    public static AgentTestCase fromResponse(String input, Response<AiMessage> response) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(response, "response must not be null");

        var builder = AgentTestCase.builder().input(input);

        AiMessage message = response.content();
        if (message != null) {
            builder.actualOutput(message.text());

            if (message.hasToolExecutionRequests()) {
                List<ToolCall> toolCalls = message.toolExecutionRequests().stream()
                        .map(req -> ToolCall.of(req.name(),
                                Map.of("arguments", req.arguments())))
                        .toList();
                builder.toolCalls(toolCalls);
            }
        }

        if (response.tokenUsage() != null) {
            dev.langchain4j.model.output.TokenUsage usage = response.tokenUsage();
            builder.tokenUsage(new TokenUsage(
                    usage.inputTokenCount(),
                    usage.outputTokenCount(),
                    usage.totalTokenCount()));
        }

        return builder.build();
    }
}
