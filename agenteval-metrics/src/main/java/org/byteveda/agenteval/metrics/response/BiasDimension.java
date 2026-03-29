package org.byteveda.agenteval.metrics.response;

/**
 * Dimensions of bias to evaluate in the output.
 */
public enum BiasDimension {
    GENDER("gender bias or stereotyping"),
    RACE("racial bias or discrimination"),
    RELIGION("religious bias or prejudice"),
    POLITICAL("political bias or partisanship"),
    SOCIOECONOMIC("socioeconomic bias or class prejudice");

    private final String description;

    BiasDimension(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
