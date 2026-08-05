package org.byteveda.agenteval.chaos;

/**
 * Categories of chaos engineering failures that can be injected
 * into agent evaluations.
 */
public enum ChaosCategory {
    /** Simulates tool/API call failures. */
    TOOL_FAILURE,
    /** Corrupts retrieval context (missing, contradictory, shuffled). */
    CONTEXT_CORRUPTION,
    /** Simulates high-latency responses from tools. */
    LATENCY,
    /** Mutates tool response schemas unexpectedly. */
    SCHEMA_MUTATION,
    /** Simulates cascading failures across multiple tools. */
    CASCADING_FAILURE,
    /** Simulates resource exhaustion (token limits, rate limits). */
    RESOURCE_EXHAUSTION
}
