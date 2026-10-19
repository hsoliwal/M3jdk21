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
 * @summary Qualify integrated collection mask selection Java and JNI boundaries
 * @modules java.base/java.util
 * @compile --patch-module java.base=${test.src} M3CollectionMasksTest.java
 * @run main/native/othervm -Xmx128m -Xcheck:jni --patch-module java.base=${test.classes} java.util.M3CollectionMasksTest full
 * @run main/native/othervm -Xmx128m -Xcheck:jni -Djdk.m3.collectionMasks.native=true --patch-module java.base=${test.classes} java.util.M3CollectionMasksTest consumer
 * @run main/native/othervm -Xmx512m -Xcheck:jni --patch-module java.base=${test.classes} java.util.M3CollectionMasksTest large
 */

package java.util;

/**
 * On a built M3JDK21, the native entry comes from libjava. For stock-JDK
 * patch-module qualification only, set test.m3.masks.native.library to the
 * absolute path of a separately compiled copy of the same production C file.
 * System.load runs from this bootstrap-loaded helper, preserving JNI lookup.
 */
public class M3CollectionMasksTest {
    private M3CollectionMasksTest() { }
    private static long cases;

    @FunctionalInterface
    private interface Selector {
        int[] select(long[] words, int from, int to);
    }

    private static Selector[] selectors = {
        M3CollectionMasks::selected,
        M3CollectionMasks::selectedJava,
        M3CollectionMasks::selectedNative,
        M3CollectionMasks::selected0
    };

    public static void main(String[] args) {
        String library = System.getProperty("test.m3.masks.native.library");
        if (library != null) {
            System.load(library);
        }
        String mode = args.length == 0 ? "full" : args[0];
        if (!Set.of("full", "consumer", "large").contains(mode)) {
            throw new IllegalArgumentException("mode must be full, consumer, or large");
        }
        if (mode.equals("consumer")) {
            selectors = new Selector[] {M3CollectionMasks::selected};
        }
        if (mode.equals("large")) {
            capacityBeyondSignedInt();
        } else {
            emptyAndBoundaryMasks();
            exhaustiveTinyMasks();
            randomWindows();
            invalidWindows();
            outputIsolation();
            snapshotConsumer();
        }
        System.out.println("M3CollectionMasksTest PASS cases=" + cases
                + " selectors=" + selectors.length + " mode=" + mode
                + " nativeRequested=" + Boolean.getBoolean("jdk.m3.collectionMasks.native"));
    }

    private static void emptyAndBoundaryMasks() {
        check(new long[0], 0, 0);
        long[][] masks = {
            {0L}, {-1L}, {Long.MIN_VALUE}, {1L},
            {Long.MIN_VALUE, 3L, -1L},
            {-1L, -1L, -1L},
            {0xaaaaaaaaaaaaaaaaL, 0x5555555555555555L, Long.MIN_VALUE}
        };
        int[] bounds = {0, 1, 2, 62, 63, 64, 65, 66, 127, 128, 129, 190, 191, 192};
        for (long[] mask : masks) {
            for (int from : bounds) {
                for (int to : bounds) {
                    if (from <= to && to <= mask.length * 64) {
                        check(mask, from, to);
                    }
                }
            }
        }
        // Both native scratch boundaries, with partial first and last words.
        long[] manyWords = new long[259];
        Arrays.fill(manyWords, -1L);
        check(manyWords, 63, manyWords.length * 64 - 17);
        check(manyWords, 64 * 128 - 1, 64 * 128 + 1);
    }

    private static void exhaustiveTinyMasks() {
        for (int mask = 0; mask < 256; mask++) {
            for (int from = 0; from <= 8; from++) {
                for (int to = from; to <= 8; to++) {
                    check(new long[] {mask}, from, to);
                }
            }
        }
    }

    private static void randomWindows() {
        Random random = new Random(0x4d334a444b32314cL);
        for (int iteration = 0; iteration < 600; iteration++) {
            long[] words = new long[random.nextInt(600)];
            for (int i = 0; i < words.length; i++) {
                words[i] = random.nextLong();
            }
            int capacity = words.length * 64;
            int from = random.nextInt(capacity + 1);
            int to = from + random.nextInt(capacity - from + 1);
            check(words, from, to);
        }
    }

    private static void invalidWindows() {
        for (Selector selector : selectors) {
            expect(NullPointerException.class, () -> selector.select(null, 0, 0));
            expect(NullPointerException.class, () -> selector.select(null, -1, -2));
            expect(IndexOutOfBoundsException.class,
                    () -> selector.select(new long[0], 0, 1));
            expect(IndexOutOfBoundsException.class,
                    () -> selector.select(new long[1], -1, 1));
            expect(IndexOutOfBoundsException.class,
                    () -> selector.select(new long[1], 2, 1));
            expect(IndexOutOfBoundsException.class,
                    () -> selector.select(new long[1], 0, 65));
            expect(IndexOutOfBoundsException.class,
                    () -> selector.select(new long[1], Integer.MIN_VALUE, Integer.MAX_VALUE));
            expect(IndexOutOfBoundsException.class,
                    () -> selector.select(new long[1], 0, Integer.MAX_VALUE));
            expect(IndexOutOfBoundsException.class,
                    () -> selector.select(new long[1], Integer.MAX_VALUE, Integer.MAX_VALUE));
            expect(IndexOutOfBoundsException.class,
                    () -> selector.select(new long[1], 0, Integer.MIN_VALUE));
        }
    }

    private static void outputIsolation() {
        for (Selector selector : selectors) {
            long[] words = {Long.MIN_VALUE, 3L};
            int[] selected = selector.select(words, 0, 128);
            equals(new int[] {63, 64, 65}, selected);
            if (words[0] != Long.MIN_VALUE || words[1] != 3L) {
                throw new AssertionError("input mask mutated");
            }
            Arrays.fill(words, 0L);
            equals(new int[] {63, 64, 65}, selected);
            selected[0] = -1;
            equals(new int[0], selector.select(words, 0, 128));
            cases++;
        }
    }

    private record SnapshotValue(int rank, int occurrence) { }

    /** Exercise the public owning APIs through the same selected() dispatch. */
    private static void snapshotConsumer() {
        Comparator<SnapshotValue> order = Comparator.nullsFirst(
                Comparator.comparingInt(SnapshotValue::rank));
        for (int length : new int[] {0, 1, 2, 63, 64, 65, 127, 128, 129, 511, 512, 513, 8193}) {
            ArrayList<SnapshotValue> source = new ArrayList<>();
            for (int index = 0; index < length; index++)
                source.add(index % 17 == 0 ? null : new SnapshotValue(index * 37 % 11, index));
            Object[] before = source.toArray();
            ArrayList<SnapshotValue> expected = new ArrayList<>();
            for (SnapshotValue value : source)
                if (value == null || (value.rank() & 1) == 0) expected.add(value);
            ArrayList<SnapshotValue> expectedSorted = new ArrayList<>(expected);
            expectedSorted.sort(order);
            int[] calls = {0};
            java.util.function.Predicate<SnapshotValue> predicate = value -> {
                calls[0]++;
                return value == null || (value.rank() & 1) == 0;
            };
            List<SnapshotValue> selected = Collections.filteredSnapshot(source, predicate);
            sameSnapshot(expected, selected);
            if (calls[0] != length) throw new AssertionError("snapshot predicate count");
            cases++;
            calls[0] = 0;
            List<SnapshotValue> sorted = Collections.filteredSortedSnapshot(source, predicate, order);
            sameSnapshot(expectedSorted, sorted);
            if (calls[0] != length) throw new AssertionError("sorted snapshot predicate count");
            cases++;
            if (source.size() != before.length) throw new AssertionError("snapshot changed source size");
            for (int index = 0; index < before.length; index++)
                if (before[index] != source.get(index)) throw new AssertionError("snapshot changed source order");
            cases++;
            if (!Collections.filteredSnapshot(source, value -> false).isEmpty())
                throw new AssertionError("all-hidden snapshot is not empty");
            cases++;
            expect(UnsupportedOperationException.class, () -> selected.add(null));
            expect(UnsupportedOperationException.class, sorted::clear);
            source.clear();
            sameSnapshot(expected, selected);
            sameSnapshot(expectedSorted, sorted);
        }
        expect(NullPointerException.class, () -> Collections.filteredSnapshot(null, value -> true));
        expect(NullPointerException.class, () -> Collections.filteredSnapshot(List.of(), null));
        expect(NullPointerException.class,
                () -> Collections.filteredSortedSnapshot(null, value -> true, order));
        expect(NullPointerException.class,
                () -> Collections.filteredSortedSnapshot(List.of(), null, order));
    }

    private static <E> void sameSnapshot(List<E> expected, List<E> actual) {
        if (expected.size() != actual.size()) throw new AssertionError("snapshot size");
        for (int index = 0; index < expected.size(); index++)
            if (expected.get(index) != actual.get(index))
                throw new AssertionError("snapshot reference or stable order at " + index);
        cases++;
    }

    private static void capacityBeyondSignedInt() {
        // 2^25 words describe 2^31 bits. Narrowing capacity to int would
        // incorrectly reject even this one-position window. Only the last
        // word is scanned, and the upper padding bit must be excluded.
        long[] words = new long[1 << 25];
        words[words.length - 1] = (1L << 62) | Long.MIN_VALUE;
        for (Selector selector : selectors) {
            equals(new int[] {Integer.MAX_VALUE - 1},
                    selector.select(words, Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
            equals(new int[0],
                    selector.select(words, Integer.MAX_VALUE, Integer.MAX_VALUE));
            cases += 2;
        }
    }

    private static void check(long[] words, int from, int to) {
        long[] original = words.clone();
        // Independent existing-JDK oracle, outside the implementation loops.
        int[] expected = BitSet.valueOf(words).stream()
                .filter(index -> index >= from && index < to).toArray();
        for (Selector selector : selectors) {
            equals(expected, selector.select(words, from, to));
            if (!Arrays.equals(original, words)) {
                throw new AssertionError("input mask mutated");
            }
            cases++;
        }
    }

    private static void equals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + ", actual " + Arrays.toString(actual));
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (error.getClass() != type) {
                throw new AssertionError("expected " + type.getName(), error);
            }
            cases++;
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }
}
