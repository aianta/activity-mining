package com.activity.mining.records;

import cc.kave.commons.model.events.IDEEvent;
import com.activity.mining.Activity;

public record IntervalRecord(long start, long end, Activity activity, IDEEvent event) {


    public long duration(){
        return end - start;
    }

}
