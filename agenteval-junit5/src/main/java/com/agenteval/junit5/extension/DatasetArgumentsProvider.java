package com.agenteval.junit5.extension;

import com.agenteval.datasets.DatasetException;
import com.agenteval.datasets.EvalDataset;
import com.agenteval.datasets.json.JsonDatasetLoader;
import com.agenteval.junit5.annotation.DatasetSource;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.io.InputStream;
import java.util.stream.Stream;

/**
 * JUnit 5 {@link ArgumentsProvider} that loads {@code AgentTestCase} instances
 * from a JSON dataset specified by {@link DatasetSource}.
 */
public final class DatasetArgumentsProvider
        implements ArgumentsProvider, AnnotationConsumer<DatasetSource> {

    private String resourcePath;

    @Override
    public void accept(DatasetSource annotation) {
        this.resourcePath = annotation.value();
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath);
        if (is == null) {
            throw new DatasetException("Dataset resource not found on classpath: " + resourcePath);
        }

        EvalDataset dataset = new JsonDatasetLoader().load(is);
        return dataset.getTestCases().stream().map(Arguments::of);
    }
}
