---
sidebar_position: 2
---

# TaskCompletion

Evaluates whether the agent successfully accomplished the user's goal. Supports both binary (did/didn't complete) and graded evaluation.

| Property | Value |
|---|---|
| Default threshold | 0.5 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput` |
| Available since | P0 |

## How It Works

An LLM judge evaluates whether the agent's output satisfies the user's stated task. You can optionally provide explicit success criteria to make the evaluation more precise.

## Example

```java
var testCase = AgentTestCase.builder()
    .input("Book a flight from New York to London on March 20th for 2 adults.")
    .actualOutput("I've searched for available flights on March 20th from JFK to LHR. "
                + "The best option is British Airways BA178 departing at 10:30 PM, "
                + "arriving March 21st at 10:30 AM. Total cost: $1,840 for 2 adults. "
                + "Please confirm to complete the booking.")
    .build();

EvalScore score = new TaskCompletion(0.5).evaluate(testCase);
// score.value()  → 0.72
// score.passed() → true
// score.reason() → "Agent found a suitable flight and provided complete details.
//                   Task partially complete — awaiting user confirmation."
```

### With explicit success criteria

```java
var testCase = AgentTestCase.builder()
    .input("Summarize the Q3 earnings report.")
    .actualOutput(agent.run("Summarize the Q3 earnings report."))
    .build();

var metric = TaskCompletion.builder()
    .threshold(0.7)
    .successCriteria("Response must include revenue, profit margin, and year-over-year growth")
    .build();

EvalScore score = metric.evaluate(testCase);
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = TaskCompletion.class, threshold = 0.7)
void agentShouldCompleteTask() {
    var testCase = AgentTestCase.builder()
        .input(complexTask)
        .actualOutput(agent.run(complexTask))
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new TaskCompletion(0.7));
}
```

## Configuration

| Option | Type | Default | Description |
|---|---|---|---|
| `threshold` | `double` | `0.5` | Minimum score to pass |
| `successCriteria` | `String` | null | Explicit criteria for success evaluation |
