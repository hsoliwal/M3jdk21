// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

/** Standalone Java 21 differential oracle; no JUnit or JNI library required. */
public final class M3IndexPathIdentitySelfTest {
    private M3IndexPathIdentitySelfTest() { }

    public static void main(String[] args) throws ReflectiveOperationException {
        Random random = new Random(0x4d334944454e5449L);
        int comparisons = 0;
        for (int size : new int[]{0, 1, 2, 31, 32, 63, 64, 65, 127, 128,
                129, 255, 256, 257, 513}) {
            for (int trial = 0; trial < 120; trial++) {
                int[] states = new int[size];
                int[] identities = new int[size];
                for (int i = 0; i < size; i++) {
                    identities[i] = i;
                    states[i] = switch (trial % 9) {
                        case 0 -> 0;
                        case 1 -> -1;
                        case 2 -> Integer.MIN_VALUE;
                        case 3 -> i & 1;
                        default -> random.nextInt();
                    };
                }
                int required = trial % 11 == 0 ? 0 : 1 << random.nextInt(32);
                int forbidden = trial % 13 == 0 ? required
                        : trial % 7 == 0 ? 0 : 1 << random.nextInt(32);
                M3IndexPathRank ordinary = M3IndexPathRank.snapshot(
                        identities, identities, new int[][]{states},
                        new int[]{required}, new int[]{forbidden}, 0, size, 1);
                M3IndexPathRank implicit = M3IndexPathRank.snapshotIdentity(
                        states, required, forbidden);
                eq(size, implicit.sourceSize(), "source size");
                eq(ordinary.size(), implicit.size(), "selected size");
                array(ordinary.toArray(), implicit.toArray(), "selected IDs");
                for (int end = 0; end <= size; end++) {
                    eq(ordinary.rank(end), implicit.rank(end), "rank");
                    if (end < size) {
                        same(ordinary.containsSourceOrdinal(end),
                                implicit.containsSourceOrdinal(end), "membership");
                    }
                    comparisons++;
                }
                for (int selected = 0; selected < ordinary.size(); selected++) {
                    eq(ordinary.selectedSourceOrdinal(selected),
                            implicit.selectedSourceOrdinal(selected), "source ordinal");
                    eq(ordinary.selectedRowId(selected),
                            implicit.selectedRowId(selected), "row ID");
                }
                for (int window = 0; window < 12; window++) {
                    int from = random.nextInt(size + 1);
                    int to = from + random.nextInt(size - from + 1);
                    M3IndexPathRank.Window a = ordinary.window(from, to);
                    M3IndexPathRank.Window b = implicit.window(from, to);
                    eq(a.size(), b.size(), "window size");
                    eq(a.sourceSize(), b.sourceSize(), "window source size");
                    array(a.toArray(), b.toArray(), "window rows");
                    // The same bulk atom must work with implicit and explicit rows.
                    int firstSelected = b.size() == 0 ? 0
                            : random.nextInt(b.size() + 1);
                    int take = random.nextInt(b.size() - firstSelected + 1);
                    int[] destination = new int[take + 2];
                    Arrays.fill(destination, 0x3456789a);
                    b.copySelectedRowIds(firstSelected, destination, 1, take);
                    eq(0x3456789a, destination[0], "bulk before destination");
                    eq(0x3456789a, destination[destination.length - 1],
                            "bulk after destination");
                    for (int k = 0; k < take; k++) {
                        eq(b.selectedRowId(firstSelected + k), destination[k + 1],
                                "implicit identity bulk");
                    }
                    for (int selected = 0; selected < a.size(); selected++) {
                        eq(a.selectedSourceOrdinal(selected),
                                b.selectedSourceOrdinal(selected), "window ordinal");
                        eq(a.selectedRowId(selected),
                                b.selectedRowId(selected), "window row");
                    }
                    M3IndexPathRank.Window aa = a.window(0, a.sourceSize());
                    M3IndexPathRank.Window bb = b.window(0, b.sourceSize());
                    array(aa.toArray(), bb.toArray(), "nested window");
                }
                int[] before = implicit.toArray();
                Arrays.fill(states, Integer.MAX_VALUE);
                Arrays.fill(identities, -99);
                array(before, implicit.toArray(), "caller-array snapshot isolation");
                Field backing = M3IndexPathRank.class.getDeclaredField("rows");
                backing.setAccessible(true);
                same(null, backing.get(implicit), "no retained identity int[]");
            }
        }
        System.out.println("PASS identity-mask rank: " + comparisons
                + " exclusive ranks; 1800 seeded Java21 cases");
    }

    private static void eq(int want, int got, String location) {
        if (want != got) throw new AssertionError(location + ": " + want + " vs " + got);
    }
    private static void same(Object want, Object got, String location) {
        if (!java.util.Objects.equals(want, got)) {
            throw new AssertionError(location + ": " + want + " vs " + got);
        }
    }
    private static void array(int[] want, int[] got, String location) {
        if (!Arrays.equals(want, got)) {
            throw new AssertionError(location + ": " + Arrays.toString(want)
                    + " vs " + Arrays.toString(got));
        }
    }
}
