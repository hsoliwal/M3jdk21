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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Frozen, exactly deduplicated rows of integers, backed by the existing offset arena.
 *
 * <p>The builder is single-writer. IDs belong to one pool and retain insertion order;
 * the built pool is immutable and permits concurrent reads. Hashes select candidates
 * only: an equal hash is always followed by element-by-element equality.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatInternedRows.java}
 * blob {@code 00a551e81ccf22c59fe78a58b9a42e012d2f93ad} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatInternedRows {
    private final M3MatOffsetIntArena rows;
    private final long[] hashes;
    private final int[] slots;

    private M3MatInternedRows(M3MatOffsetIntArena rows, long[] hashes, int[] slots) {
        this.rows = rows;
        this.hashes = hashes;
        this.slots = slots;
    }

    public static Builder builder(M3MatLimits limits) {
        return new Builder(limits);
    }

    public int size() { return rows.rowCount(); }
    public int length(int id) { return rows.length(id); }
    public int get(int id, int index) { return rows.get(id, index); }
    public int[] row(int id) { return rows.row(id); }
    public long payloadBytes() {
        return rows.payloadBytes() + 8L * hashes.length + 4L * slots.length;
    }

    /** Exact lookup; returns -1 when the row is not interned. */
    public int idOf(int... values) {
        Objects.requireNonNull(values, "values");
        long hash = hash(values);
        int mask = slots.length - 1;
        int slot = M3MatHash.mix(M3MatHash.fold32(hash)) & mask;
        for (int examined = 0; examined < slots.length; examined++) {
            int stored = slots[slot];
            if (stored == 0) return -1;
            int id = stored - 1;
            if (hashes[id] == hash && rows.length(id) == values.length) {
                if (values.length >= 64) {
                    if (rows.equalRow(id, values)) return id;
                } else {
                    // Small rows retain the existing low-overhead scalar probe.
                    boolean equal = true;
                    for (int index = 0; index < values.length; index++) {
                        if (rows.get(id, index) != values[index]) {
                            equal = false;
                            break;
                        }
                    }
                    if (equal) return id;
                }
            }
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    private static long hash(int[] values) {
        return M3MatIntRowKernel.hash(values);
    }

    /** Primitive-lane canonicalizer. No per-entry Java node is retained. */
    public static final class Builder {
        private final M3MatLimits limits;
        private int[] offsets = new int[8];
        private int[] arena = new int[32];
        private long[] hashes = new long[8];
        private int[] slots = new int[16];
        private int count;
        private int used;
        private long inputs;

        private Builder(M3MatLimits limits) {
            this.limits = Objects.requireNonNull(limits, "limits");
        }

        public int size() { return count; }

        /** Interns an exact ordered row, cloning its contents into the primitive arena. */
        public int intern(int... row) {
            Objects.requireNonNull(row, "row");
            long nextInput = Math.addExact(inputs, 1L);
            limits.inputValues(nextInput);
            limits.valueBytes(4L * row.length);
            long hash = hash(row);
            int found = find(row, hash);
            if (found >= 0) {
                inputs = nextInput;
                return found;
            }

            int nextCount = Math.addExact(count, 1);
            int nextUsed = Math.addExact(used, row.length);
            limits.entries(nextCount);
            limits.arenaBytes(4L * nextUsed);
            limits.stagingBytes(4L * nextUsed);

            offsets = grow(offsets, Math.addExact(nextCount, 1));
            arena = grow(arena, nextUsed);
            hashes = grow(hashes, nextCount);
            rehashIfNeeded(nextCount);

            System.arraycopy(row, 0, arena, used, row.length);
            offsets[nextCount] = nextUsed;
            hashes[count] = hash;
            int slot = M3MatHash.mix(M3MatHash.fold32(hash)) & (slots.length - 1);
            while (slots[slot] != 0) slot = (slot + 1) & (slots.length - 1);
            slots[slot] = nextCount;
            inputs = nextInput;
            used = nextUsed;
            count = nextCount;
            return count - 1;
        }

        public int idOf(int... row) {
            Objects.requireNonNull(row, "row");
            return find(row, hash(row));
        }

        /** Snapshot: subsequent changes to this builder cannot alter a previously built pool. */
        public M3MatInternedRows build() {
            M3MatOffsetIntArena.Builder frozen = M3MatOffsetIntArena.builder(count, used);
            for (int id = 0; id < count; id++) {
                frozen.addRow(arena, offsets[id], offsets[id + 1] - offsets[id]);
            }
            return new M3MatInternedRows(frozen.build(), Arrays.copyOf(hashes, count), slots.clone());
        }

        private int find(int[] row, long hash) {
            int slot = M3MatHash.mix(M3MatHash.fold32(hash)) & (slots.length - 1);
            for (int examined = 0; examined < slots.length; examined++) {
                int stored = slots[slot];
                if (stored == 0) return -1;
                int id = stored - 1;
                if (hashes[id] == hash && offsets[id + 1] - offsets[id] == row.length) {
                    if (M3MatIntRowKernel.equal(arena, offsets[id], row, 0, row.length)) {
                        return id;
                    }
                }
                slot = (slot + 1) & (slots.length - 1);
            }
            return -1;
        }

        private void rehashIfNeeded(int entries) {
            if (2L * entries <= slots.length) return;
            int capacity = slots.length;
            while (2L * entries > capacity) {
                if (capacity > (1 << 29)) throw new IllegalStateException("hash capacity overflow");
                capacity <<= 1;
            }
            int[] next = new int[capacity];
            for (int id = 0; id < count; id++) {
                int slot = M3MatHash.mix(M3MatHash.fold32(hashes[id])) & (capacity - 1);
                while (next[slot] != 0) slot = (slot + 1) & (capacity - 1);
                next[slot] = id + 1;
            }
            slots = next;
        }

        private static int[] grow(int[] current, int required) {
            if (required <= current.length) return current;
            int capacity = capacity(current.length, required);
            return Arrays.copyOf(current, capacity);
        }

        private static long[] grow(long[] current, int required) {
            if (required <= current.length) return current;
            int capacity = capacity(current.length, required);
            return Arrays.copyOf(current, capacity);
        }

        private static int capacity(int current, int required) {
            long candidate = Math.max((long) required, Math.max(2L, current + (current >>> 1) + 1L));
            if (candidate > Integer.MAX_VALUE - 8L) {
                throw new IllegalStateException("interned row capacity overflow");
            }
            return (int) candidate;
        }
    }
}
