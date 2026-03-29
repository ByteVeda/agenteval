package org.byteveda.agenteval.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads {@link AgentEvalConfig} from an {@code agenteval.yaml} file.
 *
 * <p>Supports environment variable resolution in values using {@code ${ENV_VAR}} syntax.</p>
 */
public final class AgentEvalConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(AgentEvalConfigLoader.class);
    private static final Pattern ENV_VAR = Pattern.compile("\\$\\{(\\w+)}");
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private AgentEvalConfigLoader() {}

    /**
     * Loads configuration from the given YAML file path.
     *
     * @param path the path to agenteval.yaml
     * @return the populated config builder (call {@code .build()} to finalize)
     */
    public static AgentEvalConfig.Builder load(Path path) {
        LOG.debug("Loading AgentEval config from {}", path);
        try {
            String content = Files.readString(path);
            return parse(content);
        } catch (IOException e) {
            throw new ConfigException("Failed to load config from " + path, e);
        }
    }

    /**
     * Loads configuration from an input stream.
     */
    public static AgentEvalConfig.Builder load(InputStream inputStream) {
        LOG.debug("Loading AgentEval config from input stream");
        try {
            String content = new String(inputStream.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            return parse(content);
        } catch (IOException e) {
            throw new ConfigException("Failed to load config from input stream", e);
        }
    }

    static AgentEvalConfig.Builder parse(String yamlContent) {
        String resolved = resolveEnvVars(yamlContent);
        try {
            YamlConfigModel model = YAML_MAPPER.readValue(resolved, YamlConfigModel.class);
            return toBuilder(model);
        } catch (IOException e) {
            throw new ConfigException("Failed to parse YAML config: " + e.getMessage(), e);
        }
    }

    private static AgentEvalConfig.Builder toBuilder(YamlConfigModel model) {
        var builder = AgentEvalConfig.builder();
        if (model == null) {
            return builder;
        }

        if (model.getDefaults() != null) {
            var defaults = model.getDefaults();
            if (defaults.getMaxRetries() != null) {
                builder.maxRetries(defaults.getMaxRetries());
            }
            if (defaults.getRetryOnRateLimit() != null) {
                builder.retryOnRateLimit(defaults.getRetryOnRateLimit());
            }
            if (defaults.getMaxConcurrentJudgeCalls() != null) {
                builder.maxConcurrentJudgeCalls(defaults.getMaxConcurrentJudgeCalls());
            }
            if (defaults.getParallelEvaluation() != null) {
                builder.parallelEvaluation(defaults.getParallelEvaluation());
            }
            if (defaults.getParallelism() != null) {
                builder.parallelism(defaults.getParallelism());
            }
        }

        if (model.getCost() != null && model.getCost().getBudget() != null) {
            builder.costBudget(model.getCost().getBudget());
        }

        return builder;
    }

    static String resolveEnvVars(String content) {
        Matcher matcher = ENV_VAR.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String envName = matcher.group(1);
            String envValue = System.getenv(envName);
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(envValue != null ? envValue : ""));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Returns the parsed {@link YamlConfigModel} for advanced use cases.
     */
    public static YamlConfigModel loadModel(Path path) {
        try {
            String content = resolveEnvVars(Files.readString(path));
            return YAML_MAPPER.readValue(content, YamlConfigModel.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to load config model from " + path, e);
        }
    }
}
