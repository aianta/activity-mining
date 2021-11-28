package com.activity.mining;

import com.activity.mining.evaluation.SilhouetteIndex;
import com.activity.mining.persistence.DataStore;
import com.activity.mining.records.ClusterRecord;
import com.activity.mining.records.ClusteringRecord;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMedoids;

import net.sf.javaml.clustering.evaluation.ClusterEvaluation;
import net.sf.javaml.clustering.evaluation.SumOfSquaredErrors;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

import net.sf.javaml.distance.CosineDistance;
import net.sf.javaml.distance.CosineSimilarity;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.EuclideanDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ComputeClusters {

    private static final Logger log = LoggerFactory.getLogger(ComputeClusters.class);
    private static final String DEFAULT_DATABASE_PATH = "activity-mining.db";
    //private static final String DEFAULT_EMBEDDINGS_FILE = "sequence_vectors_no_index.csv";
    //private static final String DEFAULT_SEQUENCES_FILE = "fss.csv";
    private static final String DEFAULT_EMBEDDINGS_FILE = "cfab4fce-9648-4747-92a3-c61e77eaad35_embeddings.csv";
    private static final String DEFAULT_SEQUENCES_FILE = "cfab4fce-9648-4747-92a3-c61e77eaad35.csv";
    private static final int INPUT_KAPPA = 1;
    private static final int MIN_K = 2;
    private static final int MAX_K = 20;
    private static final int MAX_ITERATIONS = 100;
    private static final DistanceMeasure DISTANCE_MEASURE = new CosineDistance();
    private static final int EVALUATION_ITERATIONS = 5; //Try every k-clustering this many times

    private static DataStore db;

    public static void main (String [] args){
        log.info("Initializing datastore connection");
        db = DataStore.getInstance(DEFAULT_DATABASE_PATH);

        Dataset ds = readEmbeddingsFromFiles(DEFAULT_EMBEDDINGS_FILE, DEFAULT_SEQUENCES_FILE);
        log.info("Dataset size: {}", ds.size());

        log.info("{}",extractEmbedding(ds.instance(0)));

        log.info("{}",ds.instance(0).classValue());


        cluster(ds, "cfab4fce-9648-4747-92a3-c61e77eaad35");

    }

    /**
     * Attempts to cluster the dataset for varying values of k,
     * saving results to the database.
     * @param ds dataset to cluster
     */
    public static void cluster(Dataset ds, String sourceExecutionId){

        /**
         * Starts at {@link #MAX_K} because higher values of k should
         * have their silhouette index compute faster as that portion
         * is multithreaded to the number of clusters.
         */
        for (int k = MAX_K; k >= MIN_K; k--){
            //Generate a unique clustering id
            UUID clusteringId = UUID.randomUUID();
            List<Double> iterationResults = new ArrayList<>();
            for (int iter = 0; iter < EVALUATION_ITERATIONS; iter++){
                Clusterer kMedoids = new KMedoids(k, MAX_ITERATIONS, DISTANCE_MEASURE);

                Dataset [] clusters = kMedoids.cluster(ds);
                ClusterEvaluation evaluation = new SilhouetteIndex(DISTANCE_MEASURE);
                double score = evaluation.score(clusters);
                iterationResults.add(score);


                //Save the clustering data
                for(int i = 0; i < clusters.length; i++){
                    for(Instance instance:clusters[i]){
                        db.insert(new ClusterRecord(
                                sourceExecutionId,
                                clusteringId,
                                extractEmbedding(instance),
                                ((String)instance.classValue()),
                                Integer.toString(i),
                                iter,
                                score
                        ));
                    }
                }
            }

            //Save a record of the clustering results
            db.insert(new ClusteringRecord(
                    sourceExecutionId,
                    Date.from(Instant.now()),
                    clusteringId,
                    k,
                    INPUT_KAPPA,
                    DISTANCE_MEASURE.getClass().getName(),
                    iterationResults.stream().mapToDouble(Double::doubleValue).average().getAsDouble()
            ));


        }

    }

    /** Reads a .csv file containing embeddings and .csv file containing sequences
     *  together line by line to associated the embeddings with the sequences.
     *
     * @param embeddingFile
     * @param sequenceFile
     * @return A JavaML Dataset ready for clustering.
     */
    public static Dataset readEmbeddingsFromFiles(String embeddingFile, String sequenceFile){
        Dataset result = new DefaultDataset();
        File embeddings = new File(embeddingFile);
        File sequences = new File(sequenceFile);

        try(FileReader fr = new FileReader(embeddings);
            BufferedReader br = new BufferedReader(fr);
            FileReader seqReader = new FileReader(sequences);
            BufferedReader seqBr = new BufferedReader(seqReader)
        ){

            String embedding = br.readLine(); //First line will be header
            embedding = br.readLine();

            String sequence = seqBr.readLine(); //First line will be header
            sequence = seqBr.readLine();
            while (embedding != null){
                Instance instance = parseInstance(embedding);
                instance.setClassValue(sequence.replaceAll(",",""));
                result.add(instance);
                embedding = br.readLine();
                sequence = seqBr.readLine();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Instance parseInstance(String entry){
        String [] parts = entry.split(",");
        //First column is the index, so we will skip it
        double [] data = new double[parts.length-2];
        for(int i = 0; i < data.length; i++){
            data[i] = Double.parseDouble(parts[i+1]);
        }

        log.info("Parsed: {}", Arrays.toString(data));

        return new DenseInstance(data);
    }

    public static String extractEmbedding(Instance i){
        String result = i.values().stream()
                .map(value->value.toString())
                .collect(
                        StringBuilder::new,
                        (sb,value)->sb.append(value + ","),
                        StringBuilder::append
                ).toString();

        return result.substring(0, result.length()-1);
    }

}
