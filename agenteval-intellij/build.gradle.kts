dependencies {
    compileOnly(libs.jackson.databind)
    testImplementation(libs.jackson.databind)
}

sourceSets {
    main {
        java {
            exclude(
                "com/agenteval/intellij/AgentEvalIcons.java",
                "com/agenteval/intellij/AgentEvalToolWindowFactory.java",
                "com/agenteval/intellij/AgentEvalToolWindow.java",
                "com/agenteval/intellij/ReportFileWatcher.java",
                "com/agenteval/intellij/MetricGutterIconProvider.java"
            )
        }
    }
}
