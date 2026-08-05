package org.byteveda.agenteval.junit5.extension;

import org.byteveda.agenteval.datasets.DatasetException;
import org.byteveda.agenteval.datasets.DatasetFormat;
import org.byteveda.agenteval.datasets.EvalDataset;
import org.byteveda.agenteval.datasets.csv.CsvDatasetLoader;
import org.byteveda.agenteval.datasets.json.JsonDatasetLoader;
import org.byteveda.agenteval.datasets.jsonl.JsonlDatasetLoader;
import org.byteveda.agenteval.datasets.yaml.YamlDatasetLoader;
import org.byteveda.agenteval.junit5.annotation.DatasetSource;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * JUnit 5 {@link ArgumentsProvider} that loads {@code AgentTestCase} instances
 * from a dataset specified by {@link DatasetSource}.
 *
 * <p>Auto-detects format from the resource path extension (.json, .jsonl, .csv).</p>
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

        DatasetFormat format = DatasetFormat.detect(Path.of(resourcePath));
        EvalDataset dataset = switch (format) {
            case JSON -> new JsonDatasetLoader().load(is);
            case JSONL -> new JsonlDatasetLoader().load(is);
            case CSV -> new CsvDatasetLoader().load(is);
            case YAML -> new YamlDatasetLoader().load(is);
        };
        return dataset.getTestCases().stream().map(Arguments::of);
    }
}
