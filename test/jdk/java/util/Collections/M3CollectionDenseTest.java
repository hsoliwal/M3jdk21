/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary Preserve dense mask boundaries and integrated snapshot contracts
 * @modules java.base/java.util
 * @compile --patch-module java.base=${test.src} M3CollectionDenseTest.java
 * @run main/othervm --patch-module java.base=${test.classes} java.util.M3CollectionDenseTest java
 * @run main/native/othervm -Xcheck:jni --patch-module java.base=${test.classes} java.util.M3CollectionDenseTest native
 */
package java.util;

import java.util.function.Predicate;

public final class M3CollectionDenseTest {
    private static long checks;
    private static long trace;
    private static boolean nativeMode;

    private M3CollectionDenseTest() { }

    public static void main(String[] args) {
        nativeMode = args.length == 1 && args[0].equals("native");
        String library = System.getProperty("test.m3.masks.native.library");
        if (library != null) System.load(library);
        denseWindows();
        chunkResidues();
        snapshots();
        System.out.println("M3CollectionDenseTest PASS checks=" + checks + " trace="
                + Long.toUnsignedString(trace, 16) + " native=" + nativeMode);
    }

    private static void require(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }

    private static void mask(long[] words, int from, int to) {
        long[] before = words.clone();
        int[] expected = BitSet.valueOf(words).stream()
                .filter(bit -> bit >= from && bit < to).toArray();
        int[] actual = M3CollectionMasks.selectedJava(words, from, to);
        require(Arrays.equals(expected, actual));
        require(Arrays.equals(before, words));
        require(Arrays.equals(expected, M3CollectionMasks.selected(words, from, to)));
        if (nativeMode) {
            require(Arrays.equals(expected, M3CollectionMasks.selectedNative(words, from, to)));
            require(Arrays.equals(expected, M3CollectionMasks.selected0(words, from, to)));
        }
        for (int value : actual) trace = trace * 31 + value;
    }

    private static void denseWindows() {
        long[] words = new long[6];
        Arrays.fill(words, -1L);
        for (int from = 0; from < 64; from++) {
            for (int last = 0; last < 64; last++) mask(words, from, 256 + last);
        }
        words = new long[259];
        Arrays.fill(words, -1L);
        for (int from = 0; from < 64; from++) {
            mask(words, from, words.length * 64 - from);
            mask(words, 8192 - from, 8192 + from);
        }
    }

    private static void chunkResidues() {
        // Every output-buffer residue before a complete word, then sparse tail.
        for (int prefix = 0; prefix < 256; prefix++) {
            long[] words = new long[12];
            for (int bit = 0; bit < prefix; bit++) words[bit >>> 6] |= 1L << bit;
            Arrays.fill(words, 4, 11, -1L);
            words[11] = 0x8000000000000001L;
            mask(words, 0, words.length * 64);
        }
        for (int hole = 0; hole < 64; hole++) {
            long[] words = {-1L, -1L ^ (1L << hole), -1L, Long.MIN_VALUE};
            mask(words, 0, 256);
        }
    }

    private record Item(int key, int ordinal) { }

    private static void snapshots() {
        for (int length : new int[] {0, 1, 2, 63, 64, 65, 255, 256, 257, 4096}) {
            List<Item> source = new ArrayList<>();
            for (int i = 0; i < length; i++) source.add(new Item((length - i) % 7, i));
            for (int rejected : new int[] {-1, 0, length / 2, length - 1, length}) {
                List<Item> expected = new ArrayList<>();
                for (Item item : source) if (item.ordinal() != rejected) expected.add(item);
                int[] calls = {0};
                Predicate<Item> predicate = item -> {
                    require(item == source.get(calls[0]++));
                    return item.ordinal() != rejected;
                };
                List<Item> actual = Collections.filteredSnapshot(source, predicate);
                require(calls[0] == length);
                equalIdentity(expected, actual);
                equalIdentity(expected.reversed(), actual.reversed());
                for (int bound : new int[] {0, actual.size() / 2, actual.size()}) {
                    equalIdentity(expected.subList(bound, expected.size()),
                            actual.subList(bound, actual.size()));
                }
                calls[0] = 0;
                Comparator<Item> comparator = (left, right) -> {
                    require(calls[0] == length);
                    return Integer.compare(left.key(), right.key());
                };
                List<Item> sorted = Collections.filteredSortedSnapshot(source, predicate, comparator);
                expected.sort(Comparator.comparingInt(Item::key));
                equalIdentity(expected, sorted);
                require(calls[0] == length);
                expectUnsupported(() -> sorted.clear());
                expectUnsupported(() -> sorted.removeIf(item -> false));
                expectUnsupported(() -> sorted.reversed().sort(comparator));
                List<Item> captured = new ArrayList<>(actual);
                if (!source.isEmpty()) {
                    Item first = source.set(0, new Item(-1, -1));
                    equalIdentity(captured, actual);
                    source.set(0, first);
                }
            }
        }
        List<String> nulls = Arrays.asList("b", null, "a", null);
        equalIdentity(nulls, Collections.filteredSnapshot(nulls, value -> true));
        List<String> sorted = Collections.filteredSortedSnapshot(nulls, value -> true,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        equalIdentity(Arrays.asList(null, null, "a", "b"), sorted);
        RuntimeException marker = new RuntimeException("predicate marker");
        int[] calls = {0};
        try {
            Collections.filteredSnapshot(nulls, value -> {
                if (++calls[0] == 3) throw marker;
                return true;
            });
            throw new AssertionError("missing predicate failure");
        } catch (RuntimeException failure) {
            require(failure == marker);
            require(calls[0] == 3);
        }
    }

    private static void equalIdentity(List<?> expected, List<?> actual) {
        require(expected.size() == actual.size());
        for (int i = 0; i < expected.size(); i++) require(expected.get(i) == actual.get(i));
    }

    private static void expectUnsupported(Runnable action) {
        try {
            action.run();
            throw new AssertionError("mutation accepted");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
    }
}
