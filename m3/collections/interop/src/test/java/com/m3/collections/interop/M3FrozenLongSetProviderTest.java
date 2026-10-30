/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSortedSet;
import com.m3.collections.M3FrozenLongSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.SplittableRandom;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.junit.jupiter.api.Test;

/**
 * LS1 provider differentials for {@link M3FrozenLongSet}, each against its own oracle (no cross-provider
 * equivalence): Guava {@code ImmutableSortedSet} (order, first/last, asList get/indexOf, hash, toString,
 * immutability) and Eclipse Collections {@code ImmutableLongSet} (primitive membership, size, sorted export,
 * min/max, hash). At least 40,000 operations per provider.
 */
final class M3FrozenLongSetProviderTest {
    private static final long[] EXTREMES = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};

    @Test
    void differentialAgainstGuavaImmutableSortedSet() {
        SplittableRandom random = new SplittableRandom(0x6A7A_15L);
        long operations = 0;
        for (int round = 0; round < 1000; round++) {
            long[] source = source(random, round);
            M3FrozenLongSet set = M3FrozenLongSet.copyOf(source);
            Long[] boxed = new Long[source.length];
            for (int i = 0; i < source.length; i++) boxed[i] = source[i];
            ImmutableSortedSet<Long> guava = ImmutableSortedSet.copyOf(boxed);
            List<Long> view = guava.asList();
            assertEquals(guava.size(), set.size());
            assertEquals(guava.hashCode(), set.hashCode());
            assertEquals(guava.toString(), set.toString());
            operations += 3;
            for (int step = 0; step < 32; step++) {
                long probe = probe(random, source);
                assertEquals(guava.contains(probe), set.contains(probe));
                assertEquals(view.indexOf(probe), set.rank(probe));
                int ordinal = random.nextInt(-2, view.size() + 2);
                if (ordinal >= 0 && ordinal < view.size()) {
                    assertEquals((long) view.get(ordinal), set.get(ordinal));
                } else {
                    final int bad = ordinal;
                    assertThrows(IndexOutOfBoundsException.class, () -> view.get(bad));
                    assertThrows(IndexOutOfBoundsException.class, () -> set.get(bad));
                }
                operations += 3;
            }
            if (guava.isEmpty()) {
                assertThrows(NoSuchElementException.class, guava::first);
                assertThrows(NoSuchElementException.class, set::first);
                assertThrows(NoSuchElementException.class, guava::last);
                assertThrows(NoSuchElementException.class, set::last);
            } else {
                assertEquals((long) guava.first(), set.first());
                assertEquals((long) guava.last(), set.last());
            }
            PrimitiveIterator.OfLong cursor = set.iterator();
            Iterator<Long> oracle = guava.iterator();
            while (oracle.hasNext()) {
                assertEquals((long) oracle.next(), cursor.nextLong());
                operations++;
            }
            assertFalse(cursor.hasNext());
            Set<Long> asSet = guava;
            long value = probe(random, source);
            assertThrows(UnsupportedOperationException.class, () -> asSet.add(value));
            assertThrows(UnsupportedOperationException.class, () -> set.add(value));
            assertThrows(UnsupportedOperationException.class, () -> asSet.remove(value));
            assertThrows(UnsupportedOperationException.class, () -> set.remove(value));
            operations += 4;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    @Test
    void differentialAgainstEclipseImmutableLongSet() {
        SplittableRandom random = new SplittableRandom(0xEC_15L);
        long operations = 0;
        for (int round = 0; round < 1000; round++) {
            long[] source = source(random, round);
            M3FrozenLongSet set = M3FrozenLongSet.copyOf(source);
            ImmutableLongSet eclipse = LongSets.immutable.with(source);
            assertEquals(eclipse.size(), set.size());
            assertEquals(eclipse.isEmpty(), set.isEmpty());
            assertEquals(eclipse.hashCode(), set.hashCode());
            assertArrayEquals(eclipse.toSortedArray(), set.toArray());
            operations += 4;
            for (int step = 0; step < 36; step++) {
                long probe = probe(random, source);
                assertEquals(eclipse.contains(probe), set.contains(probe));
                operations++;
            }
            if (eclipse.isEmpty()) {
                assertThrows(NoSuchElementException.class, eclipse::min);
                assertThrows(NoSuchElementException.class, set::first);
                assertThrows(NoSuchElementException.class, eclipse::max);
                assertThrows(NoSuchElementException.class, set::last);
            } else {
                assertEquals(eclipse.min(), set.first());
                assertEquals(eclipse.max(), set.last());
            }
            long[] sum = new long[2];
            eclipse.each(v -> sum[0] += v);
            set.forEach(v -> sum[1] += v);
            assertEquals(sum[0], sum[1]);
            List<Long> rebuilt = new ArrayList<>();
            set.forEach(rebuilt::add);
            assertEquals(eclipse.size(), rebuilt.size());
            operations += 4;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }

    private static long[] source(SplittableRandom random, int round) {
        int length = round % 19 == 0 ? 0 : random.nextInt(1, 40);
        long[] source = new long[length];
        for (int i = 0; i < length; i++) {
            int mode = random.nextInt(3);
            source[i] = mode == 0 ? EXTREMES[random.nextInt(EXTREMES.length)] : mode == 1 ? random.nextLong() : random.nextLong(-30, 31);
        }
        return source;
    }

    private static long probe(SplittableRandom random, long[] source) {
        int mode = random.nextInt(3);
        if (mode == 0 && source.length > 0) return source[random.nextInt(source.length)];
        return mode == 1 ? EXTREMES[random.nextInt(EXTREMES.length)] : random.nextLong(-31, 32);
    }
}
