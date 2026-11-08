// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;
import java.util.function.LongPredicate;
import jdk.m3.collections.LongSequence;

/** Bound logical growth without changing the underlying primitive backend. */
//FLLDCIM: RELOCATE - donor blob f96c3cc357db at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class BoundedLongSequence extends ForwardingLongSequence {
    private final LongSequence backend;
    private final int maximumSize;
    public BoundedLongSequence(LongSequence backend,int maximumSize) {
        this.backend=Objects.requireNonNull(backend,"backend");
        if(maximumSize<0) throw new IllegalArgumentException("negative bound");
        if(backend.size()>maximumSize) throw new IllegalArgumentException("oversized backend");
        this.maximumSize=maximumSize;
    }
    @Override protected LongSequence delegate(){return backend;}
    public int maximumSize(){return maximumSize;}
    private void requireRoom(int count) {
        if(count>maximumSize-backend.size()) throw new IllegalStateException("bound exceeded");
    }
    @Override public boolean add(long value) {
        requireRoom(1);
        return backend.add(value);
    }
    @Override public boolean addIf(long value,LongPredicate predicate) {
        Objects.requireNonNull(predicate,"predicate");
        if(!predicate.test(value)) return false;
        requireRoom(1);
        backend.add(value);
        return true;
    }
    @Override public boolean addAll(long[] values) {
        Objects.requireNonNull(values,"values");
        requireRoom(values.length);
        return backend.addAll(values);
    }
    @Override public void insert(int index,long value) {
        int size=backend.size();
        if(index<0||index>size) throw new IndexOutOfBoundsException("index: "+index);
        requireRoom(1);
        backend.insert(index,value);
    }
}
