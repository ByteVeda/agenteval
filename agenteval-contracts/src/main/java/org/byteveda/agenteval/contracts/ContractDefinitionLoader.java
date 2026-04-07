package org.byteveda.agenteval.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.byteveda.agenteval.core.judge.JudgeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loads contract definitions from JSON files.
 *
 * <pre>{@code
 * List<Contract> contracts = ContractDefinitionLoader.load(
 *     Path.of("contracts.json"), judge);
 * }</pre>
 */
public final class ContractDefinitionLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ContractDefinitionLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ContractDefinitionLoader() {}

    /**
     * Loads contracts from a file path.
     *
     * @param path path to the JSON contract definition file
     * @param judge optional judge for LLM-judged contracts (may be null)
     */
    public static List<Contract> load(Path path, JudgeModel judge) {
        Objects.requireNonNull(path, "path must not be null");
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is, judge);
        } catch (IOException e) {
            throw new ContractException("Failed to load contracts from " + path, e);
        }
    }

    /**
     * Loads contracts from a classpath resource.
     *
     * @param resourcePath classpath resource path
     * @param judge optional judge for LLM-judged contracts (may be null)
     */
    public static List<Contract> loadFromResource(String resourcePath, JudgeModel judge) {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        try (InputStream is = ContractDefinitionLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new ContractException("Resource not found: " + resourcePath);
            }
            return parse(is, judge);
        } catch (IOException e) {
            throw new ContractException("Failed to load contracts from resource " + resourcePath, e);
        }
    }

    private static List<Contract> parse(InputStream is, JudgeModel judge) throws IOException {
        ContractDefinitionFile file = MAPPER.readValue(is, ContractDefinitionFile.class);
        List<Contract> contracts = new ArrayList<>();

        for (ContractDefinition def : file.contracts) {
            contracts.add(buildContract(def, judge));
        }

        LOG.debug("Loaded {} contracts from definition file", contracts.size());
        return contracts;
    }

    private static Contract buildContract(ContractDefinition def, JudgeModel judge) {
        ContractType type = def.type != null
                ? ContractType.valueOf(def.type.toUpperCase()) : ContractType.BEHAVIORAL;
        ContractSeverity severity = def.severity != null
                ? ContractSeverity.valueOf(def.severity.toUpperCase()) : ContractSeverity.ERROR;

        if (def.llmJudged) {
            if (judge == null) {
                throw new ContractException(
                        "LLM-judged contract '" + def.name + "' requires a JudgeModel");
            }
            ContractBuilder builder = new ContractBuilder(def.name, type)
                    .description(def.description != null ? def.description : "")
                    .severity(severity)
                    .judgedBy(judge)
                    .passThreshold(def.passThreshold > 0 ? def.passThreshold : 0.8);
            return builder.build();
        }

        ContractBuilder builder = new ContractBuilder(def.name, type)
                .description(def.description != null ? def.description : "")
                .severity(severity);

        if (def.checks != null) {
            applyChecks(builder, def.checks);
        }

        return builder.build();
    }

    private static void applyChecks(ContractBuilder builder, ContractChecks checks) {
        if (checks.outputDoesNotContain != null) {
            for (String s : checks.outputDoesNotContain) {
                builder.outputDoesNotContain(s);
            }
        }
        if (checks.outputContains != null) {
            for (String s : checks.outputContains) {
                builder.outputContains(s);
            }
        }
        if (checks.outputDoesNotMatchRegex != null) {
            for (String r : checks.outputDoesNotMatchRegex) {
                builder.outputDoesNotMatchRegex(r);
            }
        }
        if (checks.outputMatches != null) {
            for (String r : checks.outputMatches) {
                builder.outputMatches(r);
            }
        }
        if (checks.outputLengthAtMost > 0) {
            builder.outputLengthAtMost(checks.outputLengthAtMost);
        }
        if (checks.outputLengthAtLeast > 0) {
            builder.outputLengthAtLeast(checks.outputLengthAtLeast);
        }
        if (checks.toolCallCountAtMost > 0) {
            builder.toolCallCountAtMost(checks.toolCallCountAtMost);
        }
        if (checks.toolNeverCalled != null) {
            for (String t : checks.toolNeverCalled) {
                builder.toolNeverCalled(t);
            }
        }
        if (checks.toolAlwaysCalled != null) {
            for (String t : checks.toolAlwaysCalled) {
                builder.toolAlwaysCalled(t);
            }
        }
        if (checks.outputMatchesJson) {
            builder.outputMatchesJson();
        }
    }

    // --- Jackson POJOs ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ContractDefinitionFile {
        public List<ContractDefinition> contracts = List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ContractDefinition {
        public String name;
        public String type;
        public String severity;
        public String description;
        public ContractChecks checks;
        public boolean llmJudged;
        public double passThreshold;
        public String promptTemplate;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ContractChecks {
        public List<String> outputDoesNotContain;
        public List<String> outputContains;
        public List<String> outputDoesNotMatchRegex;
        public List<String> outputMatches;
        public int outputLengthAtMost;
        public int outputLengthAtLeast;
        public int toolCallCountAtMost;
        public List<String> toolNeverCalled;
        public List<String> toolAlwaysCalled;
        public boolean outputMatchesJson;
    }
}
