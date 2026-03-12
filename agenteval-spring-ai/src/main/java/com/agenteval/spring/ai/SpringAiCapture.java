package com.agenteval.spring.ai;

import com.agenteval.core.model.AgentTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Objects;

/**
 * Captures Spring AI {@link ChatModel} interactions as {@link AgentTestCase} instances.
 *
 * <pre>{@code
 * var capture = new SpringAiCapture(chatModel);
 * AgentTestCase testCase = capture.call("What is Java?");
 * }</pre>
 */
public final class SpringAiCapture {

    private static final Logger LOG = LoggerFactory.getLogger(SpringAiCapture.class);

    private final ChatModel chatModel;

    public SpringAiCapture(ChatModel chatModel) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
    }

    /**
     * Calls the chat model and captures the interaction as an AgentTestCase.
     *
     * @param input the user prompt
     * @return the captured test case
     */
    public AgentTestCase call(String input) {
        LOG.debug("Capturing Spring AI call for input: {}",
                input.length() > 100 ? input.substring(0, 100) + "..." : input);

        long start = System.currentTimeMillis();
        ChatResponse response = chatModel.call(new Prompt(input));
        long latency = System.currentTimeMillis() - start;

        AgentTestCase testCase = SpringAiTestCaseBuilder.fromChatResponse(input, response);
        testCase.setLatencyMs(latency);
        return testCase;
    }
}
