// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent Java 21 differential oracle for selected-ordinal bitmap word traversal. */
public final class M3IndexPathRankWordWalkProof {
    private M3IndexPathRankWordWalkProof() { }

    public static void main(String[] args) {
        int[] lengths = {0, 1, 2, 31, 32, 63, 64, 65, 66, 127, 128,
                129, 255, 256, 257, 1023, 1024, 1025, 4095, 4096, 4097};
        Random random = new Random(0x4d33574f52444cL);
        long compared = 0;
        for (int length : lengths) {
            for (int direction = 0; direction < 2; direction++) {
                for (int pattern = 0; pattern < 5; pattern++) {
                    int[] rows = new int[length];
                    int[] slots = new int[length];
                    int[][] planes = {new int[length], new int[length]};
                    for (int index = 0; index < length; index++) {
                        rows[index] = random.nextInt(23) - 11;
                        slots[index] = index;
                        boolean accepted = switch (pattern) {
                            case 0 -> false;
                            case 1 -> true;
                            case 2 -> index % 64 == 63;
                            case 3 -> index % 64 == 0;
                            default -> random.nextBoolean();
                        };
                        planes[0][index] = accepted ? 0x8000_0000 : 0;
                        planes[1][index] = (index % 5 == 0) ? 0b101 : 0b100;
                    }
                    int first = direction == 0 || length == 0 ? 0 : length - 1;
                    int stride = direction == 0 ? 1 : -1;
                    M3IndexPathRank snapshot = M3IndexPathRank.snapshot(
                            rows, slots, planes,
                            new int[] {0x8000_0000, 0b100},
                            new int[] {0, 0b001},
                            first, length, stride);
                    for (int caseNumber = 0; caseNumber < 30; caseNumber++) {
                        int from = random.nextInt(length + 1);
                        int to = from + random.nextInt(length - from + 1);
                        M3IndexPathRank.Window window = snapshot.window(from, to);
                        int[] expected = new int[to - from];
                        int write = 0;
                        for (int ordinal = from; ordinal < to; ordinal++) {
                            int physical = direction == 0 ? ordinal : length - 1 - ordinal;
                            int state0 = planes[0][physical];
                            int state1 = planes[1][physical];
                            if ((state0 & 0x8000_0000) != 0
                                    && (state1 & 0b100) != 0
                                    && (state1 & 0b001) == 0) {
                                expected[write++] = rows[physical];
                            }
                        }
                        expected = Arrays.copyOf(expected, write);
                        if (window.size() != write || !Arrays.equals(expected, window.toArray())) {
                            throw new AssertionError(
                                    "word-walk mismatch: length=" + length
                                            + ",dir=" + direction
                                            + ",pattern=" + pattern
                                            + ",range=[" + from + "," + to + ")");
                        }
                        for (int rank = 0; rank < window.size(); rank++) {
                            if (window.selectedRowId(rank) != expected[rank]) {
                                throw new AssertionError("rank lookup mismatch");
                            }
                        }
                        if (!Arrays.equals(expected,
                                window.window(0, window.sourceSize()).toArray())) {
                            throw new AssertionError("nested window changed result");
                        }
                        compared++;
                    }
                    int[] detached = snapshot.toArray();
                    Arrays.fill(rows, Integer.MIN_VALUE);
                    Arrays.fill(slots, -1);
                    Arrays.fill(planes[0], 0);
                    Arrays.fill(planes[1], 0);
                    if (!Arrays.equals(detached, snapshot.toArray())) {
                        throw new AssertionError("immutable source snapshot changed");
                    }
                }
            }
        }
        sparseWindows();
        System.out.println("PASS M3IndexPathRank word walk: " + compared
                + " independent Java21 windows; signed rows, duplicates, strides, masks");
    }
    private static void sparseWindows() {
        int length = 1 << 18;
        int[] rows = new int[length], slots = new int[length], states = new int[length];
        int[] selected = {0, 63, 64, 129, 10001, length / 2, length - 65, length - 1};
        for (int i = 0; i < length; i++) { rows[i] = (i & 1) == 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE; slots[i] = i; }
        for (int i : selected) states[i] = 1;
        var index = M3IndexPathRank.snapshot(rows, slots, new int[][] {states},
                new int[] {1}, new int[] {0}, 0, length, 1);
        int checks = 0;
        for (int from : new int[] {0, 1, 63, 64, 65, 129, 130, 10002, length / 2, length - 64}) {
            for (int to : new int[] {length / 2, length - 1, length}) {
                if (to < from) continue;
                int[] expected = Arrays.stream(selected).filter(i -> i >= from && i < to).map(i -> rows[i]).toArray();
                var window = index.window(from, to);
                if (window.size() != expected.length || !Arrays.equals(expected, window.toArray())
                        || !Arrays.equals(expected, index.window(0, length).window(from, to).toArray())) {
                    throw new AssertionError("sparse offset window");
                }
                int[] detached = window.toArray(); Arrays.fill(detached, 5);
                if (!Arrays.equals(expected, window.toArray())) throw new AssertionError("output escaped");
                checks++;
            }
        }
        System.out.println("PASS sparse wide offset/extreme-row windows: " + checks);
    }

}
