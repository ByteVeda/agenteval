package com.agenteval.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tool window panel showing AgentEval report results.
 *
 * <p>Displays a summary bar (pass rate, avg score, case count, duration)
 * and a table of test case results with per-metric scores.</p>
 */
public class AgentEvalToolWindow {

    private final JPanel content;
    private final JBLabel summaryLabel;
    private final JBTable resultTable;
    private final Project project;

    public AgentEvalToolWindow(Project project) {
        this.project = project;
        this.content = new JPanel(new BorderLayout());
        this.summaryLabel = new JBLabel("No report loaded");
        this.resultTable = new JBTable();

        content.add(summaryLabel, BorderLayout.NORTH);
        content.add(new JBScrollPane(resultTable), BorderLayout.CENTER);

        refresh();
    }

    public JComponent getContent() {
        return content;
    }

    /**
     * Refreshes the tool window by re-reading the report file.
     */
    public void refresh() {
        Path reportPath = findReportFile();
        if (reportPath == null || !Files.exists(reportPath)) {
            summaryLabel.setText("No agenteval-report.json found");
            return;
        }

        try {
            ReportModel report = ReportParser.parseFile(reportPath);
            updateSummary(report);
            updateTable(report);
        } catch (IOException e) {
            summaryLabel.setText("Error reading report: " + e.getMessage());
        }
    }

    private void updateSummary(ReportModel report) {
        summaryLabel.setText(String.format(
                "Pass Rate: %.1f%% | Avg Score: %.3f | Cases: %d | Duration: %dms",
                report.getPassRate() * 100,
                report.getAverageScore(),
                report.getTotalCases(),
                report.getDurationMs()));
    }

    private void updateTable(ReportModel report) {
        if (report.getCaseResults() == null || report.getCaseResults().isEmpty()) {
            resultTable.setModel(new DefaultTableModel());
            return;
        }

        // Collect all metric names
        var firstCase = report.getCaseResults().getFirst();
        String[] metricNames = firstCase.getScores() != null
                ? firstCase.getScores().keySet().toArray(String[]::new)
                : new String[0];

        // Build column names: Input, Passed, then metric columns
        String[] columns = new String[2 + metricNames.length];
        columns[0] = "Input";
        columns[1] = "Passed";
        System.arraycopy(metricNames, 0, columns, 2, metricNames.length);

        // Build data
        Object[][] data = new Object[report.getCaseResults().size()][columns.length];
        for (int i = 0; i < report.getCaseResults().size(); i++) {
            var cr = report.getCaseResults().get(i);
            data[i][0] = cr.getInput();
            data[i][1] = cr.isPassed() ? "PASS" : "FAIL";
            for (int j = 0; j < metricNames.length; j++) {
                if (cr.getScores() != null && cr.getScores().containsKey(metricNames[j])) {
                    data[i][j + 2] = String.format("%.3f",
                            cr.getScores().get(metricNames[j]).getValue());
                } else {
                    data[i][j + 2] = "—";
                }
            }
        }

        resultTable.setModel(new DefaultTableModel(data, columns));
    }

    private Path findReportFile() {
        if (project.getBasePath() == null) return null;
        // Check common locations
        Path buildDir = Path.of(project.getBasePath(), "build", "agenteval",
                "agenteval-report.json");
        if (Files.exists(buildDir)) return buildDir;

        Path targetDir = Path.of(project.getBasePath(), "target", "agenteval",
                "agenteval-report.json");
        if (Files.exists(targetDir)) return targetDir;

        return null;
    }
}
