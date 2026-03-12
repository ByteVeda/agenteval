package com.agenteval.datasets;

import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Interface for writing evaluation datasets to various targets.
 */
public interface DatasetWriter {

    /**
     * Writes a dataset to a file path.
     *
     * @param dataset the dataset to write
     * @param path the target file path
     * @throws DatasetException if writing fails
     */
    void write(EvalDataset dataset, Path path);

    /**
     * Writes a dataset to an output stream.
     *
     * @param dataset the dataset to write
     * @param outputStream the target output stream
     * @throws DatasetException if writing fails
     */
    void write(EvalDataset dataset, OutputStream outputStream);
}
