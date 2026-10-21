// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.stats.streaming;
/** Welford streaming moments, CPU-first bounded state. */
public final class RunningStatistics {
    public RunningStatistics() { }
    private long count;
    private double mean,m2,min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
    public void accept(double number){
        if(!Double.isFinite(number))throw new IllegalArgumentException("nonfinite sample");
        if(count==Long.MAX_VALUE)throw new ArithmeticException("count overflow");
        count++;
        double delta=number-mean;
        mean+=delta/count;
        m2+=delta*(number-mean);
        min=Math.min(min,number);max=Math.max(max,number);
    }
    public long count(){return count;}
    public double mean(){return count==0?Double.NaN:mean;}
    public double populationVariance(){return count==0?Double.NaN:m2/count;}
    public double min(){return count==0?Double.NaN:min;}
    public double max(){return count==0?Double.NaN:max;}
    public static void main(String[] args){
        if(args.length==1&&args[0].equals("--self-test")){
            var s=new RunningStatistics();for(double x:new double[]{1,2,3})s.accept(x);
            if(s.count()!=3||s.mean()!=2.0||Math.abs(s.populationVariance()-2.0/3)>1e-12)
                throw new AssertionError("moments");
            System.out.println("M3_JMOD_TEST_PASS stats.streaming");
        }else{var s=new RunningStatistics();for(String a:args)s.accept(Double.parseDouble(a));System.out.println(s.mean());}
    }
}
