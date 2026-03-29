package org.byteveda.agenteval.metrics.llm;

import java.util.Map;

/**
 * Loads prompt templates from classpath resources and performs {@code {{variable}}} substitution.
 *
 * @deprecated Use {@link org.byteveda.agenteval.core.template.PromptTemplate} instead.
 *     This class delegates to the core implementation.
 */
@Deprecated(since = "0.2.0", forRemoval = true)
public final class PromptTemplate {

    private PromptTemplate() {}

    /**
     * @deprecated Use {@link org.byteveda.agenteval.core.template.PromptTemplate#load(String)}.
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public static String load(String resourcePath) {
        return org.byteveda.agenteval.core.template.PromptTemplate.load(resourcePath);
    }

    /**
     * @deprecated Use {@link org.byteveda.agenteval.core.template.PromptTemplate#render(String, Map)}.
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public static String render(String template, Map<String, String> variables) {
        return org.byteveda.agenteval.core.template.PromptTemplate.render(template, variables);
    }

    /**
     * @deprecated Use {@link org.byteveda.agenteval.core.template.PromptTemplate#loadAndRender(String, Map)}.
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public static String loadAndRender(String resourcePath, Map<String, String> variables) {
        return org.byteveda.agenteval.core.template.PromptTemplate.loadAndRender(resourcePath, variables);
    }
}
