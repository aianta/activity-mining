package com.activity.mining.evaluation;


import net.sf.javaml.clustering.evaluation.ClusterEvaluation;
import net.sf.javaml.core.Dataset;

import net.sf.javaml.distance.DistanceMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Inspired from description found here:
 * https://beginningwithml.wordpress.com/2019/04/26/11-6-evaluating-clusters/
 * and
 * https://en.wikipedia.org/wiki/Silhouette_(clustering)
 */
public class SilhouetteIndex implements ClusterEvaluation {

    private static final Logger log = LoggerFactory.getLogger(SilhouetteIndex.class);
    private static final int MAX_THREADS = 6;

    DistanceMeasure dm;

    public SilhouetteIndex(DistanceMeasure dm){
        this.dm = dm;
    }


    /** Computes the silhouette index, attempts to speed up the process by
     *  computing silhouette values for each cluster on its own thread.
     * @param clusters
     * @return
     */
    public double score(Dataset[] clusters){

        log.info("Computing silhouette index....");
        Instant start = Instant.now();
        List<Double> silhouettes = new ArrayList<>();

        try{
            ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
            List<SilhouetteThread> threads = new ArrayList<>();
            for (int i = 0; i < clusters.length; i++){
                SilhouetteThread task = new SilhouetteThread(clusters, i, dm);
                threads.add(task);
                pool.execute(task);
            }

            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.DAYS);

            silhouettes = threads.stream()
                    .map(SilhouetteThread::getSilhouetteValues)
                    .collect(
                            ArrayList::new,
                            ArrayList::addAll,
                            ArrayList::addAll
                    );

            //Print off thread execution time
            threads.forEach(st->log.info("{} executed in -> {}s", st.getName(), st.getExecutionTime()));

        }catch (InterruptedException ie){
            log.error(ie.getMessage(), ie);
        }

        double result = silhouettes.stream().mapToDouble(Double::doubleValue).average().getAsDouble();

        long duration = Instant.now().getEpochSecond()-start.getEpochSecond();
        log.info("Silhouette Index {} took {}s to compute!", result, duration);


        return result;
    }



    @Override
    public boolean compareScore(double score1, double score2) {
        return score2 < score1;
    }
}
