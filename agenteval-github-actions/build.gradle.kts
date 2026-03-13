plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":agenteval-core"))
    implementation(project(":agenteval-reporting"))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    testImplementation(libs.mockito.core)
}

tasks.shadowJar {
    manifest {
        attributes("Main-Class" to "com.agenteval.github.GitHubActionRunner")
    }
}
