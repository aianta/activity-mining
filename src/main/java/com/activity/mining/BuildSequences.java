package com.activity.mining;

import com.activity.mining.persistence.DataStore;
import com.activity.mining.records.Sequence;
import com.activity.mining.sequencers.OneToOne;
import com.activity.mining.sequencers.Sequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.activity.mining.IntervalGenerator.*;

/** From a database containing a table of events, converts events into intervals
 *  and uses a sequencer to turn those intervals into a sequence.
 *
 *  Inserts one sequence for each unique event session found.
 */
public class BuildSequences {

    private static final Logger log = LoggerFactory.getLogger(BuildSequences.class);
    private static final String DEFAULT_DATABASE_PATH = "activity-mining.db";

    public static void main (String [] args){

        log.info("Initalizing DataStore");
        DataStore db = DataStore.getInstance(DEFAULT_DATABASE_PATH);

        log.info("Getting unique sessions... this could take around a minute.");

        db.getUniqueSessions().forEach(
                sessionId->{
                    log.info("Computing intervals for session {}", sessionId);
                    List<Interval> intervals = generateIntervals(db.getSessionEvents(sessionId));
                    log.info("Sequencing intervals...");
                    Sequencer oneToOne = new OneToOne();
                    Sequence s = Sequencer.sequence(oneToOne, sessionId, intervals);
                    log.info("Saving sequence to database");
                    db.insert(s);
                }
        );

    }



}
