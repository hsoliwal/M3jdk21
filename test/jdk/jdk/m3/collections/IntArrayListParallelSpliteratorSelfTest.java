/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;

/** Independent primitive array oracle for split, late-binding and fail-fast semantics. */
public final class IntArrayListParallelSpliteratorSelfTest {
    private static long checks;
    private IntArrayListParallelSpliteratorSelfTest() { }

    public static void main(String[] args) {
        for (int size : new int[] {
                0, 1, 2, 3, 7, 16, 31, 32, 63, 64, 65, 127, 128, 129,
                255, 256, 257, 1023, 1024, 65535, 65536, 100000
        }) {
            int[] values = new int[size];
            for (int i = 0; i < size; i++) {
                values[i] = switch (i % 7) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> 0;
                    case 3 -> -i;
                    default -> i & 15;
                };
            }
            verify(values);
        }
        Random random = new Random(0x4d33494e544c4953L);
        for (int sample = 0; sample < 1_100; sample++) {
            int n = random.nextInt(450);
            int[] values = new int[n];
            for (int i = 0; i < n; i++) {
                values[i] = random.nextInt(21) - 10;
            }
            verify(values);
        }
        lateBinding();
        failFast();
        nonstructuralReplacement();
        System.out.println("PASS M3 IntArrayListParallelSpliterator checks=" + checks);
    }

    private static void verify(int[] source) {
        IntArrayList list = IntArrayList.copyOf(source);
        int[] baseline = source.clone();
        Spliterator.OfInt root = list.intSpliterator();
        characteristics(root);
        same(source.length, root.getExactSizeIfKnown(), "late-bind initial size");

        List<Spliterator.OfInt> partitions = new ArrayList<>();
        partition(root, partitions);
        int[] flattened = new int[baseline.length];
        int[] at = {0};
        for (Spliterator.OfInt part : partitions) {
            characteristics(part);
            int expected = Math.toIntExact(part.getExactSizeIfKnown());
            int before = at[0];
            part.forEachRemaining((int value) -> {
                if (at[0] >= flattened.length) throw new AssertionError("extra split value");
                flattened[at[0]++] = value;
            });
            same(expected, at[0] - before, "exact split consumption");
            same(0L, part.estimateSize(), "consumed split size");
        }
        same(baseline.length, at[0], "total emitted positions");
        same(baseline, flattened, "prefix/suffix partition encounter order");
        same(baseline, list.parallelIntStream().toArray(), "parallel primitive encounter order");
        long total = 0;
        for (int value : baseline) total += value;
        same(total, list.parallelIntStream().asLongStream().sum(), "parallel long reduction");
        same(baseline, list.toArray(), "source not mutated");
    }

    private static void partition(Spliterator.OfInt segment, List<Spliterator.OfInt> result) {
        long length = segment.estimateSize();
        if (length <= 64) {
            result.add(segment);
            return;
        }
        Spliterator.OfInt left = segment.trySplit();
        check(left != null, "nontrivial segment must split");
        long leftSize = left.estimateSize();
        long rightSize = segment.estimateSize();
        check(leftSize > 0 && rightSize > 0 && leftSize + rightSize == length,
                "split partitions exact disjoint range");
        partition(left, result);
        partition(segment, result);
    }

    private static void characteristics(Spliterator.OfInt view) {
        int mask = Spliterator.ORDERED | Spliterator.SIZED
                | Spliterator.SUBSIZED | Spliterator.NONNULL;
        check((view.characteristics() & mask) == mask, "supported characteristics");
        check(!view.hasCharacteristics(Spliterator.CONCURRENT), "mutable owner not concurrent");
        check(!view.hasCharacteristics(Spliterator.IMMUTABLE), "mutable owner not immutable");
    }

    private static void lateBinding() {
        IntArrayList list = IntArrayList.copyOf(new int[] {7, 8});
        Spliterator.OfInt delayed = list.intSpliterator();
        list.add(9);
        same(3L, delayed.estimateSize(), "late binding includes pre-first-use append");
        expect(ConcurrentModificationException.class, () -> {
            list.add(10);
            delayed.estimateSize();
        });
        IntArrayList second = IntArrayList.copyOf(new int[] {11, 12, 13});
        Spliterator.OfInt notBound = second.intSpliterator();
        second.add(14);
        same(new int[] {11, 12, 13, 14},
                java.util.stream.StreamSupport.intStream(notBound, false).toArray(),
                "late bound stream includes pre-traversal append");
    }

    private static void failFast() {
        IntArrayList list = IntArrayList.copyOf(new int[] {3, 2, 1, 0});
        Spliterator.OfInt cursor = list.intSpliterator();
        Spliterator.OfInt prefix = cursor.trySplit();
        check(prefix != null, "nonempty spliterator splits");
        list.add(99);
        expect(ConcurrentModificationException.class, () -> prefix.tryAdvance((int x) -> { }));
        expect(ConcurrentModificationException.class, () -> cursor.tryAdvance((int x) -> { }));
        expect(ConcurrentModificationException.class, cursor::trySplit);
        IntArrayList callbackOwner = IntArrayList.copyOf(new int[] {1, 2, 3});
        Spliterator.OfInt callback = callbackOwner.intSpliterator();
        expect(ConcurrentModificationException.class,
                () -> callback.tryAdvance((int v) -> callbackOwner.add(4)));
        expect(NullPointerException.class, () ->
                IntArrayList.copyOf(new int[] {1}).intSpliterator().tryAdvance(null));
    }

    private static void nonstructuralReplacement() {
        IntArrayList list = IntArrayList.copyOf(new int[] {8, 9, 10, 11});
        Spliterator.OfInt view = list.intSpliterator();
        same(4L, view.estimateSize(), "bound before replacing value");
        list.set(1, 99);
        same(new int[] {8, 99, 10, 11},
                java.util.stream.StreamSupport.intStream(view, false).toArray(),
                "nonstructural replacement observable without fail-fast");
        IntArrayList other = IntArrayList.copyOf(new int[] {1, 2, 3});
        Spliterator.OfInt frozen = other.intSpliterator();
        frozen.estimateSize();
        other.sort();
        expect(ConcurrentModificationException.class, frozen::estimateSize);
    }

    private static void expect(Class<? extends Throwable> type, Runnable task) {
        checks++;
        try { task.run(); }
        catch (Throwable seen) {
            if (type.isInstance(seen)) return;
            throw new AssertionError("wrong failure " + seen, seen);
        }
        throw new AssertionError("expected exception " + type.getName());
    }
    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message + " check=" + checks);
    }
    private static void same(long expected, long actual, String message) {
        checks++;
        if (expected != actual) throw new AssertionError(
                message + " expected=" + expected + " actual=" + actual);
    }
    private static void same(int[] expected, int[] actual, String message) {
        checks++;
        if (!Arrays.equals(expected, actual)) throw new AssertionError(
                message + " expected=" + Arrays.toString(expected)
                        + " actual=" + Arrays.toString(actual));
    }
}
