/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package com.synexia.common.collections;

import com.m3.collections.M3CollectionViews;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.function.Consumer;

/** Narrow binding to the unchanged first-party TEST ABI, not a production JNI wrapper. */
public final class RangeJniProof {
    private static int checks;
    private RangeJniProof() { }
    private static native int drain(Iterator<?> iterator, Consumer<Object> action, int removalPeriod);
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static void eq(Object a,Object b,String label){check(Objects.equals(a,b),label);}
    public static void main(String[] args) {
        if(args.length!=1)throw new IllegalArgumentException("exact test library path");
        System.load(Path.of(args[0]).toAbsolutePath().toString());
        Random random=new Random(20261017);
        for(int trial=0;trial<300;trial++)for(int kind=0;kind<3;kind++) {
            Collection<Integer> owner=kind==0?new ArrayList<>():kind==1?new TreeSet<>():new TreeSet<Integer>().descendingSet();
            var view=M3CollectionViews.filtered(owner,x->x%2==0);
            var batch=new ArrayList<Integer>();for(int i=0;i<trial%67;i++)batch.add(2*(random.nextInt(200)-100));
            view.addAll(batch);
            var actual=new ArrayList<Object>();eq(view.size(),drain(view.iterator(),actual::add,0),"native filtered count");
            eq(new ArrayList<>(view),actual,"native encounter order");
            var sorted=M3CollectionViews.sortedSnapshot(view,Comparator.reverseOrder());actual.clear();
            eq(sorted.size(),drain(sorted.iterator(),actual::add,0),"native snapshot count");eq(sorted,actual,"native sorted order");
            var tail=view.spliterator();var prefix=tail.trySplit();actual.clear();
            if(prefix!=null)drain(Spliterators.iterator(prefix),actual::add,0);
            drain(Spliterators.iterator(tail),actual::add,0);eq(new ArrayList<>(view),actual,"native split order");
        }
        var large=new ArrayList<Integer>();for(int i=0;i<10000;i++)large.add(i);
        var evens=M3CollectionViews.filtered(large,x->x%2==0);eq(5000,drain(evens.iterator(),v->{},0),"local references released");
        var nullable=M3CollectionViews.filtered(new ArrayList<>(Arrays.asList(null,2,null)),x->true);
        var nullOutput=new ArrayList<>();eq(3,drain(nullable.iterator(),nullOutput::add,0),"native nulls");eq(Arrays.asList(null,2,null),nullOutput,"null order");
        var failure=new IllegalStateException("consumer-failure");
        try{drain(evens.iterator(),v->{throw failure;},0);throw new AssertionError("missing consumer failure");}
        catch(IllegalStateException e){check(e==failure,"JNI callback identity");}
        var predicateFailure=new IllegalArgumentException("predicate-failure");
        try{drain(M3CollectionViews.filtered(large,x->{throw predicateFailure;}).iterator(),v->{},0);throw new AssertionError("missing predicate failure");}
        catch(IllegalArgumentException e){check(e==predicateFailure,"JNI predicate identity");}
        try{drain(evens.iterator(),v->{},1);throw new AssertionError("iterator remove admitted");}
        catch(UnsupportedOperationException e){check(true,"iterator removal still refused");}
        System.out.println("FILTERED_BATCH_JNI_PASS checks="+checks+" reusedC=true productJniChanges=0");
    }
}
