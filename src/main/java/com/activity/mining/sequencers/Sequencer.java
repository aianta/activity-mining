package com.activity.mining.sequencers;

import com.activity.mining.Interval;
import com.activity.mining.Sequence;
import com.activity.mining.SequenceData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface Sequencer {



    static SequenceData sequence(Sequencer s, Map<String,List<Interval>> intervalDataset){
        return new SequenceData(intervalDataset.entrySet().stream()
                .map(e->new Sequence(e.getKey(), s.getClass().getName(), s.sequence(e.getValue())))
                .collect(Collectors.toList()));
    }

    static Sequence sequence(Sequencer s, String sessionUUID, List<Interval> intervals){
        return new Sequence(sessionUUID, s.getClass().getName(),s.sequence(intervals));
    }

    String sequence(List<Interval> intervals);

}
