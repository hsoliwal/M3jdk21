/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary One-based indexed-list offsets map to zero-based state bits across word boundaries
 * @modules jdk.m3.collections/jdk.internal.m3.collections:open
 * @run main/othervm IndexedListReservedSlotMaskTest
 */
import java.lang.reflect.Field;
import jdk.internal.m3.collections.IndexedList;

public final class IndexedListReservedSlotMaskTest {
    private static long checks;

    public static void main(String[] arguments) throws Exception {
        for (int length : new int[] {31, 32, 33, 63, 64, 65, 31_616}) {
            exercise(length);
        }
        System.out.println("IndexedListReservedSlotMaskTest PASS " + checks
                + " checks boundaries=31,32,33,63,64,65,31616");
    }

    private static void exercise(int length) throws Exception {
        IndexedList<Integer> list = new IndexedList<>();
        for (int index = 0; index < length; index++) list.add(index);
        check(list.size() == length, "size after fill");
        for (int index = 0; index < length; index++) {
            check(list.get(index) == index, "rank " + index);
        }
        Object[] values = (Object[]) lane(list, "valueLane");
        long[] states = (long[]) lane(list, "stateMaskWords");
        int capacity = values.length - 1;
        check(values[0] == null, "reserved offset zero has no payload");
        // Physical slot 1 maps to bit pair 0; no mask pair is reserved for slot 0.
        check(states.length == (int) ((capacity + 31L) >>> 5), "exact mask capacity");
        // This is a naturally reached geometric capacity, exactly divisible by 32.
        if (length == 31_616) check(capacity == length, "natural exact-word capacity");
        verifyStates(states, length);
        check(list.remove(length - 1) == length - 1, "remove boundary slot");
        check(values[length] == null, "released boundary reference");
        check(state(states, length - 1) == 2, "deleted boundary state");
        list.add(-1);
        check(list.get(length - 1) == -1, "recycled boundary");
        check(state(states, length - 1) == 1, "recycled boundary state");
        check(lane(list, "valueLane") == values, "recycling retains payload lane");
        check(lane(list, "stateMaskWords") == states, "recycling retains mask lane");
        list.clear();
        for (Object value : values) check(value == null, "clear releases every payload");
        for (long word : states) check(word == 0, "clear resets every mask word");
        for (int index = 0; index < length; index++) list.add(index);
        check(list.size() == length && list.get(length - 1) == length - 1,
                "refill boundary");
        verifyStates((long[]) lane(list, "stateMaskWords"), length);
    }

    private static void verifyStates(long[] states, int length) {
        for (int word = 0; word < states.length; word++) {
            int occupied = Math.max(0, Math.min(32, length - (word << 5)));
            long expected = occupied == 32 ? 0x5555555555555555L
                    : 0x5555555555555555L & ((1L << (occupied << 1)) - 1);
            check(states[word] == expected, "occupied and unused bits at word " + word);
        }
    }

    private static int state(long[] states, int zeroBased) {
        return (int) ((states[zeroBased >>> 5] >>> ((zeroBased & 31) << 1)) & 3L);
    }

    private static Object lane(IndexedList<?> list, String name) throws Exception {
        Field field = IndexedList.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(list);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
