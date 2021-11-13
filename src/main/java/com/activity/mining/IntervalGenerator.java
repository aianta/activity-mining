package com.activity.mining;

import cc.kave.commons.model.events.IDEEvent;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import com.activity.mining.mappers.EventToActivityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class IntervalGenerator {

    private static final Logger log = LoggerFactory.getLogger(IntervalGenerator.class);
    private static final String DEFAULT_DATA_PATH = "./Events-170301-2/";

    String rootDataDirectoryPath;
    Map<String, List<IDEEvent>> eventDataset;

    public IntervalGenerator(String rootPath){
        this.rootDataDirectoryPath = rootPath;
    }

    public static void main (String args []){

        log.info("Executing with {} arguments. Root data path: {} ", args.length, args.length > 0 ? args[0]: DEFAULT_DATA_PATH);

        //Override default data path with first string argument if provided.
        IntervalGenerator ig = new IntervalGenerator(args.length > 0?args[0]:DEFAULT_DATA_PATH);
        ig.loadData();

        EventToActivityMapper.mapEvents(ig.eventDataset.entrySet().iterator().next().getValue())
                .entrySet().stream().forEach(entry->log.info("[{}]{} -> {}",
                        entry.getKey().getTriggeredAt().toEpochSecond(),
                        entry.getKey().getClass().getName(),
                        entry.getValue().name()));

    }

    public void loadData(){
        Set<String> userZips = IoHelper.findAllZips(rootDataDirectoryPath);

        eventDataset = userZips.stream()
                .peek(zip->log.info("Extracting events from {}", zip))
                .map(
                zip->{
                    List<IDEEvent> eventList = new ArrayList<>();
                    String sessionId = "missing";
                    try(IReadingArchive ra = new ReadingArchive(new File(rootDataDirectoryPath, zip))){
                        while (ra.hasNext() && eventList.size() < 200){
                            IDEEvent event = ra.getNext(IDEEvent.class);
                            sessionId = event.IDESessionUUID;
                            eventList.add(event);
                            log.info("Loaded {}/{} ({}%) for session: {}", eventList.size(), ra.getNumberOfEntries(), (double)eventList.size()/(double)ra.getNumberOfEntries()*100, sessionId);
                        }
                    }
                    log.info("Loaded {} events for session {}", eventList.size(), sessionId);

                    //Sort events in trigger order
                    eventList.sort(Comparator.comparing(IDEEvent::getTriggeredAt));

                    return Map.entry(sessionId, eventList);
                }
        ).collect(
                HashMap::new,
                (map,entry)->map.put(entry.getKey(),entry.getValue()),
                HashMap::putAll
        );

        log.info("Loaded {} session datasets", eventDataset.size());
    }

    public List<Interval> generateIntervals(List<IDEEvent> events){
        return null;
    }


}
