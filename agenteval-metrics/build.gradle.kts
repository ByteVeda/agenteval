dependencies {
    api(project(":agenteval-core"))
    api(project(":agenteval-judge"))
    implementation(libs.slf4j.api)
    implementation(project(":agenteval-embeddings"))
    testImplementation(libs.mockito.core)
}
