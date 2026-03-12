package com.agenteval.redteam;

/**
 * Categories of adversarial attacks for red teaming.
 */
public enum AttackCategory {
    PROMPT_INJECTION,
    INDIRECT_INJECTION,
    JAILBREAK,
    DATA_LEAKAGE,
    SYSTEM_PROMPT_EXTRACTION,
    PII_EXTRACTION,
    BOUNDARY_TESTING,
    BOUNDARY_LANGUAGE,
    ROBUSTNESS_TYPO,
    ROBUSTNESS_ENCODING
}
