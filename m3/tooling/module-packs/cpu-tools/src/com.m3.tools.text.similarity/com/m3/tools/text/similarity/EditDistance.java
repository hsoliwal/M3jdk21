// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.similarity;
import java.util.Objects;
/** Bounded UTF-16 Levenshtein oracle with O(min(length)) primitive workspace. */
public final class EditDistance {
    private EditDistance() { }
    public static int within(String left, String right, int bound) {
        Objects.requireNonNull(left); Objects.requireNonNull(right);
        if(bound<0||bound>1_000_000) throw new IllegalArgumentException("distance bound");
        if(left.length()<right.length()){String swap=left;left=right;right=swap;}
        int a=left.length(),b=right.length();
        if(a-b>bound)return bound+1;
        if((long)a*b>10_000_000L)throw new IllegalArgumentException("work budget");
        int[] previous=new int[b+1],current=new int[b+1];
        for(int j=0;j<=b;j++)previous[j]=j;
        for(int i=1;i<=a;i++){
            current[0]=i;int min=current[0];
            for(int j=1;j<=b;j++){
                int sub=previous[j-1]+(left.charAt(i-1)==right.charAt(j-1)?0:1);
                current[j]=Math.min(sub,Math.min(previous[j]+1,current[j-1]+1));
                min=Math.min(min,current[j]);
            }
            if(min>bound)return bound+1;
            int[] swap=previous;previous=current;current=swap;
        }
        return Math.min(previous[b],bound+1);
    }
    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("--self-test")){
            if(within("kitten","sitting",3)!=3)throw new AssertionError("distance");
            if(within("abc","xyz",1)!=2)throw new AssertionError("bounded");
            if(within("","",0)!=0)throw new AssertionError("empty");
            System.out.println("M3_JMOD_TEST_PASS text.similarity");
        } else if(args.length==3)System.out.println(within(args[0],args[1],Integer.parseInt(args[2])));
        else throw new IllegalArgumentException("left right bound");
    }
}
