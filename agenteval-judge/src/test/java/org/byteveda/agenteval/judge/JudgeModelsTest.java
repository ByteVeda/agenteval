package org.byteveda.agenteval.judge;

import org.byteveda.agenteval.judge.config.JudgeConfig;
import org.byteveda.agenteval.judge.provider.AzureOpenAiJudgeModel;
import org.byteveda.agenteval.judge.provider.CustomHttpJudgeModel;
import org.byteveda.agenteval.judge.provider.GoogleJudgeModel;
import org.byteveda.agenteval.judge.provider.OpenAiJudgeModel;
import org.byteveda.agenteval.judge.provider.AnthropicJudgeModel;
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
    void googleWithConfigShouldCreateGoogleModel() {
        var config = JudgeConfig.builder()
                .apiKey("test-key")
                .model("gemini-1.5-pro")
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();

        var model = JudgeModels.google(config);

        assertThat(model).isInstanceOf(GoogleJudgeModel.class);
        assertThat(model.modelId()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    void azureWithConfigShouldCreateAzureModel() {
        var config = JudgeConfig.builder()
                .apiKey("azure-key")
                .model("my-deployment")
                .baseUrl("https://myresource.openai.azure.com")
                .build();

        var model = JudgeModels.azure(config);

        assertThat(model).isInstanceOf(AzureOpenAiJudgeModel.class);
        assertThat(model.modelId()).isEqualTo("my-deployment");
    }

    @Test
    void customWithConfigShouldCreateCustomModel() {
        var config = JudgeConfig.builder()
                .model("local-model")
                .baseUrl("http://localhost:8000")
                .build();

        var model = JudgeModels.custom(config);

        assertThat(model).isInstanceOf(CustomHttpJudgeModel.class);
        assertThat(model.modelId()).isEqualTo("local-model");
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

    @Test
    void googleWithoutEnvKeyShouldThrow() {
        assertThatThrownBy(() -> JudgeModels.google("gemini-1.5-pro"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("GOOGLE_API_KEY");
    }

    @Test
    void bedrockWithoutEnvKeyShouldThrow() {
        assertThatThrownBy(() -> JudgeModels.bedrock("anthropic.claude-3-sonnet"))
                .isInstanceOf(JudgeException.class);
    }
}
