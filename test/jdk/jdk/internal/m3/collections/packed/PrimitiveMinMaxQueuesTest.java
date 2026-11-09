/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

final class PrimitiveMinMaxQueuesTest {
    @Test void byteDifferentialAgainstSortedMultiset() {
        var heap = PrimitiveMinMaxQueues.bytes(0);
        TreeMap<Byte, Integer> oracle = new TreeMap<>();
        byte[] edges = new byte[] {Byte.MIN_VALUE, -1, 0, 0, 1, Byte.MAX_VALUE};
        long state = 0x12345678abcdefL;
        int count = 0;
        for (int step = 0; step < 8000; step++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int operation = (int) (state >>> 32) & 3;
            if (count == 0 || (operation < 2 && count < 128)) {
                byte value = step % 5 == 0 ? edges[step % edges.length] : (byte) state;
                heap.add(value);
                oracle.merge(value, 1, Integer::sum);
                count++;
            } else {
                byte expected = operation == 2 ? oracle.firstKey() : oracle.lastKey();
                byte actual = operation == 2 ? heap.removeMin() : heap.removeMax();
                assertEquals(expected, actual);
                drop(oracle, expected);
                count--;
            }
            assertEquals(count, heap.size());
            assertEquals(count == 0, heap.isEmpty());
            if (count != 0) {
                assertEquals(oracle.firstKey().byteValue(), heap.min());
                assertEquals(oracle.lastKey().byteValue(), heap.max());
            }
            if ((step & 63) == 0) {
                byte[] expected = new byte[count];
                int index = 0;
                for (var entry : oracle.entrySet()) {
                    for (int n = 0; n < entry.getValue(); n++) expected[index++] = entry.getKey();
                }
                assertArrayEquals(expected, heap.toSortedArray());
            }
        }
    }

    @Test void byteBulkOwnershipCapacityStreamsAndFailureContracts() {
        byte[] values = new byte[] {Byte.MIN_VALUE, -1, 0, 0, 1, Byte.MAX_VALUE};
        byte[] original = values.clone();
        var heap = PrimitiveMinMaxQueues.bytes(values);
        values[0] = (byte) 0;
        byte[] expected = original.clone();
        Arrays.sort(expected);
        assertArrayEquals(expected, heap.toSortedArray());
        heap.addAll(original, 1, original.length - 1);
        byte[] doubled = Arrays.copyOf(original, 2 * original.length - 1);
        System.arraycopy(original, 1, doubled, original.length, original.length - 1);
        Arrays.sort(doubled);
        assertArrayEquals(doubled, heap.toSortedArray());
        byte[] before = heap.toArray();
        assertThrows(NullPointerException.class, () -> heap.addAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, 1, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> heap.ensureCapacity(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedByteMinMaxHeap(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedByteMinMaxHeap(Integer.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> new PackedByteMinMaxHeap((byte[]) null));
        assertArrayEquals(before, heap.toArray());
        heap.addAll(original, original.length, 0);
        assertArrayEquals(before, heap.toArray());
        heap.ensureCapacity(heap.size() + 100);
        int reserved = heap.capacity();
        var snapshot = heap.snapshotStream();
        heap.clear();
        assertEquals(reserved, heap.capacity());
        int[] streamed = new int[doubled.length];
        for (int i = 0; i < doubled.length; i++) streamed[i] = doubled[i];
        assertArrayEquals(streamed, snapshot.sorted().toArray());
        assertThrows(NoSuchElementException.class, heap::min);
        assertThrows(NoSuchElementException.class, heap::max);
        assertThrows(NoSuchElementException.class, heap::removeMin);
        assertThrows(NoSuchElementException.class, heap::removeMax);
        assertEquals((byte) 7, heap.minOr((byte) 7));
        assertEquals((byte) 7, heap.maxOr((byte) 7));
        heap.trimToSize();
        assertEquals(0, heap.capacity());
        heap.add((byte) 2);
        heap.add((byte) 1);
        heap.trimToSize();
        assertEquals(2, heap.capacity());
        assertEquals((byte) 2, heap.removeMax());
        assertEquals((byte) 1, heap.removeMin());
        assertTrue(heap.isEmpty());
    }
    @Test void shortDifferentialAgainstSortedMultiset() {
        var heap = PrimitiveMinMaxQueues.shorts(0);
        TreeMap<Short, Integer> oracle = new TreeMap<>();
        short[] edges = new short[] {Short.MIN_VALUE, -1, 0, 0, 1, Short.MAX_VALUE};
        long state = 0x12345678abcdefL;
        int count = 0;
        for (int step = 0; step < 8000; step++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int operation = (int) (state >>> 32) & 3;
            if (count == 0 || (operation < 2 && count < 128)) {
                short value = step % 5 == 0 ? edges[step % edges.length] : (short) state;
                heap.add(value);
                oracle.merge(value, 1, Integer::sum);
                count++;
            } else {
                short expected = operation == 2 ? oracle.firstKey() : oracle.lastKey();
                short actual = operation == 2 ? heap.removeMin() : heap.removeMax();
                assertEquals(expected, actual);
                drop(oracle, expected);
                count--;
            }
            assertEquals(count, heap.size());
            assertEquals(count == 0, heap.isEmpty());
            if (count != 0) {
                assertEquals(oracle.firstKey().shortValue(), heap.min());
                assertEquals(oracle.lastKey().shortValue(), heap.max());
            }
            if ((step & 63) == 0) {
                short[] expected = new short[count];
                int index = 0;
                for (var entry : oracle.entrySet()) {
                    for (int n = 0; n < entry.getValue(); n++) expected[index++] = entry.getKey();
                }
                assertArrayEquals(expected, heap.toSortedArray());
            }
        }
    }

    @Test void shortBulkOwnershipCapacityStreamsAndFailureContracts() {
        short[] values = new short[] {Short.MIN_VALUE, -1, 0, 0, 1, Short.MAX_VALUE};
        short[] original = values.clone();
        var heap = PrimitiveMinMaxQueues.shorts(values);
        values[0] = (short) 0;
        short[] expected = original.clone();
        Arrays.sort(expected);
        assertArrayEquals(expected, heap.toSortedArray());
        heap.addAll(original, 1, original.length - 1);
        short[] doubled = Arrays.copyOf(original, 2 * original.length - 1);
        System.arraycopy(original, 1, doubled, original.length, original.length - 1);
        Arrays.sort(doubled);
        assertArrayEquals(doubled, heap.toSortedArray());
        short[] before = heap.toArray();
        assertThrows(NullPointerException.class, () -> heap.addAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, 1, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> heap.ensureCapacity(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedShortMinMaxHeap(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedShortMinMaxHeap(Integer.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> new PackedShortMinMaxHeap((short[]) null));
        assertArrayEquals(before, heap.toArray());
        heap.addAll(original, original.length, 0);
        assertArrayEquals(before, heap.toArray());
        heap.ensureCapacity(heap.size() + 100);
        int reserved = heap.capacity();
        var snapshot = heap.snapshotStream();
        heap.clear();
        assertEquals(reserved, heap.capacity());
        int[] streamed = new int[doubled.length];
        for (int i = 0; i < doubled.length; i++) streamed[i] = doubled[i];
        assertArrayEquals(streamed, snapshot.sorted().toArray());
        assertThrows(NoSuchElementException.class, heap::min);
        assertThrows(NoSuchElementException.class, heap::max);
        assertThrows(NoSuchElementException.class, heap::removeMin);
        assertThrows(NoSuchElementException.class, heap::removeMax);
        assertEquals((short) 7, heap.minOr((short) 7));
        assertEquals((short) 7, heap.maxOr((short) 7));
        heap.trimToSize();
        assertEquals(0, heap.capacity());
        heap.add((short) 2);
        heap.add((short) 1);
        heap.trimToSize();
        assertEquals(2, heap.capacity());
        assertEquals((short) 2, heap.removeMax());
        assertEquals((short) 1, heap.removeMin());
        assertTrue(heap.isEmpty());
    }
    @Test void charDifferentialAgainstSortedMultiset() {
        var heap = PrimitiveMinMaxQueues.chars(0);
        TreeMap<Character, Integer> oracle = new TreeMap<>();
        char[] edges = new char[] {0, 1, 'A', 'A', '\uD800', '\uDC00', Character.MAX_VALUE};
        long state = 0x12345678abcdefL;
        int count = 0;
        for (int step = 0; step < 8000; step++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int operation = (int) (state >>> 32) & 3;
            if (count == 0 || (operation < 2 && count < 128)) {
                char value = step % 5 == 0 ? edges[step % edges.length] : (char) state;
                heap.add(value);
                oracle.merge(value, 1, Integer::sum);
                count++;
            } else {
                char expected = operation == 2 ? oracle.firstKey() : oracle.lastKey();
                char actual = operation == 2 ? heap.removeMin() : heap.removeMax();
                assertEquals(expected, actual);
                drop(oracle, expected);
                count--;
            }
            assertEquals(count, heap.size());
            assertEquals(count == 0, heap.isEmpty());
            if (count != 0) {
                assertEquals(oracle.firstKey().charValue(), heap.min());
                assertEquals(oracle.lastKey().charValue(), heap.max());
            }
            if ((step & 63) == 0) {
                char[] expected = new char[count];
                int index = 0;
                for (var entry : oracle.entrySet()) {
                    for (int n = 0; n < entry.getValue(); n++) expected[index++] = entry.getKey();
                }
                assertArrayEquals(expected, heap.toSortedArray());
            }
        }
    }

    @Test void charBulkOwnershipCapacityStreamsAndFailureContracts() {
        char[] values = new char[] {0, 1, 'A', 'A', '\uD800', '\uDC00', Character.MAX_VALUE};
        char[] original = values.clone();
        var heap = PrimitiveMinMaxQueues.chars(values);
        values[0] = (char) 0;
        char[] expected = original.clone();
        Arrays.sort(expected);
        assertArrayEquals(expected, heap.toSortedArray());
        heap.addAll(original, 1, original.length - 1);
        char[] doubled = Arrays.copyOf(original, 2 * original.length - 1);
        System.arraycopy(original, 1, doubled, original.length, original.length - 1);
        Arrays.sort(doubled);
        assertArrayEquals(doubled, heap.toSortedArray());
        char[] before = heap.toArray();
        assertThrows(NullPointerException.class, () -> heap.addAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, 1, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> heap.ensureCapacity(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedCharMinMaxHeap(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedCharMinMaxHeap(Integer.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> new PackedCharMinMaxHeap((char[]) null));
        assertArrayEquals(before, heap.toArray());
        heap.addAll(original, original.length, 0);
        assertArrayEquals(before, heap.toArray());
        heap.ensureCapacity(heap.size() + 100);
        int reserved = heap.capacity();
        var snapshot = heap.snapshotStream();
        heap.clear();
        assertEquals(reserved, heap.capacity());
        int[] streamed = new int[doubled.length];
        for (int i = 0; i < doubled.length; i++) streamed[i] = doubled[i];
        assertArrayEquals(streamed, snapshot.sorted().toArray());
        assertThrows(NoSuchElementException.class, heap::min);
        assertThrows(NoSuchElementException.class, heap::max);
        assertThrows(NoSuchElementException.class, heap::removeMin);
        assertThrows(NoSuchElementException.class, heap::removeMax);
        assertEquals((char) 7, heap.minOr((char) 7));
        assertEquals((char) 7, heap.maxOr((char) 7));
        heap.trimToSize();
        assertEquals(0, heap.capacity());
        heap.add((char) 2);
        heap.add((char) 1);
        heap.trimToSize();
        assertEquals(2, heap.capacity());
        assertEquals((char) 2, heap.removeMax());
        assertEquals((char) 1, heap.removeMin());
        assertTrue(heap.isEmpty());
    }
    @Test void intDifferentialAgainstSortedMultiset() {
        var heap = PrimitiveMinMaxQueues.ints(0);
        TreeMap<Integer, Integer> oracle = new TreeMap<>();
        int[] edges = new int[] {Integer.MIN_VALUE, -1, 0, 0, 1, Integer.MAX_VALUE};
        long state = 0x12345678abcdefL;
        int count = 0;
        for (int step = 0; step < 8000; step++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int operation = (int) (state >>> 32) & 3;
            if (count == 0 || (operation < 2 && count < 128)) {
                int value = step % 5 == 0 ? edges[step % edges.length] : (int) state;
                heap.add(value);
                oracle.merge(value, 1, Integer::sum);
                count++;
            } else {
                int expected = operation == 2 ? oracle.firstKey() : oracle.lastKey();
                int actual = operation == 2 ? heap.removeMin() : heap.removeMax();
                assertEquals(expected, actual);
                drop(oracle, expected);
                count--;
            }
            assertEquals(count, heap.size());
            assertEquals(count == 0, heap.isEmpty());
            if (count != 0) {
                assertEquals(oracle.firstKey().intValue(), heap.min());
                assertEquals(oracle.lastKey().intValue(), heap.max());
            }
            if ((step & 63) == 0) {
                int[] expected = new int[count];
                int index = 0;
                for (var entry : oracle.entrySet()) {
                    for (int n = 0; n < entry.getValue(); n++) expected[index++] = entry.getKey();
                }
                assertArrayEquals(expected, heap.toSortedArray());
            }
        }
    }

    @Test void intBulkOwnershipCapacityStreamsAndFailureContracts() {
        int[] values = new int[] {Integer.MIN_VALUE, -1, 0, 0, 1, Integer.MAX_VALUE};
        int[] original = values.clone();
        var heap = PrimitiveMinMaxQueues.ints(values);
        values[0] = 0;
        int[] expected = original.clone();
        Arrays.sort(expected);
        assertArrayEquals(expected, heap.toSortedArray());
        heap.addAll(original, 1, original.length - 1);
        int[] doubled = Arrays.copyOf(original, 2 * original.length - 1);
        System.arraycopy(original, 1, doubled, original.length, original.length - 1);
        Arrays.sort(doubled);
        assertArrayEquals(doubled, heap.toSortedArray());
        int[] before = heap.toArray();
        assertThrows(NullPointerException.class, () -> heap.addAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, 1, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> heap.ensureCapacity(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedIntMinMaxHeap(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedIntMinMaxHeap(Integer.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> new PackedIntMinMaxHeap((int[]) null));
        assertArrayEquals(before, heap.toArray());
        heap.addAll(original, original.length, 0);
        assertArrayEquals(before, heap.toArray());
        heap.ensureCapacity(heap.size() + 100);
        int reserved = heap.capacity();
        var snapshot = heap.snapshotStream();
        heap.clear();
        assertEquals(reserved, heap.capacity());
        assertArrayEquals(doubled, snapshot.sorted().toArray());
        assertThrows(NoSuchElementException.class, heap::min);
        assertThrows(NoSuchElementException.class, heap::max);
        assertThrows(NoSuchElementException.class, heap::removeMin);
        assertThrows(NoSuchElementException.class, heap::removeMax);
        assertEquals(7, heap.minOr(7));
        assertEquals(7, heap.maxOr(7));
        heap.trimToSize();
        assertEquals(0, heap.capacity());
        heap.add(2);
        heap.add(1);
        heap.trimToSize();
        assertEquals(2, heap.capacity());
        assertEquals(2, heap.removeMax());
        assertEquals(1, heap.removeMin());
        assertTrue(heap.isEmpty());
    }
    @Test void longDifferentialAgainstSortedMultiset() {
        var heap = PrimitiveMinMaxQueues.longs(0);
        TreeMap<Long, Integer> oracle = new TreeMap<>();
        long[] edges = new long[] {Long.MIN_VALUE, -1, 0, 0, 1, Long.MAX_VALUE};
        long state = 0x12345678abcdefL;
        int count = 0;
        for (int step = 0; step < 8000; step++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int operation = (int) (state >>> 32) & 3;
            if (count == 0 || (operation < 2 && count < 128)) {
                long value = step % 5 == 0 ? edges[step % edges.length] : state;
                heap.add(value);
                oracle.merge(value, 1, Integer::sum);
                count++;
            } else {
                long expected = operation == 2 ? oracle.firstKey() : oracle.lastKey();
                long actual = operation == 2 ? heap.removeMin() : heap.removeMax();
                assertEquals(expected, actual);
                drop(oracle, expected);
                count--;
            }
            assertEquals(count, heap.size());
            assertEquals(count == 0, heap.isEmpty());
            if (count != 0) {
                assertEquals(oracle.firstKey().longValue(), heap.min());
                assertEquals(oracle.lastKey().longValue(), heap.max());
            }
            if ((step & 63) == 0) {
                long[] expected = new long[count];
                int index = 0;
                for (var entry : oracle.entrySet()) {
                    for (int n = 0; n < entry.getValue(); n++) expected[index++] = entry.getKey();
                }
                assertArrayEquals(expected, heap.toSortedArray());
            }
        }
    }

    @Test void longBulkOwnershipCapacityStreamsAndFailureContracts() {
        long[] values = new long[] {Long.MIN_VALUE, -1, 0, 0, 1, Long.MAX_VALUE};
        long[] original = values.clone();
        var heap = PrimitiveMinMaxQueues.longs(values);
        values[0] = (long) 0;
        long[] expected = original.clone();
        Arrays.sort(expected);
        assertArrayEquals(expected, heap.toSortedArray());
        heap.addAll(original, 1, original.length - 1);
        long[] doubled = Arrays.copyOf(original, 2 * original.length - 1);
        System.arraycopy(original, 1, doubled, original.length, original.length - 1);
        Arrays.sort(doubled);
        assertArrayEquals(doubled, heap.toSortedArray());
        long[] before = heap.toArray();
        assertThrows(NullPointerException.class, () -> heap.addAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, 1, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> heap.ensureCapacity(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedLongMinMaxHeap(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedLongMinMaxHeap(Integer.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> new PackedLongMinMaxHeap((long[]) null));
        assertArrayEquals(before, heap.toArray());
        heap.addAll(original, original.length, 0);
        assertArrayEquals(before, heap.toArray());
        heap.ensureCapacity(heap.size() + 100);
        int reserved = heap.capacity();
        var snapshot = heap.snapshotStream();
        heap.clear();
        assertEquals(reserved, heap.capacity());
        assertArrayEquals(doubled, snapshot.sorted().toArray());
        assertThrows(NoSuchElementException.class, heap::min);
        assertThrows(NoSuchElementException.class, heap::max);
        assertThrows(NoSuchElementException.class, heap::removeMin);
        assertThrows(NoSuchElementException.class, heap::removeMax);
        assertEquals((long) 7, heap.minOr((long) 7));
        assertEquals((long) 7, heap.maxOr((long) 7));
        heap.trimToSize();
        assertEquals(0, heap.capacity());
        heap.add((long) 2);
        heap.add((long) 1);
        heap.trimToSize();
        assertEquals(2, heap.capacity());
        assertEquals((long) 2, heap.removeMax());
        assertEquals((long) 1, heap.removeMin());
        assertTrue(heap.isEmpty());
    }
    @Test void floatDifferentialAgainstSortedMultiset() {
        var heap = PrimitiveMinMaxQueues.floats(0);
        TreeMap<Float, Integer> oracle = new TreeMap<>();
        float[] edges = new float[] {Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, -0.0f, 0.0f, Float.MIN_VALUE, Float.POSITIVE_INFINITY, Float.NaN, Float.intBitsToFloat(0x7fc01234)};
        long state = 0x12345678abcdefL;
        int count = 0;
        for (int step = 0; step < 8000; step++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int operation = (int) (state >>> 32) & 3;
            if (count == 0 || (operation < 2 && count < 128)) {
                float value = step % 5 == 0 ? edges[step % edges.length] : Float.intBitsToFloat((int) state);
                heap.add(value);
                oracle.merge(value, 1, Integer::sum);
                count++;
            } else {
                float expected = operation == 2 ? oracle.firstKey() : oracle.lastKey();
                float actual = operation == 2 ? heap.removeMin() : heap.removeMax();
                assertEquals(expected, actual);
                drop(oracle, expected);
                count--;
            }
            assertEquals(count, heap.size());
            assertEquals(count == 0, heap.isEmpty());
            if (count != 0) {
                assertEquals(oracle.firstKey().floatValue(), heap.min());
                assertEquals(oracle.lastKey().floatValue(), heap.max());
            }
            if ((step & 63) == 0) {
                float[] expected = new float[count];
                int index = 0;
                for (var entry : oracle.entrySet()) {
                    for (int n = 0; n < entry.getValue(); n++) expected[index++] = entry.getKey();
                }
                assertArrayEquals(expected, heap.toSortedArray());
            }
        }
    }

    @Test void floatBulkOwnershipCapacityStreamsAndFailureContracts() {
        float[] values = new float[] {Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, -0.0f, 0.0f, Float.MIN_VALUE, Float.POSITIVE_INFINITY, Float.NaN, Float.intBitsToFloat(0x7fc01234)};
        float[] original = values.clone();
        var heap = PrimitiveMinMaxQueues.floats(values);
        values[0] = (float) 0;
        float[] expected = original.clone();
        Arrays.sort(expected);
        assertArrayEquals(expected, heap.toSortedArray());
        heap.addAll(original, 1, original.length - 1);
        float[] doubled = Arrays.copyOf(original, 2 * original.length - 1);
        System.arraycopy(original, 1, doubled, original.length, original.length - 1);
        Arrays.sort(doubled);
        assertArrayEquals(doubled, heap.toSortedArray());
        float[] before = heap.toArray();
        assertThrows(NullPointerException.class, () -> heap.addAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, 1, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> heap.ensureCapacity(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedFloatMinMaxHeap(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedFloatMinMaxHeap(Integer.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> new PackedFloatMinMaxHeap((float[]) null));
        assertArrayEquals(before, heap.toArray());
        heap.addAll(original, original.length, 0);
        assertArrayEquals(before, heap.toArray());
        heap.ensureCapacity(heap.size() + 100);
        int reserved = heap.capacity();
        var snapshot = heap.snapshotStream();
        heap.clear();
        assertEquals(reserved, heap.capacity());
        double[] streamed = new double[doubled.length];
        for (int i = 0; i < doubled.length; i++) streamed[i] = doubled[i];
        assertArrayEquals(streamed, snapshot.sorted().toArray());
        assertThrows(NoSuchElementException.class, heap::min);
        assertThrows(NoSuchElementException.class, heap::max);
        assertThrows(NoSuchElementException.class, heap::removeMin);
        assertThrows(NoSuchElementException.class, heap::removeMax);
        assertEquals((float) 7, heap.minOr((float) 7));
        assertEquals((float) 7, heap.maxOr((float) 7));
        heap.trimToSize();
        assertEquals(0, heap.capacity());
        heap.add((float) 2);
        heap.add((float) 1);
        heap.trimToSize();
        assertEquals(2, heap.capacity());
        assertEquals((float) 2, heap.removeMax());
        assertEquals((float) 1, heap.removeMin());
        assertTrue(heap.isEmpty());
    }
    @Test void doubleDifferentialAgainstSortedMultiset() {
        var heap = PrimitiveMinMaxQueues.doubles(0);
        TreeMap<Double, Integer> oracle = new TreeMap<>();
        double[] edges = new double[] {Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -0.0, 0.0, Double.MIN_VALUE, Double.POSITIVE_INFINITY, Double.NaN, Double.longBitsToDouble(0x7ff8000000001234L)};
        long state = 0x12345678abcdefL;
        int count = 0;
        for (int step = 0; step < 8000; step++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int operation = (int) (state >>> 32) & 3;
            if (count == 0 || (operation < 2 && count < 128)) {
                double value = step % 5 == 0 ? edges[step % edges.length] : Double.longBitsToDouble(state);
                heap.add(value);
                oracle.merge(value, 1, Integer::sum);
                count++;
            } else {
                double expected = operation == 2 ? oracle.firstKey() : oracle.lastKey();
                double actual = operation == 2 ? heap.removeMin() : heap.removeMax();
                assertEquals(expected, actual);
                drop(oracle, expected);
                count--;
            }
            assertEquals(count, heap.size());
            assertEquals(count == 0, heap.isEmpty());
            if (count != 0) {
                assertEquals(oracle.firstKey().doubleValue(), heap.min());
                assertEquals(oracle.lastKey().doubleValue(), heap.max());
            }
            if ((step & 63) == 0) {
                double[] expected = new double[count];
                int index = 0;
                for (var entry : oracle.entrySet()) {
                    for (int n = 0; n < entry.getValue(); n++) expected[index++] = entry.getKey();
                }
                assertArrayEquals(expected, heap.toSortedArray());
            }
        }
    }

    @Test void doubleBulkOwnershipCapacityStreamsAndFailureContracts() {
        double[] values = new double[] {Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -0.0, 0.0, Double.MIN_VALUE, Double.POSITIVE_INFINITY, Double.NaN, Double.longBitsToDouble(0x7ff8000000001234L)};
        double[] original = values.clone();
        var heap = PrimitiveMinMaxQueues.doubles(values);
        values[0] = (double) 0;
        double[] expected = original.clone();
        Arrays.sort(expected);
        assertArrayEquals(expected, heap.toSortedArray());
        heap.addAll(original, 1, original.length - 1);
        double[] doubled = Arrays.copyOf(original, 2 * original.length - 1);
        System.arraycopy(original, 1, doubled, original.length, original.length - 1);
        Arrays.sort(doubled);
        assertArrayEquals(doubled, heap.toSortedArray());
        double[] before = heap.toArray();
        assertThrows(NullPointerException.class, () -> heap.addAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.addAll(original, 1, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> heap.ensureCapacity(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedDoubleMinMaxHeap(-1));
        assertThrows(IllegalArgumentException.class, () -> new PackedDoubleMinMaxHeap(Integer.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> new PackedDoubleMinMaxHeap((double[]) null));
        assertArrayEquals(before, heap.toArray());
        heap.addAll(original, original.length, 0);
        assertArrayEquals(before, heap.toArray());
        heap.ensureCapacity(heap.size() + 100);
        int reserved = heap.capacity();
        var snapshot = heap.snapshotStream();
        heap.clear();
        assertEquals(reserved, heap.capacity());
        assertArrayEquals(doubled, snapshot.sorted().toArray());
        assertThrows(NoSuchElementException.class, heap::min);
        assertThrows(NoSuchElementException.class, heap::max);
        assertThrows(NoSuchElementException.class, heap::removeMin);
        assertThrows(NoSuchElementException.class, heap::removeMax);
        assertEquals((double) 7, heap.minOr((double) 7));
        assertEquals((double) 7, heap.maxOr((double) 7));
        heap.trimToSize();
        assertEquals(0, heap.capacity());
        heap.add((double) 2);
        heap.add((double) 1);
        heap.trimToSize();
        assertEquals(2, heap.capacity());
        assertEquals((double) 2, heap.removeMax());
        assertEquals((double) 1, heap.removeMin());
        assertTrue(heap.isEmpty());
    }
    @Test void booleanCountsRetainDuplicatesAndValidateBeforeMutation() {
        boolean[] source = {true, false, false, true, false};
        var queue = PrimitiveMinMaxQueues.booleans(source);
        Arrays.fill(source, true);
        assertArrayEquals(new boolean[] {false, false, false, true, true}, queue.toArray());
        assertFalse(queue.min()); assertTrue(queue.max());
        assertTrue(queue.removeMax()); assertFalse(queue.removeMin());
        queue.addAll(new boolean[] {true, true, false}, 1, 2);
        boolean[] before = queue.toArray();
        assertThrows(NullPointerException.class, () -> queue.addAll(null));
        assertThrows(IndexOutOfBoundsException.class, () -> queue.addAll(source, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> queue.addAll(source, 0, Integer.MAX_VALUE));
        assertArrayEquals(before, queue.toArray());
        queue.clear();
        assertTrue(queue.isEmpty());
        assertTrue(queue.minOr(true)); assertFalse(queue.maxOr(false));
        assertThrows(NoSuchElementException.class, queue::min);
        assertThrows(NoSuchElementException.class, queue::max);
        assertThrows(NoSuchElementException.class, queue::removeMin);
        assertThrows(NoSuchElementException.class, queue::removeMax);
    }

    @Test void everySmallBooleanMultisetSupportsMixedEndRemovals() {
        for (int falseCount = 0; falseCount <= 12; falseCount++) {
            for (int trueCount = 0; trueCount <= 12; trueCount++) {
                var queue = PrimitiveMinMaxQueues.booleans();
                for (int i = 0; i < falseCount; i++) queue.add(false);
                for (int i = 0; i < trueCount; i++) queue.add(true);
                int f = falseCount, t = trueCount, step = 0;
                while (f + t != 0) {
                    assertEquals(f + t, queue.size());
                    boolean minimum = (step++ & 1) == 0;
                    boolean expected = minimum ? f == 0 : t != 0;
                    assertEquals(expected, minimum ? queue.removeMin() : queue.removeMax());
                    if (expected) t--; else f--;
                }
                assertTrue(queue.isEmpty());
            }
        }
    }

    @Test void exhaustiveSmallIntegerInputsAndAllEndRemovalSchedules() {
        int sequences = 1;
        for (int length = 0; length <= 6; length++, sequences *= 3) {
            for (int code = 0; code < sequences; code++) {
                int[] source = new int[length];
                int digits = code;
                for (int i = 0; i < length; i++, digits /= 3) source[i] = digits % 3 - 1;
                int[] sorted = source.clone(); Arrays.sort(sorted);
                for (int schedule = 0; schedule < (1 << length); schedule++) {
                    var heap = PrimitiveMinMaxQueues.ints(0);
                    if ((schedule & 1) == 0) heap.addAll(source);
                    else for (int value : source) heap.add(value);
                    int low = 0, high = length - 1;
                    for (int step = 0; step < length; step++) {
                        assertEquals(sorted[low], heap.min());
                        assertEquals(sorted[high], heap.max());
                        if ((schedule & (1 << step)) == 0) assertEquals(sorted[low++], heap.removeMin());
                        else assertEquals(sorted[high--], heap.removeMax());
                    }
                    assertTrue(heap.isEmpty());
                }
            }
        }
    }

    @Test void retainedLayoutsHaveOnlyPrimitiveLaneAndSizeOrTwoCounters() {
        Class<?>[] owners = {PackedByteMinMaxHeap.class, PackedShortMinMaxHeap.class,
                PackedCharMinMaxHeap.class, PackedIntMinMaxHeap.class, PackedLongMinMaxHeap.class,
                PackedFloatMinMaxHeap.class, PackedDoubleMinMaxHeap.class};
        Class<?>[] components = {byte.class, short.class, char.class, int.class,
                long.class, float.class, double.class};
        for (int i = 0; i < owners.length; i++) {
            var fields = Arrays.stream(owners[i].getDeclaredFields())
                    .filter(f -> !Modifier.isStatic(f.getModifiers())).toList();
            assertEquals(2, fields.size());
            assertEquals(1, fields.stream().filter(f -> f.getType() == int.class).count());
            Class<?> component = components[i];
            assertEquals(1, fields.stream().filter(f -> f.getType().isArray()
                    && f.getType().getComponentType() == component).count());
        }
        var counters = Arrays.stream(PackedBooleanMinMaxQueue.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers())).toList();
        assertEquals(2, counters.size());
        assertTrue(counters.stream().allMatch(f -> f.getType() == int.class));
        assertTrue(PackedMinMaxHeapAtoms.minLevel(0));
        assertFalse(PackedMinMaxHeapAtoms.minLevel(1));
        assertTrue(PackedMinMaxHeapAtoms.minLevel(3));
        assertEquals(PackedHeapAtoms.parent(PackedHeapAtoms.parent(Integer.MAX_VALUE - 9)),
                PackedMinMaxHeapAtoms.grandparent(Integer.MAX_VALUE - 9));
    }
    private static <T> void drop(TreeMap<T, Integer> values, T value) {
        int count = values.get(value);
        if (count == 1) values.remove(value); else values.put(value, count - 1);
    }
}
