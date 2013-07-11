/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qc;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Molly
 */
public class Stat {
    
    public static void main(String[] args){
        //test
        Random rand = new Random();
        ArrayList<Double> a = new ArrayList<Double>();
        for(int x=0; x<10; x++){
            double num = rand.nextInt(10);
            System.out.println(num);
            a.add(num);
        }
        System.out.println();
        System.out.println("Mean: "+mean(a));
        System.out.println("Standard Deviation: "+stddev(a));
    }
    
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
        return Math.sqrt(sum/(double)(values.size()-1));
        
    }
    
}
