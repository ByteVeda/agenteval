---
sidebar_position: 1
---

# Datasets

AgentEval provides tools for loading, managing, and building golden sets for evaluation.

## Loading a Dataset

```java
// From JSON file
EvalDataset dataset = EvalDataset.load("src/test/resources/golden-set.json");

// From classpath
EvalDataset dataset = EvalDataset.loadFromClasspath("golden-set.json");

// From URL
EvalDataset dataset = EvalDataset.loadFromUrl("https://example.com/dataset.json");
```

## Building a Dataset Programmatically

```java
EvalDataset dataset = EvalDataset.builder()
    .name("refund-queries-v2")
    .description("Customer support queries about refund policies")
    .addCase(AgentTestCase.builder()
        .input("What is the refund window?")
        .expectedOutput("30 days from purchase date.")
        .retrievalContext(List.of("Full refund within 30 days of purchase."))
        .metadata(Map.of("category", "policy", "difficulty", "easy"))
        .build())
    .addCase(AgentTestCase.builder()
        .input("Can I return a digital download?")
        .expectedOutput("Digital products are non-refundable.")
        .retrievalContext(List.of("Digital products cannot be returned."))
        .metadata(Map.of("category", "policy", "difficulty", "easy"))
        .build())
    .build();

dataset.save("src/test/resources/refund-queries-v2.json");
```

## Filtering Datasets

```java
// Filter by metadata
EvalDataset refundOnly = dataset.filter(
    tc -> "refund".equals(tc.getMetadata().get("category"))
);

EvalDataset hardCases = dataset.filter(
    tc -> "hard".equals(tc.getMetadata().get("difficulty"))
);

// Slice
EvalDataset first50 = dataset.slice(0, 50);
```

## Dataset Statistics

```java
dataset.size();                    // total cases
dataset.categories();              // distinct metadata values
```

## Reference

- [Formats](./formats) — JSON, CSV, JSONL, YAML details
- [Golden Sets](./golden-sets) — version control and management
