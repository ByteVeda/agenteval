plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.0"
}

val agentevalVersion = providers.gradleProperty("agentevalVersion").getOrElse("0.2.0")

group = "org.byteveda.agenteval"
version = agentevalVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("org.byteveda.agenteval:agenteval-core:$agentevalVersion")
    implementation("org.byteveda.agenteval:agenteval-judge:$agentevalVersion")
    implementation("org.byteveda.agenteval:agenteval-metrics:$agentevalVersion")
    implementation("org.byteveda.agenteval:agenteval-datasets:$agentevalVersion")
    implementation("org.byteveda.agenteval:agenteval-reporting:$agentevalVersion")
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website.set("https://github.com/ByteVeda/agenteval")
    vcsUrl.set("https://github.com/ByteVeda/agenteval.git")
    plugins {
        create("agenteval") {
            id = "org.byteveda.agenteval.evaluate"
            implementationClass = "org.byteveda.agenteval.gradle.AgentEvalPlugin"
            displayName = "AgentEval Gradle Plugin"
            description = "Run AgentEval evaluations from Gradle builds"
            tags.set(listOf("testing", "ai", "llm", "evaluation"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
