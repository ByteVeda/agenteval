package org.byteveda.agenteval.replay;

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
 * Persists and loads {@link Recording} data as JSON files.
 *
 * <p>Recording names are validated to prevent path traversal attacks.
 * Files are stored as {@code <name>.recording.json} in the configured directory.</p>
 *
 * <pre>{@code
 * RecordingStore store = new RecordingStore(Path.of("recordings"));
 * store.save(recording);
 * Optional<Recording> loaded = store.load("my-recording");
 * }</pre>
 */
public final class RecordingStore {

    private static final Pattern VALID_NAME =
            Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");
    private static final String EXTENSION = ".recording.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path directory;

    public RecordingStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory must not be null");
    }

    /**
     * Saves a recording to disk.
     *
     * @param recording the recording to save
     * @throws RecordingIOException if writing fails
     */
    public void save(Recording recording) {
        Objects.requireNonNull(recording, "recording must not be null");
        validateName(recording.name());

        try {
            Files.createDirectories(directory);
            Path file = resolve(recording.name());
            MAPPER.writeValue(file.toFile(), recording);
        } catch (IOException e) {
            throw new RecordingIOException(
                    "Failed to save recording '" + recording.name() + "'", e);
        }
    }

    /**
     * Loads a recording by name.
     *
     * @param name the recording name
     * @return the recording data, or empty if not found
     * @throws RecordingIOException if reading fails
     */
    public Optional<Recording> load(String name) {
        validateName(name);
        Path file = resolve(name);

        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try {
            return Optional.of(MAPPER.readValue(file.toFile(), Recording.class));
        } catch (IOException e) {
            throw new RecordingIOException(
                    "Failed to load recording '" + name + "'", e);
        }
    }

    /**
     * Checks whether a recording exists.
     */
    public boolean exists(String name) {
        validateName(name);
        return Files.exists(resolve(name));
    }

    /**
     * Deletes a recording.
     *
     * @param name the recording name
     * @return true if the recording was deleted, false if it did not exist
     * @throws RecordingIOException if deletion fails
     */
    public boolean delete(String name) {
        validateName(name);
        try {
            return Files.deleteIfExists(resolve(name));
        } catch (IOException e) {
            throw new RecordingIOException(
                    "Failed to delete recording '" + name + "'", e);
        }
    }

    private Path resolve(String name) {
        return directory.resolve(name + EXTENSION);
    }

    private static void validateName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recording name must not be null or empty");
        }
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Invalid recording name: '" + name
                            + "'. Must match [a-zA-Z0-9][a-zA-Z0-9_.-]*");
        }
        if (name.contains("..")) {
            throw new IllegalArgumentException(
                    "Recording name must not contain '..'");
        }
    }

    /**
     * Runtime exception for recording I/O failures.
     */
    public static final class RecordingIOException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public RecordingIOException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
