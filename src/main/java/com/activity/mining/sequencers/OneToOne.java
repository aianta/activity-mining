package com.activity.mining.sequencers;

import com.activity.mining.Interval;

import java.util.List;

public class OneToOne implements Sequencer{
    @Override
    public String sequence(List<Interval> intervals) {

        return intervals.stream()
                .map(i->i.activity.symbol)
                .collect(
                        StringBuilder::new,
                        StringBuilder::append,
                        StringBuilder::append
                ).toString();
    }


}
