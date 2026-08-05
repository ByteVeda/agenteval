---
sidebar_position: 1
---

# ConversationCoherence

Measures whether responses maintain logical consistency across turns in a multi-turn conversation. Detects self-contradictions between earlier and later responses.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `ConversationTestCase` with multiple turns |
| Available since | P1 |

## Example

```java
var conversation = ConversationTestCase.builder()
    .turn(AgentTestCase.builder()
        .input("What is your return policy?")
        .actualOutput("We offer a 30-day return window.")
        .build())
    .turn(AgentTestCase.builder()
        .input("Can I return something I bought 3 weeks ago?")
        .actualOutput("I'm sorry, our return window is only 14 days. "
                    + "Your item is no longer eligible.")
        .build())
    .build();

EvalScore score = new ConversationCoherence(0.7).evaluate(conversation);
// score.value()  → 0.05
// score.passed() → false
// score.reason() → "Contradiction: Turn 1 states 30-day return window; Turn 2 says 14 days."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = ConversationCoherence.class, threshold = 0.7)
void conversationShouldBeConsistent() {
    var conversation = ConversationTestCase.builder()
        .turns(runMultiTurnSession(queries))
        .build();

    AgentAssertions.assertThat(conversation)
        .meetsMetric(new ConversationCoherence(0.7));
}
```
