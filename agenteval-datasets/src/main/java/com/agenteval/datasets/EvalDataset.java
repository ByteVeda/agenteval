package com.agenteval.datasets;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.datasets.csv.CsvDatasetWriter;
import com.agenteval.datasets.json.JsonDatasetWriter;
import com.agenteval.datasets.jsonl.JsonlDatasetWriter;
import com.agenteval.datasets.version.DatasetVersioner;
import com.agenteval.datasets.version.VersionedDataset;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable collection of evaluation test cases with metadata.
 */
@JsonDeserialize(builder = EvalDataset.Builder.class)
public final class EvalDataset {

    private final String name;
    private final String version;
    private final List<AgentTestCase> testCases;
    private final Map<String, Object> metadata;

    private EvalDataset(Builder builder) {
        this.name = builder.name;
        this.version = builder.version;
        this.testCases = builder.testCases == null ? List.of() : List.copyOf(builder.testCases);
        this.metadata = builder.metadata == null ? Map.of() : Map.copyOf(builder.metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public List<AgentTestCase> getTestCases() { return testCases; }
    public Map<String, Object> getMetadata() { return metadata; }

    /**
     * Returns the number of test cases in this dataset.
     */
    public int size() {
        return testCases.size();
    }

    /**
     * Saves this dataset to a JSON file.
     *
     * @param path the target file path
     * @throws DatasetException if writing fails
     */
    public void save(Path path) {
        new JsonDatasetWriter().write(this, path);
    }

    /**
     * Saves this dataset to a file in the specified format.
     *
     * @param path the target file path
     * @param format the output format
     * @throws DatasetException if writing fails
     */
    public void save(Path path, DatasetFormat format) {
        switch (format) {
            case JSON -> new JsonDatasetWriter().write(this, path);
            case JSONL -> new JsonlDatasetWriter().write(this, path);
            case CSV -> new CsvDatasetWriter().write(this, path);
            default -> throw new DatasetException("Unsupported format: " + format);
        }
    }

    /**
     * Tags this dataset with a version label and saves it to the storage directory.
     *
     * @param label      the version label (e.g., "v1.0")
     * @param storageDir the directory where versioned datasets are stored
     * @return the versioned dataset with git metadata
     */
    public VersionedDataset tagVersion(String label, Path storageDir) {
        return new DatasetVersioner(storageDir).tag(this, label);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String name;
        private String version;
        private List<AgentTestCase> testCases;
        private Map<String, Object> metadata;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder testCases(List<AgentTestCase> testCases) {
            this.testCases = testCases;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public EvalDataset build() {
            Objects.requireNonNull(testCases, "testCases must not be null");
            return new EvalDataset(this);
        }
    }
}
