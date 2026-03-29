package org.byteveda.agenteval.datasets;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Interface for loading evaluation datasets from various sources.
 */
public interface DatasetLoader {

    /**
     * Loads a dataset from a file path.
     *
     * @param path the path to the dataset file
     * @return the loaded dataset
     * @throws DatasetException if loading fails
     */
    EvalDataset load(Path path);

    /**
     * Loads a dataset from an input stream.
     *
     * @param inputStream the input stream to read from
     * @return the loaded dataset
     * @throws DatasetException if loading fails
     */
    EvalDataset load(InputStream inputStream);
}
