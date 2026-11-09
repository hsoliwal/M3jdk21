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

import jdk.m3.collections.LongLongConsumer;

import java.util.Objects;

/**
 * Sorted multiset sharing the existing parallel-array sorted-map kernel.
 * One count per distinct value; no occurrence objects. Counts and total size are exact
 * positive longs. Arithmetic overflow is checked before mutation. Lookup is O(log d),
 * distinct insertion/removal O(d). Callbacks cannot mutate this bag. Not thread-safe.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongSortedBag.java}
 * blob {@code 2578a67896a8bdbdb95919302b7c8e5fac562511} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections bag + read-only sequence batch (v6.2, receipt contract): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongSortedBag implements PackedLongBagContract {
    private final PackedLongLongSortedMap counts;
    private long total;
    private int callbacks;

    public PackedLongSortedBag() { this(4); }
    public PackedLongSortedBag(int expectedDistinct) { counts = new PackedLongLongSortedMap(expectedDistinct); }
    @Override public long add(long value) { return add(value, 1); }
    @Override public long add(long value, long occurrences) {
        writable(); positive(occurrences);
        long nextTotal = Math.addExact(total, occurrences);
        long next = Math.addExact(count(value), occurrences);
        counts.put(value, next); total = nextTotal; return next;
    }
    @Override public long count(long value) { return counts.getOrDefault(value, 0); }
    @Override public long remove(long value, long occurrences) {
        writable(); positive(occurrences);
        long current = count(value), removed = Math.min(current, occurrences), next = current - removed;
        if (next == 0) { counts.remove(value); } else { counts.put(value, next); }
        total -= removed; return next;
    }
    @Override public long removeAll(long value) {
        writable(); long current = count(value); counts.remove(value); total -= current; return current;
    }
    @Override public boolean contains(long value) { return counts.containsKey(value); }
    @Override public int distinctSize() { return counts.size(); }
    @Override public long size() { return total; }
    @Override public boolean isEmpty() { return total == 0; }
    @Override public void clear() { writable(); counts.clear(); total = 0; }
    @Override public void forEachCount(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try { counts.forEach(consumer); } finally { callbacks--; }
    }
    private static void positive(long occurrences) {
        if (occurrences <= 0) { throw new IllegalArgumentException("occurrences must be positive"); }
    }
    private void writable() {
        if (callbacks != 0) { throw new IllegalStateException("mutation during callback"); }
    }
}
