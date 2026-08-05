package org.byteveda.agenteval.datasets.version;

import org.byteveda.agenteval.datasets.DatasetException;
import org.byteveda.agenteval.datasets.EvalDataset;
import org.byteveda.agenteval.datasets.json.JsonDatasetLoader;
import org.byteveda.agenteval.datasets.json.JsonDatasetWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Manages versioned golden datasets with git metadata.
 *
 * <p>Stores versioned datasets in a directory structure:
 * {@code <storageDir>/<datasetName>/<versionLabel>/dataset.json} alongside
 * a {@code version.json} metadata file.</p>
 */
public final class DatasetVersioner {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetVersioner.class);
    private static final String DATASET_FILE = "dataset.json";
    private static final String VERSION_FILE = "version.json";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path storageDir;
    private final GitResolver gitResolver;

    /**
     * Creates a versioner that stores datasets under the given directory
     * and resolves git metadata from the given working directory.
     */
    public DatasetVersioner(Path storageDir, Path gitWorkingDir) {
        this.storageDir = Objects.requireNonNull(storageDir, "storageDir must not be null");
        this.gitResolver = new GitResolver(
                Objects.requireNonNull(gitWorkingDir, "gitWorkingDir must not be null"));
    }

    /**
     * Creates a versioner that stores datasets under the given directory.
     * Git metadata is resolved from the storage directory itself.
     */
    public DatasetVersioner(Path storageDir) {
        this(storageDir, storageDir);
    }

    /**
     * Tags a dataset with a version label, resolves git metadata, and saves it.
     *
     * @param dataset the dataset to version
     * @param label   the version label (e.g., "v1.0")
     * @return the versioned dataset
     */
    public VersionedDataset tag(EvalDataset dataset, String label) {
        Objects.requireNonNull(dataset, "dataset must not be null");
        Objects.requireNonNull(label, "label must not be null");

        String name = dataset.getName();
        if (name == null || name.isBlank()) {
            throw new DatasetException("Dataset must have a name for versioning");
        }

        GitMetadata git = gitResolver.resolve();
        DatasetVersion version = new DatasetVersion(label, git, Instant.now(), null);

        Path versionDir = storageDir.resolve(name).resolve(label);
        try {
            Files.createDirectories(versionDir);
            new JsonDatasetWriter().write(dataset, versionDir.resolve(DATASET_FILE));
            MAPPER.writeValue(versionDir.resolve(VERSION_FILE).toFile(), version);
        } catch (IOException e) {
            throw new DatasetException("Failed to save versioned dataset: " + e.getMessage(), e);
        }

        LOG.info("Tagged dataset '{}' as version '{}'", name, label);
        return new VersionedDataset(dataset, version);
    }

    /**
     * Loads a specific version of a dataset.
     *
     * @param name  the dataset name
     * @param label the version label
     * @return the versioned dataset
     */
    public VersionedDataset load(String name, String label) {
        Path versionDir = storageDir.resolve(name).resolve(label);
        Path datasetFile = versionDir.resolve(DATASET_FILE);
        Path versionFile = versionDir.resolve(VERSION_FILE);

        if (!Files.exists(datasetFile)) {
            throw new DatasetException(
                    "Version '" + label + "' not found for dataset '" + name + "'");
        }

        EvalDataset dataset = new JsonDatasetLoader().load(datasetFile);
        DatasetVersion version;
        try {
            version = MAPPER.readValue(versionFile.toFile(), DatasetVersion.class);
        } catch (IOException e) {
            throw new DatasetException("Failed to read version metadata: " + e.getMessage(), e);
        }

        return new VersionedDataset(dataset, version);
    }

    /**
     * Lists all version labels for a dataset, sorted by creation time (newest first).
     *
     * @param name the dataset name
     * @return the list of version labels
     */
    public List<String> listVersions(String name) {
        Path datasetDir = storageDir.resolve(name);
        if (!Files.isDirectory(datasetDir)) {
            return List.of();
        }

        try (Stream<Path> dirs = Files.list(datasetDir)) {
            return dirs
                    .filter(Files::isDirectory)
                    .filter(d -> Files.exists(d.resolve(VERSION_FILE)))
                    .sorted(Comparator.<Path, Long>comparing(d -> {
                        try {
                            return Files.getLastModifiedTime(d.resolve(VERSION_FILE))
                                    .toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).reversed())
                    .map(d -> d.getFileName().toString())
                    .toList();
        } catch (IOException e) {
            throw new DatasetException("Failed to list versions: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the latest (most recently created) version of a dataset.
     *
     * @param name the dataset name
     * @return the latest versioned dataset
     */
    public VersionedDataset latest(String name) {
        List<String> versions = listVersions(name);
        if (versions.isEmpty()) {
            throw new DatasetException("No versions found for dataset '" + name + "'");
        }
        return load(name, versions.getFirst());
    }
}
