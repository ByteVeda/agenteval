---
sidebar_position: 2
---

# ContextRetention

Measures whether the agent remembers and correctly uses information from earlier turns in the conversation.

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
        .input("My name is Alex and I prefer email notifications.")
        .actualOutput("Got it, Alex! I'll use email for all notifications.")
        .build())
    .turn(AgentTestCase.builder()
        .input("Order something for me.")
        .actualOutput("Your order has been placed. We'll send a text notification.")  // forgot preference
        .build())
    .build();

EvalScore score = new ContextRetention(0.7).evaluate(conversation);
// score.value()  → 0.2
// score.passed() → false
// score.reason() → "Agent forgot user's stated preference for email notifications.
//                   Turn 2 sent a text notification instead."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = ContextRetention.class, threshold = 0.8)
void agentShouldRememberUserPreferences() {
    var conversation = ConversationTestCase.builder()
        .turns(runConversation())
        .build();

    AgentAssertions.assertThat(conversation)
        .meetsMetric(new ContextRetention(0.8));
}
```
