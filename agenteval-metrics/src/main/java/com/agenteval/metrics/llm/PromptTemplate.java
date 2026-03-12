package com.agenteval.metrics.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads prompt templates from classpath resources and performs {@code {{variable}}} substitution.
 */
public final class PromptTemplate {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{(\\w+)}}");
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    private PromptTemplate() {}

    /**
     * Loads a prompt template from the classpath.
     *
     * @param resourcePath classpath resource path (e.g., "com/agenteval/metrics/prompts/faithfulness.txt")
     * @return the template content
     * @throws IllegalArgumentException if the resource is not found
     */
    public static String load(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        return CACHE.computeIfAbsent(resourcePath, PromptTemplate::doLoad);
    }

    /**
     * Renders a template by replacing {@code {{variable}}} placeholders with values.
     * Unresolved variables are left as-is.
     *
     * @param template the template string
     * @param variables map of variable names to values
     * @return the rendered string
     */
    public static String render(String template, Map<String, String> variables) {
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(variables, "variables must not be null");

        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.getOrDefault(varName, matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Loads and renders a template in one step.
     */
    public static String loadAndRender(String resourcePath, Map<String, String> variables) {
        return render(load(resourcePath), variables);
    }

    private static String doLoad(String resourcePath) {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException(
                        "Prompt template not found on classpath: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to load prompt template: " + resourcePath, e);
        }
    }
}
