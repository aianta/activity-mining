package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.EditEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class EditEventMapper implements EventToActivityMapper<EditEvent> {
    @Override
    public Optional<Activity> map(EditEvent event) {
        return Optional.of(Activity.Development);
    }
}
