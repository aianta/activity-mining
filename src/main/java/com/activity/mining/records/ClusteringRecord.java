package com.activity.mining.records;

import java.util.Date;
import java.util.UUID;

public record ClusteringRecord(
        String sourceExecutionId,
        Date timestamp,
        UUID clusteringId,
        int k,
        int kappa,
        String distanceMetric,
        double silhouetteIndex
) {
}
