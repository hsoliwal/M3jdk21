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
package jdk.internal.m3.collections.mat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import java.util.Spliterator;
import org.junit.jupiter.api.Test;

/**
 * MatRoaringIntSet after the decode leaf and its atomization against master's class
 * (RoaringReference): the same ten lanes (containers, data layout) from of, range and the four set
 * operations, and the same answers from toArray, intStream (sequential, parallel,
 * short-circuiting), select, rank, next, previous and contains, on sparse, dense, run-shaped and
 * mixed sets.
 */
//FLLDCIM: LEETCODIFY — differential proof for MatRoaringIntSet decode + atomization
class MatRoaringIntSetDecodeTest {

    private static final String[] LANES = {"size", "chunkKeys", "kinds", "offsets", "dataLengths",
        "counts", "prefix", "arrayData", "runData", "bitmapData"};

    private static String lanes(Object set) {
        StringBuilder out = new StringBuilder();
        try {
            for (String name : LANES) {
                Field f = set.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object value = f.get(set);
                out.append(name).append('=').append(value instanceof Integer ? value
                        : Arrays.deepToString(new Object[] {value})).append(';');
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return out.toString();
    }

    /** Shape 0 sparse over the whole range, 1 dense chunks, 2 runs, 3 mixed near the edges. */
    private static int[] values(Random random, int shape, int count) {
        int[] values = new int[count];
        int base = random.nextInt() & ~0xffff;
        for (int i = 0; i < count; i++) {
            values[i] = switch (shape) {
                case 0 -> random.nextInt();
                case 1 -> base + random.nextInt(3 << 16);
                case 2 -> base + (i / 50) * 97 + i % 50;
                default -> random.nextBoolean() ? Integer.MIN_VALUE + random.nextInt(70_000)
                        : Integer.MAX_VALUE - random.nextInt(70_000);
            };
        }
        return values;
    }

    private static void same(MatRoaringIntSet set, RoaringReference ref, String at) {
        assertEquals(lanes(ref), lanes(set), at + " lanes");
        int[] expected = ref.toArray();
        assertArrayEquals(expected, set.toArray(), at + " toArray");
        assertArrayEquals(expected, set.intStream().toArray(), at + " intStream");
        assertArrayEquals(expected, set.intStream().parallel().toArray(), at + " parallel");
        int k = Math.min(expected.length, 7);
        assertArrayEquals(Arrays.copyOf(expected, k), set.intStream().limit(k).toArray(),
                at + " limit");
        assertArrayEquals(expected, splitAll(set.intStream().spliterator()), at + " split pieces");
        Spliterator.OfInt split = set.intStream().spliterator();
        assertTrue(split.hasCharacteristics(Spliterator.SIZED | Spliterator.ORDERED),
                at + " SIZED");
        assertEquals(expected.length, split.estimateSize(), at + " estimateSize");
    }

    /** Splits to pieces of at most 37 values and traverses them in order (starts mid-word). */
    private static int[] splitAll(Spliterator.OfInt split) {
        java.util.stream.IntStream.Builder out = java.util.stream.IntStream.builder();
        collect(split, out);
        return out.build().toArray();
    }

    private static void collect(Spliterator.OfInt split, java.util.stream.IntStream.Builder out) {
        Spliterator.OfInt first = split.estimateSize() > 37 ? split.trySplit() : null;
        if (first != null) {
            collect(first, out);
            collect(split, out);
        } else {
            split.tryAdvance((java.util.function.IntConsumer) out::add);
            split.forEachRemaining((java.util.function.IntConsumer) out::add);
        }
    }

    private static void queries(MatRoaringIntSet set, RoaringReference ref, Random random,
            String at) {
        for (int q = 0; q < 50; q++) {
            int v = q % 2 == 0 && ref.size() > 0
                    ? ref.select(random.nextInt(ref.size())) + random.nextInt(3) - 1
                    : random.nextInt();
            assertEquals(ref.contains(v), set.contains(v), at + " contains " + v);
            assertEquals(ref.rank(v), set.rank(v), at + " rank " + v);
            assertEquals(ref.next(v), set.next(v), at + " next " + v);
            assertEquals(ref.previous(v), set.previous(v), at + " previous " + v);
            if (ref.size() > 0) {
                int ordinal = random.nextInt(ref.size());
                assertEquals(ref.select(ordinal), set.select(ordinal), at + " select " + ordinal);
            }
        }
    }

    @Test
    void operationsAgainstMaster() {
        for (int seed = 0; seed < 400; seed++) {
            Random random = new Random(seed);
            boolean large = seed % 10 == 0;
            int[] a = values(random, large ? (seed / 10) % 4 : seed % 4,
                    random.nextInt(large ? 20_000 : 600));
            int[] b = values(random, (seed / 4) % 4, random.nextInt(600));
            MatRoaringIntSet x = MatRoaringIntSet.of(a);
            MatRoaringIntSet y = MatRoaringIntSet.of(b);
            RoaringReference rx = RoaringReference.of(a);
            RoaringReference ry = RoaringReference.of(b);
            String at = "seed " + seed;
            same(x, rx, at + " of");
            queries(x, rx, random, at);
            same(x.union(y), rx.union(ry), at + " union");
            same(x.intersect(y), rx.intersect(ry), at + " intersect");
            same(x.xor(y), rx.xor(ry), at + " xor");
            same(x.minus(y), rx.minus(ry), at + " minus");
            long from = random.nextInt();
            long to = Math.min(1L + Integer.MAX_VALUE, from + random.nextInt(Integer.MAX_VALUE));
            same(x.range(from, to), rx.range(from, to), at + " range");
        }
    }

    @Test
    void fullChunksEmptyAndEdges() {
        int[] full = new int[1 << 16];
        for (int i = 0; i < full.length; i++) full[i] = i;
        int[] runs1024 = new int[1024 * 5]; // 1024 runs of 5: RUNS ties BITMAP at 8 KiB
        for (int i = 0; i < runs1024.length; i++) runs1024[i] = (i / 5) * 64 + i % 5;
        int[][] cases = {{}, {0}, {Integer.MIN_VALUE, Integer.MAX_VALUE}, full, runs1024,
            Arrays.copyOf(full, 4096), Arrays.copyOf(full, 4097),
            Arrays.stream(full).filter(v -> v % 2 == 0).toArray()};
        for (int[] values : cases) {
            String at = "case of " + values.length;
            same(MatRoaringIntSet.of(values), RoaringReference.of(values), at);
            long all = 1L + Integer.MAX_VALUE;
            same(MatRoaringIntSet.of(values).range(Integer.MIN_VALUE, all),
                    RoaringReference.of(values).range(Integer.MIN_VALUE, all), at);
        }
    }
}
