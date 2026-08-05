package org.byteveda.agenteval.datasets.version;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.datasets.EvalDataset;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An {@link EvalDataset} paired with version metadata.
 *
 * <p>Provides delegate methods for convenient access to the underlying dataset.</p>
 *
 * @param dataset the evaluation dataset
 * @param version the version metadata
 */
public record VersionedDataset(EvalDataset dataset, DatasetVersion version) {

    public VersionedDataset {
        Objects.requireNonNull(dataset, "dataset must not be null");
        Objects.requireNonNull(version, "version must not be null");
    }

    /** Delegate: returns the dataset name. */
    public String getName() { return dataset.getName(); }

    /** Delegate: returns the dataset version string. */
    public String getVersion() { return dataset.getVersion(); }

    /** Delegate: returns the test cases. */
    public List<AgentTestCase> getTestCases() { return dataset.getTestCases(); }

    /** Delegate: returns the dataset metadata. */
    public Map<String, Object> getMetadata() { return dataset.getMetadata(); }

    /** Delegate: returns the number of test cases. */
    public int size() { return dataset.size(); }
}
