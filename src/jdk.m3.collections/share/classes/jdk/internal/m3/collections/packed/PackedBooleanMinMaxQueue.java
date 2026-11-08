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

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Double-ended boolean priority queue. False precedes true; only two counters are retained.
 * Duplicates count, all scalar operations are O(1), and mutation is not thread-safe.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedBooleanMinMaxQueue.java}
 * blob {@code 0e47221982f2c5be2a75cd99f86655c4d7e020cb} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedBooleanMinMaxQueue {
    private int size;
    private int falseCount;

    public PackedBooleanMinMaxQueue() { }
    /** Counts the supplied values; retains no reference or payload array. */
    public PackedBooleanMinMaxQueue(boolean[] values) { addAll(values); }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public void clear() { size = 0; falseCount = 0; }

    public void add(boolean value) {
        if (size == Integer.MAX_VALUE) throw new IllegalStateException("boolean queue size exceeded");
        if (!value) falseCount++;
        size++;
    }

    public void addAll(boolean[] values) {
        Objects.requireNonNull(values, "values");
        addAll(values, 0, values.length);
    }

    /** Complete validation precedes counting and publication of both counters. */
    public void addAll(boolean[] values, int offset, int length) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromIndexSize(offset, length, values.length);
        if (length > Integer.MAX_VALUE - size) throw new IllegalStateException("boolean queue size exceeded");
        int addedFalse = 0;
        for (int i = offset, end = offset + length; i < end; i++) if (!values[i]) addedFalse++;
        falseCount += addedFalse;
        size += length;
    }

    public boolean min() { requireElement(); return falseCount == 0; }
    public boolean max() { requireElement(); return falseCount < size; }
    public boolean minOr(boolean fallback) { return size == 0 ? fallback : falseCount == 0; }
    public boolean maxOr(boolean fallback) { return size == 0 ? fallback : falseCount < size; }

    public boolean removeMin() {
        boolean result = min();
        if (!result) falseCount--;
        size--;
        return result;
    }

    public boolean removeMax() {
        boolean result = max();
        if (!result) falseCount--;
        size--;
        return result;
    }

    /** Independent ascending snapshot; this is the explicit allocation boundary. */
    public boolean[] toArray() { return toSortedArray(); }
    public boolean[] toSortedArray() {
        boolean[] values = new boolean[size];
        Arrays.fill(values, falseCount, size, true);
        return values;
    }

    private void requireElement() {
        if (size == 0) throw new NoSuchElementException("empty boolean min-max queue");
    }
}
