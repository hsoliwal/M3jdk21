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

import java.util.NoSuchElementException;

/**
 * Exact extrema over the most recent capacity values; amortized O(1) insertion.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongMinMaxWindow.java}
 * blob {@code bd9ef6fe870bbe3831da565cd497859ee06f97a8} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongMinMaxWindow implements M3LongMinMaxWindow {
    private final int capacity;
    private final long[] minima, maxima, minimumAt, maximumAt;
    private int minHead, maxHead, minSize, maxSize;
    private long accepted;
    public M3PackedLongMinMaxWindow(int capacity) {
        if (capacity < 1 || capacity > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
        minima = new long[capacity]; maxima = new long[capacity];
        minimumAt = new long[capacity]; maximumAt = new long[capacity];
    }
    public int capacity() { return capacity; }
    public int size() { return (int) Math.min(accepted, capacity); }
    public long accepted() { return accepted; }
    public long arrayPayloadBytes() { return 32L * capacity; }
    /** After Long.MAX_VALUE inputs throws before mutation; clear starts a new window. */
    public void add(long value) {
        if (accepted == Long.MAX_VALUE) throw new IllegalStateException("window sequence exhausted");
        long position = accepted++, expired = position - capacity;
        if (minSize > 0 && minimumAt[minHead] <= expired) { minHead = next(minHead); minSize--; }
        if (maxSize > 0 && maximumAt[maxHead] <= expired) { maxHead = next(maxHead); maxSize--; }
        while (minSize > 0 && minima[at(minHead, minSize - 1)] >= value) minSize--;
        while (maxSize > 0 && maxima[at(maxHead, maxSize - 1)] <= value) maxSize--;
        int lo = at(minHead, minSize++), hi = at(maxHead, maxSize++);
        minima[lo] = value; minimumAt[lo] = position;
        maxima[hi] = value; maximumAt[hi] = position;
    }
    public long minimum() { if (minSize == 0) throw new NoSuchElementException(); return minima[minHead]; }
    public long maximum() { if (maxSize == 0) throw new NoSuchElementException(); return maxima[maxHead]; }
    public void clear() { accepted = 0; minHead = 0; maxHead = 0; minSize = 0; maxSize = 0; }
    private int next(int index) { return index + 1 == capacity ? 0 : index + 1; }
    private int at(int head, int offset) { return (int) ((head + (long) offset) % capacity); }
}
