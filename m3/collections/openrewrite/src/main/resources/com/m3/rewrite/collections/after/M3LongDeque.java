/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.LongDeque
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
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
