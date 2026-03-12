package com.agenteval.junit5.extension;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.datasets.DatasetException;
import com.agenteval.junit5.annotation.DatasetSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DatasetArgumentsProviderTest {

    @Test
    void shouldLoadDatasetFromClasspath() {
        var provider = new DatasetArgumentsProvider();
        provider.accept(datasetSource("test-dataset.json"));

        var args = provider.provideArguments(mock(ExtensionContext.class)).toList();

        assertThat(args).hasSize(2);
        AgentTestCase first = (AgentTestCase) args.get(0).get()[0];
        assertThat(first.getInput()).isEqualTo("What is the return policy?");
    }

    @Test
    void shouldThrowWhenResourceNotFound() {
        var provider = new DatasetArgumentsProvider();
        provider.accept(datasetSource("nonexistent.json"));

        assertThatThrownBy(() -> provider.provideArguments(mock(ExtensionContext.class)).toList())
                .isInstanceOf(DatasetException.class)
                .hasMessageContaining("not found");
    }

    private DatasetSource datasetSource(String path) {
        return new DatasetSource() {
            @Override
            public String value() { return path; }

            @Override
            public Class<? extends Annotation> annotationType() { return DatasetSource.class; }
        };
    }
}
