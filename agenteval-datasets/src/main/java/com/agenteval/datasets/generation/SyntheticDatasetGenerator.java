package com.agenteval.datasets.generation;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.template.PromptTemplate;
import com.agenteval.datasets.EvalDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates synthetic evaluation datasets using an LLM.
 *
 * <pre>{@code
 * var generator = SyntheticDatasetGenerator.builder()
 *     .config(GenerationConfig.builder()
 *         .judgeModel(myJudge)
 *         .maxCasesPerDocument(5)
 *         .build())
 *     .build();
 *
 * EvalDataset dataset = generator.fromDocuments(List.of("doc1 content", "doc2 content"));
 * }</pre>
 */
public final class SyntheticDatasetGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(SyntheticDatasetGenerator.class);

    private static final String FROM_DOC_PROMPT =
            "com/agenteval/datasets/prompts/generate-from-document.txt";
    private static final String VARIATION_PROMPT =
            "com/agenteval/datasets/prompts/generate-variation.txt";
    private static final String ADVERSARIAL_PROMPT =
            "com/agenteval/datasets/prompts/generate-adversarial.txt";

    private static final Pattern QA_PATTERN = Pattern.compile(
            "Q:\\s*(.+?)\\s*A:\\s*(.+?)(?=Q:|$)", Pattern.DOTALL);

    private final GenerationConfig config;

    private SyntheticDatasetGenerator(Builder builder) {
        this.config = Objects.requireNonNull(builder.config, "config must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Generates test cases from a list of documents.
     */
    public EvalDataset fromDocuments(List<String> documents) {
        Objects.requireNonNull(documents, "documents must not be null");
        LOG.info("Generating test cases from {} documents", documents.size());

        JudgeModel judge = config.judgeModel();
        List<AgentTestCase> allCases = new ArrayList<>();

        for (String document : documents) {
            Map<String, String> vars = new HashMap<>();
            vars.put("document", document);
            vars.put("maxCases", String.valueOf(config.maxCasesPerDocument()));
            vars.put("difficulty", config.difficulty());

            String prompt = PromptTemplate.loadAndRender(FROM_DOC_PROMPT, vars);
            JudgeResponse response = judge.judge(prompt);

            List<AgentTestCase> cases = parseQAPairs(response.reason());
            allCases.addAll(cases);
            LOG.debug("Generated {} cases from document", cases.size());
        }

        return EvalDataset.builder()
                .name("synthetic")
                .version("1.0")
                .testCases(allCases)
                .metadata(Map.of("source", "synthetic",
                        "difficulty", config.difficulty()))
                .build();
    }

    /**
     * Generates variations of existing test cases.
     */
    public EvalDataset variations(List<AgentTestCase> sourceCases) {
        Objects.requireNonNull(sourceCases, "sourceCases must not be null");
        LOG.info("Generating variations for {} source cases", sourceCases.size());

        JudgeModel judge = config.judgeModel();
        List<AgentTestCase> allCases = new ArrayList<>();

        for (AgentTestCase source : sourceCases) {
            Map<String, String> vars = new HashMap<>();
            vars.put("input", source.getInput());
            vars.put("expectedOutput", source.getExpectedOutput() != null
                    ? source.getExpectedOutput() : "(none)");

            String prompt = PromptTemplate.loadAndRender(VARIATION_PROMPT, vars);
            JudgeResponse response = judge.judge(prompt);

            List<AgentTestCase> cases = parseQAPairs(response.reason());
            allCases.addAll(cases);
        }

        return EvalDataset.builder()
                .name("synthetic-variations")
                .version("1.0")
                .testCases(allCases)
                .metadata(Map.of("source", "variation"))
                .build();
    }

    /**
     * Generates adversarial test cases from existing ones.
     */
    public EvalDataset adversarial(List<AgentTestCase> sourceCases) {
        Objects.requireNonNull(sourceCases, "sourceCases must not be null");
        LOG.info("Generating adversarial cases for {} source cases", sourceCases.size());

        JudgeModel judge = config.judgeModel();
        List<AgentTestCase> allCases = new ArrayList<>();

        for (AgentTestCase source : sourceCases) {
            Map<String, String> vars = new HashMap<>();
            vars.put("input", source.getInput());
            vars.put("expectedOutput", source.getExpectedOutput() != null
                    ? source.getExpectedOutput() : "(none)");

            String prompt = PromptTemplate.loadAndRender(ADVERSARIAL_PROMPT, vars);
            JudgeResponse response = judge.judge(prompt);

            List<AgentTestCase> cases = parseQAPairs(response.reason());
            allCases.addAll(cases);
        }

        return EvalDataset.builder()
                .name("synthetic-adversarial")
                .version("1.0")
                .testCases(allCases)
                .metadata(Map.of("source", "adversarial"))
                .build();
    }

    static List<AgentTestCase> parseQAPairs(String text) {
        List<AgentTestCase> cases = new ArrayList<>();
        if (text == null || text.isBlank()) return cases;

        Matcher matcher = QA_PATTERN.matcher(text);
        while (matcher.find()) {
            String question = matcher.group(1).trim();
            String answer = matcher.group(2).trim();
            if (!question.isEmpty() && !answer.isEmpty()) {
                cases.add(AgentTestCase.builder()
                        .input(question)
                        .expectedOutput(answer)
                        .build());
            }
        }
        return cases;
    }

    public static final class Builder {
        private GenerationConfig config;

        private Builder() {}

        public Builder config(GenerationConfig config) {
            this.config = config;
            return this;
        }

        public SyntheticDatasetGenerator build() {
            return new SyntheticDatasetGenerator(this);
        }
    }
}
