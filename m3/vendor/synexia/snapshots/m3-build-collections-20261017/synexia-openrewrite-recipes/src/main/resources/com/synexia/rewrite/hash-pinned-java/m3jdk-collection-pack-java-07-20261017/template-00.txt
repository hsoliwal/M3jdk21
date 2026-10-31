/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
// Modified 2026: M3JDK optional collection projection; source contracts preserved.
package com.m3.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.LongPredicate;

/** Independent behavioral oracle frozen before exact-fit filter payload authoring. */
public final class M3PrimitiveIndexViewFilterContract {
    private static long assertions;

    private M3PrimitiveIndexViewFilterContract() { }

    public static void main(String[] args) {
        Random random = new Random(0x4d33566965774cL);
        for (M3PrimitiveKind kind : M3PrimitiveKind.values()) {
            for (int size : new int[] {0, 1, 2, 7, 63, 64, 65, 257}) {
                verifyFixture(kind, size, random);
            }
        }
        verifyExceptionAndVersionOrder();
        System.out.println("M3PrimitiveIndexViewFilterContract PASS assertions=" + assertions
                + " kinds=8 fixtures=64");
    }

    private static void verifyFixture(M3PrimitiveKind kind, int size, Random random) {
        M3PrimitiveSoaStore store = new M3PrimitiveSoaStore(Math.max(1, size),
                M3PrimitiveSoaStore.STATE_BYTES | M3PrimitiveSoaStore.ROW_IDS
                        | M3PrimitiveSoaStore.VERSION_LONGS,
                kind, M3PrimitiveKind.LONG);
        long[] values = new long[size];
        byte[] states = new byte[size];
        long[] specials = {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE,
                0x7ff8000000000042L, 0x7fc00001L, 0x80000000L, 1L};
        for (int i = 0; i < size; i++) {
            long input = i < specials.length ? specials[i] : random.nextLong();
            values[i] = storedBits(kind, input);
            states[i] = (byte) (i * 37 + 255);
            store.appendRow(new long[] {input, 1000L + i}, states[i], i * 11L);
        }
        long version = store.version();
        M3PrimitiveIndexView identity = M3PrimitiveIndexView.identity(store);
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < size; i++) rows.add(i);
        verifyProjection(identity, rows, store, values, states);
        List<Integer> reversed = new ArrayList<>(rows);
        Collections.reverse(reversed);
        verifyProjection(identity.reversed(), reversed, store, values, states);
        int from = size == 0 ? 0 : 1;
        int to = Math.max(from, size - 1);
        verifyProjection(identity.slice(from, to), rows.subList(from, to),
                store, values, states);
        for (boolean ascending : new boolean[] {true, false}) {
            List<Integer> ordered = new ArrayList<>(rows);
            ordered.sort((a, b) -> {
                int comparison = compareBits(kind, values[a], values[b]);
                if (comparison == 0) comparison = Integer.compare(a, b);
                return ascending ? comparison : -comparison;
            });
            verifyProjection(identity.sortedByLane(0, ascending), ordered,
                    store, values, states);
        }
        equal(version, store.version(), "queries preserve owner generation");
        for (int row = 0; row < size; row++) {
            equal(values[row], store.getBits(0, row), "owner primary lane");
            equal(1000L + row, store.getBits(1, row), "owner secondary lane");
            equal(states[row], store.stateAt(row), "owner state");
        }
    }

    private static void verifyProjection(M3PrimitiveIndexView view, List<Integer> rows,
            M3PrimitiveSoaStore store, long[] values, byte[] states) {
        equal(rows, indexes(view), "projection order");
        for (int mode = 0; mode < 3; mode++) {
            final int selectedMode = mode;
            List<Long> observed = new ArrayList<>();
            M3PrimitiveIndexView selected = view.filterLane(0, bits -> {
                observed.add(bits);
                return admitted(selectedMode, bits);
            });
            List<Long> expectedTrace = new ArrayList<>();
            List<Integer> expectedRows = new ArrayList<>();
            for (int row : rows) {
                expectedTrace.add(values[row]);
                if (admitted(mode, values[row])) expectedRows.add(row);
            }
            equal(expectedTrace, observed, "one ordered callback per source row");
            verifyExport(selected, expectedRows, store, values, states);
        }
        for (int mask : new int[] {0, 1, 2, 3, 127, 128, 255, -1, Integer.MIN_VALUE}) {
            List<Integer> expectedRows = new ArrayList<>();
            for (int row : rows) {
                long state = states[row];
                if ((state & (long) mask) == (long) mask) expectedRows.add(row);
            }
            verifyExport(view.filterStateMask(mask), expectedRows, store, values, states);
        }
        equal(rows, indexes(view), "filtering preserves original projection");
    }

    private static void verifyExport(M3PrimitiveIndexView selected, List<Integer> expectedRows,
            M3PrimitiveSoaStore store, long[] values, byte[] states) {
        equal(expectedRows, indexes(selected), "selected row IDs");
        equal(expectedRows.size(), selected.size(), "selection size");
        equal(expectedRows.isEmpty(), selected.isEmpty(), "selection emptiness");
        int[] exported = selected.toIndexArray();
        Arrays.fill(exported, -7);
        equal(expectedRows, indexes(selected), "index export is defensive");
        M3PrimitiveSoaStore copy = selected.materialize();
        equal(expectedRows.size(), copy.size(), "materialized size");
        for (int ordinal = 0; ordinal < expectedRows.size(); ordinal++) {
            int row = expectedRows.get(ordinal);
            equal(row, selected.sourceRow(ordinal), "source row");
            equal(values[row], selected.getBits(0, ordinal), "selected payload");
            equal(row, selected.rowIdAt(ordinal), "selected stable row ID");
            equal(states[row], selected.stateAt(ordinal), "selected state");
            equal(values[row], copy.getBits(0, ordinal), "materialized payload");
            equal(1000L + row, copy.getBits(1, ordinal), "materialized second lane");
            equal(row, copy.rowIdAt(ordinal), "materialized stable row ID");
            equal(states[row], copy.stateAt(ordinal), "materialized state");
            equal(row * 11L, copy.rowVersionAt(ordinal), "materialized metadata version");
        }
        if (!expectedRows.isEmpty()) {
            copy.setBits(1, 0, -100000L);
            equal(1000L + expectedRows.getFirst(),
                    store.getBits(1, expectedRows.getFirst()), "materialized storage is independent");
            equal(expectedRows, indexes(selected), "copy mutation does not invalidate source view");
        }
    }

    private static void verifyExceptionAndVersionOrder() {
        M3PrimitiveSoaStore store = smallStore();
        M3PrimitiveIndexView view = M3PrimitiveIndexView.identity(store);
        throwsType(NullPointerException.class, () -> view.filterLane(0, null));
        List<Long> trace = new ArrayList<>();
        throwsType(IndexOutOfBoundsException.class,
                () -> view.filterLane(-1, bits -> { trace.add(bits); return true; }));
        equal(List.of(), trace, "invalid lane precedes callback");
        for (int at = 0; at < 3; at++) {
            final int failureAt = at;
            Marker marker = new Marker();
            trace.clear();
            Throwable observed = thrown(() -> view.filterLane(0, bits -> {
                trace.add(bits);
                if (trace.size() - 1 == failureAt) throw marker;
                return true;
            }));
            same(marker, observed, "callback exception identity");
            equal(List.of(10L, 20L, 30L).subList(0, at + 1), trace,
                    "callback exception exact prefix");
        }
        store.setBits(0, 0, 11L);
        throwsType(NullPointerException.class, () -> view.filterLane(0, null));
        trace.clear();
        throwsType(ConcurrentModificationException.class,
                () -> view.filterLane(0, bits -> { trace.add(bits); return true; }));
        equal(List.of(), trace, "stale view prevents callbacks");
        throwsType(ConcurrentModificationException.class, () -> view.filterStateMask(0));

        M3PrimitiveSoaStore modified = smallStore();
        M3PrimitiveIndexView modifiedView = M3PrimitiveIndexView.identity(modified);
        trace.clear();
        throwsType(ConcurrentModificationException.class, () -> modifiedView.filterLane(0, bits -> {
            trace.add(bits);
            if (trace.size() == 1) modified.setBits(0, 1, 99L);
            return true;
        }));
        equal(List.of(10L, 99L, 30L), trace, "version check follows full callback scan");

        M3PrimitiveSoaStore shortened = smallStore();
        M3PrimitiveIndexView shortenedView = M3PrimitiveIndexView.identity(shortened);
        trace.clear();
        throwsType(IndexOutOfBoundsException.class, () -> shortenedView.filterLane(0, bits -> {
            trace.add(bits);
            if (trace.size() == 1) shortened.removeAt(2);
            return true;
        }));
        equal(List.of(10L, 20L), trace, "source access exception precedes final version check");

        M3PrimitiveSoaStore changedThenFailed = smallStore();
        M3PrimitiveIndexView failedView = M3PrimitiveIndexView.identity(changedThenFailed);
        Marker marker = new Marker();
        same(marker, thrown(() -> failedView.filterLane(0, bits -> {
            changedThenFailed.setBits(0, 0, 99L);
            throw marker;
        })), "callback failure precedes final version check");

        M3PrimitiveIndexView empty = M3PrimitiveIndexView.identity(new M3PrimitiveSoaStore(M3PrimitiveKind.LONG));
        LongPredicate forbidden = bits -> { throw new AssertionError("empty callback"); };
        equal(List.of(), indexes(empty.filterLane(-1, forbidden)), "empty view does not validate lane");
        equal(List.of(), indexes(empty.filterStateMask(1)), "empty view does not read absent metadata");
        M3PrimitiveSoaStore noMetadata = new M3PrimitiveSoaStore(M3PrimitiveKind.LONG);
        noMetadata.append1(1L);
        throwsType(IllegalStateException.class,
                () -> M3PrimitiveIndexView.identity(noMetadata).filterStateMask(0));
    }

    private static M3PrimitiveSoaStore smallStore() {
        M3PrimitiveSoaStore store = new M3PrimitiveSoaStore(3, M3PrimitiveSoaStore.STATE_BYTES,
                M3PrimitiveKind.LONG);
        for (long value : new long[] {10L, 20L, 30L}) {
            store.appendRow(new long[] {value}, (byte) 1, 0L);
        }
        return store;
    }

    private static boolean admitted(int mode, long bits) {
        return mode == 0 || (mode == 2 && (bits & 3L) == 1L);
    }

    private static long storedBits(M3PrimitiveKind kind, long raw) {
        return switch (kind) {
            case BOOLEAN -> raw == 0L ? 0L : 1L;
            case BYTE -> (byte) raw;
            case SHORT -> (short) raw;
            case CHAR -> (char) raw;
            case INT -> (int) raw;
            case LONG, DOUBLE -> raw;
            case FLOAT -> Integer.toUnsignedLong((int) raw);
        };
    }

    private static int compareBits(M3PrimitiveKind kind, long a, long b) {
        return switch (kind) {
            case BOOLEAN, BYTE, SHORT, CHAR, INT, LONG -> Long.compare(a, b);
            case FLOAT -> Float.compare(Float.intBitsToFloat((int) a), Float.intBitsToFloat((int) b));
            case DOUBLE -> Double.compare(Double.longBitsToDouble(a), Double.longBitsToDouble(b));
        };
    }

    private static List<Integer> indexes(M3PrimitiveIndexView view) {
        List<Integer> result = new ArrayList<>();
        for (int row : view.toIndexArray()) result.add(row);
        return result;
    }

    private static Throwable thrown(Runnable action) {
        try { action.run(); } catch (RuntimeException failure) { return failure; }
        throw new AssertionError("expected exception");
    }

    private static void throwsType(Class<? extends RuntimeException> type, Runnable action) {
        Throwable actual = thrown(action);
        if (!type.isInstance(actual)) throw new AssertionError("expected " + type + " got " + actual);
        assertions++;
    }

    private static void same(Object expected, Object actual, String reason) {
        if (expected != actual) throw new AssertionError(reason);
        assertions++;
    }

    private static void equal(Object expected, Object actual, String reason) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(reason + " expected=" + expected + " observed=" + actual);
        }
        assertions++;
    }

    private static final class Marker extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
