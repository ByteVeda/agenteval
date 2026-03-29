# Installing AgentEval

## From Maven Central

Add the dependencies to your project:

### Maven

```xml
<dependency>
    <groupId>org.byteveda.agenteval</groupId>
    <artifactId>agenteval-junit5</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.byteveda.agenteval</groupId>
    <artifactId>agenteval-metrics</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

### Gradle

```kotlin
testImplementation("org.byteveda.agenteval:agenteval-junit5:0.1.0-SNAPSHOT")
testImplementation("org.byteveda.agenteval:agenteval-metrics:0.1.0-SNAPSHOT")
```

---

## Building from Source

If you prefer to build and install AgentEval locally instead of pulling from Maven Central:

### Prerequisites

- **Java 21+** — the only requirement. Maven is bundled via the wrapper.

### Clone and Install

```bash
git clone https://github.com/ByteVeda/agenteval.git
cd agenteval
```

**Linux / macOS:**

```bash
./scripts/install.sh                # Quick install (skips tests)
./scripts/install.sh --with-tests   # Install with tests
./scripts/install.sh --skip-javadoc # Skip javadoc for faster builds
./scripts/install.sh --help         # Show all options
```

**Windows:**

```cmd
scripts\install.bat                 REM Quick install (skips tests)
scripts\install.bat /with-tests     REM Install with tests
scripts\install.bat /skip-javadoc   REM Skip javadoc for faster builds
scripts\install.bat /help           REM Show all options
```

The scripts use the bundled Maven wrapper, so no separate Maven installation is needed. Artifacts are installed to your local Maven repository (`~/.m2/repository`).

### Manual Build (without scripts)

You can also build directly with the Maven wrapper:

```bash
./mvnw clean install -DskipTests                          # Linux / macOS
mvnw.cmd clean install -DskipTests                        # Windows
```

---

## Verifying the Installation

After installing, verify the artifacts are in your local Maven repository:

```bash
ls ~/.m2/repository/org/byteveda/agenteval/
```

You should see directories for each module (`agenteval-core`, `agenteval-metrics`, etc.).

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java        | 21+     |
| Maven       | 3.9+ (bundled via wrapper) |
| Gradle      | 8.5+ (if using Gradle to consume the library) |
