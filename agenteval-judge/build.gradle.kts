dependencies {
    api(project(":agenteval-core"))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    testImplementation(libs.mockito.core)
}
