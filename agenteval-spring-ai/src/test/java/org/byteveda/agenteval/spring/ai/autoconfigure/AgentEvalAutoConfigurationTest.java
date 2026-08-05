package org.byteveda.agenteval.spring.ai.autoconfigure;

import org.byteveda.agenteval.spring.ai.SpringAiAdvisorInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentEvalAutoConfigurationTest {

    @Test
    void producesAdvisorInterceptorBean() {
        var config = new AgentEvalAutoConfiguration();

        SpringAiAdvisorInterceptor advisor = config.agentEvalAdvisorInterceptor();

        assertThat(advisor).isNotNull();
        assertThat(advisor.getName()).isEqualTo("AgentEvalAdvisorInterceptor");
    }
}
