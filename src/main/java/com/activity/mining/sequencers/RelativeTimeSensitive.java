package com.activity.mining.sequencers;

import com.activity.mining.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public class RelativeTimeSensitive implements Sequencer{
    private static final Logger log = LoggerFactory.getLogger(RelativeTimeSensitive.class);

    @Override
    public String sequence(List<Interval> intervals) {

        long totalDuration = intervals.stream().mapToLong(Interval::duration).sum();
        double percent = 0.01;

        long timeUnit = (long)Math.floor((double)totalDuration*percent);
        log.info("Total sequence length {}s 1% time unit: {}",totalDuration, timeUnit);

        StringBuilder sb = new StringBuilder();
        Iterator<Interval> it = intervals.iterator();
        while (it.hasNext()){
            Interval interval = it.next();
            var count = timeUnit > 0? Math.floorDiv(interval.duration(), timeUnit):0; //Divide interval into 1% time chunks, avoid dividing by 0
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
