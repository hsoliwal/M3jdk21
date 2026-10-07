// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.function.LongPredicate;

/** Primitive-long double-ended queue contract. */
public interface M3LongDeque extends M3LongQueue {
    void addFirst(long value);
    void addLast(long value);
    boolean addLastIf(long value, LongPredicate predicate);
    long first();
    long last();
    long removeFirst();
    long removeLast();
    long get(int index);
    int filterInPlace(LongPredicate predicate);
}
