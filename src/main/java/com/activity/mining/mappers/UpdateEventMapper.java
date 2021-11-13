package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.UpdateEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class UpdateEventMapper implements EventToActivityMapper<UpdateEvent> {
    @Override
    public Optional<Activity> map(UpdateEvent event) {
        return Optional.of(Activity.LocalConfiguration);
    }
}
