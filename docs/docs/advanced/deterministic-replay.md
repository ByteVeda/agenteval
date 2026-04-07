---
sidebar_position: 7
---

# Deterministic Replay

The `agenteval-replay` module records agent and judge interactions during an evaluation run, then replays them deterministically. This enables zero-cost regression testing and determinism verification without calling any live LLM.

## Dependency

```xml
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-replay</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

## Recording

Use `ReplaySuite.record()` to capture all agent and judge interactions during an evaluation run. The recording is persisted to disk via a `RecordingStore`:

```java
var store = new RecordingStore(Path.of("src/test/resources/recordings"));

var suite = ReplaySuite.builder()
    .agent(myAgent::call)
    .judgeModel(openAiJudge)
    .metric(answerRelevancy)
    .testCase(AgentTestCase.builder()
        .input("What is the capital of France?")
        .expectedOutput("Paris")
        .build())
    .recordingStore(store)
    .recordingName("baseline-v1")
    .build();

Recording recording = suite.record();
```

The `record()` method:

1. Wraps the agent in a `RecordingAgentWrapper` that captures each input/output pair
2. Wraps the judge in a `RecordingJudgeModel` that captures each prompt/response pair
3. Runs all test cases through the agent and evaluates each metric
4. Saves all interactions to the `RecordingStore`

## Replaying

Use `ReplaySuite.replay()` to load a saved recording, re-run the evaluation with live calls, and then replay from the recording to verify that metric scores match:

```java
ReplayVerification verification = suite.replay();

// Did all metric scores match between live and replayed runs?
assert verification.allMatch();

// Inspect any mismatches
for (String mismatch : verification.mismatches()) {
    System.out.println("Mismatch: " + mismatch);
}
```

The `ReplayVerification` record contains:

| Field | Description |
|---|---|
| `recordingName()` | Name of the recording that was replayed |
| `originalScores()` | Metric scores from the live run |
| `replayedScores()` | Metric scores from the replay |
| `allMatch()` | `true` if all replayed scores match the originals exactly |
| `mismatches()` | Descriptions of any score differences |

## RecordingStore Persistence

`RecordingStore` persists recordings as JSON files named `<name>.recording.json` in a configured directory:

```java
var store = new RecordingStore(Path.of("recordings"));

// Save
store.save(recording);

// Load
Optional<Recording> loaded = store.load("baseline-v1");

// Check existence
boolean exists = store.exists("baseline-v1");

// Delete
boolean deleted = store.delete("baseline-v1");
```

Recording names are validated against the pattern `[a-zA-Z0-9][a-zA-Z0-9_.-]*` and must not contain `..` to prevent path traversal. Invalid names throw `IllegalArgumentException`. I/O failures throw `RecordingStore.RecordingIOException`.

## Manual Recording and Replay with Decorators

You can also use the recording decorators directly without `ReplaySuite`. This is useful when you want to integrate recording into an existing evaluation pipeline.

### RecordingJudgeModel

Wraps any `JudgeModel` and captures all interactions:

```java
JudgeModel delegate = new OpenAiJudgeModel(config);
var recordingJudge = new RecordingJudgeModel(delegate);

// Use recordingJudge as a normal judge -- all calls are captured
JudgeResponse response = recordingJudge.judge(prompt);

// Retrieve captured interactions
List<RecordedInteraction> interactions = recordingJudge.getInteractions();

// Clear captured data
recordingJudge.clear();

// Check count
int count = recordingJudge.size();
```

`RecordingJudgeModel` is thread-safe, using a `CopyOnWriteArrayList` internally.

## Use Cases

**Zero-cost regression testing:** Record a baseline evaluation once (paying for LLM calls), then replay it in CI indefinitely at no cost. If the replayed scores diverge from the recording, you know something changed.

**Determinism verification:** Run `replay()` to compare live scores against recorded scores. If the LLM judge returns different scores for the same prompts, the mismatches will surface non-determinism.
