dependencies {
    api(project(":agenteval-core"))
    implementation(project(":agenteval-judge"))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.slf4j.api)
    testImplementation(libs.mockito.core)
}
