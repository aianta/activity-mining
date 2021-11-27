package com.activity.mining.mappers;

import cc.kave.commons.model.events.SystemEvent;
import com.activity.mining.Activity;

import java.util.Optional;

public class SystemEventMapper implements EventToActivityMapper<SystemEvent> {
    @Override
    public Optional<Activity> map(SystemEvent event) {

        return Optional.of(switch (event.Type){
            case Lock -> Activity.LeaveIDE;
            case Unlock -> Activity.EnterIDE;
            case Resume -> Activity.EnterIDE;
            case Unknown -> Activity.Any;
            case RemoteConnect -> Activity.EnterIDE;
            case RemoteDisconnect -> Activity.LeaveIDE;
            case Suspend -> Activity.LeaveIDE;
        });


    }
}
