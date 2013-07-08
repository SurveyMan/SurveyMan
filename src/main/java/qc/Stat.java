/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qc;

import java.util.ArrayList;

/**
 *
 * @author Molly
 */
public class Stat {
    
    public static double mean(ArrayList<Double> values){
        double sum = 0;
        for(double d : values){
            sum+=d;
        }
        return sum/(values.size());
    }
    
    public static double stddev(ArrayList<Double> values){
        double mean = mean(values);
        double sum = 0;
        for(double d: values){
            sum+=Math.pow(Math.abs(mean-d),2);
        }
        return Math.sqrt(sum/(values.size()));
        
    }
    
}
