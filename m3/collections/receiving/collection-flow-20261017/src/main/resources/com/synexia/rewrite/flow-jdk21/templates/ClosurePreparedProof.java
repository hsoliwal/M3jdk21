// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target namespace and explicit test progress fixture.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
public final class ClosurePreparedProof {
    private static long checks;
    private ClosurePreparedProof() { }
    private static void equal(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        boolean nativeMode=args.length>0;
        if(nativeMode)System.load(args[0]);
        var backend=nativeMode?M3SortedView.Backend.JNI_REQUIRED:M3SortedView.Backend.JAVA;
        long[] domain={Long.MIN_VALUE,-1,0,1,Long.MAX_VALUE};
        for(int n=0;n<=6;n++) {
            int cases=(int)Math.pow(domain.length,n);
            for(int code=0;code<cases;code++) {
                long[] keys=new long[n];int state=code;for(int i=0;i<n;i++){keys[i]=domain[state%domain.length];state/=domain.length;}
                for(int selection=0;selection<3;selection++) {
                    final int rule=selection;var calls=new AtomicInteger();var selected=new AtomicInteger();
                    var expected=new ArrayList<Integer>();for(int i=0;i<n;i++)if(rule==0 || (rule==1 && i%2==0))expected.add(i);
                    expected.sort(Comparator.comparingLong(i->keys[i]));
                    try(var view=M3SortedView.byLong(n,i->{throw new AssertionError("payload materialized");},()->0L,
                            i->{selected.incrementAndGet();return rule==0 || (rule==1 && i%2==0);},i->{calls.incrementAndGet();return keys[i];},backend,48L*n,null)) {
                        equal(n,selected.get());equal(expected.size(),calls.get());equal(expected.size(),view.size());
                        for(int i=0;i<expected.size();i++)equal(expected.get(i),view.sourceIndex(i));
                    }
                }
            }
        }
        var monitor=new M3FlowReceiverProof.Monitor();var revision=new AtomicLong();
        var parent=new ArrayList<>(List.of("b","a","a"));
        try(var view=M3SortedView.by(parent,Comparator.naturalOrder(),revision::get,144,monitor)){
            monitor.cancelled=true; // preparation monitor must not become a retained read-time policy
            equal("a",view.get(0));equal(List.of("a","a","b"),view.materialize());
            List<String> copy=view.materialize();parent.set(0,"c");revision.incrementAndGet();equal(List.of("a","a","b"),copy);
        }
        int max=M3SortedView.MAX_ROWS;
        try(var empty=M3SortedView.byLong(max,i->{throw new AssertionError("payload");},()->0L,i->false,
                i->{throw new AssertionError("hidden key");},backend,48L*max,null)){equal(0,empty.size());}
        System.out.println("CLOSURE_PREPARED_CHECKS="+checks+" NATIVE="+nativeMode+" PASS");
    }
}
