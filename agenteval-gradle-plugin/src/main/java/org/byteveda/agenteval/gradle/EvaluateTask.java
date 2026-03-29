package org.byteveda.agenteval.gradle;

import org.byteveda.agenteval.core.config.AgentEvalConfigLoader;
import org.byteveda.agenteval.core.config.YamlConfigModel;
import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.datasets.DatasetLoaders;
import org.byteveda.agenteval.datasets.EvalDataset;
import org.byteveda.agenteval.judge.JudgeModels;
import org.byteveda.agenteval.reporting.EvalReporter;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gradle task that runs AgentEval evaluations against a dataset.
 */
public abstract class EvaluateTask extends DefaultTask {

    @Input
    public abstract Property<String> getDatasetPath();

    @Input
    @Optional
    public abstract Property<String> getConfigFile();

    @Input
    @Optional
    public abstract Property<String> getReportFormats();

    @Input
    @Optional
    public abstract Property<String> getOutputDirectory();

    @Input
    @Optional
    public abstract Property<Boolean> getFailOnRegression();

    @Input
    @Optional
    public abstract Property<Double> getThreshold();

    @Input
    @Optional
    public abstract Property<String> getMetrics();

    @TaskAction
    public void execute() {
        try {
            String dsPathStr = getDatasetPath().get();
            getLogger().lifecycle("AgentEval: Loading dataset from " + dsPathStr);

            Path dsPath = Path.of(dsPathStr);
            if (!Files.exists(dsPath)) {
                throw new GradleException("Dataset not found: " + dsPathStr);
            }

            EvalDataset dataset = DatasetLoaders.forPath(dsPath);
            getLogger().lifecycle("AgentEval: Loaded " + dataset.size() + " test cases");

            JudgeModel judge = resolveJudge();

            List<EvalMetric> evalMetrics = resolveMetrics(judge);
            getLogger().lifecycle("AgentEval: Running " + evalMetrics.size() + " metrics");

            long start = System.currentTimeMillis();
            List<CaseResult> caseResults = new ArrayList<>();
            for (AgentTestCase testCase : dataset.getTestCases()) {
                Map<String, EvalScore> scores = new HashMap<>();
                boolean allPassed = true;
                for (EvalMetric metric : evalMetrics) {
                    EvalScore score = metric.evaluate(testCase);
                    score = score.withMetricName(metric.name());
                    scores.put(metric.name(), score);
                    if (!score.passed()) {
                        allPassed = false;
                    }
                }
                caseResults.add(new CaseResult(testCase, scores, allPassed));
            }
            long duration = System.currentTimeMillis() - start;
            EvalResult result = EvalResult.of(caseResults, duration);

            Path outDir = Path.of(getOutputDirectory().get());
            Files.createDirectories(outDir);
            List<EvalReporter> reporters =
                    ReportFormatResolver.resolve(getReportFormats().get(), outDir);
            for (EvalReporter reporter : reporters) {
                reporter.report(result);
            }

            if (Boolean.TRUE.equals(getFailOnRegression().getOrNull())
                    && result.passRate() < 1.0) {
                throw new GradleException(String.format(
                        "AgentEval: Evaluation failed — pass rate %.1f%% (threshold: 100%%)",
                        result.passRate() * 100));
            }

            getLogger().lifecycle(String.format(
                    "AgentEval: Completed — %.1f%% pass rate, avg score %.3f",
                    result.passRate() * 100, result.averageScore()));

        } catch (GradleException e) {
            throw e;
        } catch (Exception e) {
            throw new GradleException("AgentEval evaluation failed", e);
        }
    }

    @SuppressWarnings("IllegalCatch")
    private JudgeModel resolveJudge() {
        String cfgFile = getConfigFile().getOrElse("agenteval.yaml");
        Path cfgPath = Path.of(cfgFile);
        if (Files.exists(cfgPath)) {
            try {
                YamlConfigModel model = AgentEvalConfigLoader.loadModel(cfgPath);
                if (model.getJudge() != null) {
                    String provider = model.getJudge().getProvider();
                    String modelName = model.getJudge().getModel();
                    if (provider != null && modelName != null) {
                        return createJudge(provider, modelName);
                    }
                }
            } catch (Exception e) {
                getLogger().debug("Could not load config: " + e.getMessage());
            }
        }

        String provider = System.getenv("AGENTEVAL_JUDGE_PROVIDER");
        String model = System.getenv("AGENTEVAL_JUDGE_MODEL");
        if (provider != null && model != null) {
            return createJudge(provider, model);
        }

        return null;
    }

    private JudgeModel createJudge(String provider, String model) {
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "openai" -> JudgeModels.openai(model);
            case "anthropic" -> JudgeModels.anthropic(model);
            case "ollama" -> JudgeModels.ollama(model);
            default -> throw new IllegalArgumentException("Unknown judge provider: " + provider);
        };
    }

    private List<EvalMetric> resolveMetrics(JudgeModel judge) {
        List<EvalMetric> resolved = new ArrayList<>();
        String metricsStr = getMetrics().getOrElse("AnswerRelevancy");
        for (String name : metricsStr.split(",")) {
            resolved.add(MetricResolver.resolve(name.trim(), judge));
        }
        return resolved;
    }
}
