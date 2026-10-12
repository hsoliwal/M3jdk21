/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class IntBufferTest {

    @Test
    void allocatesZeroFilledAndCopiesOnOf() {
        IntBuffer zero = IntBuffer.allocate(5);
        assertEquals(5, zero.length());
        assertArrayEquals(new int[5], zero.toArray());

        int[] source = {1, -2, Integer.MIN_VALUE, Integer.MAX_VALUE};
        IntBuffer buffer = IntBuffer.of(source);
        source[0] = 99;
        assertArrayEquals(new int[] {1, -2, Integer.MIN_VALUE, Integer.MAX_VALUE}, buffer.toArray());
        assertEquals(0, IntBuffer.of().length());
    }

    @Test
    void getsSetsAndCopiesWithLengthChecks() {
        IntBuffer buffer = IntBuffer.allocate(3);
        buffer.set(1, 42);
        assertEquals(42, buffer.get(1));

        int[] target = new int[3];
        buffer.copyTo(target);
        assertArrayEquals(new int[] {0, 42, 0}, target);
        buffer.copyFrom(new int[] {7, 8, 9});
        assertArrayEquals(new int[] {7, 8, 9}, buffer.toArray());

        assertThrows(IllegalArgumentException.class, () -> buffer.copyTo(new int[2]));
        assertThrows(IllegalArgumentException.class, () -> buffer.copyFrom(new int[4]));
        assertThrows(IllegalArgumentException.class, () -> buffer.copyFrom(IntBuffer.allocate(2)));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.set(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> IntBuffer.allocate(-1));
    }

    @Test
    void copiesAreIndependentAndComparedByContent() {
        IntBuffer original = IntBuffer.of(1, 2, 3);
        IntBuffer copy = original.copy();
        assertNotSame(original, copy);
        assertTrue(original.contentEquals(copy));
        assertEquals(-1, original.mismatch(copy));
        assertFalse(original.equals(copy));

        copy.set(2, 4);
        assertFalse(original.contentEquals(copy));
        assertEquals(2, original.mismatch(copy));
        assertEquals(3, original.get(2));
        assertFalse(original.contentEquals(IntBuffer.of(1, 2)));
        assertFalse(original.contentEquals(null));

        IntBuffer target = IntBuffer.allocate(3);
        target.copyFrom(original);
        assertTrue(target.contentEquals(original));
    }
}
