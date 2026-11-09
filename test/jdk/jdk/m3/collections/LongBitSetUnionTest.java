/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import jdk.m3.collections.bits.LongBitSet;
import org.junit.jupiter.api.Test;

/** API_UNION U2: LongBitSet is one interface; the Synexia contract and the M3 queries meet on it. */
class LongBitSetUnionTest {

    /** A minimal foreign implementation: operands are read through wordsCopy(), never through private arrays. */
    private static LongBitSet foreign(int... bits) {
        LongBitSet inner = LongBitSet.of(256);
        IntStream.of(bits).forEach(inner::set);
        return (LongBitSet) java.lang.reflect.Proxy.newProxyInstance(LongBitSet.class.getClassLoader(),
                new Class<?>[] {LongBitSet.class}, (proxy, method, args) -> method.invoke(inner, args));
    }

    @Test
    void synexiaContractMembers() {
        LongBitSet bits = LongBitSet.of();
        bits.set(3, true);
        bits.set(70, true);
        bits.set(3, false);
        assertFalse(bits.get(3));
        assertTrue(bits.get(70));
        assertEquals(1, bits.cardinality());
        assertArrayEquals(bits.toLongArray(), bits.wordsCopy());
        bits.clearAll();
        assertTrue(bits.isEmpty());
        assertTrue(bits.capacityBits() >= 128);
    }

    @Test
    void operandsOfAnotherImplementation() {
        LongBitSet left = LongBitSet.of(64);
        IntStream.of(1, 5, 9, 200).forEach(left::set);
        LongBitSet right = foreign(5, 9, 130);
        assertTrue(left.intersects(right));
        left.and(right);
        assertArrayEquals(new int[] {5, 9}, IntStream.iterate(left.nextSetBit(0), b -> b >= 0, b -> left.nextSetBit(b + 1)).toArray());
        left.or(right);
        assertEquals(3, left.cardinality());
        left.andNot(right);
        assertTrue(left.isEmpty());
        left.xor(right);
        assertTrue(left.get(130));
    }
}
