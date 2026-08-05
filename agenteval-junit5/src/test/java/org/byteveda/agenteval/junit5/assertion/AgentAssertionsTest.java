package org.byteveda.agenteval.junit5.assertion;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentAssertionsTest {

    private static final EvalMetric PASSING_METRIC = new EvalMetric() {
        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(0.9, 0.7, "good");
        }

        @Override
        public String name() { return "PassingMetric"; }
    };

    private static final EvalMetric FAILING_METRIC = new EvalMetric() {
        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(0.3, 0.7, "bad");
        }

        @Override
        public String name() { return "FailingMetric"; }
    };

    @Test
    void meetsMetricShouldPassForPassingMetric() {
        var tc = AgentTestCase.builder().input("q").actualOutput("a").build();
        assertThatCode(() -> AgentAssertions.assertThat(tc).meetsMetric(PASSING_METRIC))
                .doesNotThrowAnyException();
    }

    @Test
    void meetsMetricShouldFailForFailingMetric() {
        var tc = AgentTestCase.builder().input("q").actualOutput("a").build();
        assertThatThrownBy(() -> AgentAssertions.assertThat(tc).meetsMetric(FAILING_METRIC))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("FailingMetric");
    }

    @Test
    void hasToolCallsShouldPassWhenToolCallsExist() {
        var tc = AgentTestCase.builder()
                .input("q")
                .toolCalls(List.of(ToolCall.of("search")))
                .build();
        assertThatCode(() -> AgentAssertions.assertThat(tc).hasToolCalls())
                .doesNotThrowAnyException();
    }

    @Test
    void hasToolCallsShouldFailWhenEmpty() {
        var tc = AgentTestCase.builder().input("q").build();
        assertThatThrownBy(() -> AgentAssertions.assertThat(tc).hasToolCalls())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected tool calls");
    }

    @Test
    void calledToolShouldPassWhenToolWasCalled() {
        var tc = AgentTestCase.builder()
                .input("q")
                .toolCalls(List.of(ToolCall.of("search"), ToolCall.of("lookup")))
                .build();
        assertThatCode(() -> AgentAssertions.assertThat(tc).calledTool("search"))
                .doesNotThrowAnyException();
    }

    @Test
    void calledToolShouldFailWhenToolNotCalled() {
        var tc = AgentTestCase.builder()
                .input("q")
                .toolCalls(List.of(ToolCall.of("search")))
                .build();
        assertThatThrownBy(() -> AgentAssertions.assertThat(tc).calledTool("delete"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("delete");
    }

    @Test
    void neverCalledToolShouldPassWhenToolNotCalled() {
        var tc = AgentTestCase.builder()
                .input("q")
                .toolCalls(List.of(ToolCall.of("search")))
                .build();
        assertThatCode(() -> AgentAssertions.assertThat(tc).neverCalledTool("delete"))
                .doesNotThrowAnyException();
    }

    @Test
    void neverCalledToolShouldFailWhenToolWasCalled() {
        var tc = AgentTestCase.builder()
                .input("q")
                .toolCalls(List.of(ToolCall.of("delete")))
                .build();
        assertThatThrownBy(() -> AgentAssertions.assertThat(tc).neverCalledTool("delete"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("NOT be called");
    }

    @Test
    void outputContainsShouldPassWhenContained() {
        var tc = AgentTestCase.builder().input("q").actualOutput("refund policy applies").build();
        assertThatCode(() -> AgentAssertions.assertThat(tc).outputContains("refund"))
                .doesNotThrowAnyException();
    }

    @Test
    void outputContainsShouldFailWhenNotContained() {
        var tc = AgentTestCase.builder().input("q").actualOutput("no match").build();
        assertThatThrownBy(() -> AgentAssertions.assertThat(tc).outputContains("refund"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void outputContainsShouldFailWhenOutputNull() {
        var tc = AgentTestCase.builder().input("q").build();
        assertThatThrownBy(() -> AgentAssertions.assertThat(tc).outputContains("anything"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void outputDoesNotContainShouldPass() {
        var tc = AgentTestCase.builder().input("q").actualOutput("hello").build();
        assertThatCode(() -> AgentAssertions.assertThat(tc).outputDoesNotContain("goodbye"))
                .doesNotThrowAnyException();
    }

    @Test
    void outputDoesNotContainShouldFail() {
        var tc = AgentTestCase.builder().input("q").actualOutput("hello world").build();
        assertThatThrownBy(() -> AgentAssertions.assertThat(tc).outputDoesNotContain("hello"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void hasOutputShouldPassWhenOutputExists() {
        var tc = AgentTestCase.builder().input("q").actualOutput("response").build();
        assertThatCode(() -> AgentAssertions.assertThat(tc).hasOutput())
                .doesNotThrowAnyException();
    }

    @Test
    void hasOutputShouldFailWhenOutputNull() {
        var tc = AgentTestCase.builder().input("q").build();
        assertThatThrownBy(() -> AgentAssertions.assertThat(tc).hasOutput())
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void shouldSupportFluentChaining() {
        var tc = AgentTestCase.builder()
                .input("q")
                .actualOutput("refund processed")
                .toolCalls(List.of(ToolCall.of("SearchOrders")))
                .build();

        assertThatCode(() ->
                AgentAssertions.assertThat(tc)
                        .hasOutput()
                        .hasToolCalls()
                        .calledTool("SearchOrders")
                        .neverCalledTool("DeleteOrder")
                        .outputContains("refund")
                        .meetsMetric(PASSING_METRIC)
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldReturnTestCase() {
        var tc = AgentTestCase.builder().input("q").build();
        var result = AgentAssertions.assertThat(tc).testCase();
        org.assertj.core.api.Assertions.assertThat(result).isSameAs(tc);
    }
}
