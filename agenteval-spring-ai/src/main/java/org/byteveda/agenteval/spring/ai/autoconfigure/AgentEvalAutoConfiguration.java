package org.byteveda.agenteval.spring.ai.autoconfigure;

import org.byteveda.agenteval.spring.ai.SpringAiAdvisorInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for AgentEval Spring AI integration.
 *
 * <p>Registers the advisor interceptor for automatic RAG context capture.</p>
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.ai.chat.model.ChatModel")
public class AgentEvalAutoConfiguration {

    @Bean
    public SpringAiAdvisorInterceptor agentEvalAdvisorInterceptor() {
        return new SpringAiAdvisorInterceptor();
    }
}
