// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import jdk.internal.m3.collections.ForwardingLongSequence;

/** Actual LongArrayList backend plus one-dispatch operation counters. */
//FLLDCIM: RELOCATE - donor blob c5ed056f42fe at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
final class M3PolicyProbe extends ForwardingLongSequence {
    final LongArrayList data;
    int adds,addIfs,bulks,inserts,sets,removeAts,removes,filters,clears,trims;
    int eaches,spliterators,streams,parallelStreams;
    long[] lastBulk;
    M3PolicyProbe(long... values){data=LongArrayList.copyOf(values);}
    @Override protected M3LongSequence delegate(){return data;}
    @Override public boolean add(long v){adds++;return data.add(v);}
    @Override public boolean addIf(long v,LongPredicate p) {
        addIfs++;return data.addIf(v,p);
    }
    @Override public boolean addAll(long[] v){bulks++;lastBulk=v;return data.addAll(v);}
    @Override public void insert(int i,long v){inserts++;data.insert(i,v);}
    @Override public long set(int i,long v){sets++;return data.set(i,v);}
    @Override public long removeAt(int i){removeAts++;return data.removeAt(i);}
    @Override public boolean removeValue(long v){removes++;return data.removeValue(v);}
    @Override public int filterInPlace(LongPredicate p) {
        filters++;return data.filterInPlace(p);
    }
    @Override public void clear(){clears++;data.clear();}
    @Override public void trimToSize(){trims++;data.trimToSize();}
    @Override public void forEachLong(LongConsumer c){eaches++;data.forEachLong(c);}
    @Override public Spliterator.OfLong longSpliterator() {
        spliterators++;return data.longSpliterator();
    }
    @Override public LongStream longStream(){streams++;return data.longStream();}
    @Override public LongStream parallelLongStream() {
        parallelStreams++;return data.parallelLongStream();
    }
}
