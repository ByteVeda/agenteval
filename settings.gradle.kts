rootProject.name = "agenteval"

// This Gradle build is scoped to agenteval-gradle-plugin only, because that
// module is published to the Gradle Plugin Portal via `publishPlugins` which
// has no Maven equivalent. Every other module is built by Maven (see pom.xml).
include("agenteval-gradle-plugin")

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
