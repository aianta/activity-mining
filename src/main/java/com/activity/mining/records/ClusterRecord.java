package com.activity.mining.records;

import java.util.UUID;

public record ClusterRecord(
        String sourceExecutionId,
        UUID clusteringId,
        String embedding,
        String sequence,
        String cluster,
        int iteration,
        double iterationSilhouette
) {
}
