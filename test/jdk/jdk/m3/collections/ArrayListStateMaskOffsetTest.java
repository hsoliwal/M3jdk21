/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Stable primitive list compaction over logical offsets in larger state lanes
 * @modules jdk.m3.collections
 * @run main/othervm ArrayListStateMaskOffsetTest
 */
import java.util.Arrays;
import java.util.Random;
import jdk.m3.collections.IntArrayList;
import jdk.m3.collections.LongArrayList;

public class ArrayListStateMaskOffsetTest {
    private static long checks;
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static void fails(Class<? extends Throwable> type, Runnable task) {
        checks++;
        try { task.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("wrong exception, expected " + type, error);
        }
        throw new AssertionError("missing exception " + type);
    }
    private static void trial(Random random, int size, int stateOffset, int mask) {
        int[] ints = new int[size];
        long[] longs = new long[size];
        byte[] states = new byte[size + stateOffset + 5];
        for (int i = 0; i < states.length; i++) states[i] = (byte) random.nextInt();
        for (int i = 0; i < size; i++) {
            ints[i] = random.nextInt();
            longs[i] = random.nextLong();
        }
        int[] expectedInts = new int[size];
        long[] expectedLongs = new long[size];
        int retained = 0;
        for (int i = 0; i < size; i++) {
            if ((((states[stateOffset + i] & 0xff) & mask) == mask)) {
                expectedInts[retained] = ints[i];
                expectedLongs[retained] = longs[i];
                retained++;
            }
        }
        IntArrayList intList = IntArrayList.copyOf(ints);
        LongArrayList longList = LongArrayList.copyOf(longs);
        check(intList.retainByStateMask(states, stateOffset, mask) == size - retained, "int removed");
        check(longList.retainByStateMask(states, stateOffset, mask) == size - retained, "long removed");
        check(Arrays.equals(Arrays.copyOf(expectedInts, retained), intList.toArray()), "int stable");
        check(Arrays.equals(Arrays.copyOf(expectedLongs, retained), longList.toArray()), "long stable");
    }
    public static void main(String[] args) {
        Random random = new Random(0x5a766172726179L);
        int[] masks = {0, 1, 2, 3, 0x40, 0x80, 0xff};
        for (int length = 0; length <= 192; length++) {
            for (int offset : new int[] {0, 1, 2, 7, 63, 64}) {
                for (int mask : masks) trial(random, length, offset, mask);
            }
        }
        IntArrayList ints = IntArrayList.copyOf(new int[] {2, 4, 6});
        LongArrayList longs = LongArrayList.copyOf(new long[] {2, 4, 6});
        byte[] lane = {9, 1, 3, 0, 9};
        fails(IndexOutOfBoundsException.class, () -> ints.retainByStateMask(lane, 3, 1));
        fails(IndexOutOfBoundsException.class, () -> longs.retainByStateMask(lane, -1, 1));
        fails(IllegalArgumentException.class, () -> ints.retainByStateMask(lane, 1, 0x100));
        fails(IllegalArgumentException.class, () -> longs.retainByStateMask(lane, 1, -1));
        fails(IllegalArgumentException.class, () -> ints.retainByStateMask(lane, 1));
        fails(IllegalArgumentException.class, () -> longs.retainByStateMask(lane, 1));
        check(Arrays.equals(ints.toArray(), new int[] {2,4,6}), "int failed-call atomicity");
        check(Arrays.equals(longs.toArray(), new long[] {2,4,6}), "long failed-call atomicity");
        check(ints.retainByStateMask(lane, 1, 0) == 0, "int mask zero");
        check(longs.retainByStateMask(lane, 1, 0) == 0, "long mask zero");
        check(Arrays.equals(ints.toArray(), new int[] {2,4,6}), "int zero-mask stability");
        check(Arrays.equals(longs.toArray(), new long[] {2,4,6}), "long zero-mask stability");
        System.out.println("ArrayListStateMaskOffsetTest PASS checks=" + checks);
    }
}
