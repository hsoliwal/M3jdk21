/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import jdk.m3.collections.packed.DeltaEncodedLongList;

/**
 * Independent Java 21 oracle for the existing delta wire, private query checkpoints
 * and all public read behaviors. No internal checkpoint implementation is inspected.
 */
final class DeltaEncodedLongListCheckpointTest {
    private static int encodedWidth(long value) {
        int bytes = 1;
        while ((value & ~0x7fL) != 0) {
            value >>>= 7;
            bytes++;
        }
        return bytes;
    }

    @Test void everyCheckpointBoundaryPreservesFullSignedDomainAndWireLength() {
        int[] lengths = {0, 1, 2, 3, 63, 64, 127, 128, 129, 130, 255,
                256, 257, 511, 512, 513, 1023, 1024, 2049, 4097};
        Random rng = new Random(0x4d33564c4f4e474cL);
        for (int trial = 0; trial < lengths.length * 3; trial++) {
            int length = lengths[trial % lengths.length];
            DeltaEncodedLongList data = new DeltaEncodedLongList(trial % 2);
            long[] expected = new long[length];
            long value = trial % 3 == 0 ? Long.MIN_VALUE
                    : trial % 3 == 1 ? Long.MAX_VALUE - 65_535L
                    : -123_456_789_012_345L;
            int bytes = length == 0 ? 0 : Long.BYTES;
            for (int i = 0; i < length; i++) {
                if (i != 0) {
                    long step = rng.nextInt(31) == 0 ? 1024L : rng.nextInt(5);
                    value = value > Long.MAX_VALUE - step ? Long.MAX_VALUE : value + step;
                    bytes += encodedWidth(value - expected[i - 1]);
                }
                expected[i] = value;
                data.add(value);
            }
            assertEquals(length, data.size());
            assertEquals(bytes, data.encodedBytes(), "canonical varint encoding");
            assertArrayEquals(expected, data.toArray(), "independent decode");
            long[] visitor = new long[length];
            int[] used = {0};
            data.forEach(v -> visitor[used[0]++] = v);
            assertArrayEquals(expected, visitor, "ascending forEach");
            if (length > 0) {
                assertEquals(expected[0], data.first());
                assertEquals(expected[length - 1], data.last());
            }
            for (int index : IntStream.range(0, length).toArray()) {
                assertEquals(expected[index], data.get(index),
                        "trial=" + trial + " index=" + index);
            }
            assertThrows(IndexOutOfBoundsException.class, () -> data.get(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> data.get(length));
            data.clear();
            assertEquals(0, data.size());
            assertTrue(data.isEmpty());
            data.add(Long.MIN_VALUE);
            data.add(Long.MAX_VALUE);
            assertEquals(Long.MAX_VALUE, data.get(1), "unsigned 64-bit delta");
        }
    }

    @Test void rejectionClearAndForEachKeepOriginalContract() {
        DeltaEncodedLongList data = new DeltaEncodedLongList(0);
        assertDoesNotThrow(() -> data.forEach(null), "empty callback accepted by baseline");
        assertThrows(java.util.NoSuchElementException.class, data::first);
        assertThrows(java.util.NoSuchElementException.class, data::last);
        data.add(8);
        assertThrows(NullPointerException.class, () -> data.forEach(null));
        int encoded = data.encodedBytes();
        assertThrows(IllegalArgumentException.class, () -> data.add(7));
        assertEquals(encoded, data.encodedBytes());
        assertEquals(1, data.size());
        data.clear();
        data.add(Long.MIN_VALUE);
        data.add(Long.MAX_VALUE);
        assertArrayEquals(new long[] {Long.MIN_VALUE, Long.MAX_VALUE},
                data.toArray());
        assertEquals(Long.BYTES + 10, data.encodedBytes());
        assertTrue(data.payloadBytes() >= 10);
        assertArrayEquals(new long[] {Long.MIN_VALUE, Long.MAX_VALUE},
                Arrays.copyOf(data.toArray(), data.size()));
    }
}
