# Changelog

All notable changes to AgentEval are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] — 2026-04-24

### Added
- Six new modules extending the evaluation surface beyond core metrics:
  - `agenteval-contracts` — contract testing for agent responses
  - `agenteval-statistics` — statistical analysis of eval runs
  - `agenteval-chaos` — chaos engineering (fault injection, resilience evaluation)
  - `agenteval-replay` — deterministic replay of captured agent interactions
  - `agenteval-mutation` — mutation testing for evaluation robustness
  - `agenteval-fingerprint` — capability fingerprinting of models under test
- Cost metrics and root-cause analysis helpers
- Smoke test coverage for `agenteval-langchain4j` (11 tests) and `agenteval-spring-ai` (12 tests)
- Chaos module tests for `LatencyInjector`, `SchemaMutationInjector`, and `ResilienceEvaluator` (22 tests)
- `AUDIT.md` — a full audit report of the library with severity-ranked findings

### Changed
- Gradle root build removed; Gradle is now scoped to `agenteval-gradle-plugin` only
  (the module that must be Gradle-native for `publishPlugins` to the Gradle Plugin
  Portal). Maven is the authoritative build for all 22 other modules, so the two
  module lists can no longer drift
- `DatasetVersionerTest` no longer relies on `Thread.sleep` for timestamp ordering;
  it explicitly sets file modification times for deterministic assertions
- `agenteval-bom` now documents why build-tooling modules are intentionally omitted
- Bumped dependency versions via Dependabot: Jackson (to 2.21.x via BOM),
  Logback 1.5.32, Spring AI 1.1.4, LangGraph4j (latest), Mockito 5.23.0, and
  GitHub Actions (`actions/checkout@v6`, `actions/setup-node@v6`,
  `actions/upload-artifact@v7`, `actions/upload-pages-artifact@v5`,
  `actions/deploy-pages@v5`, `actions/stale@v10`, `dorny/test-reporter@v3`)
- Test fixtures now use neutral API key strings (`fake-key-for-tests`,
  `fake-ant-key-for-tests`) instead of `sk-test` / `sk-ant-test` so credential
  scanners do not match on shape

### Deprecated
- `org.byteveda.agenteval.metrics.llm.PromptTemplate` — use
  `org.byteveda.agenteval.core.template.PromptTemplate` instead.
  Scheduled for removal in `1.0.0`.
- `SemanticSimilarityMetric.cosineSimilarity(List, List)` — use
  `VectorMath.cosineSimilarity` instead. Scheduled for removal in `1.0.0`.

### Fixed
- MDX parsing errors in the documentation site, plus a PR build check to
  catch future regressions (#68, #69)
- `JunitXmlReporter` now configures `DocumentBuilderFactory` with full XXE
  defenses (`disallow-doctype-decl`, external entity/DTD disabling,
  `setXIncludeAware(false)`, `setExpandEntityReferences(false)`)
- `YamlDatasetLoader` now caps alias expansion (≤50), nesting depth (≤50),
  and code points (≤3 MiB) and disallows duplicate/recursive keys — defense
  in depth on top of SnakeYAML 2.x's default `SafeConstructor`
- SpotBugs suppressions narrowed from broad regex patterns
  (`~...datasets.json.Json.*`, `~...datasets.version..*`) to explicit
  `<Or><Class .../></Or>` enumerations so new classes in those packages
  surface genuine findings instead of being blanket-suppressed

### Security
- XXE hardening in `JunitXmlReporter` (`agenteval-reporting`)
- YAML resource-exhaustion hardening in `YamlDatasetLoader` (`agenteval-datasets`)
- `.gitignore` now covers common secret patterns (`.env*`, `*.jks`, `*.keystore`,
  `*.p12`, `credentials.json`)

## [0.1.0] — 2026-03-29

Initial public release. Includes:

- **Core evaluation engine** — `AgentTestCase`, `EvalMetric` SPI, `EvalScore`
  (records normalized to `[0.0, 1.0]` with threshold validation), virtual-thread
  parallel evaluation with bounded concurrency, progress callbacks with ETA
- **23 evaluation metrics** across five categories:
  - Response (9): `AnswerRelevancy`, `Faithfulness`, `Hallucination`,
    `Correctness`, `SemanticSimilarity`, `Coherence`, `Conciseness`,
    `Toxicity`, `Bias`
  - RAG (3): `ContextualRelevancy`, `ContextualPrecision`, `ContextualRecall`
  - Agent (9): `TaskCompletion`, `ToolSelectionAccuracy`,
    `ToolArgumentCorrectness`, `ToolResultUtilization`, `PlanQuality`,
    `PlanAdherence`, `RetrievalCompleteness`, `StepLevelErrorLocalization`,
    `TrajectoryOptimality`
  - Conversation (4): `ConversationCoherence`, `ContextRetention`,
    `TopicDriftDetection`, `ConversationResolution`
  - Utility (2): `CostNormalized`, `LatencyNormalized`
- **7 LLM-as-judge providers** — OpenAI, Anthropic, Ollama, Google Gemini,
  Azure OpenAI, Amazon Bedrock (SigV4-signed), and a Custom HTTP provider
  compatible with vLLM / LiteLLM / LocalAI
- **Multi-model judge consensus** — `ConsensusStrategy` with `MAJORITY`,
  `AVERAGE`, `WEIGHTED_AVERAGE`, and `UNANIMOUS` modes; virtual-thread fan-out
- **JUnit 5 integration** — `@AgentTest`, `@Metric`, custom assertions
- **Datasets** — JSON / JSONL / CSV / YAML loaders, synthetic dataset
  generation (from-documents, variations, adversarial), golden-set versioning
  with git metadata via `DatasetVersioner`
- **Reporting** — Console, JUnit XML, JSON, HTML (single-file, self-contained),
  Markdown (GFM), snapshot testing with baseline/compare/update modes,
  regression comparison, benchmark mode with variant comparison
- **Framework integrations** — Spring AI (auto-configuration + advisor
  interceptor), LangChain4j (chat model capture + content retriever capture),
  LangGraph4j (graph execution capture), MCP (tool call capture)
- **Red teaming** — `RedTeamSuite`, `AttackTemplateLibrary` with 20 attack
  templates, `AttackEvaluator`
- **Build plugins** — Maven plugin (`EvaluateMojo` @ `verify` phase),
  Gradle plugin (published to Gradle Plugin Portal), GitHub Actions composite
  action with PR commenter (marker-based update)
- **IntelliJ IDEA plugin** — `AgentEvalToolWindow`, metric gutter icons,
  live report file watcher
- **Configuration** — programmatic (`AgentEvalConfig.builder()`) and
  file-based (`agenteval.yaml`); environment variables
  `AGENTEVAL_JUDGE_PROVIDER`, `AGENTEVAL_JUDGE_MODEL`

[Unreleased]: https://github.com/ByteVeda/agenteval/compare/0.2.0...HEAD
[0.2.0]: https://github.com/ByteVeda/agenteval/releases/tag/0.2.0
[0.1.0]: https://github.com/ByteVeda/agenteval/releases/tag/0.1.0
