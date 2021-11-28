package com.activity.mining.records;

public record ClusterRecord(String executionId, int numClusters, int maxIterations, String distanceMetric) {
}
