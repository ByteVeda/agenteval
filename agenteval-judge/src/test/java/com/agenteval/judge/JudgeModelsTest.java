package com.agenteval.judge;

import com.agenteval.judge.config.JudgeConfig;
import com.agenteval.judge.provider.OpenAiJudgeModel;
import com.agenteval.judge.provider.AnthropicJudgeModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JudgeModelsTest {

    @Test
    void openaiWithConfigShouldCreateOpenAiModel() {
        var config = JudgeConfig.builder()
                .apiKey("test-key")
                .model("gpt-4o")
                .baseUrl("https://api.openai.com")
                .build();

        var model = JudgeModels.openai(config);

        assertThat(model).isInstanceOf(OpenAiJudgeModel.class);
        assertThat(model.modelId()).isEqualTo("gpt-4o");
    }

    @Test
    void anthropicWithConfigShouldCreateAnthropicModel() {
        var config = JudgeConfig.builder()
                .apiKey("test-key")
                .model("claude-sonnet-4-20250514")
                .baseUrl("https://api.anthropic.com")
                .build();

        var model = JudgeModels.anthropic(config);

        assertThat(model).isInstanceOf(AnthropicJudgeModel.class);
        assertThat(model.modelId()).isEqualTo("claude-sonnet-4-20250514");
    }

    @Test
    void openaiWithoutEnvKeyShouldThrow() {
        assertThatThrownBy(() -> JudgeModels.openai("gpt-4o"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void anthropicWithoutEnvKeyShouldThrow() {
        assertThatThrownBy(() -> JudgeModels.anthropic("claude-sonnet-4-20250514"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }
}
