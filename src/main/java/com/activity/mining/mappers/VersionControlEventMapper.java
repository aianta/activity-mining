package com.activity.mining.mappers;

import cc.kave.commons.model.events.versioncontrolevents.VersionControlEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class VersionControlEventMapper implements EventToActivityMapper<VersionControlEvent> {
    @Override
    public Optional<Activity> map(VersionControlEvent event) {
        return Optional.of(Activity.ProjectManagement);
    }
}
