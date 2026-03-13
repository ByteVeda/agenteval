---
sidebar_position: 4
---

# ConversationResolution

Evaluates whether a multi-turn conversation reached a satisfactory conclusion — did the user's original goal get accomplished?

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
        .input("I want to cancel my subscription.")
        .actualOutput("I can help with that. Can you confirm your account email?")
        .build())
    .turn(AgentTestCase.builder()
        .input("user@example.com")
        .actualOutput("Your subscription has been cancelled. "
                    + "You'll have access until March 31st, 2026.")
        .build())
    .build();

EvalScore score = new ConversationResolution(0.5).evaluate(conversation);
// score.value()  → 0.92
// score.passed() → true
// score.reason() → "User's goal (cancel subscription) was fully accomplished with
//                   clear confirmation and end date provided."
```

### With success criteria

```java
var metric = ConversationResolution.builder()
    .threshold(0.7)
    .successCriteria("Subscription must be cancelled and user must receive confirmation with end date")
    .build();
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = ConversationResolution.class, threshold = 0.7)
void conversationShouldReachResolution() {
    var conversation = ConversationTestCase.builder()
        .turns(runConversation(userGoal))
        .build();

    AgentAssertions.assertThat(conversation)
        .meetsMetric(new ConversationResolution(0.7));
}
```
