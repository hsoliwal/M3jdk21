/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* @test
 * @summary Direct java.base masked index/JNI scan and original live filtered/sorted owner integration
 * @modules java.base/jdk.internal.m3
 */
package jdk.internal.m3;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;

/** Behavioral oracle over the actual M3JDK21 java.base originals; no runtime adapter. */
public final class M3MaskCollectionViewIntegrationTest {
    private static final int CANARY = 0x5a7a21ff;
    private static long checks;

    public static void main(String[] args) {
        String mode = args.length == 0 ? "java" : args[0];
        if (args.length > 2 || !(mode.equals("java") || mode.equals("native"))) {
            throw new IllegalArgumentException("expected java or native [absolute-library-path]");
        }
        if (args.length == 2) System.load(args[1]);
        M3IndexMaskWindow.Backend backend = mode.equals("native")
                ? M3IndexMaskWindow.Backend.NATIVE_REQUIRED : M3IndexMaskWindow.Backend.JAVA;
        if (backend == M3IndexMaskWindow.Backend.NATIVE_REQUIRED) {
            check(M3IndexMaskWindow.nativeAvailable(), "JNI scan ABI absent");
            check(M3IndexMaskWindow.nativeCountAvailable(), "JNI count ABI absent");
        }
        check(CollectionViews.class.getClassLoader() == null, "owner not bootstrap");
        check(CollectionViews.class.getModule() == Object.class.getModule(), "owner not java.base");
        Random random = new Random(0x4d33434f4c4c4543L);
        for (int length : new int[]{0, 1, 2, 63, 64, 65, 127, 128, 129, 256, 257, 512, 513, 4097}) {
            for (int trial = 0; trial < 3; trial++) {
                int[] rows = new int[length];
                byte[] states = new byte[length];
                random.nextBytes(states);
                for (int i = 0; i < length; i++) rows[i] = trial == 0 ? i / 3 : random.nextInt(17) - 8;
                for (int[] flags : new int[][]{{0, 0}, {1, 0}, {128, 0}, {255, 0},
                        {1, 2}, {0, 128}, {128, 1}, {3, 3}}) {
                    verify(rows, states, flags[0], flags[1], backend);
                }
            }
        }
        refuse(backend);
        System.out.println("M3_INTEGRATED_MASK_FILTER_PASS mode=" + mode + " checks=" + checks);
    }

    private static void verify(int[] rows, byte[] states, int required, int excluded,
            M3IndexMaskWindow.Backend backend) {
        int n = rows.length;
        List<Integer> ordinals = new AbstractList<>() {
            @Override public int size() { return n; }
            @Override public Integer get(int index) { java.util.Objects.checkIndex(index, n); return index; }
        };
        Collection<Integer> live = CollectionViews.filtered((Collection<Integer>) ordinals,
                i -> matches(states[i], required, excluded));
        int[] visible = physical(live, rows);
        check(Arrays.equals(visible, scan(rows, states, required, excluded, backend)), "live order");
        long counted = M3IndexMaskWindow.countBounded(states, 0, n, required, excluded, backend);
        check(M3IndexMaskWindow.nextIndex(counted) == n, "count end");
        check(M3IndexMaskWindow.written(counted) == visible.length, "count matches");

        Comparator<Integer> byValueThenSlot = (a, b) -> {
            int byValue = Integer.compare(rows[a], rows[b]);
            return byValue != 0 ? byValue : Integer.compare(a, b);
        };
        NavigableSet<Integer> sorted = new TreeSet<>(byValueThenSlot);
        for (int i = 0; i < n; i++) sorted.add(i);
        NavigableSet<Integer> filtered = CollectionViews.filtered(sorted,
                i -> matches(states[i], required, excluded));
        for (boolean descending : new boolean[]{false, true}) {
            NavigableSet<Integer> source = descending ? sorted.descendingSet() : sorted;
            NavigableSet<Integer> selected = descending ? filtered.descendingSet() : filtered;
            int[] ordered = new int[n];
            byte[] marks = new byte[n];
            int cursor = 0;
            for (int ordinal : source) {
                ordered[cursor] = rows[ordinal];
                marks[cursor++] = states[ordinal];
            }
            check(Arrays.equals(physical(selected, rows),
                    scan(ordered, marks, required, excluded, backend)), "sorted order");
        }
        if (n > 0) {
            int[] snapshot = visible.clone();
            byte original = states[0];
            states[0] = (byte) (original ^ 0xff);
            try {
                check(Arrays.equals(physical(live, rows),
                        scan(rows, states, required, excluded, backend)), "live mutation");
                check(Arrays.equals(visible, snapshot), "detached output mutated");
            } finally {
                states[0] = original;
            }
        }
    }

    private static int[] physical(Iterable<Integer> ordinals, int[] rows) {
        ArrayList<Integer> selected = new ArrayList<>();
        for (int ordinal : ordinals) selected.add(rows[ordinal]);
        return selected.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] scan(int[] rows, byte[] states, int required, int excluded,
            M3IndexMaskWindow.Backend backend) {
        int[] result = new int[rows.length];
        int cursor = 0, written = 0;
        while (cursor < rows.length) {
            int[] page = new int[15];
            Arrays.fill(page, CANARY);
            long packed = M3IndexMaskWindow.scanBounded(rows, states, cursor, 19,
                    required, excluded, page, 1, 13, backend);
            int next = M3IndexMaskWindow.nextIndex(packed);
            int count = M3IndexMaskWindow.written(packed);
            check(next > cursor && next <= Math.min(rows.length, cursor + 19), "scan progress");
            check(count >= 0 && count <= 13 && count <= next - cursor, "scan count");
            check(page[0] == CANARY && page[14] == CANARY, "output canary");
            System.arraycopy(page, 1, result, written, count);
            written += count;
            cursor = next;
        }
        return Arrays.copyOf(result, written);
    }

    private static boolean matches(byte state, int required, int excluded) {
        int bits = Byte.toUnsignedInt(state);
        for (int b = 0; b < 8; b++) {
            int mask = 1 << b;
            if ((required & mask) != 0 && (bits & mask) == 0) return false;
            if ((excluded & mask) != 0 && (bits & mask) != 0) return false;
        }
        return true;
    }

    private static void refuse(M3IndexMaskWindow.Backend backend) {
        int[] rows = {1, 2};
        byte[] states = {1, 2};
        illegal(() -> M3IndexMaskWindow.scan(rows, states, 0, 0, 0, rows, 0, 0, backend));
        illegal(() -> M3IndexMaskWindow.scan(rows, states, 0, 256, 0, new int[3], 0, 1, backend));
        illegal(() -> M3IndexMaskWindow.countBounded(states, 0, -1, 0, 0, backend));
    }

    private static void illegal(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("missing refusal");
    }

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason + " @" + checks);
    }
}
