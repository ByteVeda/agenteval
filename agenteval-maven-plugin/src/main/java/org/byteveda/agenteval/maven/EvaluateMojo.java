package org.byteveda.agenteval.maven;

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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs AgentEval evaluations against a dataset and reports results.
 *
 * <pre>{@code
 * <plugin>
 *   <groupId>org.byteveda.agenteval</groupId>
 *   <artifactId>agenteval-maven-plugin</artifactId>
 *   <configuration>
 *     <datasetPath>src/test/resources/golden-set.json</datasetPath>
 *     <metrics>AnswerRelevancy,Faithfulness,Correctness</metrics>
 *     <reportFormats>console,json</reportFormats>
 *   </configuration>
 * </plugin>
 * }</pre>
 */
@Mojo(name = "evaluate", defaultPhase = LifecyclePhase.VERIFY)
public class EvaluateMojo extends AbstractMojo {

    @Parameter(property = "agenteval.datasetPath", required = true)
    private String datasetPath;

    @Parameter(property = "agenteval.configFile", defaultValue = "agenteval.yaml")
    private String configFile;

    @Parameter(property = "agenteval.reportFormats", defaultValue = "console,json")
    private String reportFormats;

    @Parameter(property = "agenteval.outputDirectory",
            defaultValue = "${project.build.directory}/agenteval")
    private String outputDirectory;

    @Parameter(property = "agenteval.failOnRegression", defaultValue = "false")
    private boolean failOnRegression;

    @Parameter(property = "agenteval.threshold", defaultValue = "0.7")
    private double threshold;

    @Parameter(property = "agenteval.metrics", defaultValue = "AnswerRelevancy")
    private String metrics;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("AgentEval: Loading dataset from " + datasetPath);

            Path dsPath = Path.of(datasetPath);
            if (!Files.exists(dsPath)) {
                throw new MojoExecutionException("Dataset not found: " + datasetPath);
            }

            EvalDataset dataset = DatasetLoaders.forPath(dsPath);
            getLog().info("AgentEval: Loaded " + dataset.size() + " test cases");

            // Resolve judge from config file or environment
            JudgeModel judge = resolveJudge();

            // Resolve metrics
            List<EvalMetric> evalMetrics = resolveMetrics(judge);
            getLog().info("AgentEval: Running " + evalMetrics.size() + " metrics");

            // Run evaluation
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

            // Resolve and run reporters
            Path outDir = Path.of(outputDirectory);
            Files.createDirectories(outDir);
            List<EvalReporter> reporters = ReportFormatResolver.resolve(reportFormats, outDir);
            for (EvalReporter reporter : reporters) {
                reporter.report(result);
            }

            // Check pass rate
            if (failOnRegression && result.passRate() < 1.0) {
                throw new MojoFailureException(String.format(
                        "AgentEval: Evaluation failed — pass rate %.1f%% (threshold: 100%%)",
                        result.passRate() * 100));
            }

            getLog().info(String.format("AgentEval: Completed — %.1f%% pass rate, avg score %.3f",
                    result.passRate() * 100, result.averageScore()));

        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("AgentEval evaluation failed", e);
        }
    }

    @SuppressWarnings("IllegalCatch")
    private JudgeModel resolveJudge() {
        Path cfgPath = Path.of(configFile);
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
                getLog().debug("Could not load config: " + e.getMessage());
            }
        }

        // Fall back to environment variables
        String provider = System.getenv("AGENTEVAL_JUDGE_PROVIDER");
        String model = System.getenv("AGENTEVAL_JUDGE_MODEL");
        if (provider != null && model != null) {
            return createJudge(provider, model);
        }

        return null;
    }

    private JudgeModel createJudge(String provider, String model) {
        return switch (provider.toLowerCase(java.util.Locale.ROOT)) {
            case "openai" -> JudgeModels.openai(model);
            case "anthropic" -> JudgeModels.anthropic(model);
            case "ollama" -> JudgeModels.ollama(model);
            default -> throw new IllegalArgumentException("Unknown judge provider: " + provider);
        };
    }

    private List<EvalMetric> resolveMetrics(JudgeModel judge) {
        List<EvalMetric> resolved = new ArrayList<>();
        for (String name : metrics.split(",")) {
            resolved.add(MetricResolver.resolve(name.trim(), judge));
        }
        return resolved;
    }
}
