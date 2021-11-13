package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.SolutionEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class SolutionEventMapper implements EventToActivityMapper<SolutionEvent> {
    @Override
    public Optional<Activity> map(SolutionEvent event) {
        return Optional.empty();
    }
}
