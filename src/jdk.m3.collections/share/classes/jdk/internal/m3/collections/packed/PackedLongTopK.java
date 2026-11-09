/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongCollection;
import jdk.m3.collections.LongIterator;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Fixed-limit streaming selection over a primitive array heap. Duplicates count.
 * The limit and polarity cannot drift between insertions. Not thread-safe.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongTopK.java}
 * blob {@code 799e0ef462ffda78fae231ad5850d763e6e28661} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections catalogue chain after API_UNION U3 (v6.2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongTopK implements LongCollection {
    private final int limit;
    private final boolean largest;
    private final PackedLongHeap heap;

    private PackedLongTopK(int limit, boolean largest) {
        if (limit < 0 || limit > PackedSupport.MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("limit");
        }
        this.limit = limit;
        this.largest = largest;
        heap = new PackedLongHeap(largest, limit);
    }

    public static PackedLongTopK largest(int limit) {
        return new PackedLongTopK(limit, true);
    }

    public static PackedLongTopK smallest(int limit) {
        return new PackedLongTopK(limit, false);
    }

    /** Once full, rejects at the boundary before retention. Equal ties do not replace. */
    public boolean offer(long value) {
        return largest ? heap.offerTop(value, limit) : heap.offerBottom(value, limit);
    }

    /** Predicate must not mutate this collector. */
    public boolean offerIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate.test(value) && offer(value);
    }

    /** Returns successful admissions/replacements, not net size change. */
    public int offerAll(long[] values) {
        Objects.requireNonNull(values, "values");
        int accepted = 0;
        for (long value : values) {
            if (offer(value)) {
                accepted++;
            }
        }
        return accepted;
    }

    public int limit() {
        return limit;
    }

    public boolean keepsLargest() {
        return largest;
    }

    /** Smallest retained value for largest-K, greatest for smallest-K; throws on empty. */
    public long boundary() {
        return heap.peek();
    }

    @Override
    public int size() {
        return heap.size();
    }

    @Override
    public boolean contains(long value) {
        return heap.contains(value);
    }

    @Override
    public void clear() {
        heap.clear();
    }

    /** Snapshot in heap-storage order, not sorted order. */
    @Override
    public long[] toArray() {
        return heap.toArray();
    }

    /** Ascending snapshot regardless of selection direction. */
    public long[] toSortedArray() {
        long[] values = toArray();
        Arrays.sort(values);
        return values;
    }

    @Override
    public LongIterator iterator() {
        return heap.iterator();
    }
}
