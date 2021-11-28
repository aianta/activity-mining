package com.activity.mining.evaluation;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.DistanceMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class SilhouetteThread extends Thread{

    private static final Logger log = LoggerFactory.getLogger(SilhouetteThread.class);

    private volatile List<Double> silhouetteValues = new ArrayList<>();
    private volatile long executionTime = -1;

    private int index;
    Dataset [] clusters;
    DistanceMeasure dm;

    public SilhouetteThread(Dataset[] clusters, int index, DistanceMeasure dm){
        this.index = index;
        this.clusters = clusters;
        this.dm = dm;
        setName("SThread-" + index);
    }

    @Override
    public void run() {
        Instant start = Instant.now();
        try{
            for (int i = 0; i < clusters[this.index].size(); i++){
                Instance instance = clusters[this.index].instance(i);
                double a_i = a(i,clusters[this.index]);
                double b_i = b(instance,
                        //All clusters except the one containing instance
                        Stream.concat(
                                        Arrays.stream(Arrays.copyOfRange(clusters,0, this.index)),
                                        Arrays.stream(Arrays.copyOfRange(clusters, this.index+1, clusters.length)))
                                .toArray(Dataset[]::new)
                );
                double silhouette = silhouette(a_i,b_i,clusters[this.index]);
                silhouetteValues.add(silhouette);
                log.info("[{}] Computed silhouette {}/{} for cluster {}/{}", this.getName(), i+1,clusters[this.index].size(), this.index+1, clusters.length);

            }
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }

        this.executionTime = Instant.now().getEpochSecond()-start.getEpochSecond();

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

    public List<Double> getSilhouetteValues(){
        return silhouetteValues;
    }

    public long getExecutionTime(){
        return executionTime;
    }
}
