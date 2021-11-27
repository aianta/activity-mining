package com.activity.mining;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.tools.InstanceTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeClusters {

    private static final Logger log = LoggerFactory.getLogger(ComputeClusters.class);
    private static final String DEFAULT_DATABASE_PATH = "activity-mining.db";

    public static void main (String [] args){
        /* values of the attributes. */
        double[] values = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        /*
         * The simplest incarnation of the DenseInstance constructor will only
         * take a double array as argument an will create an instance with given
         * values as attributes and no class value set. For unsupervised machine
         * learning techniques this is probably the most convenient constructor.
         */
        Instance instance = new DenseInstance(values);

        Dataset dataset = new DefaultDataset();
        dataset.add(instance);




    }

}
