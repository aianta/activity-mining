package com.activity.mining.mappers;

import cc.kave.commons.model.events.*;
import cc.kave.commons.model.events.completionevents.CompletionEvent;
import cc.kave.commons.model.events.testrunevents.TestRunEvent;
import cc.kave.commons.model.events.userprofiles.UserProfileEvent;
import cc.kave.commons.model.events.versioncontrolevents.VersionControlEvent;
import cc.kave.commons.model.events.visualstudio.*;
import com.activity.mining.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public interface EventToActivityMapper<T extends IDEEvent> {

    static final Logger log = LoggerFactory.getLogger(EventToActivityMapper.class);

    String commandCSV = "./CommandIdToActivityMapping.csv";

    BuildEventMapper buildEventMapper = new BuildEventMapper();
    CommandEventMapper commandEventMapper = new CommandEventMapper(commandCSV);
    CompletionEventMapper completionEventMapper = new CompletionEventMapper();
    DebuggerEventMapper debuggerEventMapper = new DebuggerEventMapper();
    DocumentEventMapper documentEventMapper = new DocumentEventMapper();
    EditEventMapper editEventMapper = new EditEventMapper();
    FindEventMapper findEventMapper = new FindEventMapper();
    IDEStateEventMapper ideStateEventMapper = new IDEStateEventMapper();
    InstallEventMapper installEventMapper = new InstallEventMapper();
    SolutionEventMapper solutionEventMapper = new SolutionEventMapper();
    UpdateEventMapper updateEventMapper = new UpdateEventMapper();
    WindowEventMapper windowEventMapper = new WindowEventMapper();
    VersionControlEventMapper versionControlEventMapper = new VersionControlEventMapper();
    NavigationEventMapper navigationEventMapper = new NavigationEventMapper();
    SystemEventMapper systemEventMapper = new SystemEventMapper();

    static Optional<Activity> mapEvent (IDEEvent event){

        if (event instanceof BuildEvent) return buildEventMapper.map((BuildEvent) event);
        if (event instanceof CommandEvent) return commandEventMapper.map((CommandEvent) event);
        if (event instanceof CompletionEvent) return completionEventMapper.map((CompletionEvent) event);
        if (event instanceof DebuggerEvent) return  debuggerEventMapper.map((DebuggerEvent) event);
        if (event instanceof DocumentEvent) return documentEventMapper.map((DocumentEvent) event);
        if (event instanceof EditEvent) return editEventMapper.map((EditEvent) event);
        if (event instanceof FindEvent) return findEventMapper.map((FindEvent) event);
        if (event instanceof IDEStateEvent) return ideStateEventMapper.map((IDEStateEvent) event);
        if (event instanceof InstallEvent) return installEventMapper.map((InstallEvent) event);
        if (event instanceof SolutionEvent) return solutionEventMapper.map((SolutionEvent) event);
        if (event instanceof UpdateEvent) return updateEventMapper.map((UpdateEvent) event);
        if (event instanceof WindowEvent) return windowEventMapper.map((WindowEvent) event);
        if (event instanceof VersionControlEvent) return versionControlEventMapper.map((VersionControlEvent) event);
        if (event instanceof NavigationEvent) return navigationEventMapper.map((NavigationEvent) event);
        if (event instanceof ActivityEvent) return Optional.of(Activity.Any);
        if (event instanceof SystemEvent) return systemEventMapper.map((SystemEvent) event);
        if (event instanceof TestRunEvent) return Optional.of(Activity.Development);
        if (event instanceof UserProfileEvent) return Optional.of(Activity.LocalConfiguration);

        log.error("No event mapper for event! {} {}", event.getClass().getName(),
               event.toString());
        return Optional.of(Activity.Mystery);
    }

    static Map<IDEEvent, Activity> mapEvents (List<IDEEvent> events){
        return events.stream()
                .map(event->Map.entry(event,EventToActivityMapper.mapEvent(event)))
                .filter(entry->entry.getValue().isPresent())
                .map(entry->Map.entry(entry.getKey(), entry.getValue().get()))
                .collect(
                        LinkedHashMap::new,
                        (map,entry)->map.put(entry.getKey(),entry.getValue()),
                        LinkedHashMap::putAll
                );
    }

    Optional<Activity> map(T event);

}
