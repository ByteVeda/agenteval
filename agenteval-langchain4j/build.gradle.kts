dependencies {
    api(project(":agenteval-core"))
    compileOnly(libs.langchain4j.core)
    implementation(libs.slf4j.api)
}
