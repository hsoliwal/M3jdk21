// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;
import java.util.function.LongPredicate;
import jdk.m3.collections.M3LongSequence;

/** Predicate admission; caller bulk arrays are snapshotted before validation. */
//FLLDCIM: RELOCATE - donor blob 522c3ae14d22 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class M3ValidatingLongSequence extends M3ForwardingLongSequence {
    private final M3LongSequence backend;
    private final LongPredicate validator;
    public M3ValidatingLongSequence(M3LongSequence backend,LongPredicate validator) {
        this.backend=Objects.requireNonNull(backend,"backend");
        this.validator=Objects.requireNonNull(validator,"validator");
        long[] snapshot=backend.toArray().clone();
        for(long value:snapshot) validate(value);
    }
    @Override protected M3LongSequence delegate(){return backend;}
    private void validate(long value) {
        if(!validator.test(value)) throw new IllegalArgumentException("invalid: "+value);
    }
    @Override public boolean add(long value) {
        validate(value);
        return backend.add(value);
    }
    @Override public boolean addIf(long value,LongPredicate predicate) {
        Objects.requireNonNull(predicate,"predicate");
        if(!predicate.test(value)) return false;
        validate(value);
        backend.add(value);
        return true;
    }
    @Override public boolean addAll(long[] values) {
        long[] snapshot=Objects.requireNonNull(values,"values").clone();
        for(long value:snapshot) validate(value);
        return backend.addAll(snapshot);
    }
    @Override public void insert(int index,long value) {
        int size=backend.size();
        if(index<0||index>size) throw new IndexOutOfBoundsException("index: "+index);
        validate(value);
        backend.insert(index,value);
    }
    @Override public long set(int index,long value) {
        int size=backend.size();
        if(index<0||index>=size) throw new IndexOutOfBoundsException("index: "+index);
        validate(value);
        return backend.set(index,value);
    }
}
