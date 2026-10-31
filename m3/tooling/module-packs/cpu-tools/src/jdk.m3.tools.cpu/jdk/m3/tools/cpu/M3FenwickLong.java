// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Objects;

/** Checked long prefix/range sums; primitive lanes and failure-atomic point changes. */
public final class M3FenwickLong {
    private static final int MAX_ITEMS = 1_000_000;
    private final long[] values;
    private final long[] tree;

    public M3FenwickLong(long[] input) {
        Objects.requireNonNull(input, "input");
        if (input.length > MAX_ITEMS) throw new IllegalArgumentException("Fenwick size budget exceeded");
        values = input.clone();
        tree = new long[input.length + 1];
        for (int i = 1; i < tree.length; i++) {
            tree[i] = Math.addExact(tree[i], values[i - 1]);
            int parent = i + (i & -i);
            if (parent < tree.length) tree[parent] = Math.addExact(tree[parent], tree[i]);
        }
    }

    public int size() { return values.length; }
    public long get(int index) { return values[java.util.Objects.checkIndex(index, size())]; }
    public long[] values() { return values.clone(); }

    /** Sum of [0,end) with ordinary Java signed-long overflow refusal. */
    public long prefix(int end) {
        if (end < 0 || end > size()) throw new IndexOutOfBoundsException(end);
        long value = 0;
        for (int i = end; i > 0; i -= i & -i) value = Math.addExact(value, tree[i]);
        return value;
    }

    /** Sum of half-open [start,end), empty range is zero. */
    public long range(int start, int end) {
        if (start < 0 || end < start || end > size()) throw new IndexOutOfBoundsException();
        // Empty intervals are identically zero even when the prefix before
        // the interval is too large to represent in one signed long.
        if (start == end) return 0L;
        return Math.subtractExact(prefix(end), prefix(start));
    }

    /** Preflights every affected primitive lane before changing any of them. */
    public void add(int index, long delta) {
        java.util.Objects.checkIndex(index, size());
        long updated = Math.addExact(values[index], delta);
        for (int i = index + 1; i < tree.length; i += i & -i) {
            Math.addExact(tree[i], delta);
        }
        values[index] = updated;
        for (int i = index + 1; i < tree.length; i += i & -i) tree[i] += delta;
    }

    /** Replace one value without an intermediate per-entry object. */
    public void set(int index, long value) {
        java.util.Objects.checkIndex(index, size());
        add(index, Math.subtractExact(value, values[index]));
    }
}
