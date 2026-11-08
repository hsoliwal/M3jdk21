// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongLongConsumer;
import jdk.m3.collections.LongLongPredicate;

/**
 * Mutable primitive {@code long -> long} map contract.
 *
 * <p>No Map.Entry, Long, Iterator<Entry<...>>, or object-valued mutation path is required.
 */
//FLLDCIM: RELOCATE - donor blob 50d7b4c139fd at commit d5ad5090d87e (repository, path, URL and digests: m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/four-library-bags-20261106/relocate.py v1
public interface PackedLongLongMap {
    /**
     * @return true when a new key is inserted, false when replacing an existing mapping
     */
    boolean put(long key, long value);

    boolean containsKey(long key);

    long getOrDefault(long key, long defaultValue);

    long getOrThrow(long key);

    boolean remove(long key);

    int filterInPlace(LongLongPredicate predicate);

    void forEach(LongLongConsumer consumer);

    int size();

    boolean isEmpty();

    int capacity();

    void clear();

    void trimToSize();

    long[] keysToArray();

    long[] valuesToArray();
}
