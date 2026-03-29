dependencies {
    compileOnly(libs.jackson.databind)
    testImplementation(libs.jackson.databind)
}

sourceSets {
    main {
        java {
            exclude(
                "org/byteveda/agenteval/intellij/AgentEvalIcons.java",
                "org/byteveda/agenteval/intellij/AgentEvalToolWindowFactory.java",
                "org/byteveda/agenteval/intellij/AgentEvalToolWindow.java",
                "org/byteveda/agenteval/intellij/ReportFileWatcher.java",
                "org/byteveda/agenteval/intellij/MetricGutterIconProvider.java"
            )
        }
    }
}
