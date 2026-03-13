dependencies {
    api(project(":agenteval-core"))
    api(project(":agenteval-datasets"))
    api(libs.junit.jupiter.api)
    api(libs.junit.jupiter.params)
    implementation(libs.slf4j.api)
    testImplementation(libs.mockito.core)
    testImplementation(libs.junit.platform.testkit)
}
