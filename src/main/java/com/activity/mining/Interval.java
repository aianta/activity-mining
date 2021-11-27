package com.activity.mining;

import cc.kave.commons.model.events.IDEEvent;
import com.activity.mining.mappers.EventToActivityMapper;

import java.time.Instant;
import java.util.List;

public class Interval {

    public Activity activity;
    IDEEvent event;
    long start; //In epoch second
    long end; //In epoch second

    public static long sum(List<Interval> intervals){
        return intervals.stream().mapToLong(Interval::duration).sum();
    }

    public static double mean(List<Interval> intervals){
        return intervals.stream().mapToLong(Interval::duration).average().getAsDouble();
    }

    public long duration(){
        return end - start;
    }

    public void open(IDEEvent e){
        start = e.getTriggeredAt().toEpochSecond();
        event = e;
        activity = EventToActivityMapper.mapEvent(e).orElse(null);
    }

    public Interval close(IDEEvent e){
        //TODO -> might want this to be e.getTriggeredAt()? Not quite sure.
        end = e.getTerminatedAt().toEpochSecond();
        return this;
    }

    public Interval close(long time){
        end = time;
        return this;
    }

    public String toString(){
        return String.format("%12d<-[%20s (%15ds)]->%12d", start, activity != null?activity.name():"null",duration(), end);
    }

}
