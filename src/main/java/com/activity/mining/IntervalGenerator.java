package com.activity.mining;


import cc.kave.commons.model.events.IDEEvent;
import cc.kave.commons.utils.io.IReadingArchive;
import cc.kave.commons.utils.io.ReadingArchive;
import cc.kave.commons.utils.io.json.JsonUtils;
import com.activity.mining.mappers.EventToActivityMapper;
import com.activity.mining.persistence.DataStore;
import com.activity.mining.sequencers.OneToOne;
import com.activity.mining.sequencers.Sequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.Instant;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;


public class IntervalGenerator {

    private static final Logger log = LoggerFactory.getLogger(IntervalGenerator.class);
    private static final String DEFAULT_DATA_PATH = "./Events-170301-2/";
    private static final String DEFAULT_DATABASE_PATH = "activity-mining.db";
    private static final int STARTING_ARCHIVE = 0;
    private static final int STARTING_RECORD = 0;

    String rootDataDirectoryPath;
    Map<String, List<IDEEvent>> eventDataset;
    Map<String, List<Interval>> intervalDataset;

    public IntervalGenerator(String rootPath){
        this.rootDataDirectoryPath = rootPath;
    }

    public static void main (String args []){

        log.info("Executing with {} arguments. Root data path: {} ", args.length, args.length > 0 ? args[0]: DEFAULT_DATA_PATH);

        log.info("Initializing DataStore");
        DataStore.getInstance(DEFAULT_DATABASE_PATH);

        //Override default data path with first string argument if provided.
        IntervalGenerator ig = new IntervalGenerator(args.length > 0?args[0]:DEFAULT_DATA_PATH);
        ig.loadData(STARTING_ARCHIVE, STARTING_RECORD);

//
//        ig.orderListsByTimestamp(ig.eventDataset);
//        ig.intervalDataset = ig.generateIntervals(ig.eventDataset);
//        //ig.printIntervals();
//        SequenceData sData = Sequencer.sequence(new OneToOne(), ig.intervalDataset);
//        sData.sequenceData()
//                .stream()
//                .forEach(s->log.info("{} [{}]: {}", s.sessionId(),s.length(), s.sequence()));
//
//        try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("onetooneSequenceData.dat"))){
//            out.writeObject(sData);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void printIntervals(){
        printIntervals(intervalDataset);
    }

    public void printIntervals(Map<String,List<Interval>> intervalDataset){
        for (Map.Entry<String,List<Interval>> entry:intervalDataset.entrySet()){
            log.info("Intervals for session {} -> Total Duration: {}s over {} intervals", entry.getKey(), Interval.sum(entry.getValue()), entry.getValue().size());
            entry.getValue().stream().map(Interval::toString).map(s->"\t" + s).forEach(log::info);
        }
        log.info("{} interval sessions", intervalDataset.size());

        log.info("Average session length: {}s",
                intervalDataset.entrySet().stream()
                        .mapToLong(e->Interval.sum(e.getValue()))
                        .average()
                        .getAsDouble()
                );
        long maxLength =intervalDataset.entrySet().stream()
                .mapToLong(e->Interval.sum(e.getValue()))
                .max()
                .getAsLong();
        log.info("Max session length: {}",
                maxLength
        );



    }

    /**
     * Loads data from provided data path into a map {@link IntervalGenerator#eventDataset} <sessionId, List<Events>>.
     */
    public void loadData(int startingArchive, int startingRecord){
        Set<String> userZips = IoHelper.findAllZips(rootDataDirectoryPath);

        eventDataset = new HashMap<>();
        int archiveCount = 0;
        for (String zip:userZips){
            archiveCount++;

            //Skip already processed archives
            if (startingArchive > archiveCount){
                log.info("Skipping archive {}", archiveCount);
                continue;
            }

            log.info("ArchiveCount: {}", archiveCount);
            String sessionId = "missing";
            try(IReadingArchive ra = new ReadingArchive(new File(rootDataDirectoryPath, zip))){
                int count = 0;
                while (ra.hasNext()){
                    log.info("Archive Event Count:{} [{}]",archiveCount, count++);

                    //Skip already processed records
                    if(startingArchive == archiveCount && startingRecord > count){
                        log.info("Skipping record {}", count);
                        continue;
                    }

                    IDEEvent event = ra.getNext(IDEEvent.class);
                    sessionId = event.IDESessionUUID;

                    DataStore.getInstance().insert(event);


//                    List<IDEEvent> eventList = eventDataset.getOrDefault(sessionId, new ArrayList<>());
//                    eventList.add(event);
//                    eventDataset.put(sessionId, eventList);
                  }
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }



        }



        log.info("Loaded {} session datasets", eventDataset.size());
    }

    public void orderListsByTimestamp(Map<String,List<IDEEvent>> eventDataset){
        for(Map.Entry<String,List<IDEEvent>> entry: eventDataset.entrySet()){
            List<IDEEvent> events = entry.getValue();
            events.sort(Comparator.comparing(IDEEvent::getTriggeredAt));
        }
    }

    public Map<String, List<Interval>> generateIntervals(Map<String, List<IDEEvent>> eventDataset){
        Map<String, List<Interval>> result = new HashMap<>();

        for(Map.Entry<String, List<IDEEvent>> entry: eventDataset.entrySet()){
            result.put(entry.getKey(), generateIntervals(entry.getValue()));
        }

        return result;
    }

    /**
     *
     * @param events
     * @return
     */
    public static List<Interval> generateIntervals(List<IDEEvent> events){

        List<Interval> result = new ArrayList<>();
        /** Initialize an iterator for processing the events.
         * Assumes {@param events} represents the events for a developer session on one day
         * Assumes {@param events} is ordered by timestamp
         */
        ListIterator<IDEEvent> iterator = events.listIterator();

        Interval currentInterval = null;

        final long timeout = 15;
        long currentTick = -1;
        while (iterator.hasNext()){
            IDEEvent event = iterator.next();
            Activity correspondingActivity = EventToActivityMapper.mapEvent(event).orElse(null);

            //just keep going if the duration of the event cannot be determined.
            //just keep going if the activity mapper wants to drop this event.
            //TODO - might have to think of the implications of this...
            if(event.Duration == null || correspondingActivity == null){
                continue;
            }

            //log.info("Processing event: {}", event);

            //If the new event happened the next day
            // Discard the last interval
            if(currentInterval!= null &&
                    Instant.ofEpochSecond(currentInterval.start)
                    .atZone(event.getTriggeredAt().getZone()).getDayOfYear() !=
                    event.getTriggeredAt().getDayOfYear()
            ){
                currentTick = event.getTriggeredAt().toEpochSecond();
                currentInterval = null;
            }

            //The current event starts more than timeout (15) seconds after the last event.
            if (currentTick != -1 && event.getTriggeredAt().toEpochSecond() > currentTick + timeout){



                //Close the current interval
                result.add(currentInterval.close(currentTick + timeout));
                /* We create an inactivity interval from when the last interval timed out.
                 */
                currentInterval = new Interval();
                currentInterval.start = currentTick + timeout;
                currentInterval.activity = Activity.Inactive;

            }

            //Update current tick
            currentTick = event.getTerminatedAt().toEpochSecond();

            if (currentInterval == null){
                currentInterval = new Interval();
                currentInterval.open(event);
            }else if(correspondingActivity == Activity.LeaveIDE){
                result.add(currentInterval.close(event));
            }else if (correspondingActivity != currentInterval.activity){
                result.add(currentInterval.close(event));
                currentInterval = new Interval();
                currentInterval.open(event);
            }


        }

        return result;
    }




}
