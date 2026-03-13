---
sidebar_position: 1
---

# JUnit 5 Integration

AgentEval integrates with JUnit 5 as a standard extension. Evaluations run as test methods — no new tooling, no separate test runner.

## Setup

```xml
<dependency>
  <groupId>com.agenteval</groupId>
  <artifactId>agenteval-junit5</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

Enable the extension on your test class:

```java
@ExtendWith(AgentEvalExtension.class)
class MyAgentTest {
    // ...
}
```

## Basic Test

```java
@ExtendWith(AgentEvalExtension.class)
class RefundAgentTest {

    private final RefundAgent agent = new RefundAgent();

    @Test
    @AgentTest
    @Metric(value = AnswerRelevancy.class, threshold = 0.7)
    @Metric(value = Faithfulness.class, threshold = 0.8)
    void shouldAnswerRefundQuestions() {
        var testCase = AgentTestCase.builder()
            .input("What is the refund policy?")
            .actualOutput(agent.run("What is the refund policy?"))
            .retrievalContext(retrievedDocs)
            .build();

        AgentAssertions.assertThat(testCase)
            .meetsMetric(new AnswerRelevancy(0.7))
            .meetsMetric(new Faithfulness(0.8));
    }
}
```

## Test Output

Evaluation results appear in the JUnit test output:

```
[AgentEval] RefundAgentTest#shouldAnswerRefundQuestions
  AnswerRelevancy  0.87  PASS  (threshold: 0.70)
  Faithfulness     0.91  PASS  (threshold: 0.80)

[AgentEval] PASSED  2/2 metrics  avg: 0.89  time: 1.2s
```

## Class-Level Configuration

Apply annotations at the class level to set defaults for all tests:

```java
@ExtendWith(AgentEvalExtension.class)
@JudgeModel(provider = "anthropic", model = "claude-haiku-4-5-20251001")
@Tag("eval")
class CustomerSupportEvalTest {

    @Test
    @AgentTest
    @Metric(value = AnswerRelevancy.class, threshold = 0.7)
    void test1() { ... }

    @Test
    @AgentTest
    @JudgeModel(provider = "openai", model = "gpt-4o")  // overrides class-level
    @Metric(value = Faithfulness.class, threshold = 0.8)
    void highStakesTest() { ... }
}
```

## Selective Execution

Use tags to run evaluations separately from unit tests:

```bash
# Run only evaluation tests
mvn test -Dgroups=eval

# Skip evaluation tests (fast build)
mvn test -DexcludeGroups=eval
```

## Reference

- [Annotations](./annotations) — full annotation reference
- [Assertions](./assertions) — `AgentAssertions` fluent API
- [Parameterized Tests](./parameterized-tests) — batch evaluation from datasets
