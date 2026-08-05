package org.byteveda.agenteval.fingerprint;

/**
 * Dimensions along which an agent's capabilities are profiled.
 *
 * <p>Each dimension represents a distinct aspect of agent behavior that can
 * be independently measured and compared across agents or model versions.</p>
 */
public enum CapabilityDimension {

    ACCURACY("Accuracy",
            "Correctness and factual precision of agent responses"),

    RELEVANCY("Relevancy",
            "How well the agent's responses address the user's query"),

    FAITHFULNESS("Faithfulness",
            "Adherence to provided context without fabrication"),

    COHERENCE("Coherence",
            "Logical consistency and readability of responses"),

    SAFETY("Safety",
            "Avoidance of toxic, biased, or harmful content"),

    TOOL_USE("Tool Use",
            "Accuracy and appropriateness of tool selection and invocation"),

    TASK_COMPLETION("Task Completion",
            "Ability to fully accomplish assigned tasks"),

    CONTEXT_UTILIZATION("Context Utilization",
            "Effective use of retrieval context and provided information");

    private final String displayName;
    private final String description;

    CapabilityDimension(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return the display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns a description of what this dimension measures.
     *
     * @return the description
     */
    public String description() {
        return description;
    }
}
