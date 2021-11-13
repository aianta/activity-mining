package com.activity.mining.mappers;

import cc.kave.commons.model.events.completionevents.CompletionEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class CompletionEventMapper implements EventToActivityMapper<CompletionEvent> {
    @Override
    public Optional<Activity> map(CompletionEvent event) {
        return Optional.of(Activity.Development);
    }
}
