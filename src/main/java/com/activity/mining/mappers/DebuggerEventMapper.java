package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.DebuggerEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class DebuggerEventMapper implements EventToActivityMapper<DebuggerEvent> {
    @Override
    public Optional<Activity> map(DebuggerEvent event) {
        return Optional.of(Activity.Development);
    }
}
