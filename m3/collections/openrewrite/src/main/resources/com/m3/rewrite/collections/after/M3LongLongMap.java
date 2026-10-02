/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.PackedLongLongMap
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
package com.m3.collections;

/** Mutable primitive long-to-long map contract. */
public interface M3LongLongMap {
    boolean put(long key, long value);

    boolean containsKey(long key);

    long getOrDefault(long key, long defaultValue);

    long getOrThrow(long key);

    boolean remove(long key);

    int filterInPlace(M3LongLongPredicate predicate);

    void forEach(M3LongLongConsumer consumer);

    int size();

    boolean isEmpty();

    int capacity();

    void clear();

    void trimToSize();

    long[] keysToArray();

    long[] valuesToArray();
}
