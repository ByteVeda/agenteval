# Java AI Agent Evaluation & Testing Library — Feature Specification

> **Working Name:** TBD (candidates: `agenteval`, `agentest`, `jeval`, `evalkit`)
> **Language:** Java 21+ (LTS baseline)
> **License:** Apache 2.0
> **Build:** Maven Central artifact, Gradle/Maven compatible
> **Philosophy:** Library, not framework. JUnit-native. Local-first. Framework-agnostic.

---

## 1. Core Test Case Model

The foundational data model that captures everything needed to evaluate an AI interaction.

### 1.1 `AgentTestCase` — Single-Turn Evaluation

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `input` | `String` | Yes | The user query or prompt sent to the agent |
| `actualOutput` | `String` | Yes | The agent's actual response |
| `expectedOutput` | `String` | No | Ground truth / ideal response for comparison |
| `retrievalContext` | `List<String>` | No | Documents/chunks retrieved by RAG pipeline |
| `context` | `List<String>` | No | Ground truth context (what should have been retrieved) |
| `toolCalls` | `List<ToolCall>` | No | Tools invoked by the agent during execution |
| `expectedToolCalls` | `List<ToolCall>` | No | Expected tool invocations for comparison |
| `reasoningTrace` | `List<ReasoningStep>` | No | Agent's chain-of-thought / planning steps |
| `latencyMs` | `long` | No | End-to-end execution time |
| `tokenUsage` | `TokenUsage` | No | Input/output/total token counts |
| `cost` | `BigDecimal` | No | Estimated cost of the interaction |
| `metadata` | `Map<String, Object>` | No | Arbitrary key-value pairs for filtering/grouping |

### 1.2 `ConversationTestCase` — Multi-Turn Evaluation

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `turns` | `List<AgentTestCase>` | Yes | Ordered list of conversation turns |
| `conversationId` | `String` | No | Identifier for the conversation session |
| `systemPrompt` | `String` | No | System prompt used across all turns |

### 1.3 Supporting Types

- **`ToolCall`** — `name: String`, `arguments: Map<String, Object>`, `result: String`, `durationMs: long`
- **`ReasoningStep`** — `type: enum(PLAN, THOUGHT, OBSERVATION, ACTION)`, `content: String`, `toolCall: ToolCall?`
- **`TokenUsage`** — `inputTokens: int`, `outputTokens: int`, `totalTokens: int`
- **`EvalScore`** — `value: double` (0.0–1.0), `threshold: double`, `passed: boolean`, `reason: String`, `metricName: String`

### 1.4 Builder Pattern

All test case types use immutable builders:

```java
var testCase = AgentTestCase.builder()
    .input("What is our refund policy?")
    .actualOutput(agent.run("What is our refund policy?"))
    .expectedOutput("Full refund within 30 days of purchase.")
    .retrievalContext(List.of(doc1, doc2))
    .build();
```

---

## 2. Metrics — Response Quality

Metrics that evaluate the quality of the agent's text output. All metrics implement the `EvalMetric` interface and return an `EvalScore` (0.0–1.0).

### 2.1 Answer Relevancy

- Measures whether the output is relevant to the input question
- Uses LLM-as-judge to generate synthetic questions from the output, then measures similarity to the original input
- Penalizes off-topic content and irrelevant information
- **Config:** threshold (default 0.7), strictMode (boolean)

### 2.2 Faithfulness

- Measures whether claims in the output are supported by the retrieval context
- Extracts individual claims from the output, then verifies each against the provided context
- Core metric for RAG pipelines — catches hallucinated facts that aren't grounded in source documents
- **Config:** threshold (default 0.7)

### 2.3 Hallucination Detection

- Measures whether the output contains fabricated information relative to provided context
- Different from faithfulness: specifically targets invented entities, false statistics, non-existent citations
- Can operate with or without ground truth context
- **Config:** threshold (default 0.5), contextRequired (boolean)

### 2.4 Correctness (G-Eval)

- General-purpose metric using the G-Eval framework
- Takes custom evaluation criteria as natural language instructions
- Compares actual output against expected output using LLM-as-judge with chain-of-thought
- The most flexible metric — can be configured for any evaluation dimension
- **Config:** criteria (String), evaluationSteps (List<String>), threshold (default 0.5)

### 2.5 Semantic Similarity

- Measures embedding-based cosine similarity between actual and expected output
- Does not require LLM-as-judge (uses embedding model only)
- Fast and deterministic — good for regression testing
- **Config:** threshold (default 0.7), embeddingModel (configurable)

### 2.6 Toxicity

- Detects harmful, offensive, or inappropriate content in the output
- Covers categories: hate speech, threats, sexual content, self-harm, profanity
- Uses LLM-as-judge with specialized safety rubric
- **Config:** threshold (default 0.5), categories (Set<ToxicityCategory>)

### 2.7 Bias Detection

- Detects biased content across dimensions: gender, race, religion, political, socioeconomic
- Evaluates whether the output unfairly favors or discriminates against any group
- **Config:** threshold (default 0.5), dimensions (Set<BiasDimension>)

### 2.8 Conciseness

- Measures whether the output is appropriately concise without losing essential information
- Penalizes verbosity, repetition, and filler content
- **Config:** threshold (default 0.5)

### 2.9 Coherence

- Measures logical flow, consistency, and readability of the output
- Checks for contradictions within the response
- **Config:** threshold (default 0.7)

---

## 3. Metrics — RAG-Specific

Metrics specifically designed for evaluating Retrieval-Augmented Generation pipelines.

### 3.1 Contextual Precision

- Measures whether the retrieved documents that are actually relevant are ranked higher than irrelevant ones
- Requires ground truth expected output to determine relevance
- Higher score = relevant documents appear earlier in the retrieval results
- **Config:** threshold (default 0.7)

### 3.2 Contextual Recall

- Measures whether all relevant information needed to produce the expected output was actually retrieved
- Aligns sentences in the expected output to sentences in the retrieval context
- Low score = the retrieval pipeline missed important source documents
- **Config:** threshold (default 0.7)

### 3.3 Contextual Relevancy

- Measures what proportion of the retrieved context is actually relevant to the input
- Penalizes retrieval of irrelevant/noisy documents that dilute useful context
- **Config:** threshold (default 0.7)

### 3.4 Retrieval Completeness

- Checks whether all ground truth context documents were retrieved
- Set-based comparison: were the right documents fetched?
- Supports both exact match and fuzzy/semantic matching modes
- **Config:** threshold (default 0.8), matchMode (EXACT | SEMANTIC)

---

## 4. Metrics — Agent-Specific

Metrics designed for evaluating autonomous agent behavior, including tool use and multi-step reasoning.

### 4.1 Tool Selection Accuracy

- Measures whether the agent selected the correct tools for the task
- Compares actual tool calls against expected tool calls (by name)
- Handles cases where tool order matters and where it doesn't
- **Config:** threshold (default 0.8), orderMatters (boolean, default false)

### 4.2 Tool Argument Correctness

- Measures whether the arguments passed to tools were correct
- Supports type-safe generic assertions: `assertToolArg(SearchTool.class, args -> args.query().contains("refund"))`
- Deep comparison of argument maps against expected values
- **Config:** threshold (default 0.8), strictMode (boolean — fail on extra args)

### 4.3 Tool Result Utilization

- Measures whether the agent actually used the results returned by tool calls in its final output
- Detects cases where the agent calls a tool but ignores its response
- **Config:** threshold (default 0.7)

### 4.4 Plan Quality

- Evaluates whether the agent's generated plan is logical, complete, and efficient
- Checks: does the plan address all aspects of the task? Are steps in a sensible order? Are there redundant steps?
- Requires reasoning trace capture from the agent
- **Config:** threshold (default 0.7)

### 4.5 Plan Adherence

- Evaluates whether the agent followed its own plan during execution
- Compares the planned steps against the actual execution trace
- Detects deviations, skipped steps, and unplanned actions
- **Config:** threshold (default 0.7)

### 4.6 Task Completion

- Binary + graded evaluation of whether the agent accomplished the stated goal
- LLM-as-judge determines if the final outcome satisfies the original task
- Can incorporate custom success criteria
- **Config:** threshold (default 0.5), successCriteria (String, optional natural language)

### 4.7 Trajectory Optimality

- Measures whether the agent took an efficient path to the solution
- Compares the number and type of steps taken against a reference optimal trajectory
- Penalizes unnecessary tool calls, redundant LLM invocations, and circular reasoning
- **Config:** threshold (default 0.5), maxSteps (int, optional)

### 4.8 Step-Level Error Localization

- Identifies which specific step in the agent's execution chain caused a failure
- Evaluates each reasoning step and tool call individually, flagging the first point of divergence
- Produces a diagnostic report pointing to the root cause
- **Config:** threshold (default 0.5)

---

## 5. Metrics — Conversation-Specific

Metrics for evaluating multi-turn agent interactions.

### 5.1 Conversation Coherence

- Measures whether responses maintain logical consistency across turns
- Detects self-contradictions between earlier and later responses
- **Config:** threshold (default 0.7)

### 5.2 Context Retention

- Measures whether the agent remembers and correctly uses information from earlier turns
- Tests: does the agent recall user preferences, prior answers, and established facts?
- **Config:** threshold (default 0.7)

### 5.3 Topic Drift Detection

- Measures whether the agent stays on topic across the conversation
- Detects when responses diverge from the user's original intent
- **Config:** threshold (default 0.5)

### 5.4 Conversation Resolution

- Evaluates whether the multi-turn conversation reached a satisfactory conclusion
- Determines if the user's original goal was ultimately accomplished
- **Config:** threshold (default 0.5), successCriteria (String)

---

## 6. Custom Metrics

Infrastructure for users to define their own evaluation metrics.

### 6.1 G-Eval Custom Metric Builder

- Define arbitrary evaluation criteria in natural language
- The library generates chain-of-thought evaluation prompts automatically
- Supports specifying which test case fields to include in evaluation

```java
var customMetric = GEval.builder()
    .name("TechnicalAccuracy")
    .criteria("Evaluate whether the response contains technically accurate Java code examples")
    .evaluationSteps(List.of(
        "Check if code snippets compile",
        "Verify API usage is correct for the stated Java version",
        "Check for deprecated method usage"
    ))
    .threshold(0.8)
    .build();
```

### 6.2 Deterministic Custom Metrics

- Implement the `EvalMetric` interface for rule-based checks
- No LLM calls required — pure Java logic
- Useful for: regex matching, schema validation, JSON structure checks, keyword presence

```java
public class ContainsDisclaimerMetric implements EvalMetric {
    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        boolean hasDisclaimer = testCase.getActualOutput()
            .contains("not financial advice");
        return EvalScore.of(hasDisclaimer ? 1.0 : 0.0, 0.5, "Disclaimer check");
    }
}
```

### 6.3 Composite Metrics

- Combine multiple metrics with weighted averaging
- Define pass/fail logic: ALL must pass, ANY must pass, or WEIGHTED average

```java
var composite = CompositeMetric.builder()
    .name("OverallQuality")
    .add(new AnswerRelevancy(), 0.4)
    .add(new Faithfulness(), 0.4)
    .add(new Conciseness(), 0.2)
    .strategy(CompositeStrategy.WEIGHTED_AVERAGE)
    .threshold(0.7)
    .build();
```

---

## 7. LLM-as-Judge Engine

The evaluation backbone — manages LLM calls used by metrics to score test cases.

### 7.1 Provider Support

- **OpenAI** — GPT-4o, GPT-4o-mini, GPT-4.1, etc.
- **Anthropic** — Claude Sonnet, Claude Haiku
- **Google** — Gemini Flash, Gemini Pro
- **Ollama** — Any locally hosted model (llama3, mistral, etc.)
- **Azure OpenAI** — Enterprise endpoint support
- **Amazon Bedrock** — AWS-native model access
- **Custom** — Implement `JudgeModel` interface for any HTTP-compatible LLM

### 7.2 Configuration

```java
AgentEval.configure()
    .judgeModel(JudgeModels.anthropic("claude-sonnet-4-20250514"))
    .embeddingModel(EmbeddingModels.openai("text-embedding-3-small"))
    .maxConcurrentJudgeCalls(4)
    .retryOnRateLimit(true, maxRetries: 3)
    .cachJudgeResults(true)  // avoid re-evaluating identical test cases
    .build();
```

### 7.3 Cost Management

- Token usage tracking per metric evaluation
- Estimated cost reporting per test run
- Budget limits — abort eval run if cost exceeds threshold
- Judge result caching to avoid redundant LLM calls across runs

### 7.4 Judge Prompt Templates

- Research-backed prompt templates for each metric (G-Eval, etc.)
- Templates are open and customizable — override any metric's judge prompt
- Chain-of-thought prompting with structured output parsing
- All templates available as resources for inspection and modification

---

## 8. JUnit 5 Integration

First-class integration with JUnit 5, the standard Java testing framework.

### 8.1 Extension

- `@ExtendWith(AgentEvalExtension.class)` on test classes
- Automatically collects results, manages lifecycle, generates reports
- Integrates with JUnit's test lifecycle hooks (beforeAll, afterAll, etc.)

### 8.2 Annotations

| Annotation | Target | Description |
|-----------|--------|-------------|
| `@AgentTest` | Method | Marks a test method as an agent evaluation |
| `@Metric` | Method | Applies a metric with configurable threshold |
| `@Metrics` | Method | Container for multiple `@Metric` annotations |
| `@DatasetSource` | Method | Loads test cases from a file (JSON/CSV) |
| `@GoldenSet` | Parameter | Injects golden dataset into parameterized test |
| `@JudgeModel` | Class/Method | Overrides the judge LLM for specific tests |
| `@EvalTimeout` | Method | Sets max time for evaluation to complete |
| `@Tag("eval")` | Class/Method | Standard JUnit tag for selective execution |

### 8.3 Assertion API

Fluent assertion API compatible with AssertJ style:

```java
AgentAssertions.assertThat(testCase)
    .meetsMetric(new AnswerRelevancy(0.7))
    .meetsMetric(new Faithfulness(0.8))
    .hasToolCalls()
    .toolCallCount(2)
    .calledTool("SearchOrders")
    .neverCalledTool("DeleteOrder")
    .outputContains("refund")
    .outputMatchesSchema(RefundResponse.class);
```

### 8.4 Parameterized Dataset Tests

```java
@ParameterizedTest
@DatasetSource("src/test/resources/golden-set.json")
@Metric(value = AnswerRelevancy.class, threshold = 0.7)
@Metric(value = Faithfulness.class, threshold = 0.8)
void evaluateAcrossDataset(AgentTestCase testCase) {
    var response = agent.run(testCase.getInput());
    testCase.setActualOutput(response);
}
```

### 8.5 Selective Execution

- Run only eval tests: `mvn test -Dgroups=eval`
- Run only fast (deterministic) metrics: `mvn test -Dgroups=eval-fast`
- Skip eval tests in quick builds: `mvn test -DexcludeGroups=eval`

---

## 9. Standalone Evaluation Runner

For batch evaluation outside of JUnit (scripting, notebooks, CI scripts).

### 9.1 Programmatic API

```java
var results = AgentEval.evaluate(
    dataset,
    List.of(
        new AnswerRelevancy(0.7),
        new Faithfulness(0.8),
        new ToolSelectionAccuracy(0.9)
    )
);

results.summary();           // Console summary
results.averageScore();      // Overall average
results.passRate();           // Percentage of test cases that passed all metrics
results.failedCases();        // Stream of failed test cases with reasons
```

### 9.2 Parallel Execution

- Evaluates multiple test cases concurrently using virtual threads
- Configurable concurrency limit (default: number of available processors)
- Thread-safe metric implementations

### 9.3 Progress Reporting

- Real-time console progress bar for long-running evaluations
- Callback interface for custom progress handling
- Estimated time remaining based on throughput

---

## 10. Dataset Management

Infrastructure for managing evaluation datasets.

### 10.1 Format Support

| Format | Read | Write | Notes |
|--------|------|-------|-------|
| JSON | Yes | Yes | Primary format, supports full test case model |
| CSV | Yes | Yes | Flat structure, good for simple input/output pairs |
| JSONL | Yes | Yes | Streaming-friendly, one test case per line |
| YAML | Yes | No | Human-readable alternative to JSON |

### 10.2 Golden Set Management

- Load from files, classpath resources, or URLs
- Filter and slice datasets by metadata tags
- Split into train/test for prompt optimization workflows
- Version tracking — associate datasets with commit hashes or release tags

### 10.3 Dataset Builder

```java
var dataset = EvalDataset.builder()
    .name("refund-queries-v2")
    .addCase(AgentTestCase.builder()
        .input("How do I get a refund?")
        .expectedOutput("You can request a refund within 30 days...")
        .metadata(Map.of("category", "refund", "difficulty", "easy"))
        .build())
    .addCase(...)
    .build();

dataset.save("src/test/resources/refund-queries-v2.json");
```

### 10.4 Synthetic Dataset Generation (P2)

- Generate test cases from existing documents using LLM
- Produce variations of existing golden set entries (paraphrasing, edge cases)
- Generate adversarial inputs designed to expose weaknesses

---

## 11. Reporting & Output

### 11.1 Console Report

- Summary table: metric name, average score, pass rate, min/max
- Failed test case details with LLM-judge explanations
- Color-coded output (pass=green, fail=red, warning=yellow)
- Execution time and cost summary

### 11.2 JUnit XML Report

- Standard JUnit XML format compatible with all CI systems
- Each metric evaluation maps to a test case in the report
- Jenkins, GitHub Actions, GitLab CI all render these natively

### 11.3 JSON Report

- Machine-readable full results export
- Contains all scores, explanations, metadata, and configuration
- Useful for custom dashboards or historical tracking

### 11.4 HTML Report (P2)

- Self-contained single-file HTML report
- Metric scorecards with distribution charts
- Drill-down into individual test cases
- Side-by-side comparison of actual vs expected output

### 11.5 Regression Comparison (P2)

- Compare two evaluation runs side-by-side
- Identify metrics that improved or degraded
- Highlight specific test cases that changed pass/fail status
- Useful for validating prompt changes or model swaps

---

## 12. Framework Integrations

Optional modules that auto-capture agent execution details from popular Java AI frameworks.

### 12.1 Spring AI Integration (`agentest-spring-ai`)

- Auto-capture `ChatClient` responses, tool calls, and token usage
- Intercept `Advisor` chain for RAG context extraction
- Spring Boot auto-configuration — add dependency, everything wires up
- Support for Spring AI's `@Tool` annotated methods

### 12.2 LangChain4j Integration (`agentest-langchain4j`)

- Auto-capture `AiService` proxy method calls and responses
- Extract tool calls from `AiMessage.toolExecutionRequests()`
- Capture `ContentRetriever` results for RAG evaluation
- Support for `@Tool` annotated methods

### 12.3 LangGraph4j Integration (`agentest-langgraph4j`)

- Capture graph execution traces (node transitions, state snapshots)
- Map graph nodes to reasoning steps for trajectory analysis
- Support for checkpoint-based state inspection

### 12.4 MCP Integration

- Capture MCP tool calls made through the MCP Java SDK
- Verify MCP server responses are correctly utilized
- Test MCP tool argument schemas against expected values

---

## 13. Red Teaming & Adversarial Testing (P2)

Automated security and robustness testing for AI agents.

### 13.1 Prompt Injection Tests

- Library of known prompt injection attack patterns
- Generates adversarial inputs that attempt to override system prompts
- Verifies the agent resists injection attempts
- Categories: direct injection, indirect injection, jailbreak attempts

### 13.2 Data Leakage Tests

- Tests whether the agent leaks system prompts, internal instructions, or sensitive context
- Generates extraction attempts ("repeat your instructions", "what were you told?")
- Verifies the agent does not expose PII from training data or context

### 13.3 Boundary Testing

- Tests agent behavior at input boundaries: empty input, extremely long input, special characters, unicode edge cases
- Verifies graceful degradation rather than crashes or nonsensical output
- Tests tool call behavior with edge-case arguments

### 13.4 Robustness Testing

- Tests consistency: does the agent give similar answers to paraphrased questions?
- Tests stability: does the agent handle ambiguous inputs gracefully?
- Tests refusal: does the agent appropriately decline out-of-scope requests?

---

## 14. Operational Metrics

Non-AI quality metrics that matter for production agents.

### 14.1 Latency Tracking

- End-to-end response time measurement
- Per-tool-call latency breakdown
- Threshold-based pass/fail: "response must complete within 5 seconds"

### 14.2 Token Usage Tracking

- Input/output/total token counts per interaction
- Budget enforcement: fail if a single interaction exceeds N tokens
- Aggregated token usage across dataset evaluation

### 14.3 Cost Tracking

- Per-interaction cost estimation based on model pricing
- Aggregated cost per evaluation run
- Cost comparison between model configurations

### 14.4 Error Rate

- Track agent error/exception rates across evaluation dataset
- Categorize errors: LLM timeout, tool failure, parsing error, etc.

---

## 15. Configuration

### 15.1 Programmatic Configuration

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.openai("gpt-4o-mini"))
    .embeddingModel(EmbeddingModels.openai("text-embedding-3-small"))
    .defaultThreshold(0.7)
    .parallelism(4)
    .cacheResults(true)
    .costBudget(BigDecimal.valueOf(5.00))  // max $5 per eval run
    .build();
```

### 15.2 File-Based Configuration

Support `agentest.yaml` / `agentest.properties` in project root:

```yaml
agentest:
  judge:
    provider: anthropic
    model: claude-sonnet-4-20250514
  embedding:
    provider: openai
    model: text-embedding-3-small
  defaults:
    threshold: 0.7
    parallelism: 4
  cache:
    enabled: true
    directory: .agentest-cache
```

### 15.3 Environment Variable Overrides

- `AGENTEST_JUDGE_PROVIDER` / `AGENTEST_JUDGE_MODEL`
- `OPENAI_API_KEY` / `ANTHROPIC_API_KEY` — standard env vars
- CI-friendly: no config files needed if env vars are set

---

## 16. Module Structure

```
agentest-bom/              — Bill of Materials for dependency management
agentest-core/             — Test case model, metric interfaces, scoring engine
agentest-metrics/          — All built-in metric implementations
agentest-judge/            — LLM-as-judge engine, provider integrations
agentest-embeddings/       — Embedding model integrations (for semantic metrics)
agentest-junit5/           — JUnit 5 extension, annotations, assertion API
agentest-datasets/         — Dataset loading, management, generation
agentest-reporting/        — Console, JUnit XML, JSON, HTML report generation
agentest-spring-ai/        — Spring AI auto-capture integration
agentest-langchain4j/      — LangChain4j auto-capture integration
agentest-langgraph4j/      — LangGraph4j graph execution capture
agentest-mcp/              — MCP Java SDK tool call capture
agentest-redteam/          — Adversarial testing and prompt injection library
```

---

## 17. Priority Tiers

### P0 — MVP (First Release)

- Core test case model (`AgentTestCase`, `ToolCall`, `EvalScore`)
- 5 response quality metrics: AnswerRelevancy, Faithfulness, Correctness (G-Eval), Hallucination, Toxicity
- 2 agent metrics: ToolSelectionAccuracy, TaskCompletion
- LLM-as-judge engine with OpenAI + Anthropic support
- JUnit 5 extension with `@AgentTest`, `@Metric` annotations
- Fluent assertion API
- JSON dataset loading
- Console + JUnit XML reporting
- Programmatic configuration

### P1 — Fast Follow

- Remaining response quality metrics: Bias, Conciseness, Coherence, SemanticSimilarity
- All RAG metrics: ContextualPrecision, ContextualRecall, ContextualRelevancy
- Remaining agent metrics: ToolArgumentCorrectness, PlanQuality, PlanAdherence, TrajectoryOptimality
- Conversation metrics: Coherence, ContextRetention
- Ollama / local model support for judge
- Spring AI integration module
- LangChain4j integration module
- CSV/JSONL dataset support
- JSON report output
- Cost tracking and budget limits
- File-based configuration (YAML)

### P2 — Growth

- LangGraph4j integration
- MCP integration
- HTML report generation
- Regression comparison (diff two runs)
- Synthetic dataset generation
- Red teaming / adversarial testing module
- Custom embedding model support
- Conversation metrics: TopicDrift, Resolution
- Step-level error localization
- Tool result utilization metric
- Parallel evaluation with virtual threads
- Progress bar and ETA for batch runs

### P3 — Ecosystem

- Maven plugin for `mvn agentest:evaluate`
- Gradle plugin
- GitHub Actions integration (post results as PR comments)
- Golden set versioning with Git integration
- Benchmark mode (compare multiple models/prompts across same dataset)
- Multi-model judge (evaluate with multiple judges, take consensus)
- Snapshot testing (save and compare outputs across releases)
- IntelliJ IDEA plugin (run eval tests with inline metric results)

---

## 18. Non-Goals

Things this library explicitly does **not** aim to do:

- **Not an observability platform.** No dashboards, no production monitoring, no trace storage. Use Langfuse, LangSmith, or OpenTelemetry for that.
- **Not a cloud service.** No accounts, no SaaS, no data leaves your machine. Optional future: export to external platforms.
- **Not a framework for building agents.** Use Spring AI, LangChain4j, or LangGraph4j for that. This library tests what you've already built.
- **Not a benchmark suite.** Spring AI Bench benchmarks coding agents on Spring tasks. This library tests your custom agents against your custom criteria.
- **Not a training/fine-tuning tool.** This evaluates, it doesn't train. Evaluation results can inform fine-tuning decisions, but that's a different tool.

---

## 19. Technical Constraints

- **Java 21+ baseline** — leverages records, sealed interfaces, pattern matching, virtual threads
- **Zero required runtime dependencies** on Spring or LangChain4j — core is standalone
- **JUnit 5.10+** for extension API
- **Jackson** for JSON serialization (aligned with MCP Java SDK choice)
- **SLF4J** for logging (no specific implementation required)
- **Java HTTP Client** (built-in `java.net.http`) for LLM API calls — no OkHttp/Apache HttpClient dependency
- **No reflection-heavy magic** — prefer explicit configuration over classpath scanning
