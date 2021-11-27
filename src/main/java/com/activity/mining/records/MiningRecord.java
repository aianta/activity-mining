package com.activity.mining.records;

public record MiningRecord(
        String executionId,
        String timestamp,
        long startTimestamp,
        long endTimestamp,
        int support,
        int gamma,
        int lambda,
        String sequencer) {
}
