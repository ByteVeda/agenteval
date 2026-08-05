package org.byteveda.agenteval.redteam.attack;

import org.byteveda.agenteval.redteam.AttackCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Loads attack templates from classpath JSON resources.
 */
public final class AttackTemplateLibrary {

    private static final Logger LOG = LoggerFactory.getLogger(AttackTemplateLibrary.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RESOURCE_BASE = "com/agenteval/redteam/attacks/";

    private static final Map<AttackCategory, String> RESOURCE_FILES = Map.of(
            AttackCategory.PROMPT_INJECTION, "prompt-injection.json",
            AttackCategory.INDIRECT_INJECTION, "prompt-injection.json",
            AttackCategory.JAILBREAK, "prompt-injection.json",
            AttackCategory.DATA_LEAKAGE, "data-leakage.json",
            AttackCategory.SYSTEM_PROMPT_EXTRACTION, "data-leakage.json",
            AttackCategory.PII_EXTRACTION, "data-leakage.json",
            AttackCategory.BOUNDARY_TESTING, "boundary.json",
            AttackCategory.BOUNDARY_LANGUAGE, "boundary.json",
            AttackCategory.ROBUSTNESS_TYPO, "robustness.json",
            AttackCategory.ROBUSTNESS_ENCODING, "robustness.json"
    );

    private final Map<AttackCategory, List<AttackTemplate>> templates;

    private AttackTemplateLibrary(Map<AttackCategory, List<AttackTemplate>> templates) {
        this.templates = templates;
    }

    /**
     * Loads all attack templates from classpath resources.
     */
    public static AttackTemplateLibrary load() {
        Map<AttackCategory, List<AttackTemplate>> all = new EnumMap<>(AttackCategory.class);

        for (String resourceFile : RESOURCE_FILES.values().stream().distinct().toList()) {
            String path = RESOURCE_BASE + resourceFile;
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(path)) {
                if (is == null) {
                    LOG.warn("Attack template resource not found: {}", path);
                    continue;
                }

                List<TemplateEntry> entries = MAPPER.readValue(is,
                        new TypeReference<>() {});

                for (TemplateEntry entry : entries) {
                    AttackCategory cat = AttackCategory.valueOf(entry.category());
                    all.computeIfAbsent(cat, k -> new ArrayList<>())
                            .add(new AttackTemplate(cat, entry.template(),
                                    entry.description()));
                }
            } catch (IOException e) {
                LOG.error("Failed to load attack templates from {}", path, e);
            }
        }

        return new AttackTemplateLibrary(all);
    }

    /**
     * Returns templates for the specified category.
     */
    public List<AttackTemplate> forCategory(AttackCategory category) {
        return templates.getOrDefault(category, List.of());
    }

    /**
     * Returns all templates across all categories.
     */
    public List<AttackTemplate> all() {
        return templates.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    /**
     * Returns the number of templates loaded.
     */
    public int size() {
        return templates.values().stream().mapToInt(List::size).sum();
    }

    private record TemplateEntry(String category, String template, String description) {}
}
