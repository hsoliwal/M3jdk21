/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Java 21 Arrays.asList oracle for an unboxed, fixed-size backing-array view. */
final class M3FixedSizeLongListViewContractTest {
    @Test
    void backingArrayIdentityAndBidirectionalWrites() {
        long[] source = {0, Long.MIN_VALUE, Long.MAX_VALUE, 7, 7, -1};
        M3FixedSizeLongListView view = M3FixedSizeLongListView.of(source);
        M3FixedSizeLongListView alias = M3FixedSizeLongListView.of(source);
        assertEquals(source.length, view.size());
        assertEquals(Long.MIN_VALUE, view.get(1));
        assertEquals(Long.MIN_VALUE, view.set(1, 12));
        assertEquals(12, source[1]);
        assertEquals(12, alias.get(1));
        source[0] = 99;
        assertEquals(99, view.get(0));
        assertEquals(3, view.indexOf(7));
        assertTrue(view.contains(7));
        assertFalse(view.contains(42));
        assertEquals(-1, view.indexOf(42));
        assertArrayEquals(source, view.toArray());
        long[] snapshot = view.toArray();
        assertNotSame(source, snapshot);
        snapshot[0] = 123;
        assertEquals(99, view.get(0));
    }

    @Test
    void randomizedFixedSizeOperationsMatchJdkArraysAsList() {
        Random random = new Random(0x4D335F4649584544L);
        for (int trial = 0; trial < 600; trial++) {
            int length = random.nextInt(130);
            long[] source = new long[length];
            Long[] boxed = new Long[length];
            for (int i = 0; i < length; i++) {
                long value = randomValue(random, i);
                source[i] = value;
                boxed[i] = value;
            }
            List<Long> oracle = Arrays.asList(boxed);
            M3FixedSizeLongListView actual = M3FixedSizeLongListView.of(source);
            for (int operation = 0; operation < 125; operation++) {
                int mode = random.nextInt(5);
                long value = randomValue(random, operation);
                if (length > 0 && mode == 0) {
                    int index = random.nextInt(length);
                    assertEquals(oracle.set(index, value).longValue(), actual.set(index, value));
                } else if (length > 0 && mode == 1) {
                    int index = random.nextInt(length);
                    source[index] = value;
                    oracle.set(index, value);
                    assertEquals(value, actual.get(index));
                } else if (length > 0 && mode == 2) {
                    int index = random.nextInt(length);
                    assertEquals(oracle.get(index).longValue(), actual.get(index));
                } else if (mode == 3) {
                    assertEquals(oracle.indexOf(value), actual.indexOf(value));
                    assertEquals(oracle.contains(value), actual.contains(value));
                } else {
                    assertEquals(oracle.size(), actual.size());
                    assertThrows(UnsupportedOperationException.class, () -> actual.add(value));
                    assertThrows(UnsupportedOperationException.class, () -> oracle.add(value));
                }
                if ((operation & 15) == 0) {
                    for (int i = 0; i < length; i++) {
                        assertEquals(oracle.get(i).longValue(), source[i]);
                    }
                    assertArrayEquals(oracle.stream().mapToLong(Long::longValue).toArray(), actual.toArray());
                }
            }
        }
    }

    @Test
    void invalidInputsAndNoSizeChangesPreserveJdkStyleExceptions() {
        assertThrows(NullPointerException.class, () -> M3FixedSizeLongListView.of(null));
        M3FixedSizeLongListView empty = new M3FixedSizeLongListView();
        assertEquals(0, empty.size());
        assertEquals(-1, empty.indexOf(0));
        assertArrayEquals(new long[0], empty.toArray());
        assertThrows(UnsupportedOperationException.class, () -> empty.add(1));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.set(0, 1));
        long[] source = {3, 2, 1};
        M3FixedSizeLongListView view = M3FixedSizeLongListView.of(source);
        assertThrows(IndexOutOfBoundsException.class, () -> view.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> view.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> view.set(-1, 7));
        assertThrows(IndexOutOfBoundsException.class, () -> view.set(3, 7));
        assertThrows(UnsupportedOperationException.class, () -> view.add(7));
        assertEquals(3, view.size());
        assertArrayEquals(new long[]{3, 2, 1}, source);
    }

    @Test
    void retainsOriginalM3ApiButNoStoredNodeOrStatePerElement() throws Exception {
        assertEquals("M3FixedSizeLongListView|FIXED_SIZE_LONG_LIST|LONG|FIXED_SIZE|long[] valueLane",
                M3FixedSizeLongListView.scaffoldDescriptor());
        long[] source = {1, 2};
        var view = M3FixedSizeLongListView.of(source);
        Field backing = M3FixedSizeLongListView.class.getDeclaredField("valueLane");
        backing.setAccessible(true);
        assertSame(source, backing.get(view));
        assertTrue(Modifier.isFinal(backing.getModifiers()));
        for (Field field : M3FixedSizeLongListView.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            assertEquals(long[].class, field.getType(), "unexpected retained owner " + field.getName());
        }
        assertEquals(0, M3FixedSizeLongListView.class.getInterfaces().length);
    }

    private static long randomValue(Random random, int index) {
        return switch (index % 9) {
            case 0 -> 0;
            case 1 -> Long.MIN_VALUE;
            case 2 -> Long.MAX_VALUE;
            case 3 -> -1;
            case 4 -> 1;
            default -> random.nextInt(25) - 12;
        };
    }
}
