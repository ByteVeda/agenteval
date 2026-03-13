---
sidebar_position: 6
---

# Custom Judge Provider

Implement the `JudgeModel` interface to connect AgentEval to any HTTP-compatible LLM.

## Implementing JudgeModel

```java
public class MyCustomJudge implements JudgeModel {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String endpoint;
    private final String apiKey;

    public MyCustomJudge(String endpoint, String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    @Override
    public JudgeResponse evaluate(JudgeRequest request) {
        // request.systemPrompt() — the evaluation system prompt
        // request.userPrompt()   — the evaluation user prompt

        var httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(endpoint + "/v1/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(buildPayload(request)))
            .build();

        try {
            var response = httpClient.send(httpRequest, BodyHandlers.ofString());
            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            throw new JudgeException("Judge call failed", e);
        }
    }

    private String buildPayload(JudgeRequest request) {
        // Build JSON payload for your LLM API
        return """
            {
              "model": "my-model",
              "messages": [
                {"role": "system", "content": "%s"},
                {"role": "user", "content": "%s"}
              ]
            }
            """.formatted(request.systemPrompt(), request.userPrompt());
    }

    private JudgeResponse parseResponse(String body) {
        // Parse the response and return
        // JudgeResponse.of(scoreText, tokenUsage)
        var json = new ObjectMapper().readTree(body);
        String content = json.at("/choices/0/message/content").asText();
        return JudgeResponse.of(content);
    }
}
```

## Usage

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.custom(new MyCustomJudge(endpoint, apiKey)))
    .build();
```

## Azure OpenAI

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.azure(
        "my-gpt4o-deployment",
        "https://my-resource.openai.azure.com/",
        System.getenv("AZURE_OPENAI_API_KEY")
    ))
    .build();
```

## Amazon Bedrock

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.bedrock("anthropic.claude-3-haiku-20240307-v1:0"))
    .build();
```
