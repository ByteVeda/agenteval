---
sidebar_position: 2
---

# Maven & Gradle Plugins

AgentEval provides plugins for Maven and Gradle to run evaluations as part of the build lifecycle.

## Maven Plugin

```xml
<plugin>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-maven-plugin</artifactId>
  <version>1.0.0</version>
  <configuration>
    <judgeProvider>openai</judgeProvider>
    <judgeModel>gpt-4o-mini</judgeModel>
    <datasetPath>src/test/resources/golden</datasetPath>
    <reportDir>target/agenteval</reportDir>
    <failOnRegressionBelow>0.90</failOnRegressionBelow>
  </configuration>
  <executions>
    <execution>
      <id>evaluate</id>
      <phase>verify</phase>
      <goals>
        <goal>evaluate</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

### Run

```bash
# Run evaluations only
mvn agenteval:evaluate

# Run as part of verify phase
mvn verify

# Skip evaluations
mvn verify -Dagenteval.skip=true
```

## Gradle Plugin (Kotlin DSL)

```kotlin
plugins {
    id("org.byteveda.agenteval") version "1.0.0"
}

agenteval {
    judgeProvider = "openai"
    judgeModel = "gpt-4o-mini"
    datasetPath = "src/test/resources/golden"
    reportDir = "build/agenteval"
    failOnRegressionBelow = 0.90
}
```

### Run

```bash
# Run evaluations
./gradlew agentEval

# Run as part of check
./gradlew check

# Skip evaluations
./gradlew check -x agentEval
```

## Plugin Output

Both plugins produce:
- Console summary with pass/fail breakdown
- JSON report at the configured `reportDir`
- JUnit XML compatible with CI systems
- Build failure if `failOnRegressionBelow` threshold is not met
