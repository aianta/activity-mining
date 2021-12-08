package com.activity.mining;

import com.activity.mining.persistence.DataStore;
import com.activity.mining.records.Sequence;
import com.activity.mining.sequencers.RelativeTimeSensitive;
import com.activity.mining.sequencers.Sequencer;
import com.activity.mining.sequencers.TimeSensitive5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ListIterator;

import static com.activity.mining.IntervalGenerator.*;

/** From a database containing a table of events, converts events into intervals
 *  and uses a sequencer to turn those intervals into a sequence.
 *
 *  Inserts one sequence for each unique event session found.
 */
public class BuildSequences {

    private static final Logger log = LoggerFactory.getLogger(BuildSequences.class);
    private static final String DEFAULT_DATABASE_PATH = "activity-mining.db";
    private static final Sequencer sequencer = new TimeSensitive5();
    private static final int SKIP = 1605; //Number of sequences to skip -> used to resume in progress sequence building that got interrupted.

    public static void main (String [] args){

        log.info("Initalizing DataStore");
        DataStore db = DataStore.getInstance(DEFAULT_DATABASE_PATH);

        log.info("Getting unique sessions... this could take around a minute.");

        ListIterator<String> sessionIterator = db.getUniqueSessions().listIterator();
        while (sessionIterator.hasNext()){
            if (sessionIterator.nextIndex() < SKIP){
                log.info("Skipping session {}", sessionIterator.nextIndex());
                sessionIterator.next();
                continue;
            }
            String sessionId = sessionIterator.next();
            log.info("Computing intervals for session {}", sessionId);
            List<Interval> intervals = generateIntervals(db.getSessionEvents(sessionId));
            log.info("Sequencing intervals...");

            Sequence s = Sequencer.sequence(sequencer, sessionId, intervals);
            log.info("Saving sequence {} to database", s.sequence());
            db.insert(s);
        }

    }



}
