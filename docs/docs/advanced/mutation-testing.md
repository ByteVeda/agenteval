---
sidebar_position: 8
---

# Mutation Testing

The `agenteval-mutation` module tests whether your evaluation metrics are sensitive enough to detect meaningful changes in your agent's system prompt. It applies targeted mutations to the prompt, runs the agent, and checks whether the metrics catch the degradation. Undetected mutations signal blind spots in your evaluation suite.

## Dependency

```xml
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-mutation</artifactId>
  <version>0.2.0</version>
  <scope>test</scope>
</dependency>
```

## MutationSuite Usage

`MutationSuite` is the main entry point. Configure it with a system prompt, an `AgentFactory`, mutators, metrics, and test inputs:

```java
var result = MutationSuite.builder()
    .systemPrompt("You are a helpful assistant that always cites sources...")
    .agentFactory(prompt -> input -> myLlmClient.call(prompt, input))
    .addMutator(new RemoveInstructionMutator())
    .addMutator(new WeakenConstraintMutator())
    .addMetric(new AnswerRelevancy(judge, 0.7))
    .addTestInput("What is the capital of France?")
    .addTestInput("Explain quantum computing")
    .build()
    .run();
```

To add all five built-in mutators at once:

```java
MutationSuite.builder()
    .systemPrompt(systemPrompt)
    .agentFactory(factory)
    .addAllBuiltInMutators()
    .addMetric(metric)
    .addTestInput("test query")
    .build()
    .run();
```

For each mutator, the suite:

1. Applies the mutation to the system prompt
2. Creates a new agent instance via `AgentFactory`
3. Runs the agent against all test inputs
4. Evaluates each metric on the agent's output
5. Reports whether any metric score fell below its threshold (detected)

## Built-in Mutators

Five mutators are provided out of the box. The `Mutator` interface is sealed, permitting these five plus `PluggableMutator` for custom implementations.

| Mutator | What It Does |
|---|---|
| `RemoveInstructionMutator` | Removes an instruction line from the system prompt |
| `WeakenConstraintMutator` | Weakens constraint language (e.g., "must" to "may") |
| `SwapToolDescriptionMutator` | Swaps or alters tool descriptions in the prompt |
| `InjectContradictionMutator` | Injects a contradictory instruction |
| `RemoveSafetyInstructionMutator` | Removes safety-related instructions |

## Custom Mutators

Use `PluggableMutator` to define custom mutation logic without implementing the sealed interface:

```java
var customMutator = new PluggableMutator(
    "remove-all-examples",
    prompt -> prompt.replaceAll("(?m)^Example:.*$", "")
);

MutationSuite.builder()
    .systemPrompt(systemPrompt)
    .agentFactory(factory)
    .addMutator(customMutator)
    .addMetric(metric)
    .addTestInput("test query")
    .build()
    .run();
```

`PluggableMutator` is a record that takes a name and a `UnaryOperator<String>` that transforms the system prompt.

## AgentFactory

`AgentFactory` is a functional interface that creates an agent function from a system prompt. This abstraction lets the mutation suite swap system prompts while reusing the same agent execution logic:

```java
@FunctionalInterface
public interface AgentFactory {
    Function<String, String> create(String systemPrompt);
}
```

Example implementation:

```java
AgentFactory factory = systemPrompt -> userInput -> {
    return myLlmClient.chat(systemPrompt, userInput);
};
```

## MutationSuiteResult Interpretation

The `run()` method returns a `MutationSuiteResult`:

```java
MutationSuiteResult result = suite.run();

// Detection rate: fraction of mutations caught (0.0 to 1.0)
double rate = result.detectionRate();

// Counts
int total = result.totalMutations();
int detected = result.detectedCount();

// Mutations the evaluation missed
List<MutationResult> missed = result.undetectedMutations();
for (MutationResult mr : missed) {
    System.out.printf("UNDETECTED: %s%n", mr.mutatorName());
}
```

Each `MutationResult` contains:

- `mutatorName()` -- name of the mutator that was applied
- `originalPrompt()` -- the original system prompt
- `mutatedPrompt()` -- the mutated system prompt
- `scores()` -- list of `EvalScore` results across all test inputs and metrics
- `detected()` -- `true` if any metric score fell below its threshold

## Detection Threshold

A mutation is considered "detected" if **any** metric on **any** test input produces a score below the metric's configured threshold (i.e., `score.passed()` returns `false`). A high detection rate means your evaluation metrics are sensitive to prompt changes. A low detection rate suggests your metrics may have blind spots or your prompt instructions may be redundant.

As a rule of thumb:

- **80%+ detection rate** -- strong evaluation coverage
- **50--80% detection rate** -- acceptable but review undetected mutations
- **Under 50% detection rate** -- evaluation metrics need improvement
