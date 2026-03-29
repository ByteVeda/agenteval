package org.byteveda.agenteval.reporting.snapshot;

import org.byteveda.agenteval.reporting.ReportException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Persists and loads {@link SnapshotData} as JSON files.
 *
 * <p>Snapshot names are validated to prevent path traversal attacks.
 * Files are stored as {@code <name>.snapshot.json} in the configured directory.</p>
 */
public final class SnapshotStore {

    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");
    private static final String EXTENSION = ".snapshot.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path directory;

    public SnapshotStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory must not be null");
    }

    /**
     * Saves snapshot data to disk.
     *
     * @param snapshot the snapshot to save
     * @throws ReportException if writing fails
     */
    public void save(SnapshotData snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        validateName(snapshot.snapshotName());

        try {
            Files.createDirectories(directory);
            Path file = resolve(snapshot.snapshotName());
            MAPPER.writeValue(file.toFile(), snapshot);
        } catch (IOException e) {
            throw new ReportException(
                    "Failed to save snapshot '" + snapshot.snapshotName() + "'", e);
        }
    }

    /**
     * Loads a snapshot by name.
     *
     * @param name the snapshot name
     * @return the snapshot data, or empty if not found
     * @throws ReportException if reading fails
     */
    public Optional<SnapshotData> load(String name) {
        validateName(name);
        Path file = resolve(name);

        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try {
            return Optional.of(MAPPER.readValue(file.toFile(), SnapshotData.class));
        } catch (IOException e) {
            throw new ReportException("Failed to load snapshot '" + name + "'", e);
        }
    }

    /**
     * Checks whether a snapshot exists.
     */
    public boolean exists(String name) {
        validateName(name);
        return Files.exists(resolve(name));
    }

    /**
     * Deletes a snapshot.
     *
     * @param name the snapshot name
     * @return true if the snapshot was deleted, false if it did not exist
     * @throws ReportException if deletion fails
     */
    public boolean delete(String name) {
        validateName(name);
        try {
            return Files.deleteIfExists(resolve(name));
        } catch (IOException e) {
            throw new ReportException("Failed to delete snapshot '" + name + "'", e);
        }
    }

    private Path resolve(String name) {
        return directory.resolve(name + EXTENSION);
    }

    private static void validateName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Snapshot name must not be null or empty");
        }
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Invalid snapshot name: '" + name
                            + "'. Must match [a-zA-Z0-9][a-zA-Z0-9_.-]*");
        }
        if (name.contains("..")) {
            throw new IllegalArgumentException(
                    "Snapshot name must not contain '..'");
        }
    }
}
