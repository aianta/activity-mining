package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.FindEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class FindEventMapper implements EventToActivityMapper<FindEvent> {
    @Override
    public Optional<Activity> map(FindEvent event) {
        return Optional.of(Activity.Navigation);
    }
}
