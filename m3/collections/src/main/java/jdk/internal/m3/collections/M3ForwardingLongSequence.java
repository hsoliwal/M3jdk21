// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import jdk.m3.collections.LongIterator;
import jdk.m3.collections.M3LongSequence;

/** Primitive direct forwarding, including native bulk and primitive traversal. */
//FLLDCIM: RELOCATE - donor blob 492ecb94f0de at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public abstract class M3ForwardingLongSequence implements M3LongSequence {
    protected M3ForwardingLongSequence() { }
    protected abstract M3LongSequence delegate();
    @Override public boolean add(long v) { return delegate().add(v); }
    @Override public boolean addIf(long v, LongPredicate p) { return delegate().addIf(v,p); }
    @Override public boolean addAll(long[] v) { return delegate().addAll(v); }
    @Override public void insert(int i,long v) { delegate().insert(i,v); }
    @Override public long get(int i) { return delegate().get(i); }
    @Override public long set(int i,long v) { return delegate().set(i,v); }
    @Override public long removeAt(int i) { return delegate().removeAt(i); }
    @Override public boolean removeValue(long v) { return delegate().removeValue(v); }
    @Override public int indexOf(long v) { return delegate().indexOf(v); }
    @Override public int lastIndexOf(long v) { return delegate().lastIndexOf(v); }
    @Override public int filterInPlace(LongPredicate p) { return delegate().filterInPlace(p); }
    @Override public int capacity() { return delegate().capacity(); }
    @Override public void trimToSize() { delegate().trimToSize(); }
    @Override public int size() { return delegate().size(); }
    @Override public boolean contains(long v) { return delegate().contains(v); }
    @Override public void clear() { delegate().clear(); }
    @Override public LongIterator iterator() { return delegate().iterator(); }
    @Override public long[] toArray() { return delegate().toArray(); }
    @Override public boolean isEmpty() { return delegate().isEmpty(); }
    @Override public void forEachLong(LongConsumer c) { delegate().forEachLong(c); }
    @Override public Spliterator.OfLong longSpliterator() { return delegate().longSpliterator(); }
    @Override public LongStream longStream() { return delegate().longStream(); }
    @Override public LongStream parallelLongStream() { return delegate().parallelLongStream(); }
}
