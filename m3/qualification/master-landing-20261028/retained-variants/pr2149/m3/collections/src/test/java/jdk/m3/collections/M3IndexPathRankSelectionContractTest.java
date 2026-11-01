/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Independent occurrence-list oracle for bitmap prefix selection and source windows. */
final class M3IndexPathRankSelectionContractTest {
    private static int[] rows(int length) {
        int[] rows = new int[length];
        for (int i = 0; i < length; i++) {
            rows[i] = switch (i % 4) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                default -> -7;
            };
        }
        return rows;
    }

    private static int[] identity(int length) {
        int[] offsets = new int[length];
        for (int i = 0; i < length; i++) offsets[i] = i;
        return offsets;
    }

    private static M3IndexPathRank sparse(int[] rows, int[] acceptedOrdinals) {
        int[] states = new int[rows.length];
        for (int ordinal : acceptedOrdinals) states[ordinal] = 1;
        return M3IndexPathRank.snapshot(rows, identity(rows.length), new int[][] {states},
                new int[] {1}, new int[] {0}, 0, rows.length, 1);
    }

    private static int[] selectedRows(int[] capturedRows, int[] acceptedOrdinals) {
        int[] result = new int[acceptedOrdinals.length];
        for (int i = 0; i < result.length; i++) result[i] = capturedRows[acceptedOrdinals[i]];
        return result;
    }

    private static void check(M3IndexPathRank actual, int[] capturedRows, int[] acceptedOrdinals) {
        assertEquals(capturedRows.length, actual.sourceSize());
        assertEquals(acceptedOrdinals.length, actual.size());
        int before = 0;
        for (int end = 0; end <= capturedRows.length; end++) {
            while (before < acceptedOrdinals.length && acceptedOrdinals[before] < end) before++;
            assertEquals(before, actual.rank(end), "exclusive source rank " + end);
            if (end < capturedRows.length) {
                boolean selected = before < acceptedOrdinals.length && acceptedOrdinals[before] == end;
                assertEquals(selected, actual.containsSourceOrdinal(end), "source membership " + end);
            }
        }
        for (int rank = 0; rank < acceptedOrdinals.length; rank++) {
            assertEquals(acceptedOrdinals[rank], actual.selectedSourceOrdinal(rank), "selected rank " + rank);
            assertEquals(capturedRows[acceptedOrdinals[rank]], actual.selectedRowId(rank));
        }
        int[] expectedRows = selectedRows(capturedRows, acceptedOrdinals);
        assertArrayEquals(expectedRows, actual.toArray());
        int[] copy = actual.toArray();
        Arrays.fill(copy, 42);
        assertArrayEquals(expectedRows, actual.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> actual.selectedSourceOrdinal(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.selectedSourceOrdinal(actual.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.selectedSourceOrdinal(Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.selectedRowId(actual.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.rank(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.rank(capturedRows.length + 1));
    }

    private static void checkWindow(M3IndexPathRank.Window actual, int[] capturedRows,
            int[] acceptedOrdinals, int from, int to) {
        List<Integer> selected = new ArrayList<>();
        for (int ordinal : acceptedOrdinals) if (ordinal >= from && ordinal < to) selected.add(ordinal);
        assertEquals(to - from, actual.sourceSize());
        assertEquals(selected.size(), actual.size());
        int before = 0;
        for (int end = 0; end <= to - from; end++) {
            while (before < selected.size() && selected.get(before) < from + end) before++;
            assertEquals(before, actual.rank(end));
        }
        int[] expectedRows = new int[selected.size()];
        for (int rank = 0; rank < selected.size(); rank++) {
            int ordinal = selected.get(rank);
            assertEquals(ordinal - from, actual.selectedSourceOrdinal(rank));
            assertEquals(capturedRows[ordinal], actual.selectedRowId(rank));
            expectedRows[rank] = capturedRows[ordinal];
        }
        assertArrayEquals(expectedRows, actual.toArray());
        // Nonzero page starts also exercise the owner's select operation.
        for (int start = 0; start <= selected.size(); start++) {
            int count = selected.size() - start;
            int[] destination = new int[count + 4];
            Arrays.fill(destination, 42);
            actual.copySelectedRowIds(start, destination, 2, count);
            assertArrayEquals(Arrays.copyOfRange(expectedRows, start, expectedRows.length),
                    Arrays.copyOfRange(destination, 2, count + 2));
            assertEquals(42, destination[0]);
            assertEquals(42, destination[1]);
            assertEquals(42, destination[count + 2]);
            assertEquals(42, destination[count + 3]);
        }
        assertThrows(IndexOutOfBoundsException.class, () -> actual.selectedSourceOrdinal(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.selectedSourceOrdinal(actual.size()));
        assertThrows(IndexOutOfBoundsException.class, () -> actual.selectedRowId(actual.size()));
    }

    @Test void skipsLeadingInteriorAndTrailingEmptyWords() {
        int[] capturedRows = rows(641);
        int[] accepted = {128, 191, 320, 383, 511, 576};
        check(sparse(capturedRows, accepted), capturedRows, accepted);
    }

    @Test void wordEdgesAndLastPartialWordNeverSelectPadding() {
        for (int length : new int[] {0, 1, 31, 32, 63, 64, 65, 66, 127, 128, 129}) {
            int[] capturedRows = rows(length);
            List<Integer> accepted = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                if (i == 0 || i == 31 || i == 32 || i == 62 || i == 63
                        || i == 64 || i == 65 || i == length - 1) accepted.add(i);
            }
            int[] ordinals = accepted.stream().mapToInt(Integer::intValue).toArray();
            check(sparse(capturedRows, ordinals), capturedRows, ordinals);
        }
    }

    @Test void nestedSourceWindowsTranslateSelectedRanksAcrossEmptyWords() {
        int[] capturedRows = rows(641);
        int[] accepted = {0, 63, 64, 65, 128, 191, 320, 383, 511, 576};
        M3IndexPathRank snapshot = sparse(capturedRows, accepted);
        for (int[] range : new int[][] {{0, 641}, {63, 577}, {64, 512}, {65, 576},
                {129, 384}, {192, 320}, {319, 384}, {576, 641}, {641, 641}}) {
            int from = range[0];
            int to = range[1];
            M3IndexPathRank.Window window = snapshot.window(from, to);
            checkWindow(window, capturedRows, accepted, from, to);
            int trim = Math.min(1, to - from);
            checkWindow(window.window(trim, to - from), capturedRows, accepted, from + trim, to);
            checkWindow(window.window(0, to - from - trim), capturedRows, accepted, from, to - trim);
        }
    }

    @Test void negativeStrideKeepsSignedDuplicateRowsAndDetachesEveryInputLane() {
        int[] inputRows = rows(271);
        int[] offsets = new int[inputRows.length];
        for (int i = 0; i < offsets.length; i++) offsets[i] = (i * 37) % offsets.length;
        int[][] planes = {new int[inputRows.length], new int[inputRows.length]};
        Arrays.fill(planes[1], 4);
        int[] needs = {Integer.MIN_VALUE, 4};
        int[] denies = {2, 1};
        int first = 270;
        int count = 136;
        int stride = -2;
        int[] capturedRows = new int[count];
        for (int ordinal = 0; ordinal < count; ordinal++) {
            capturedRows[ordinal] = inputRows[first + ordinal * stride];
        }
        int[] accepted = {0, 31, 32, 63, 64, 65, 127, 135};
        for (int ordinal : accepted) planes[0][offsets[first + ordinal * stride]] = Integer.MIN_VALUE;
        // A matching required bit must still lose to a forbidden bit.
        planes[0][offsets[first + 10 * stride]] = Integer.MIN_VALUE | 2;
        M3IndexPathRank snapshot = M3IndexPathRank.snapshot(inputRows, offsets, planes,
                needs, denies, first, count, stride);
        Arrays.fill(inputRows, 42);
        Arrays.fill(offsets, -1);
        Arrays.fill(planes[0], 0);
        Arrays.fill(planes[1], 0);
        Arrays.fill(needs, -1);
        Arrays.fill(denies, -1);
        check(snapshot, capturedRows, accepted);
        checkWindow(snapshot.window(31, 136).window(32, 105), capturedRows, accepted, 63, 136);
    }

    @Test void allNoneAndZeroCountEndPathsRetainSelectionBounds() {
        int[] capturedRows = rows(129);
        int[] offsets = identity(capturedRows.length);
        int[][] states = {new int[capturedRows.length]};
        M3IndexPathRank all = M3IndexPathRank.snapshot(capturedRows, offsets, states,
                new int[] {0}, new int[] {0}, 0, capturedRows.length, 1);
        check(all, capturedRows, identity(capturedRows.length));
        M3IndexPathRank none = M3IndexPathRank.snapshot(capturedRows, offsets, states,
                new int[] {1}, new int[] {1}, 0, capturedRows.length, 1);
        check(none, capturedRows, new int[0]);
        checkWindow(none.window(63, 129), capturedRows, new int[0], 63, 129);
        M3IndexPathRank empty = M3IndexPathRank.snapshot(capturedRows, offsets, states,
                new int[] {0}, new int[] {0}, capturedRows.length, 0, -3);
        check(empty, new int[0], new int[0]);
        checkWindow(empty.window(0, 0), new int[0], new int[0], 0, 0);
    }
}
