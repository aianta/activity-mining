package com.activity.mining.sequencers;

import com.activity.mining.Interval;

import java.util.Iterator;
import java.util.List;

/**
 * Squencer that encodes time in increments of ~5 seconds. If an activity A took 15 seconds in the
 * source intervals this sequencer will produce AAA for that activity in the final sequence.
 */
public class TimeSensitive5 implements Sequencer{
    @Override
    public String sequence(List<Interval> intervals) {

        StringBuilder sb = new StringBuilder();
        Iterator<Interval> it = intervals.iterator();
        while (it.hasNext()){
            Interval interval = it.next();
            var count = Math.floorDiv(interval.duration(), 5); //Divide interval into 5 second chunks
            if (count > 0){
                while (count > 0){
                    sb.append(interval.activity.symbol);
                    count--;
                }
            }else{
                sb.append(interval.activity.symbol);
            }
        }

        return sb.toString();
    }
}
