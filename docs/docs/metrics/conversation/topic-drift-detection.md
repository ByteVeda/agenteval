---
sidebar_position: 3
---

# TopicDriftDetection

Measures whether the agent stays on topic across a multi-turn conversation. Detects when the agent's responses diverge from the user's original intent.

| Property | Value |
|---|---|
| Default threshold | 0.5 |
| Requires LLM judge | Yes |
| Required fields | `ConversationTestCase` with multiple turns |
| Available since | P1 |

## Example

```java
var conversation = ConversationTestCase.builder()
    .turn(AgentTestCase.builder()
        .input("Help me debug this Java NullPointerException.")
        .actualOutput("Sure! Please share the stack trace.")
        .build())
    .turn(AgentTestCase.builder()
        .input("Here it is: at MyClass.process(MyClass.java:42)")
        .actualOutput("I see! By the way, have you considered switching to Kotlin? "
                    + "It has null safety built in and is really popular these days.")
        .build())
    .build();

EvalScore score = new TopicDriftDetection(0.5).evaluate(conversation);
// score.value()  → 0.3
// score.passed() → false
// score.reason() → "Agent drifted from debugging the NullPointerException to promoting Kotlin."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = TopicDriftDetection.class, threshold = 0.6)
void agentShouldStayOnTopic() {
    var conversation = ConversationTestCase.builder()
        .turns(runConversation())
        .build();

    AgentAssertions.assertThat(conversation)
        .meetsMetric(new TopicDriftDetection(0.6));
}
```
