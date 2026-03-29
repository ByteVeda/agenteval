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
            id = "org.byteveda.agenteval.evaluate"
            implementationClass = "org.byteveda.agenteval.gradle.AgentEvalPlugin"
        }
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
