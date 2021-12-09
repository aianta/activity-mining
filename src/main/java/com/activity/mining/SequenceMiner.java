package com.activity.mining;

import com.activity.mining.persistence.DataStore;
import com.activity.mining.records.FrequentSubSequence;
import com.activity.mining.records.MiningRecord;
import com.activity.mining.records.Sequence;
import com.activity.mining.sequencers.OneToOne;
import com.activity.mining.sequencers.RelativeTimeSensitive;
import com.activity.mining.sequencers.Sequencer;
import com.activity.mining.sequencers.TimeSensitive5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.spec.MGF1ParameterSpec;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** From a database containing a table of sequences, produces a @see <a href="http://uma-pi1.github.io/mgfsm/">MG-FSM</a>
 *  input file and mines frequent subsequences.
 *
 */
public class SequenceMiner {

    private static final Logger log = LoggerFactory.getLogger(SequenceMiner.class);
    private static String DEFAULT_DATABASE_PATH = "activity-mining.db";
    private static String MGFSM_INPUT_DIR = "seqInput";
    private static String MGFSM_OUTPUT_DIR = "seqOutput";
    private static String JAVA_HOME_ENV = "C:\\Program Files\\AdoptOpenJDK\\jdk-8.0.212.03-hotspot";
    private static String OUTPUT_FILE_NAME = "translatedFS";
    private static Sequencer sequencer = new TimeSensitive5();

    //MGFSM Parameters
    private static int SUPPORT = 757; // ~20% of sequences
    private static int GAMMA = 1; //gap size
    private static int LAMBDA = 360; //Longest sequence length

    private static DataStore db;

    public static void main (String [] args) throws Exception {

        //Export mined sequences to CSV or mine sequences depending on arguments
        if (args.length > 0){
            switch (args[0]){
                case "export": export(args[1], args[1] + ".csv"); break;
                case "mine":
                    DEFAULT_DATABASE_PATH = args[1];
                    MGFSM_INPUT_DIR = args[2];
                    MGFSM_OUTPUT_DIR = args[3];
                    JAVA_HOME_ENV = args[4];
                    OUTPUT_FILE_NAME = args[5];
                    sequencer = switch (args[6]){
                        case "TimeSensitive5" -> new TimeSensitive5();
                        case "RelativeTimeSensitive" -> new RelativeTimeSensitive();
                        case "OneToOne" -> new OneToOne();
                        default -> throw new Exception("Unrecognized sequencer");
                    };
                    SUPPORT = Integer.parseInt(args[7]);
                    GAMMA = Integer.parseInt(args[8]);
                    LAMBDA = Integer.parseInt(args[9]);

                    log.info("DATABASE_PATH: {} MGFSM_INPUT_DIR: {} MGFSM_OUTPUT_DIR: {} JAVA_HOME_ENV: {} OUTPUT_FILE_NAME: {} SEQUENCER: {} SUPPORT: {} GAMMA: {} LAMBDA:{}",
                            DEFAULT_DATABASE_PATH,
                            MGFSM_INPUT_DIR,
                            MGFSM_OUTPUT_DIR,
                            JAVA_HOME_ENV,
                            OUTPUT_FILE_NAME,
                            sequencer.getClass().getName(),
                            SUPPORT,
                            GAMMA,
                            LAMBDA);

                    //Establish database connection and get sequences
                    db = DataStore.getInstance(DEFAULT_DATABASE_PATH);
                    mine(); break;
                default: log.warn("Unrecognized input argument");
            }
        }else{


            throw new Exception("Expected args!"); //No args means something is wrong
        }





    }

    public static void export(String executionId, String outFile){
        List<FrequentSubSequence> subSequences = db.getFrequentSubSequences(executionId)
                .stream()
                .filter(fss->fss.sequence().contains(""+Activity.Inactive.symbol))
                .collect(Collectors.toList());
        toCSV(subSequences, outFile);
    }

    public static void mine(){

        List<Sequence> sequences = db.getSequences(sequencer.getClass());

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
            Files.move(inputFile.toPath(), Path.of(currentExecutionInPath.toString() , inputFile.toPath().toString()));

            /* Create a corresponding subfolder in the root output folder for the current execution. */
            Files.createDirectory(currentExecutionOutPath);

            File outputFile = executeMGFSM(currentExecutionId, currentExecutionInPath, currentExecutionOutPath, SUPPORT, GAMMA, LAMBDA, sequences.get(0).sequencer());

            //Read output file and save subsequences to database.
            log.info("Reading MGFSM output...");
            List<FrequentSubSequence> subSequences = readFrequentSubsequences(currentExecutionId, outputFile);
            log.info("Saving frequent subsequences to database");
            db.insert(subSequences);

        }catch (IOException e){
            log.error(e.getMessage(),e);
        }
    }

    /**
     * Executes MG-FSM.
     * @param executionId The unique id of this execution
     * @param input input directory containing sequence file(s)
     * @param output output directory
     * @param support The minimum number of times the sequence to be mined must be present in the database
     * @param gamma The maximum amount of gap that can be taken for a sequence to be mined by MG-FSM
     * @param lambda The maximum length of the sequence to be mined.
     * @return The output file produced by MG-FSM
     */
    public static File executeMGFSM(UUID executionId, Path input, Path output, int support, int gamma, int lambda, String sequencer){
        try {
            log.info("Executing MGFSM with input folder: {} and output folder: {}", input.toString(), output.toString());

            Date executionStart = Date.from(Instant.now());

            ProcessBuilder pb = new ProcessBuilder("java","-Xmx50G", "-jar", "mgfsm.jar",
                    "-i", input.toString() ,
                    "-o", output.toString(),
                    "-s", Integer.toString(support),
                    "-g", Integer.toString(gamma),
                    "-l", Integer.toString(lambda),
                    "-t", "m" //Set type to maximal See Obsidian note on Nov 27th for additional details.
            );

            Map<String,String> env = pb.environment();
            env.put("JAVA_HOME", JAVA_HOME_ENV);

            //Create an output log file
            File log = new File(Path.of(output.toString(), Path.of(executionId.toString() + ".log").toString()).toString());
            pb.redirectErrorStream(true);
            pb.redirectOutput(log);

            long miningStart = Instant.now().toEpochMilli();
            Process mgfsm = pb.start();
            mgfsm.waitFor();
            long miningEnd = Instant.now().toEpochMilli();

            //Produce a record of this mining execution
            MiningRecord miningRecord = new MiningRecord(executionId.toString(),executionStart.toString(), miningStart, miningEnd, support, gamma, lambda, sequencer);
            DataStore.getInstance().insert(miningRecord); // Save the record to the database.

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return new File(Path.of(output.toString(),OUTPUT_FILE_NAME).toString());
    }

    /** Reads a list of frequent subsequences from an MG-FSM output file.
     *
     * @param executionId mining execution id that produced the file
     * @param f the MG-FSM output file
     * @return A list of {@link FrequentSubSequence}
     */
    public static List<FrequentSubSequence> readFrequentSubsequences (UUID executionId, File f){
        List<FrequentSubSequence> result = new ArrayList<>();

        try(FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
        ) {

            String line = br.readLine();
            while (line != null){
                String [] parts = line.split("\t");
                result.add(new FrequentSubSequence(
                        executionId.toString(),
                        compressSequence(parts[0]), //Strip whitespace
                        Integer.parseInt(parts[1])));
                line = br.readLine();
            }

        } catch (FileNotFoundException e) {
            log.error(e.getMessage(),e);
        } catch (IOException e) {
            log.error(e.getMessage(),e);
        }

        return result;
    }

    public static File toCSV(List<FrequentSubSequence> fssList, String fileName){
        File f = new File(fileName);
        try(FileWriter fw = new FileWriter(f);
            BufferedWriter bw = new BufferedWriter(fw);
        ){
            fssList.forEach(fss-> {
                try {
                    bw.write(fss.sequence() + ",\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
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

    private static String compressSequence(String sequence){
        return sequence.replaceAll("\\s+", "");
    }


}
