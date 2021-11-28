package com.activity.mining;

import com.activity.mining.evaluation.SilhouetteIndex;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMedoids;

import net.sf.javaml.clustering.evaluation.ClusterEvaluation;
import net.sf.javaml.clustering.evaluation.SumOfSquaredErrors;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.EuclideanDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;

public class ComputeClusters {

    private static final Logger log = LoggerFactory.getLogger(ComputeClusters.class);
    private static final String DEFAULT_DATABASE_PATH = "activity-mining.db";
    private static final String DEFAULT_EMBEDDINGS_FILE = "sequence_vectors_no_index.csv";

    public static void main (String [] args){

        Dataset ds = readEmbeddingsFromFile(DEFAULT_EMBEDDINGS_FILE);
        log.info("Dataset size: {}", ds.size());

        log.info("Initializing Clusterer");
        Clusterer kMedoids = new KMedoids();

        Dataset [] clusters = kMedoids.cluster(ds);

        log.info("{} clusters", clusters.length);

        ClusterEvaluation evaluation = new SilhouetteIndex(new EuclideanDistance());

        double score = evaluation.score(clusters);

        log.info("silhouetteIndex: {}", score);

    }

    /**
     * Attempts to cluster the dataset for varying values of k,
     * saving results to the database.
     * @param ds dataset to cluster
     */
    public static void cluster(Dataset ds){

    }

    public static Dataset readEmbeddingsFromFile(String fileName){
        Dataset result = new DefaultDataset();
        File embeddings = new File(fileName);

        try(FileReader fr = new FileReader(embeddings);
            BufferedReader br = new BufferedReader(fr);
        ){

            String line = br.readLine(); //First line will be header
            line = br.readLine();
            while (line != null){
                result.add(parseInstance(line));
                line = br.readLine();
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

}
