package com.activity.mining.mappers;

import cc.kave.commons.model.events.visualstudio.DocumentAction;
import cc.kave.commons.model.events.visualstudio.DocumentEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class DocumentEventMapper implements EventToActivityMapper<DocumentEvent> {
    @Override
    public Optional<Activity> map(DocumentEvent event) {
        return Optional.of(event.Action == DocumentAction.Saved ? Activity.Development : Activity.Navigation);
    }
}
