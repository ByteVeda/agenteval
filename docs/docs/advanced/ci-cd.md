---
sidebar_position: 1
---

# CI/CD Integration

Run AgentEval evaluations in CI/CD pipelines with zero configuration beyond environment variables.

## GitHub Actions

```yaml
name: AgentEval

on:
  push:
    branches: [main]
  pull_request:

jobs:
  evaluate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run evaluations
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          AGENTEVAL_JUDGE_MODEL: gpt-4o-mini
        run: mvn test -Dgroups=eval

      - name: Upload evaluation report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: agenteval-report
          path: target/agenteval-report.json
```

## Separate Eval from Unit Tests

Keep evaluation tests separate from fast unit tests:

```yaml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - run: mvn test -DexcludeGroups=eval   # fast — no LLM calls

  eval-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    steps:
      - run: mvn test -Dgroups=eval           # slow — requires API key
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
```

## GitLab CI

```yaml
stages:
  - test
  - evaluate

unit-tests:
  stage: test
  script:
    - mvn test -DexcludeGroups=eval

agent-evaluations:
  stage: evaluate
  script:
    - mvn test -Dgroups=eval
  variables:
    OPENAI_API_KEY: $OPENAI_API_KEY
  artifacts:
    paths:
      - target/agenteval-report.json
    when: always
```

## Cost Control in CI

Limit evaluation costs per run:

```yaml
agenteval:
  judge:
    provider: openai
    model: gpt-4o-mini   # cheaper model for CI
  cost:
    budget: 1.00         # fail if run exceeds $1
```

Or use Ollama for free CI evaluations:

```yaml
- name: Start Ollama
  run: |
    ollama serve &
    ollama pull llama3.2

- name: Run evaluations
  env:
    AGENTEVAL_JUDGE_PROVIDER: ollama
    AGENTEVAL_JUDGE_MODEL: llama3.2
  run: mvn test -Dgroups=eval
```

## Pass/Fail on Regression

Fail the CI job if pass rate drops below a threshold:

```java
@Test
@Tag("eval")
void goldenSetShouldNotRegress() {
    var results = AgentEval.evaluate(goldenSet, metrics);

    assertTrue(results.passRate() >= 0.95,
        "CI gate: pass rate regressed to " + results.passRate());
}
```
