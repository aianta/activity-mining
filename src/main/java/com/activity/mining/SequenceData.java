package com.activity.mining;

import java.io.Serializable;
import java.util.List;

public record SequenceData(List<Sequence> sequenceData) implements Serializable {
}
