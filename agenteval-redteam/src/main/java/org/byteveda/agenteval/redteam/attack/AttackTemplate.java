package org.byteveda.agenteval.redteam.attack;

import org.byteveda.agenteval.redteam.AttackCategory;

/**
 * A single attack template used for adversarial testing.
 *
 * @param category the attack category
 * @param template the attack prompt template (may contain {{target}} placeholder)
 * @param description human-readable description of the attack
 */
public record AttackTemplate(
        AttackCategory category,
        String template,
        String description
) {}
