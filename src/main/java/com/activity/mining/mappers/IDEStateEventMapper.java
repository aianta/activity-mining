package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.IDEStateEvent;
import cc.kave.commons.model.events.visualstudio.LifecyclePhase;
import com.activity.mining.Activity;

import java.util.Optional;

public class IDEStateEventMapper implements EventToActivityMapper<IDEStateEvent> {
    @Override
    public Optional<Activity> map(IDEStateEvent event) {
        if (event.IDELifecyclePhase == LifecyclePhase.Runtime){
            return Optional.empty();
        }

        return Optional.of(event.IDELifecyclePhase == LifecyclePhase.Startup?Activity.EnterIDE:Activity.LeaveIDE);
    }
}
