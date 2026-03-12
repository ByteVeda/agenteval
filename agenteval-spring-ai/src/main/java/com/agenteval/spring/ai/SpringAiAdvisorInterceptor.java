package com.agenteval.spring.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI advisor interceptor that captures RAG retrieval context.
 *
 * <p>Extracts document content from advised requests for use as
 * retrieval context in AgentEval test cases.</p>
 */
public final class SpringAiAdvisorInterceptor implements CallAdvisor {

    private static final Logger LOG = LoggerFactory.getLogger(SpringAiAdvisorInterceptor.class);

    private final List<String> capturedContext = new ArrayList<>();

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request,
                                         CallAdvisorChain chain) {
        if (request.context() != null) {
            var context = request.context();
            Object docs = context.get("qa_advisor_retrieved_documents");
            if (docs instanceof List<?> docList) {
                for (Object doc : docList) {
                    if (doc instanceof Document document) {
                        capturedContext.add(document.getText());
                    }
                }
                LOG.debug("Captured {} retrieval context documents",
                        capturedContext.size());
            }
        }

        return chain.nextCall(request);
    }

    @Override
    public String getName() {
        return "AgentEvalAdvisorInterceptor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * Returns the captured retrieval context and clears the buffer.
     */
    public List<String> consumeCapturedContext() {
        var result = List.copyOf(capturedContext);
        capturedContext.clear();
        return result;
    }
}
