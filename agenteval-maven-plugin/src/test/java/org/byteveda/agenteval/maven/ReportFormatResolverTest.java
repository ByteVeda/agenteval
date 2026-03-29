package org.byteveda.agenteval.maven;

import org.byteveda.agenteval.reporting.ConsoleReporter;
import org.byteveda.agenteval.reporting.EvalReporter;
import org.byteveda.agenteval.reporting.HtmlReporter;
import org.byteveda.agenteval.reporting.JsonReporter;
import org.byteveda.agenteval.reporting.JunitXmlReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportFormatResolverTest {

    @Test
    void shouldResolveConsoleFormat(@TempDir Path tempDir) {
        List<EvalReporter> reporters = ReportFormatResolver.resolve("console", tempDir);

        assertThat(reporters).hasSize(1);
        assertThat(reporters.getFirst()).isInstanceOf(ConsoleReporter.class);
    }

    @Test
    void shouldResolveJsonFormat(@TempDir Path tempDir) {
        List<EvalReporter> reporters = ReportFormatResolver.resolve("json", tempDir);

        assertThat(reporters).hasSize(1);
        assertThat(reporters.getFirst()).isInstanceOf(JsonReporter.class);
    }

    @Test
    void shouldResolveXmlFormat(@TempDir Path tempDir) {
        List<EvalReporter> reporters = ReportFormatResolver.resolve("xml", tempDir);

        assertThat(reporters).hasSize(1);
        assertThat(reporters.getFirst()).isInstanceOf(JunitXmlReporter.class);
    }

    @Test
    void shouldResolveHtmlFormat(@TempDir Path tempDir) {
        List<EvalReporter> reporters = ReportFormatResolver.resolve("html", tempDir);

        assertThat(reporters).hasSize(1);
        assertThat(reporters.getFirst()).isInstanceOf(HtmlReporter.class);
    }

    @Test
    void shouldResolveMultipleFormats(@TempDir Path tempDir) {
        List<EvalReporter> reporters = ReportFormatResolver.resolve("console,json,xml", tempDir);

        assertThat(reporters).hasSize(3);
        assertThat(reporters.get(0)).isInstanceOf(ConsoleReporter.class);
        assertThat(reporters.get(1)).isInstanceOf(JsonReporter.class);
        assertThat(reporters.get(2)).isInstanceOf(JunitXmlReporter.class);
    }

    @Test
    void shouldBeCaseInsensitive(@TempDir Path tempDir) {
        List<EvalReporter> reporters = ReportFormatResolver.resolve("CONSOLE,JSON", tempDir);

        assertThat(reporters).hasSize(2);
        assertThat(reporters.get(0)).isInstanceOf(ConsoleReporter.class);
        assertThat(reporters.get(1)).isInstanceOf(JsonReporter.class);
    }

    @Test
    void shouldThrowForUnknownFormat(@TempDir Path tempDir) {
        assertThatThrownBy(() -> ReportFormatResolver.resolve("pdf", tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown report format");
    }
}
