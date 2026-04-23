package org.byteveda.agenteval.spring.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiAdvisorInterceptorTest {

    private static ChatClientResponse simpleResponse() {
        ChatResponse chatResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage("ok"))));
        return new ChatClientResponse(chatResponse, Map.of());
    }

    @Test
    void advisorMetadataIsCorrect() {
        var advisor = new SpringAiAdvisorInterceptor();
        assertThat(advisor.getName()).isEqualTo("AgentEvalAdvisorInterceptor");
        assertThat(advisor.getOrder()).isZero();
    }

    @Test
    void consumeReturnsEmptyListWhenNothingCaptured() {
        var advisor = new SpringAiAdvisorInterceptor();
        assertThat(advisor.consumeCapturedContext()).isEmpty();
    }

    @Test
    void capturesDocumentsFromQaAdvisorContextKey() {
        var advisor = new SpringAiAdvisorInterceptor();
        Map<String, Object> context = new HashMap<>();
        context.put("qa_advisor_retrieved_documents", List.of(
                new Document("first doc", Map.of()),
                new Document("second doc", Map.of())));
        ChatClientRequest request = new ChatClientRequest(new Prompt("q"), context);
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(request)).thenReturn(simpleResponse());

        advisor.adviseCall(request, chain);

        assertThat(advisor.consumeCapturedContext()).containsExactly("first doc", "second doc");
    }

    @Test
    void consumeClearsBufferBetweenCalls() {
        var advisor = new SpringAiAdvisorInterceptor();
        Map<String, Object> context = new HashMap<>();
        context.put("qa_advisor_retrieved_documents", List.of(new Document("only", Map.of())));
        ChatClientRequest request = new ChatClientRequest(new Prompt("q"), context);
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(request)).thenReturn(simpleResponse());

        advisor.adviseCall(request, chain);
        assertThat(advisor.consumeCapturedContext()).hasSize(1);
        assertThat(advisor.consumeCapturedContext()).isEmpty();
    }

    @Test
    void passesThroughWhenContextIsAbsent() {
        var advisor = new SpringAiAdvisorInterceptor();
        ChatClientRequest request = new ChatClientRequest(new Prompt("q"), Map.of());
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(request)).thenReturn(simpleResponse());

        var response = advisor.adviseCall(request, chain);

        assertThat(response).isNotNull();
        assertThat(advisor.consumeCapturedContext()).isEmpty();
    }
}
