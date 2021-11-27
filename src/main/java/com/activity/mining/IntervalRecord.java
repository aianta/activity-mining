package com.activity.mining;

import cc.kave.commons.model.events.IDEEvent;

public record IntervalRecord(long start, long end, Activity activity, IDEEvent event) {


    public long duration(){
        return end - start;
    }

}
