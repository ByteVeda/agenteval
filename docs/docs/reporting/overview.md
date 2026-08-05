---
sidebar_position: 1
---

# Reporting

AgentEval produces evaluation reports in multiple formats.

## Console Report

Human-readable summary printed to stdout after each evaluation run:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 AgentEval Results — RefundAgentTest
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 Test Cases : 24
 Pass Rate  : 91.7% (22/24)
 Avg Score  : 0.84
 Duration   : 8.3s
 Judge Cost : ~$0.02 (OpenAI gpt-4o-mini)

 Metric Breakdown:
  AnswerRelevancy     0.88  Pass: 23/24
  Faithfulness        0.80  Pass: 22/24

 Failed Cases:
  [FAIL] "Can I return software after installation?"
    Faithfulness: 0.42 — Response claimed refunds are possible; context says non-refundable.
  [FAIL] "What is the international return policy?"
    AnswerRelevancy: 0.31 — Response discussed domestic policy only.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## JUnit XML Report

Standard JUnit XML output — compatible with Jenkins, GitHub Actions, GitLab CI, and any CI/CD system that reads Surefire reports:

```xml
<testsuite name="RefundAgentTest" tests="24" failures="2" time="8.3">
  <testcase name="evaluateGoldenSet[1]" time="0.34">
    <!-- passes produce no additional output -->
  </testcase>
  <testcase name="evaluateGoldenSet[7]" time="0.41">
    <failure message="Faithfulness: 0.42 (threshold: 0.80)">
      Reason: Response claimed refunds are possible; context says non-refundable.
    </failure>
  </testcase>
</testsuite>
```

This is automatically produced by Maven Surefire when running `mvn test`.

## JSON Report

Machine-readable full results export:

```java
EvalResults results = AgentEval.evaluate(dataset, metrics);
results.exportJson("target/agenteval-report.json");
```

Output format:

```json
{
  "runId": "run-2026-03-13T10:30:00",
  "summary": {
    "totalCases": 24,
    "passed": 22,
    "passRate": 0.917,
    "averageScore": 0.84,
    "durationMs": 8300,
    "estimatedCostUsd": 0.02
  },
  "metrics": {
    "AnswerRelevancy": { "average": 0.88, "passed": 23 },
    "Faithfulness":    { "average": 0.80, "passed": 22 }
  },
  "cases": [
    {
      "input": "What is the refund window?",
      "scores": {
        "AnswerRelevancy": { "value": 0.93, "passed": true },
        "Faithfulness":    { "value": 0.97, "passed": true }
      }
    }
  ]
}
```

## HTML Report (P2)

Self-contained single-file HTML report with metric scorecards and distribution charts:

```java
results.exportHtml("target/agenteval-report.html");
```

Features:
- Metric score distributions
- Failed case drill-down with LLM judge reasoning
- Side-by-side comparison of two evaluation runs

## Programmatic Access

```java
EvalResults results = AgentEval.evaluate(dataset, metrics);

results.passRate();            // 0.917
results.averageScore();        // 0.84
results.failedCases();         // Stream<FailedCase>
results.scoreFor("Faithfulness");  // EvalMetricSummary
results.judgeTokenUsage();     // total tokens used
results.estimatedCost();       // estimated USD
```
