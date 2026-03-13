dependencies {
    api(project(":agenteval-core"))
    compileOnly(libs.spring.ai.model)
    compileOnly(libs.spring.ai.client.chat)
    compileOnly(libs.spring.ai.commons)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.slf4j.api)
}
