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
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.ObjLongConsumer;

/**
 * Parallel payload/rank lanes shared by priority and bounded-admission shapes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/RankStore.java}
 * blob {@code a46c10c187c8ac70882b5e6724cc1456c2619432} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
abstract class M3RankStore<E> {
    private static final Object[] EMPTY_VALUES = new Object[0];
    private static final long[] EMPTY_RANKS = new long[0];
    Object[] values = EMPTY_VALUES;
    long[] ranks = EMPTY_RANKS;
    int size;
    private final int maximum;
    private final boolean minimum;
    M3RankStore(int maximum, boolean minimum) { this.maximum = maximum; this.minimum = minimum; }
    abstract int boundaryIndex();
    abstract void add(E value, long rank);
    abstract E removeBoundary();
    abstract void replaceBoundary(E value, long rank);
    final boolean before(long a, long b) { return minimum ? a < b : a > b; }
    final void requireValue() { if (size == 0) { throw new NoSuchElementException(); } }
    final E value(int index) { return M3ShapeGuard.read(values, index); }
    final E boundaryValue() { requireValue(); return value(boundaryIndex()); }
    final long boundaryRank() { requireValue(); return ranks[boundaryIndex()]; }
    final void room() {
        if (size < values.length) { return; }
        int capacity = M3ShapeGuard.grown(values.length, size + 1, maximum);
        Object[] nextValues = Arrays.copyOf(values, capacity);
        long[] nextRanks = Arrays.copyOf(ranks, capacity);
        values = nextValues; ranks = nextRanks;
    }
    final void write(int index, E value, long rank) { values[index] = value; ranks[index] = rank; }
    final void move(int to, int from) { values[to] = values[from]; ranks[to] = ranks[from]; }
    final void erase(int index) { values[index] = null; ranks[index] = 0; }
    final void walk(ObjLongConsumer<? super E> action) {
        for (int i = 0; i < size; i++) { action.accept(value(i), ranks[i]); }
    }
    final void clear() { values = EMPTY_VALUES; ranks = EMPTY_RANKS; size = 0; }
    final M3ShapeStorage storage() {
        return new M3ShapeStorage(values.length, (long) ranks.length * Long.BYTES, values.length == 0 ? 0 : 2, 0);
    }
}
