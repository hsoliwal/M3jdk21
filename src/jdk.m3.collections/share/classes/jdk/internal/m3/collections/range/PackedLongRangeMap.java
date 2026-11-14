/* *****************************************************************************
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
package jdk.internal.m3.collections.range;

import java.util.Arrays;
import java.util.Objects;
import jdk.internal.m3.collections.LongLongLongConsumer;
import jdk.internal.m3.collections.LongLongLongPredicate;

/** Mutable closed-interval -> long map in sorted primitive parallel arrays.
 * Assignment splits overwritten intervals; adjacent equal values coalesce. Endpoints
 * include Long.MIN_VALUE and Long.MAX_VALUE without a sentinel or overflowing successor.
 * Point lookup is O(log r), update is O(r) and uses staged arrays for exception safety.
 * It is a piecewise-constant map, not an overlapping interval multimap. Not thread-safe.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongRangeMap.java}
 * blob {@code f7801945161b45f3c6cee580a09e07ed6e98c0b4} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongRangeMap {
    private long[] starts = new long[0];
    private long[] ends = new long[0];
    private long[] values;
    private final boolean valued;
    private int size;
    private int callbacks;

    public PackedLongRangeMap() { this(true); }
    // Set facade reuses interval mechanics without allocating a value lane.
    PackedLongRangeMap(boolean valued) { this.valued = valued; values = valued ? new long[0] : null; }
    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }
    private long value(int row) { return valued ? values[row] : 0; }
    private int containing(long point) {
        return PackedRangeAtoms.containing(starts, ends, size, point);
    }
    public boolean contains(long point) { return containing(point) >= 0; }
    public boolean containsRange(long from, long to) {
        PackedRangeAtoms.checkRange(from, to);
        int row = containing(from);
        if (row < 0) return false;
        long covered = ends[row];
        while (covered < to && ++row < size) {
            if (!PackedRangeAtoms.adjacent(covered, starts[row])) return false;
            covered = ends[row];
        }
        return covered >= to;
    }
    public long getOrDefault(long point, long fallback) {
        int row = containing(point); return row < 0 ? fallback : value(row);
    }
    public void put(long from, long to, long value) { rewrite(from, to, value, false); }
    public void remove(long from, long to) { rewrite(from, to, 0, true); }

    private int emit(long[] s, long[] e, long[] v, int used, long from, long to, long val) {
        if (used > 0 && PackedRangeAtoms.adjacent(e[used - 1], from)
                && (!valued || v[used - 1] == val)) {
            e[used - 1] = to; return used;
        }
        s[used] = from; e[used] = to;
        if (valued) v[used] = val;
        return used + 1;
    }
    private void rewrite(long from, long to, long val, boolean removing) {
        writable(); PackedRangeAtoms.checkRange(from, to);
        int capacity = Math.addExact(size, 2);
        long[] s = new long[capacity], e = new long[capacity];
        long[] v = valued ? new long[capacity] : null;
        int used = 0; boolean inserted = removing;
        for (int i = 0; i < size; i++) {
            long a = starts[i], b = ends[i], oldValue = value(i);
            if (b < from) {
                used = emit(s, e, v, used, a, b, oldValue);
            } else if (a > to) {
                if (!inserted) { used = emit(s, e, v, used, from, to, val); inserted = true; }
                used = emit(s, e, v, used, a, b, oldValue);
            } else {
                if (a < from) used = emit(s, e, v, used, a, PackedRangeAtoms.leftRemainderEnd(from), oldValue);
                if (!inserted) { used = emit(s, e, v, used, from, to, val); inserted = true; }
                if (b > to) used = emit(s, e, v, used, PackedRangeAtoms.rightRemainderStart(to), b, oldValue);
            }
        }
        if (!inserted) used = emit(s, e, v, used, from, to, val);
        starts = s; ends = e; values = v; size = used;
    }
    public int filterInPlace(LongLongLongPredicate predicate) {
        writable(); Objects.requireNonNull(predicate, "predicate");
        long[] s = new long[size], e = new long[size], v = valued ? new long[size] : null;
        int used = 0; callbacks++;
        try {
            for (int i = 0; i < size; i++) if (predicate.test(starts[i], ends[i], value(i)))
                used = emit(s, e, v, used, starts[i], ends[i], value(i));
        } finally { callbacks--; }
        int removed = size - used;
        starts = s; ends = e; values = v; size = used;
        return removed;
    }
    public void forEach(LongLongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try { for (int i = 0; i < size; i++) consumer.accept(starts[i], ends[i], value(i)); }
        finally { callbacks--; }
    }
    /** Emits clipped intersections in sorted order; never materializes interval objects. */
    public void intersections(long from, long to, LongLongLongConsumer consumer) {
        PackedRangeAtoms.checkRange(from, to); Objects.requireNonNull(consumer, "consumer");
        int first = PackedRangeAtoms.firstIntersection(ends, size, from);
        callbacks++;
        try {
            for (int i = first; i < size && starts[i] <= to; i++)
                consumer.accept(Math.max(from, starts[i]), Math.min(to, ends[i]), value(i));
        } finally { callbacks--; }
    }
    public int rangeCount() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long startAt(int row) { Objects.checkIndex(row, size); return starts[row]; }
    public long endAt(int row) { Objects.checkIndex(row, size); return ends[row]; }
    public long valueAt(int row) { Objects.checkIndex(row, size); return value(row); }
    public long[] startsToArray() { return Arrays.copyOf(starts, size); }
    public long[] endsToArray() { return Arrays.copyOf(ends, size); }
    public long[] valuesToArray() { return valued ? Arrays.copyOf(values, size) : new long[size]; }
    public long arrayPayloadBytes() { return 8L * (starts.length + (long) ends.length
            + (valued ? values.length : 0)); }
    public void clear() { writable(); size = 0; }
}
