package com.synexia.primitives;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

/** Standalone strict verification for the exact-width primitive layer. */
public final class ExactWidthCollectionsSelfTest {
    private static long checks;

    private ExactWidthCollectionsSelfTest() {}

    public static void main(String[] args) {
        allKinds();
        fullMapMatrix();
        exactLongByteLayout();
        floatEdges();
        deque();
        sortedDifferential();
        gapMapDifferential();
        hashMapDifferential();
        heapAndTopK();
        filters();
        System.out.println("PASS exact-width checks=" + checks);
    }

    private static void allKinds() {
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            int count = kind == PrimitiveKind.BOOLEAN ? 2 : 64;

            PrimitiveArrayList list = new PrimitiveArrayList(kind, 1);
            for (int i = count - 1; i >= 0; i--) list.addBits(sample(kind, i));
            eq(count, list.size());
            ok(Array.getLength(list.toPrimitiveArray()) == count);
            ok(list.backingArray().getClass().getComponentType().isPrimitive());
            list.sort();
            for (int i = 0; i < count; i++) ok(list.binarySearchBits(sample(kind, i)) >= 0);

            PrimitiveHashSet hash = new PrimitiveHashSet(kind, 1, 0.61f);
            for (int i = 0; i < count; i++) ok(hash.addBits(sample(kind, i)));
            for (int i = 0; i < count; i++) ok(hash.containsBits(sample(kind, i)));
            for (int i = 0; i < count; i += 2) ok(hash.removeBits(sample(kind, i)));

            PrimitiveSortedArraySet denseSorted = new PrimitiveSortedArraySet(kind, 1);
            PrimitiveSortedGapSet gapSorted = new PrimitiveSortedGapSet(kind, 1, 0.70f);
            for (int i = count - 1; i >= 0; i--) {
                ok(denseSorted.addBits(sample(kind, i)));
                ok(gapSorted.addBits(sample(kind, i)));
            }
            eq(count, denseSorted.size());
            eq(count, gapSorted.size());
            for (int i = 0; i < count; i++) {
                eqBits(kind, denseSorted.getBits(i), gapSorted.getBits(i));
            }

            PrimitiveMinHeap heap = new PrimitiveMinHeap(kind, 1);
            for (int i = count - 1; i >= 0; i--) heap.addBits(sample(kind, i));
            long previous = heap.pollBits();
            while (!heap.isEmpty()) {
                long next = heap.pollBits();
                ok(kind.compareBits(previous, next) <= 0);
                previous = next;
            }
        }
    }

    private static void fullMapMatrix() {
        for (PrimitiveKind keyKind : PrimitiveKind.values()) {
            int count = keyKind == PrimitiveKind.BOOLEAN ? 2 : 24;
            for (PrimitiveKind valueKind : PrimitiveKind.values()) {
                PrimitivePrimitiveHashMap hash =
                        new PrimitivePrimitiveHashMap(keyKind, valueKind, 1, 0.62f);
                PrimitiveSortedArrayMap sorted =
                        new PrimitiveSortedArrayMap(keyKind, valueKind, 1);
                PrimitiveSortedGapMap gap =
                        new PrimitiveSortedGapMap(keyKind, valueKind, 1, 0.72f);

                for (int i = count - 1; i >= 0; i--) {
                    long key = sample(keyKind, i);
                    long value = sample(valueKind, i + 3);
                    hash.putBits(key, value, 0);
                    sorted.putBits(key, value, 0);
                    gap.putBits(key, value, 0);
                }
                eq(count, hash.size());
                eq(count, sorted.size());
                eq(count, gap.size());
                for (int i = 0; i < count; i++) {
                    long key = sample(keyKind, i);
                    long expected = sample(valueKind, i + 3);
                    eqBits(valueKind, expected, hash.getOrDefaultBits(key, Long.MIN_VALUE));
                    eqBits(valueKind, expected, sorted.getOrDefaultBits(key, Long.MIN_VALUE));
                    eqBits(valueKind, expected, gap.getOrDefaultBits(key, Long.MIN_VALUE));
                }
            }
        }
    }

    private static void exactLongByteLayout() {
        PrimitivePrimitiveHashMap hash =
                new PrimitivePrimitiveHashMap(PrimitiveKind.LONG, PrimitiveKind.BYTE, 16, 0.65f);
        ok(hash.keyArray() instanceof long[]);
        ok(hash.valueArray() instanceof byte[]);
        eq((long) hash.capacity() * (Long.BYTES + Byte.BYTES), hash.payloadBytes());
        for (var field : PrimitivePrimitiveHashMap.class.getDeclaredFields()) {
            ok(!field.getName().equals("occupied"));
            ok(field.getType() != byte[].class);
        }
        hash.putBits(0L, Byte.MIN_VALUE, 1L);
        hash.putBits(31L, Byte.MAX_VALUE, 1L);
        eq(2, hash.size());
        eq(Byte.MIN_VALUE, hash.getOrDefaultBits(0L, 1L));
        eq(Byte.MAX_VALUE, hash.getOrDefaultBits(31L, 1L));
        int nonzeroKeys = 0;
        for (long key : (long[]) hash.keyArray()) {
            if (key != 0L) nonzeroKeys++;
        }
        eq(1, nonzeroKeys);
        eq((long) hash.capacity() * (Long.BYTES + Byte.BYTES), hash.payloadBytes());

        PrimitiveSortedGapMap sorted =
                new PrimitiveSortedGapMap(PrimitiveKind.LONG, PrimitiveKind.BYTE, 16, 0.75f);
        ok(sorted.keyArray() instanceof long[]);
        ok(sorted.valueArray() instanceof byte[]);
    }

    private static void floatEdges() {
        PrimitiveHashSet floats = new PrimitiveHashSet(PrimitiveKind.FLOAT);
        long nanA = Integer.toUnsignedLong(0x7fc00001);
        long nanB = Integer.toUnsignedLong(0x7fc00002);
        ok(floats.addBits(nanA));
        ok(!floats.addBits(nanB));
        ok(floats.addBits(Integer.toUnsignedLong(Float.floatToRawIntBits(-0.0f))));
        ok(floats.addBits(Integer.toUnsignedLong(Float.floatToRawIntBits(+0.0f))));

        PrimitiveHashSet doubles = new PrimitiveHashSet(PrimitiveKind.DOUBLE);
        ok(doubles.addBits(0x7ff8000000000001L));
        ok(!doubles.addBits(0x7ff8000000000002L));
        ok(doubles.addBits(Double.doubleToRawLongBits(-0.0d)));
        ok(doubles.addBits(Double.doubleToRawLongBits(+0.0d)));
    }

    private static void deque() {
        PrimitiveArrayDeque deque = new PrimitiveArrayDeque(PrimitiveKind.BYTE, 2);
        deque.addLastBits((byte) 2);
        deque.addFirstBits((byte) 1);
        deque.addLastBits((byte) 3);
        eq(1, (byte) deque.removeFirstBits());
        eq(3, (byte) deque.removeLastBits());
        eq(2, (byte) deque.peekFirstBits());
        ok(deque.backingArray() instanceof byte[]);
    }

    private static void sortedDifferential() {
        PrimitiveSortedGapSet dense = new PrimitiveSortedGapSet(PrimitiveKind.INT, 1, 0.73f);
        TreeSet<Integer> oracle = new TreeSet<>();
        Random random = new Random(0x51A7EDL);
        for (int iteration = 0; iteration < 5_000; iteration++) {
            int value = random.nextInt(2_000) - 1_000;
            if (random.nextBoolean()) {
                ok(dense.addBits(value) == oracle.add(value));
            } else {
                ok(dense.removeBits(value) == oracle.remove(value));
            }
            eq(oracle.size(), dense.size());
            int index = 0;
            for (int expected : oracle) eq(expected, dense.getBits(index++));
        }
    }

    private static void gapMapDifferential() {
        PrimitiveSortedGapMap dense =
                new PrimitiveSortedGapMap(PrimitiveKind.LONG, PrimitiveKind.BYTE, 1, 0.71f);
        TreeMap<Long, Byte> oracle = new TreeMap<>();
        Random random = new Random(0x6A9L);
        for (int iteration = 0; iteration < 5_000; iteration++) {
            long key = random.nextInt(2_000) - 1_000;
            if (random.nextBoolean()) {
                byte value = (byte) random.nextInt();
                Byte previous = oracle.put(key, value);
                long actual = dense.putBits(key, value, 999L);
                eq(previous == null ? 999L : previous, actual);
            } else {
                Byte previous = oracle.remove(key);
                long actual = dense.removeOrDefaultBits(key, 999L);
                eq(previous == null ? 999L : previous, actual);
            }
            eq(oracle.size(), dense.size());
            for (Map.Entry<Long, Byte> entry : oracle.entrySet()) {
                eq(entry.getValue(), dense.getOrDefaultBits(entry.getKey(), 999L));
            }
        }
    }

    private static void hashMapDifferential() {
        PrimitivePrimitiveHashMap dense =
                new PrimitivePrimitiveHashMap(PrimitiveKind.INT, PrimitiveKind.INT);
        Map<Integer, Integer> oracle = new HashMap<>();
        Random random = new Random(0xA11CE);
        for (int iteration = 0; iteration < 30_000; iteration++) {
            int key = random.nextInt(2_000) - 1_000;
            int operation = random.nextInt(3);
            if (operation == 0) {
                int value = random.nextInt();
                Integer previous = oracle.put(key, value);
                eq(previous == null ? 0L : previous, dense.putBits(key, value, 0L));
            } else if (operation == 1) {
                Integer previous = oracle.remove(key);
                eq(previous == null ? 0L : previous, dense.removeOrDefaultBits(key, 0L));
            } else {
                eq(oracle.getOrDefault(key, 0), dense.getOrDefaultBits(key, 0L));
            }
            eq(oracle.size(), dense.size());
        }
    }

    private static void heapAndTopK() {
        PrimitiveMinHeap heap = new PrimitiveMinHeap(PrimitiveKind.DOUBLE);
        for (double value : new double[] {5, 1, 4, 2, 3}) {
            heap.addBits(Double.doubleToRawLongBits(value));
        }
        for (int expected = 1; expected <= 5; expected++) {
            eq(Double.doubleToRawLongBits(expected), heap.pollBits());
        }

        PrimitiveTopK largest =
                new PrimitiveTopK(PrimitiveKind.INT, 3, PrimitiveTopK.Mode.KEEP_LARGEST);
        for (int value = 0; value < 10; value++) largest.offerBits(value);
        ok(Arrays.equals(new int[] {7, 8, 9}, (int[]) largest.toSortedPrimitiveArray()));

        PrimitiveTopK smallest =
                new PrimitiveTopK(PrimitiveKind.INT, 3, PrimitiveTopK.Mode.KEEP_SMALLEST);
        for (int value = 9; value >= 0; value--) smallest.offerBits(value);
        ok(Arrays.equals(new int[] {0, 1, 2}, (int[]) smallest.toSortedPrimitiveArray()));
    }

    private static void filters() {
        PrimitiveFilteredSet evens =
                new PrimitiveFilteredSet(PrimitiveKind.INT, bits -> (((int) bits) & 1) == 0);
        for (int value = 0; value < 20; value++) evens.addBits(value);
        eq(10, evens.size());

        PrimitiveFilteredList positiveBytes =
                new PrimitiveFilteredList(PrimitiveKind.BYTE, bits -> (byte) bits > 0);
        for (int value = -10; value <= 10; value++) positiveBytes.addBits((byte) value);
        eq(10, positiveBytes.size());
    }

    private static long sample(PrimitiveKind kind, int index) {
        return switch (kind) {
            case BOOLEAN -> index & 1;
            case BYTE -> (byte) (index - 64);
            case SHORT -> (short) (index * 257 - 8_000);
            case CHAR -> (char) (index * 257);
            case INT -> index * 1_000_003 - 50_000_000;
            case LONG -> index * 1_000_000_007L - 50_000_000_000L;
            case FLOAT -> Integer.toUnsignedLong(Float.floatToRawIntBits(index * 1.25f - 40.0f));
            case DOUBLE -> Double.doubleToRawLongBits(index * 1.25d - 40.0d);
        };
    }

    private static void eqBits(PrimitiveKind kind, long expected, long actual) {
        checks++;
        if (!kind.equalBits(expected, actual)) {
            throw new AssertionError(kind + ": " + expected + " != " + actual);
        }
    }

    private static void eq(long expected, long actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }

    private static void ok(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError();
    }
}
