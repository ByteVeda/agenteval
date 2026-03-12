package com.agenteval.core.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolCallTest {

    @Test
    void shouldCreateToolCall() {
        var tc = new ToolCall("search", Map.of("query", "refund"), "found 3 results", 150);

        assertThat(tc.name()).isEqualTo("search");
        assertThat(tc.arguments()).containsEntry("query", "refund");
        assertThat(tc.result()).isEqualTo("found 3 results");
        assertThat(tc.durationMs()).isEqualTo(150);
    }

    @Test
    void ofNameShouldCreateMinimalToolCall() {
        var tc = ToolCall.of("search");

        assertThat(tc.name()).isEqualTo("search");
        assertThat(tc.arguments()).isEmpty();
        assertThat(tc.result()).isNull();
        assertThat(tc.durationMs()).isZero();
    }

    @Test
    void ofNameArgsShouldCreateToolCallWithArgs() {
        var tc = ToolCall.of("search", Map.of("query", "test"));

        assertThat(tc.name()).isEqualTo("search");
        assertThat(tc.arguments()).containsEntry("query", "test");
        assertThat(tc.result()).isNull();
    }

    @Test
    void shouldDefensiveCopyArguments() {
        var args = new java.util.HashMap<String, Object>();
        args.put("key", "value");
        var tc = new ToolCall("test", args, null, 0);

        // Modifying the original map should not affect the record
        args.put("other", "val");
        assertThat(tc.arguments()).hasSize(1);
    }

    @Test
    void shouldDefaultNullArgumentsToEmptyMap() {
        var tc = new ToolCall("test", null, null, 0);
        assertThat(tc.arguments()).isEmpty();
    }

    @Test
    void shouldRejectNullName() {
        assertThatThrownBy(() -> new ToolCall(null, Map.of(), null, 0))
                .isInstanceOf(NullPointerException.class);
    }
}
