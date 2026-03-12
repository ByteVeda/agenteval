package com.agenteval.datasets;

import java.nio.file.Path;

/**
 * Supported dataset file formats.
 */
public enum DatasetFormat {
    JSON, JSONL, CSV;

    /**
     * Auto-detects the dataset format from a file path extension.
     *
     * @param path the file path to inspect
     * @return the detected format
     * @throws DatasetException if the extension is not recognized
     */
    public static DatasetFormat detect(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            throw new DatasetException("Cannot detect format from root path");
        }
        String name = fileName.toString().toLowerCase();
        if (name.endsWith(".jsonl")) {
            return JSONL;
        } else if (name.endsWith(".json")) {
            return JSON;
        } else if (name.endsWith(".csv")) {
            return CSV;
        }
        throw new DatasetException("Unsupported dataset format: " + name
                + ". Supported extensions: .json, .jsonl, .csv");
    }
}
