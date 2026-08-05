package org.byteveda.agenteval.judge.parse;

import org.byteveda.agenteval.judge.JudgeException;
import org.byteveda.agenteval.judge.parse.JudgeResponseParser.ParsedScore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class JudgeResponseParserTest {

    @Test
    void shouldParseCleanJson() {
        ParsedScore result = JudgeResponseParser.parse(
                "{\"score\": 0.85, \"reason\": \"Output is relevant\"}");

        assertThat(result.score()).isCloseTo(0.85, within(0.001));
        assertThat(result.reason()).isEqualTo("Output is relevant");
    }

    @Test
    void shouldParseJsonWithExtraFields() {
        ParsedScore result = JudgeResponseParser.parse(
                "{\"score\": 0.7, \"reason\": \"Good\", \"confidence\": \"high\"}");

        assertThat(result.score()).isCloseTo(0.7, within(0.001));
        assertThat(result.reason()).isEqualTo("Good");
    }

    @Test
    void shouldParseJsonEmbeddedInText() {
        String response = "Here is my evaluation:\n"
                + "{\"score\": 0.6, \"reason\": \"Partially relevant\"}\n"
                + "I hope this helps.";

        ParsedScore result = JudgeResponseParser.parse(response);

        assertThat(result.score()).isCloseTo(0.6, within(0.001));
        assertThat(result.reason()).isEqualTo("Partially relevant");
    }

    @Test
    void shouldParseBareScore() {
        ParsedScore result = JudgeResponseParser.parse(
                "After careful analysis, I'd rate this 0.75 overall.");

        assertThat(result.score()).isCloseTo(0.75, within(0.001));
        assertThat(result.reason()).isEmpty();
    }

    @Test
    void shouldParsePerfectScore() {
        ParsedScore result = JudgeResponseParser.parse("{\"score\": 1.0, \"reason\": \"Perfect\"}");
        assertThat(result.score()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void shouldParseZeroScore() {
        ParsedScore result = JudgeResponseParser.parse("{\"score\": 0, \"reason\": \"Terrible\"}");
        assertThat(result.score()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void shouldParseScoreWithoutReason() {
        ParsedScore result = JudgeResponseParser.parse("{\"score\": 0.5}");

        assertThat(result.score()).isCloseTo(0.5, within(0.001));
        assertThat(result.reason()).isEmpty();
    }

    @Test
    void shouldRejectNullInput() {
        assertThatThrownBy(() -> JudgeResponseParser.parse(null))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("Empty");
    }

    @Test
    void shouldRejectEmptyInput() {
        assertThatThrownBy(() -> JudgeResponseParser.parse(""))
                .isInstanceOf(JudgeException.class);
    }

    @Test
    void shouldRejectBlankInput() {
        assertThatThrownBy(() -> JudgeResponseParser.parse("   "))
                .isInstanceOf(JudgeException.class);
    }

    @Test
    void shouldRejectTextWithNoScore() {
        assertThatThrownBy(() -> JudgeResponseParser.parse("No score here at all."))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("Could not extract score");
    }

    @Test
    void shouldHandleScoreInMarkdownCodeBlock() {
        String response = "```json\n{\"score\": 0.9, \"reason\": \"Very good\"}\n```";
        ParsedScore result = JudgeResponseParser.parse(response);
        assertThat(result.score()).isCloseTo(0.9, within(0.001));
    }
}
