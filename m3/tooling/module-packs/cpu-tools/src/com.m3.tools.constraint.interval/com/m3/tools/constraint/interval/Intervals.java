// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.constraint.interval;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/** Half-open [start,end) time or offset intervals, no OS-specific clock dependency. */
public final class Intervals {
    private Intervals() { }
    public record Span(long start,long end) {
        public Span {if(end<start)throw new IllegalArgumentException("inverted interval");}
    }
    public static boolean intersects(Span a,Span b) {
        Objects.requireNonNull(a);Objects.requireNonNull(b);
        return a.start()<a.end()&&b.start()<b.end()&&a.start()<b.end()&&b.start()<a.end();
    }
    public static List<Integer> conflicting(List<Span> intervals,Span request){
        Objects.requireNonNull(intervals);Objects.requireNonNull(request);
        List<Integer> ids=new ArrayList<>();
        for(int i=0;i<intervals.size();i++)if(intersects(Objects.requireNonNull(intervals.get(i)),request))ids.add(i);
        return List.copyOf(ids);
    }
    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("--self-test")){
            if(intersects(new Span(0,2),new Span(2,3)))throw new AssertionError("touching boundary");
            if(!intersects(new Span(0,3),new Span(2,4)))throw new AssertionError("overlap");
            if(!conflicting(List.of(new Span(0,3),new Span(4,8)),new Span(2,5))
                    .equals(List.of(0,1)))throw new AssertionError("conflicts");
            System.out.println("M3_JMOD_TEST_PASS constraint.interval");
        }else throw new IllegalArgumentException("--self-test");
    }
}
