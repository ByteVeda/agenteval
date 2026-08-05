package org.byteveda.agenteval.contracts;

/**
 * Categories of agent behavioral contracts.
 */
public enum ContractType {
    /** Agent must never perform dangerous or unauthorized actions. */
    SAFETY,
    /** Agent must consistently exhibit expected behavioral patterns. */
    BEHAVIORAL,
    /** Agent must follow correct tool usage protocols. */
    TOOL_USAGE,
    /** Agent output must conform to a required format. */
    OUTPUT_FORMAT,
    /** Agent must respect resource and size boundaries. */
    BOUNDARY,
    /** Agent must comply with regulatory or policy requirements. */
    COMPLIANCE
}
