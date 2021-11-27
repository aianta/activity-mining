package com.activity.mining;

import com.activity.mining.persistence.DataStore;
import com.activity.mining.sequencers.OneToOne;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/** From a database containing a table of sequences, produces a @see <a href="http://uma-pi1.github.io/mgfsm/">MG-FSM</a>
 *  input file and mines frequent subsequences.
 *
 */
public class SequenceMiner {

    private static final Logger log = LoggerFactory.getLogger(SequenceMiner.class);
    private static final String DEFAULT_DATABASE_PATH = "activity-mining.db";
    private static final String MGFSM_INPUT_DIR = "seqInput";
    private static final String MGFSM_OUTPUT_DIR = "seqOutput";

    public static void main (String [] args){

        //Establish database connection and get sequences
        DataStore db = DataStore.getInstance(DEFAULT_DATABASE_PATH);
        List<Sequence> sequences = db.getSequences(OneToOne.class);

        //Prepare mining environment
        try{
            File inputFile = createMGFSMInputFile(sequences);

            //Generate a unique ID for this mining execution
            UUID currentExecutionId = UUID.randomUUID();

            Path inputDirPath = Path.of(MGFSM_INPUT_DIR);
            Path outputDirPath = Path.of(MGFSM_OUTPUT_DIR);
            Path currentExecutionInPath = Path.of(MGFSM_INPUT_DIR, currentExecutionId.toString());
            Path currentExecutionOutPath = Path.of(MGFSM_OUTPUT_DIR, currentExecutionId.toString());

            //If the MGFSM root input dir doesn't exist create it.
            if(!Files.exists(inputDirPath)){
                Files.createDirectory(inputDirPath);
            }

            //If the MGFSM root output dir doesn't exist create it.
            if (!Files.exists(outputDirPath)){
                Files.createDirectory(outputDirPath);
            }

            /* Create a new sub-directory for the current execution to keep things organized.
               Move the generated input file to the current execution's sub folder.
             */
            Files.createDirectory(currentExecutionInPath);
            Files.move(inputFile.toPath(), currentExecutionInPath);

            /* Create a corresponding subfolder in the root output folder for the current execution. */
            Files.createDirectory(currentExecutionOutPath);

        }catch (IOException e){
            log.error(e.getMessage(),e);
        }
    }

    public static File executeMGFSM(Path input, Path output){

        log.info("Executing MGFSM with input folder: {} and output folder: {}", input.toString(), output.toString());

        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "mgfsm.jar",
                "-i", input.toString() ,
                "-o", output.toString() );

        return null;
    }

    /**
     * Creates an input file for MG-FSM containing a list of sequences.
     * @param sequences sequences to write to MG-FSM input file.
     * @return The MG-FSM input file.
     */
    public static File createMGFSMInputFile(List<Sequence> sequences){
        if (sequences.size() == 0){
            log.error("Cannot create MG-FSM input file with no sequences");
            return null;
        }

        //Must be .txt file extension
        File result = new File(makeFriendly(sequences.get(0).sequencer()) + ".txt");
        try(FileWriter fw = new FileWriter(result);
            BufferedWriter bw = new BufferedWriter(fw);
        ) {

            /** Create sequence entries following the format specified here:
             * http://uma-pi1.github.io/mgfsm/
             */
            sequences.forEach(sequence -> {

                //Begin with a sequence id -> In our case the sessionId
                String entry = sequence.sessionId();

                StringBuilder sb = new StringBuilder();

                char [] sequenceChars = sequence.sequence().toCharArray();
                IntStream.range(0,sequenceChars.length)
                        .mapToObj(i->sequenceChars[i])
                        .forEach(c->sb.append(" "+ c));

                entry = entry + sb.toString() + "\n";

                try {
                    bw.write(entry);

                } catch (IOException e) {
                    log.error("Error writing sequence entry!");
                    log.error(e.getMessage(), e);

                }
            });

            bw.flush();

        } catch (IOException e) {
            log.error("Error creating MG-FSM input file.");
            log.error(e.getMessage(),e);
        }

        return result;
    }

    /** Makes a sequencer name file name friendly
     * @param sequencerName
     * @return a file name friendly string for the sequencer
     */
    private static String makeFriendly(String sequencerName){
        String [] parts = sequencerName.split("[.]");
        return parts[parts.length-1];
    }



}
