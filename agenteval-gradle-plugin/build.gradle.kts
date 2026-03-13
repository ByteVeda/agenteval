plugins {
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":agenteval-core"))
    implementation(project(":agenteval-judge"))
    implementation(project(":agenteval-metrics"))
    implementation(project(":agenteval-datasets"))
    implementation(project(":agenteval-reporting"))
    implementation(libs.slf4j.api)
}

gradlePlugin {
    plugins {
        create("agenteval") {
            id = "com.agenteval.evaluate"
            implementationClass = "com.agenteval.gradle.AgentEvalPlugin"
        }
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
