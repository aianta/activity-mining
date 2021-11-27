package com.activity.mining.records;

import com.activity.mining.records.Sequence;

import java.io.Serializable;
import java.util.List;

public record SequenceData(List<Sequence> sequenceData) implements Serializable {
}
