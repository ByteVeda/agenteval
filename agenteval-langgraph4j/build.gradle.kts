dependencies {
    api(project(":agenteval-core"))
    compileOnly(libs.langgraph4j.core)
    implementation(libs.slf4j.api)
}
