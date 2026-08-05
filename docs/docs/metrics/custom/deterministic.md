---
sidebar_position: 2
---

# Deterministic Custom Metrics

Implement `EvalMetric` with pure Java logic — no LLM calls, fast and reproducible. Use for regex matching, JSON schema validation, keyword presence, and other rule-based checks.

## Implementing EvalMetric

```java
public class JsonValidityMetric implements EvalMetric {

    private final double threshold;

    public JsonValidityMetric(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        String output = testCase.getActualOutput();
        try {
            new ObjectMapper().readTree(output);
            return EvalScore.pass(name(), 1.0, threshold, "Output is valid JSON.");
        } catch (JsonProcessingException e) {
            return EvalScore.fail(name(), 0.0, threshold,
                "Output is not valid JSON: " + e.getOriginalMessage());
        }
    }

    @Override
    public String name() {
        return "JsonValidity";
    }
}
```

## More Examples

### Keyword presence check

```java
public class KeywordPresenceMetric implements EvalMetric {

    private final List<String> requiredKeywords;
    private final double threshold;

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        String output = testCase.getActualOutput().toLowerCase();
        long matched = requiredKeywords.stream()
            .filter(kw -> output.contains(kw.toLowerCase()))
            .count();
        double score = (double) matched / requiredKeywords.size();
        String reason = String.format("Found %d/%d required keywords.", matched, requiredKeywords.size());
        return EvalScore.of(name(), score, threshold, reason);
    }

    @Override
    public String name() { return "KeywordPresence"; }
}
```

### Response length check

```java
public class ResponseLengthMetric implements EvalMetric {

    private final int maxWords;
    private final double threshold;

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        int wordCount = testCase.getActualOutput().split("\\s+").length;
        double score = wordCount <= maxWords ? 1.0 : (double) maxWords / wordCount;
        return EvalScore.of(name(), score, threshold,
            String.format("Response has %d words (max: %d).", wordCount, maxWords));
    }

    @Override
    public String name() { return "ResponseLength"; }
}
```

## Usage

```java
AgentAssertions.assertThat(testCase)
    .meetsMetric(new JsonValidityMetric(1.0))
    .meetsMetric(new KeywordPresenceMetric(List.of("refund", "30 days"), 0.8))
    .meetsMetric(new ResponseLengthMetric(200, 0.9));
```
