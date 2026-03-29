package org.byteveda.agenteval.gradle;

import org.byteveda.agenteval.reporting.ConsoleReporter;
import org.byteveda.agenteval.reporting.EvalReporter;
import org.byteveda.agenteval.reporting.HtmlReportConfig;
import org.byteveda.agenteval.reporting.HtmlReporter;
import org.byteveda.agenteval.reporting.JsonReporter;
import org.byteveda.agenteval.reporting.JunitXmlReporter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resolves report format strings to {@link EvalReporter} instances.
 *
 * <p>Supported formats: console, json, xml, html.</p>
 */
public final class ReportFormatResolver {

    private ReportFormatResolver() {}

    /**
     * Resolves a list of format names to reporter instances.
     *
     * @param formats         comma-separated format names
     * @param outputDirectory the directory for file-based reports
     * @return the list of reporters
     * @throws IllegalArgumentException if a format name is unknown
     */
    public static List<EvalReporter> resolve(String formats, Path outputDirectory) {
        List<EvalReporter> reporters = new ArrayList<>();
        for (String format : formats.split(",")) {
            reporters.add(resolveOne(format.trim(), outputDirectory));
        }
        return reporters;
    }

    private static EvalReporter resolveOne(String format, Path outputDirectory) {
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "console" -> new ConsoleReporter();
            case "json" -> new JsonReporter(outputDirectory.resolve("agenteval-report.json"));
            case "xml" -> new JunitXmlReporter(outputDirectory.resolve("agenteval-report.xml"));
            case "html" -> new HtmlReporter(HtmlReportConfig.builder()
                    .outputPath(outputDirectory.resolve("agenteval-report.html"))
                    .build());
            default -> throw new IllegalArgumentException(
                    "Unknown report format: " + format
                            + ". Supported: console, json, xml, html");
        };
    }
}
