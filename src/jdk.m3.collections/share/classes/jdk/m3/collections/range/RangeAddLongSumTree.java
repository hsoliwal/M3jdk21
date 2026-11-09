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
package jdk.m3.collections.range;

import java.util.Objects;

/**
 * Lazy segment tree for range-add and range-sum using two primitive long[] lanes.
 *
 * <p>The recursive call stack is O(log n); retained tree state has no node/tag objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/RangeAddLongSumTree.java}
 * blob {@code 756fe761bc764843bfd897df15a91c02a0cb2984} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class RangeAddLongSumTree {
    private final int size;
    private final long[] sums;
    private final long[] lazyAdds;

    public RangeAddLongSumTree(int size) {
        if (size < 0) throw new IllegalArgumentException("size < 0");
        this.size = size;
        int slots = Math.multiplyExact(Math.max(1, size), 4);
        sums = new long[slots];
        lazyAdds = new long[slots];
    }

    public RangeAddLongSumTree(long[] values) {
        Objects.requireNonNull(values, "values");
        size = values.length;
        int slots = Math.multiplyExact(Math.max(1, size), 4);
        sums = new long[slots];
        lazyAdds = new long[slots];
        if (size != 0) build(1, 0, size, values);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() {
        return (long) (sums.length + lazyAdds.length) * Long.BYTES;
    }

    public void add(int fromInclusive, int toExclusive, long delta) {
        checkRange(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive || delta == 0L) return;
        add(1, 0, size, fromInclusive, toExclusive, delta);
    }

    public long sum(int fromInclusive, int toExclusive) {
        checkRange(fromInclusive, toExclusive);
        if (fromInclusive == toExclusive) return 0L;
        return sum(1, 0, size, fromInclusive, toExclusive, 0L);
    }

    public long get(int index) {
        checkIndex(index);
        return sum(index, index + 1);
    }

    public void set(int index, long value) {
        long current = get(index);
        add(index, index + 1, value - current);
    }

    public long total() {
        return size == 0 ? 0L : sums[1];
    }

    private void build(int node, int left, int right, long[] values) {
        if (right - left == 1) {
            sums[node] = values[left];
            return;
        }
        int mid = (left + right) >>> 1;
        build(node << 1, left, mid, values);
        build((node << 1) | 1, mid, right, values);
        sums[node] = sums[node << 1] + sums[(node << 1) | 1];
    }

    private void add(
            int node, int left, int right,
            int from, int to, long delta) {
        if (to <= left || right <= from) return;
        if (from <= left && right <= to) {
            sums[node] += delta * (right - left);
            lazyAdds[node] += delta;
            return;
        }
        int mid = (left + right) >>> 1;
        add(node << 1, left, mid, from, to, delta);
        add((node << 1) | 1, mid, right, from, to, delta);
        sums[node] =
                sums[node << 1]
                        + sums[(node << 1) | 1]
                        + lazyAdds[node] * (right - left);
    }

    private long sum(
            int node, int left, int right,
            int from, int to, long inheritedAdd) {
        if (to <= left || right <= from) return 0L;
        if (from <= left && right <= to) {
            return sums[node] + inheritedAdd * (right - left);
        }
        long nextInherited = inheritedAdd + lazyAdds[node];
        int mid = (left + right) >>> 1;
        return sum(node << 1, left, mid, from, to, nextInherited)
                + sum((node << 1) | 1, mid, right, from, to, nextInherited);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    private void checkRange(int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || fromInclusive > toExclusive || toExclusive > size) {
            throw new IndexOutOfBoundsException(fromInclusive + ".." + toExclusive);
        }
    }
}
