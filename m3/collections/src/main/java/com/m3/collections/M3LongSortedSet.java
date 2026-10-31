// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.function.LongPredicate;

/** Mutable navigable primitive-long set contract. */
public interface M3LongSortedSet extends M3LongCollection {
    boolean add(long value);
    boolean remove(long value);
    long first();
    long last();
    long floor(long value, long defaultValue);
    long ceiling(long value, long defaultValue);
    long lower(long value, long defaultValue);
    long higher(long value, long defaultValue);
    int filterInPlace(LongPredicate predicate);
}
