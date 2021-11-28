package com.activity.mining.evaluation;


import net.sf.javaml.clustering.evaluation.ClusterEvaluation;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.DistanceMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Inspired from description found here:
 * https://beginningwithml.wordpress.com/2019/04/26/11-6-evaluating-clusters/
 * and
 * https://en.wikipedia.org/wiki/Silhouette_(clustering)
 */
public class SilhouetteIndex implements ClusterEvaluation {

    private static final Logger log = LoggerFactory.getLogger(SilhouetteIndex.class);
    private static final int MAX_THREADS = 14;

    DistanceMeasure dm;

    public SilhouetteIndex(DistanceMeasure dm){
        this.dm = dm;
    }


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

    /**
     *
     * @param clusters
     * @return
     */
    public double scoreOld(Dataset[] clusters) {
        log.info("Computing silhouetteIndex!");
        Instant start = Instant.now();
        List<Double> silhouettes = new ArrayList<>();

        try{
            for (int c = 0; c < clusters.length; c++){
                for (int i = 0; i < clusters[c].size(); i++){
                    Instance instance = clusters[c].instance(i);
                    double a_i = a(i,clusters[c]);
                    double b_i = b(instance,
                            //All clusters except the one containing instance
                            Stream.concat(
                                            Arrays.stream(Arrays.copyOfRange(clusters,0, c)),
                                            Arrays.stream(Arrays.copyOfRange(clusters, c+1, clusters.length)))
                                    .toArray(Dataset[]::new)
                    );
                    double silhouette = silhouette(a_i,b_i,clusters[c]);
                    silhouettes.add(silhouette);
                    log.info("Computed silhouette {}/{} for cluster {}/{}", i+1,clusters[c].size(), c+1, clusters.length);
                }
            }
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }

        double result = silhouettes.stream().mapToDouble(Double::doubleValue).average().getAsDouble();

        long duration = Instant.now().getEpochSecond()-start.getEpochSecond();
        log.info("Silhouette Index {} took {}s to compute!", result, duration);

        return result;
    }

    private double a(int i, Dataset cluster){

        double constant = 1.0/(cluster.size() - 1);

        double sumOfDistances = 0.0;

        for(int j = 0; j < cluster.size(); j++){
            if(j == i){ //Because we're interested the sum of distances where iIndex != j
                 continue;
            }
            sumOfDistances += dm.measure(cluster.instance(i),cluster.instance(j));
        }

        return constant*sumOfDistances;

    }

    private double b(Instance i, Dataset [] clusters){

        double [] results = new double[clusters.length];

        for (int c = 0; c < clusters.length; c++){
            Dataset cluster = clusters[c];
            double constant = 1/cluster.size();
            double sumOfDistances = 0.0;
            for(int j = 0; j < cluster.size(); j++){
                sumOfDistances += dm.measure(i, cluster.instance(j));
            }
            results[c] = constant*sumOfDistances;
        }

        return Arrays.stream(results).min().getAsDouble();

    }

    private double silhouette(double a , double b, Dataset cluster) throws Exception {
        if (cluster.size() > 1){
            return (b-a)/Math.max(a,b);
        }

        if(cluster.size() == 1){
            return 0;
        }

        throw new Exception("Cluster size is negative? What did you do?");
    }

    @Override
    public boolean compareScore(double score1, double score2) {
        return score2 < score1;
    }
}
