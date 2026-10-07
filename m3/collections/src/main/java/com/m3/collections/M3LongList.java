// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.function.LongPredicate;

/** Mutable indexed primitive-long sequence. */
public interface M3LongList extends M3LongCollection {
    void add(long value);
    boolean addIf(long value, LongPredicate predicate);
    void addAll(long[] values);
    void insert(int index, long value);
    long get(int index);
    long set(int index, long value);
    long removeAt(int index);
    boolean removeValue(long value);
    int indexOf(long value);
    int lastIndexOf(long value);
    int filterInPlace(LongPredicate predicate);
    int capacity();
    void trimToSize();
}
