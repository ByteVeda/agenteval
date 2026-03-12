package com.agenteval.langchain4j;

import com.agenteval.core.model.AgentTestCase;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Captures LangChain4j {@link ChatLanguageModel} interactions as {@link AgentTestCase} instances.
 *
 * <pre>{@code
 * var capture = new LangChain4jCapture(chatModel);
 * AgentTestCase testCase = capture.call("What is Java?");
 * }</pre>
 */
public final class LangChain4jCapture {

    private static final Logger LOG = LoggerFactory.getLogger(LangChain4jCapture.class);

    private final ChatLanguageModel model;

    public LangChain4jCapture(ChatLanguageModel model) {
        this.model = Objects.requireNonNull(model, "model must not be null");
    }

    /**
     * Calls the chat model and captures the interaction as an AgentTestCase.
     *
     * @param input the user prompt
     * @return the captured test case
     */
    public AgentTestCase call(String input) {
        LOG.debug("Capturing LangChain4j call for input: {}",
                input.length() > 100 ? input.substring(0, 100) + "..." : input);

        long start = System.currentTimeMillis();
        Response<AiMessage> response = model.generate(UserMessage.from(input));
        long latency = System.currentTimeMillis() - start;

        AgentTestCase testCase = LangChain4jTestCaseBuilder.fromResponse(input, response);
        testCase.setLatencyMs(latency);
        return testCase;
    }
}
