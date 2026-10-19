/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.Multiset;
import com.m3.collections.M3FrozenLongBag;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/**
 * LS2 provider differential for {@link M3FrozenLongBag} against Guava, each oracle on its own:
 * {@code ImmutableMultiset} (total size, counts, distinct size, hash, immutability; first-insertion order NOT
 * claimed) and {@code ImmutableSortedMultiset} (ascending distinct order, ordinals, rendering). At least 40,000 ops.
 */
final class M3FrozenLongBagProviderTest {
    private static final long[] EXTREMES = {Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE};

    @Test
    void differentialAgainstGuavaImmutableMultisetAndSortedMultiset() {
        SplittableRandom random = new SplittableRandom(0x6A7A_BA6L);
        long operations = 0;
        for (int round = 0; round < 1000; round++) {
            int length = round % 13 == 0 ? 0 : random.nextInt(1, 50);
            long[] source = new long[length];
            Long[] boxed = new Long[length];
            for (int i = 0; i < length; i++) {
                int mode = random.nextInt(3);
                source[i] = mode == 0 ? EXTREMES[random.nextInt(EXTREMES.length)] : random.nextLong(-10, 11);
                boxed[i] = source[i];
            }
            ImmutableMultiset<Long> guava = ImmutableMultiset.copyOf(boxed);
            ImmutableSortedMultiset<Long> sorted = ImmutableSortedMultiset.copyOf(boxed);
            List<Long> distinct = sorted.elementSet().asList();
            M3FrozenLongBag bag = M3FrozenLongBag.copyOf(source);
            assertEquals(guava.size(), bag.size());
            assertEquals(guava.elementSet().size(), bag.distinctSize());
            assertEquals(guava.hashCode(), bag.hashCode());
            assertEquals(sorted.toString(), bag.toString());
            assertArrayEquals(distinct.stream().mapToLong(Long::longValue).toArray(), bag.distinctToArray());
            operations += 5;
            for (int step = 0; step < 32; step++) {
                long probe = random.nextInt(3) == 0 && length > 0 ? source[random.nextInt(length)] : random.nextLong(-12, 13);
                assertEquals(guava.count(probe), bag.occurrencesOf(probe));
                assertEquals(guava.contains(probe), bag.contains(probe));
                int ordinal = random.nextInt(-1, distinct.size() + 1);
                if (ordinal >= 0 && ordinal < distinct.size()) {
                    assertEquals((long) distinct.get(ordinal), bag.elementAt(ordinal));
                    assertEquals(sorted.count(distinct.get(ordinal)), bag.countAt(ordinal));
                } else {
                    final int bad = ordinal;
                    assertThrows(IndexOutOfBoundsException.class, () -> distinct.get(bad));
                    assertThrows(IndexOutOfBoundsException.class, () -> bag.elementAt(bad));
                }
                operations += 3;
            }
            Multiset<Long> asMultiset = guava;
            assertThrows(UnsupportedOperationException.class, () -> asMultiset.add(1L, 2));
            assertThrows(UnsupportedOperationException.class, () -> bag.addOccurrences(1L, 2));
            assertThrows(UnsupportedOperationException.class, () -> asMultiset.remove(1L, 1));
            assertThrows(UnsupportedOperationException.class, () -> bag.removeOccurrences(1L, 1));
            operations += 4;
        }
        assertTrue(operations >= 40_000, "operations " + operations);
    }
}
