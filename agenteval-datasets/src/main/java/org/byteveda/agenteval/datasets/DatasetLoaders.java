package org.byteveda.agenteval.datasets;

import org.byteveda.agenteval.datasets.csv.CsvDatasetLoader;
import org.byteveda.agenteval.datasets.json.JsonDatasetLoader;
import org.byteveda.agenteval.datasets.jsonl.JsonlDatasetLoader;
import org.byteveda.agenteval.datasets.yaml.YamlDatasetLoader;

import java.nio.file.Path;

/**
 * Factory for auto-detecting and loading datasets from file paths.
 *
 * <pre>{@code
 * EvalDataset dataset = DatasetLoaders.forPath(Path.of("data.csv"));
 * }</pre>
 */
public final class DatasetLoaders {

    private DatasetLoaders() {}

    /**
     * Auto-detects the format from the file extension and loads the dataset.
     *
     * @param path the dataset file path
     * @return the loaded dataset
     * @throws DatasetException if loading or format detection fails
     */
    public static EvalDataset forPath(Path path) {
        DatasetFormat format = DatasetFormat.detect(path);
        return switch (format) {
            case JSON -> new JsonDatasetLoader().load(path);
            case JSONL -> new JsonlDatasetLoader().load(path);
            case CSV -> new CsvDatasetLoader().load(path);
            case YAML -> new YamlDatasetLoader().load(path);
        };
    }
}
