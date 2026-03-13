---
sidebar_position: 2
---

# Dataset Formats

AgentEval supports JSON, CSV, JSONL, and YAML formats.

## JSON (Primary)

Full support for all `AgentTestCase` fields:

```json
[
  {
    "input": "What is the refund policy?",
    "expectedOutput": "Items can be returned within 30 days.",
    "retrievalContext": [
      "Customers may return items within 30 days of purchase.",
      "Returns require original packaging."
    ],
    "context": [
      "Full refund within 30 days of purchase."
    ],
    "expectedToolCalls": [
      {
        "name": "SearchKnowledgeBase",
        "arguments": { "query": "refund policy" }
      }
    ],
    "metadata": {
      "category": "policy",
      "difficulty": "easy",
      "source": "customer-support-v1"
    }
  }
]
```

## CSV

Simple flat format for input/output pairs:

```csv
input,expectedOutput
"What is the refund window?","30 days from purchase."
"Can I return without receipt?","Yes, with valid ID."
"Is there a restocking fee?","No restocking fees."
```

Load:
```java
EvalDataset dataset = EvalDataset.load("test-cases.csv");
```

## JSONL (JSON Lines)

One test case per line — ideal for large datasets and streaming:

```jsonl
{"input": "What is the refund window?", "expectedOutput": "30 days."}
{"input": "Can I return digital products?", "expectedOutput": "No, digital products are final sale."}
{"input": "How long do refunds take?", "expectedOutput": "5–7 business days."}
```

Load:
```java
EvalDataset dataset = EvalDataset.load("large-dataset.jsonl");
```

## YAML

Human-readable format for manually authored datasets:

```yaml
- input: "What is the refund window?"
  expectedOutput: "30 days from purchase."
  retrievalContext:
    - "Customers may return items within 30 days."
  metadata:
    category: policy

- input: "Can I return a digital product?"
  expectedOutput: "Digital products are non-refundable."
  metadata:
    category: digital
```

## Format Auto-Detection

AgentEval detects format from file extension:

```java
EvalDataset.load("dataset.json");    // JSON
EvalDataset.load("dataset.csv");     // CSV
EvalDataset.load("dataset.jsonl");   // JSONL
EvalDataset.load("dataset.yaml");    // YAML
```

## Format Summary

| Format | Read | Write | Best For |
|---|---|---|---|
| JSON | Yes | Yes | Full field support, primary format |
| CSV | Yes | Yes | Simple input/output pairs |
| JSONL | Yes | Yes | Large datasets, streaming |
| YAML | Yes | No | Hand-authored datasets |
