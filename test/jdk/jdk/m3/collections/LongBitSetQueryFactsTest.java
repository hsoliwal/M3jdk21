// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import jdk.m3.collections.bits.LongBitSet;

/** Exact state/iteration contract of the M3 bitmap query-fact cache. */
final class LongBitSetQueryFactsTest {
    @Test
    void repeatedQueriesAndMutationsMatchJavaBitSet() {
        Random random = new Random(0xC011EC7L);
        for (int bits : new int[] {0, 1, 63, 64, 65, 127, 128, 129, 8192}) {
            LongBitSet actual = LongBitSet.of(bits);
            BitSet reference = new BitSet(bits);
            for (int i = 0; i < 4096; i++) {
                int index = random.nextInt(Math.max(1, bits + 64));
                switch (i % 5) {
                    case 0 -> { actual.set(index); reference.set(index); }
                    case 1 -> { actual.clear(index); reference.clear(index); }
                    case 2 -> { actual.flip(index); reference.flip(index); }
                    case 3 -> { actual.clear(); reference.clear(); }
                    default -> { /* Read-only query; keep the prior cache valid. */ }
                }
                assertEquals(reference.cardinality(), actual.cardinality());
                assertEquals(reference.cardinality(), actual.cardinality());
                assertEquals(reference.isEmpty(), actual.isEmpty());
                assertEquals(reference.isEmpty(), actual.isEmpty());
                assertEquals(reference.length(), actual.length());
                assertEquals(reference.get(index), actual.get(index));
            }
        }
    }

    @Test
    void bulkOperationsAndSelfAliasingInvalidateCorrectly() {
        LongBitSet actual = LongBitSet.of(1000);
        BitSet expected = new BitSet(1000);
        LongBitSet mask = LongBitSet.of(1000);
        BitSet referenceMask = new BitSet(1000);
        for (int i = 0; i < 1000; i += 3) { actual.set(i); expected.set(i); }
        for (int i = 0; i < 1000; i += 7) { mask.set(i); referenceMask.set(i); }
        actual.cardinality();
        actual.or(mask); expected.or(referenceMask);
        assertEquals(expected.cardinality(), actual.cardinality());
        actual.xor(mask); expected.xor(referenceMask);
        assertEquals(expected.cardinality(), actual.cardinality());
        actual.and(mask); expected.and(referenceMask);
        assertEquals(expected.isEmpty(), actual.isEmpty());
        actual.andNot(mask); expected.andNot(referenceMask);
        assertEquals(expected.cardinality(), actual.cardinality());
        assertTrue(actual.isEmpty());
        actual.set(130); expected.set(130);
        actual.or(actual); expected.or(expected);
        assertEquals(expected.cardinality(), actual.cardinality());
        actual.xor(actual); expected.xor(expected);
        assertEquals(expected.cardinality(), actual.cardinality());
        assertTrue(actual.isEmpty());
        actual.set(63); actual.set(64);
        actual.andNot(actual);
        assertEquals(0, actual.cardinality());
        assertTrue(actual.isEmpty());
    }

    @Test
    void callbackMutationStillObservesLiveFutureBits() {
        LongBitSet bits = LongBitSet.of(256);
        bits.set(1); bits.set(64); bits.set(66);
        List<Integer> observed = new ArrayList<>();
        bits.forEachSetBit(index -> {
            observed.add(index);
            if (index == 1) { bits.clear(64); bits.set(65); }
        });
        assertEquals(List.of(1, 65, 66), observed);
        assertEquals(3, bits.cardinality());
        assertFalse(bits.isEmpty());
        bits.clear();
        assertTrue(bits.isEmpty());
        assertEquals(0, bits.cardinality());
    }
}
