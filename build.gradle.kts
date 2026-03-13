plugins {
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.shadow) apply false
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "checkstyle")

    group = "com.agenteval"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror", "-parameters"))
    }

    dependencies {
        "implementation"(platform("org.junit:junit-bom:5.11.4"))
        "implementation"(platform("com.fasterxml.jackson:jackson-bom:2.18.3"))

        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.assertj:assertj-core:3.27.7")
        "testRuntimeOnly"("ch.qos.logback:logback-classic:1.5.25")
    }

    configure<CheckstyleExtension> {
        toolVersion = "10.21.4"
        configFile = rootProject.file("checkstyle.xml")
    }

    if (name != "agenteval-gradle-plugin" && name != "agenteval-intellij") {
        apply(plugin = "com.github.spotbugs")

        configure<com.github.spotbugs.snom.SpotBugsExtension> {
            excludeFilter = rootProject.file("spotbugs-exclude.xml")
        }

        tasks.matching { it.name == "spotbugsTest" }.configureEach {
            enabled = false
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
