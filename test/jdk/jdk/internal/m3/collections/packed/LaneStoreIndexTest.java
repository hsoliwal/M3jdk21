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
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * PackedLongLaneStore filters with a rejected-row mask and one sorted-index compaction, registers
 * sorted indexes with one stable merge sort and moves a set() row with one span copy: against the
 * master copies (LaneStoreRef and its SortedIndexRef/HashIndexRef/HeapIndexRef/SelectionRef),
 * driven by the same random appends, sets, swap-removes, filters (throwing and re-entrant
 * predicates included), clears, heap polls and index registrations, equal results, exceptions,
 * lanes, payload bytes and every index projection after every operation.
 */
//FLLDCIM: LEETCODIFY - lockstep proof for the lane store's sorted-index offset lanes and masks
class LaneStoreIndexTest {
    private static final long[] VALUES = {-3, -2, -1, 0, 1, 2, 3, Long.MIN_VALUE, Long.MAX_VALUE,
        Long.MIN_VALUE + 1};
    private static final long[] PROBES = {-4, -3, -1, 0, 2, 4, Long.MIN_VALUE, Long.MAX_VALUE, -2};

    private static String outcome(Supplier<Object> action) {
        try {
            Object value = action.get();
            return "=" + (value instanceof int[] a ? Arrays.toString(a)
                    : value instanceof long[] l ? Arrays.toString(l) : String.valueOf(value));
        } catch (RuntimeException e) {
            return "!" + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    /** One registered index of each side, read through its public projections. */
    private record Pair(Object impl, Object ref) { }

    private static String sorted(Object index, int rows) {
        boolean impl = index instanceof PackedLongLaneSortedIndex;
        PackedLongLaneSortedIndex a = impl ? (PackedLongLaneSortedIndex) index : null;
        SortedIndexRef b = impl ? null : (SortedIndexRef) index;
        StringBuilder text = new StringBuilder("sorted");
        int size = impl ? a.size() : b.size();
        for (int k = 0; k < size; k++) {
            text.append(',').append(impl ? a.rowAtRank(k) : b.rowAtRank(k));
        }
        for (int r = 0; r < rows; r++) {
            int row = r;
            text.append(';').append(outcome(() -> impl ? a.rankOfRow(row) : b.rankOfRow(row)));
        }
        for (long p : PROBES) {
            text.append('|').append(outcome(() -> impl ? a.lowerBound(p) : b.lowerBound(p)))
                    .append(outcome(() -> impl ? a.upperBound(p) : b.upperBound(p)))
                    .append(outcome(() -> impl ? a.rowOf(p) : b.rowOf(p)))
                    .append(outcome(() -> impl ? a.floorRow(p, -9) : b.floorRow(p, -9)))
                    .append(outcome(() -> impl ? a.ceilingRow(p, -9) : b.ceilingRow(p, -9)));
        }
        return text + outcome(() -> impl ? a.firstRow() : b.firstRow())
                + outcome(() -> impl ? a.lastRow() : b.lastRow());
    }

    private static String view(Object index, int rows) {
        if (index instanceof PackedLongLaneSortedIndex || index instanceof SortedIndexRef) {
            return sorted(index, rows);
        }
        if (index instanceof PackedLongLaneHashIndex h) {
            return "hash" + h.size() + Arrays.toString(Arrays.stream(PROBES).mapToInt(h::rowOf)
                    .toArray());
        }
        if (index instanceof HashIndexRef h) {
            return "hash" + h.size() + Arrays.toString(Arrays.stream(PROBES).mapToInt(h::rowOf)
                    .toArray());
        }
        if (index instanceof PackedLongLaneHeapIndex h) {
            return "heap" + Arrays.toString(h.heapRowsToArray()) + outcome(h::peekRow);
        }
        if (index instanceof HeapIndexRef h) {
            return "heap" + Arrays.toString(h.heapRowsToArray()) + outcome(h::peekRow);
        }
        if (index instanceof PackedLongLaneSelection s) {
            return "selection" + Arrays.toString(s.rowsToArray());
        }
        return "selection" + Arrays.toString(((SelectionRef) index).rowsToArray());
    }

    private static final class Twin {
        final PackedLongLaneStore impl;
        final LaneStoreRef ref;
        final int lanes;
        final List<Pair> indexes = new ArrayList<>();

        Twin(int lanes, int expected) {
            this.impl = new PackedLongLaneStore(lanes, expected);
            this.ref = new LaneStoreRef(lanes, expected);
            this.lanes = lanes;
        }

        void same(String where) {
            assertEquals(ref.size(), impl.size(), where);
            assertEquals(ref.capacity(), impl.capacity(), where);
            assertEquals(ref.payloadBytes(), impl.payloadBytes(), where);
            for (int lane = 0; lane < lanes; lane++) {
                assertEquals(Arrays.toString(ref.laneToArray(lane)),
                        Arrays.toString(impl.laneToArray(lane)), where);
            }
            for (Pair p : indexes) {
                assertEquals(view(p.ref(), ref.size() + 2), view(p.impl(), impl.size() + 2), where);
            }
        }

        void step(String what, Supplier<Object> onRef, Supplier<Object> onImpl) {
            assertEquals(outcome(onRef), outcome(onImpl), what);
            same(what);
        }
    }

    private static long v(Random random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    private static void append(Twin t, Random random) {
        if (t.lanes == 2 && random.nextBoolean()) {
            long a = v(random);
            long b = v(random);
            t.step("append2", () -> t.ref.append2(a, b), () -> t.impl.append2(a, b));
            return;
        }
        long[] row = new long[random.nextInt(10) == 0 ? t.lanes + 1 : t.lanes];
        for (int i = 0; i < row.length; i++) {
            row[i] = v(random);
        }
        t.step("appendRow", () -> t.ref.appendRow(row.clone()), () -> t.impl.appendRow(row.clone()))
                ;
    }

    private static void register(Twin t, Random random) {
        int lane = random.nextInt(t.lanes);
        int kind = random.nextInt(6);
        boolean flag = random.nextBoolean();
        Object[] made = new Object[2];
        String a = outcome(() -> made[0] = kind < 4
                ? t.ref.sortedIndex(lane, (kind & 1) != 0, (kind & 2) != 0)
                : kind == 4 ? t.ref.heapIndex(lane, flag) : t.ref.selection(lane, x -> x > 0));
        String b = outcome(() -> made[1] = kind < 4
                ? t.impl.sortedIndex(lane, (kind & 1) != 0, (kind & 2) != 0)
                : kind == 4 ? t.impl.heapIndex(lane, flag) : t.impl.selection(lane, x -> x > 0));
        assertEquals(a.startsWith("!") ? a : "=index", b.startsWith("!") ? b : "=index", "register")
                ;
        if (made[0] != null && made[1] != null) {
            t.indexes.add(new Pair(made[1], made[0]));
        }
        t.same("register " + kind);
    }

    /** Keeps a row from its value and the call number, throwing or re-entering on cue. */
    private static boolean keep(long value, int call, int mode, int throwAt) {
        if (mode == 1 && call == throwAt) {
            throw new IllegalStateException("predicate stop at " + call);
        }
        return Math.floorMod(value * 31 + call * 7, 10) < 5;
    }

    private static void filter(Twin t, Random random) {
        int mode = random.nextInt(3);
        int throwAt = random.nextInt(Math.max(1, t.ref.size()) + 1);
        int[] calls = new int[2];
        List<String> inner = new ArrayList<>();
        t.step("filter mode " + mode, () -> t.ref.filterInPlace((s, row) -> {
            if (mode == 2) {
                inner.add(outcome(() -> { s.set(row, 0, 1L); return null; }));
            }
            return keep(s.get(row, 0), calls[0]++, mode, throwAt);
        }), () -> t.impl.filterInPlace((s, row) -> {
            if (mode == 2) {
                inner.add(outcome(() -> { s.set(row, 0, 1L); return null; }));
            }
            return keep(s.get(row, 0), calls[1]++, mode, throwAt);
        }));
        assertEquals(calls[0], calls[1], "predicate calls");
        for (int i = 0; i + 1 < inner.size(); i += 2) {
            assertEquals(inner.get(i), inner.get(i + 1), "re-entrant set");
        }
    }

    private static void operate(Twin t, Random random) {
        int op = random.nextInt(20);
        int rows = Math.max(1, t.ref.size());
        int row = random.nextInt(rows + 2) - 1;
        int lane = random.nextInt(t.lanes);
        long value = v(random);
        if (op < 7) {
            append(t, random);
        } else if (op < 12) {
            t.step("set", () -> { t.ref.set(row, lane, value); return null; },
                    () -> { t.impl.set(row, lane, value); return null; });
        } else if (op < 14) {
            t.step("removeSwap", () -> t.ref.removeSwap(row), () -> t.impl.removeSwap(row));
        } else if (op < 16) {
            filter(t, random);
        } else if (op == 16 && random.nextInt(6) == 0) {
            t.step("clear", () -> { t.ref.clear(); return null; },
                    () -> { t.impl.clear(); return null; });
        } else if (op == 17) {
            register(t, random);
        } else {
            t.step("get", () -> t.ref.get(row, lane), () -> t.impl.get(row, lane));
        }
    }

    @Test
    void randomOperationsMatchMaster() {
        for (int seed = 0; seed < 400; seed++) {
            Random random = new Random(0x1A9E5L * 1_000_003L + seed);
            Twin t = new Twin(1 + random.nextInt(3), random.nextInt(5));
            for (int step = 0; step < 400; step++) {
                operate(t, random);
            }
        }
    }

    /** Sorted registration over many equal keys keeps the reference's tie order (stable sort). */
    @Test
    void bulkRegistrationKeepsTieOrder() {
        Random random = new Random(0x1A9E6L);
        for (int trial = 0; trial < 200; trial++) {
            Twin t = new Twin(2, 0);
            int rows = random.nextInt(trial % 10 == 0 ? 3000 : 120);
            for (int r = 0; r < rows; r++) {
                long a = VALUES[random.nextInt(trial % 3 == 0 ? 2 : VALUES.length)];
                long b = v(random);
                t.step("append", () -> t.ref.append2(a, b), () -> t.impl.append2(a, b));
            }
            for (int kind = 0; kind < 4; kind++) {
                register(t, random);
            }
            for (int k = 0; k < 40 && t.ref.size() > 0; k++) {
                int row = random.nextInt(t.ref.size());
                long value = v(random);
                t.step("set after register", () -> { t.ref.set(row, 0, value); return null; },
                        () -> { t.impl.set(row, 0, value); return null; });
            }
            filter(t, random);
        }
        assertTrue(true);
    }
}
