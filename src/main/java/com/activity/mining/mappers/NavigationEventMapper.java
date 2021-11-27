package com.activity.mining.mappers;

import cc.kave.commons.model.events.NavigationEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class NavigationEventMapper implements EventToActivityMapper<NavigationEvent> {
    @Override
    public Optional<Activity> map(NavigationEvent event) {
        return Optional.of(Activity.Navigation);
    }
}
