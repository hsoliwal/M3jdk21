// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
// Modified 2026 by Hitesh Soliwal and contributors: align receiving mutation returns
// with the existing M3LongArrayList boolean contracts; canonical Synexia APIs are unchanged.
package com.m3.collections;

import java.util.function.LongPredicate;

/** Mutable indexed primitive-long sequence. */
public interface M3LongSequence extends M3LongCollection {
    /** Adds one value and returns true. */
    boolean add(long value);
    boolean addIf(long value, LongPredicate predicate);
    /** Adds the array contents and returns whether any value was added. */
    boolean addAll(long[] values);
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
