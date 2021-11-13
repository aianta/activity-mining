package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.InstallEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class InstallEventMapper implements EventToActivityMapper<InstallEvent> {
    @Override
    public Optional<Activity> map(InstallEvent event) {
        return Optional.of(Activity.LocalConfiguration);
    }
}
