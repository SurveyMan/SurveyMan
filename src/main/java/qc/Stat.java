/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qc;

/**
 *
 * @author Molly
 */
public class Stat {
    
    public static double mean(double[] values){
        double sum = 0;
        for(double d : values){
            sum+=d;
        }
        return sum/(values.length);
    }
    
    public static double stddev(double[] values){
        double mean = mean(values);
        double sum = 0;
        for(double d: values){
            sum+=Math.pow(Math.abs(mean-d),2);
        }
        return Math.sqrt(sum/(values.length));
        
    }
    
}
