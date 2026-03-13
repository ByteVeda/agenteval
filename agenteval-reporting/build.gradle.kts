dependencies {
    api(project(":agenteval-core"))
    implementation(libs.slf4j.api)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.mockito.core)
}
