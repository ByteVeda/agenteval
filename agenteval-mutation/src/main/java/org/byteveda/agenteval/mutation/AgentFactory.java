package org.byteveda.agenteval.mutation;

import java.util.function.Function;

/**
 * Factory that creates an agent function from a system prompt.
 *
 * <p>The returned function accepts a user input and returns the agent's response.
 * This abstraction allows mutation testing to swap system prompts while reusing
 * the same agent execution logic.</p>
 */
@FunctionalInterface
public interface AgentFactory {

    /**
     * Creates an agent function bound to the given system prompt.
     *
     * @param systemPrompt the system prompt to configure the agent with
     * @return a function that maps user input to agent output
     */
    Function<String, String> create(String systemPrompt);
}
