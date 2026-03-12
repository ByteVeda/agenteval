package com.agenteval.core.eval;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleProgressBarTest {

    @Test
    void shouldPrintProgressBar() {
        var baos = new ByteArrayOutputStream();
        var out = new PrintStream(baos, true, StandardCharsets.UTF_8);
        var bar = new ConsoleProgressBar(out);

        bar.onProgress(new ProgressEvent(5, 10, 1000, 1000));

        String output = baos.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("5/10");
        assertThat(output).contains("50%");
        assertThat(output).contains("[");
        assertThat(output).contains("]");
    }

    @Test
    void shouldPrintNewlineOnCompletion() {
        var baos = new ByteArrayOutputStream();
        var out = new PrintStream(baos, true, StandardCharsets.UTF_8);
        var bar = new ConsoleProgressBar(out);

        bar.onProgress(new ProgressEvent(10, 10, 5000, 0));

        String output = baos.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("10/10");
        assertThat(output).contains("100%");
        assertThat(output).endsWith("\n");
    }

    @Test
    void shouldShowDashesForUnknownEta() {
        var baos = new ByteArrayOutputStream();
        var out = new PrintStream(baos, true, StandardCharsets.UTF_8);
        var bar = new ConsoleProgressBar(out);

        bar.onProgress(new ProgressEvent(1, 10, 100, -1));

        String output = baos.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("ETA: --");
    }
}
