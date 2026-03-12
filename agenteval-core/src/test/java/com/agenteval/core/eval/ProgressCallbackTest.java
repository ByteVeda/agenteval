package com.agenteval.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressCallbackTest {

    @Test
    void progressEventShouldCalculateCompletionRatio() {
        var event = new ProgressEvent(5, 10, 1000, 1000);
        assertThat(event.completionRatio()).isEqualTo(0.5);
    }

    @Test
    void completionRatioShouldBeOneWhenNoTotalCases() {
        var event = new ProgressEvent(0, 0, 0, 0);
        assertThat(event.completionRatio()).isEqualTo(1.0);
    }

    @Test
    void completionRatioShouldBeOneWhenAllDone() {
        var event = new ProgressEvent(10, 10, 5000, 0);
        assertThat(event.completionRatio()).isEqualTo(1.0);
    }

    @Test
    void lambdaShouldWorkAsProgressCallback() {
        var holder = new Object() { ProgressEvent captured; };

        ProgressCallback callback = event -> holder.captured = event;
        callback.onProgress(new ProgressEvent(1, 5, 100, 400));

        assertThat(holder.captured).isNotNull();
        assertThat(holder.captured.completedCases()).isEqualTo(1);
        assertThat(holder.captured.totalCases()).isEqualTo(5);
    }
}
