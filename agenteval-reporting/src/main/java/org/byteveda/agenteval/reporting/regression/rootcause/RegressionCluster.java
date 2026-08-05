package org.byteveda.agenteval.reporting.regression.rootcause;

import org.byteveda.agenteval.reporting.regression.CaseStatusChange;

import java.util.List;
import java.util.Objects;

/**
 * A cluster of regressed test cases sharing common failure patterns.
 *
 * @param clusterName descriptive name for this cluster (e.g., the shared regressed metrics)
 * @param cases the regressed cases in this cluster
 * @param impactScore the impact score: |avgDelta| x clusterSize
 * @param patterns detected failure patterns in this cluster
 */
public record RegressionCluster(
        String clusterName,
        List<CaseStatusChange> cases,
        double impactScore,
        List<FailurePattern> patterns
) {
    public RegressionCluster {
        Objects.requireNonNull(clusterName, "clusterName must not be null");
        cases = List.copyOf(cases);
        patterns = List.copyOf(patterns);
    }
}
