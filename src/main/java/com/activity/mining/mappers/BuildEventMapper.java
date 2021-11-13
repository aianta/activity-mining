package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.BuildEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class BuildEventMapper implements EventToActivityMapper<BuildEvent> {
    @Override
    public Optional<Activity> map(BuildEvent event) {
        return Optional.of(Activity.Waiting);
    }
}
