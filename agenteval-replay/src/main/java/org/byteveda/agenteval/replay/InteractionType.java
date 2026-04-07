package org.byteveda.agenteval.replay;

/**
 * Identifies the source of a recorded LLM interaction.
 */
public enum InteractionType {

    /** An interaction with the agent under test. */
    AGENT,

    /** An interaction with the LLM-as-judge evaluation model. */
    JUDGE
}
