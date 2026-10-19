// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.ConcurrentModificationException;

/** JDK21 contract oracle for zero-mask no-op and state/offset compaction. */
public final class M3ZeroMaskArrayFastPathProof {
    private static long checks;
    private M3ZeroMaskArrayFastPathProof() { }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void equalsLong(long[] expected, long[] actual, String reason) {
        checks++;
        if (!Arrays.equals(expected, actual)) throw new AssertionError(reason);
    }
    private static void equalsInt(int[] expected, int[] actual, String reason) {
        checks++;
        if (!Arrays.equals(expected, actual)) throw new AssertionError(reason);
    }
    private static void equalsBytes(byte[] expected, byte[] actual, String reason) {
        checks++;
        if (!Arrays.equals(expected, actual)) throw new AssertionError(reason);
    }
    private static void refuses(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError("wrong exception", actual);
        }
        throw new AssertionError("missing expected " + type.getSimpleName());
    }

    private static void exactAllPassAndFailFast() {
        long[] values = {Long.MIN_VALUE, 0, -7, 42, Long.MAX_VALUE, -7};
        byte[] states = {(byte) 0xff, 0, (byte) 0x80, 1, (byte) 0x55, -1};
        M3LongArrayList list = M3LongArrayList.copyOf(values);
        M3LongIterator before = list.iterator();
        check(list.retainByStateMask(states, 0) == 0, "zero-mask removes nothing");
        equalsLong(values, list.toArray(), "zero-mask preserves element occurrences");
        check(before.hasNext() && before.nextLong() == Long.MIN_VALUE,
                "no-op cannot invalidate already-open iterator");
        list.add(123);
        refuses(ConcurrentModificationException.class, before::hasNext);
        check(list.retainByStateMask(new byte[values.length + 1], 0) == 0,
                "valid aligned zero mask remains no-op");
        refuses(IllegalArgumentException.class, () -> list.retainByStateMask(states, 0));
        refuses(IllegalArgumentException.class,
                () -> list.retainByStateMask(new byte[list.size()], -1));
        refuses(IllegalArgumentException.class,
                () -> list.retainByStateMask(new byte[list.size()], 256));
        refuses(NullPointerException.class, () -> list.retainByStateMask(null, 0));

        int[] slots = {9, 0, -1, 9, Integer.MAX_VALUE, -999, 7};
        byte[] flags = {1, -1, 0, (byte) 0x80, 7, 3, (byte) 0xff};
        int[] initialSlots = slots.clone();
        byte[] initialFlags = flags.clone();
        check(M3StateMaskArrays.compactWindow(slots, flags, 2, 6, 0, 0) == 6,
                "zero masks retain exclusive offset");
        equalsInt(initialSlots, slots, "all-pass array index directory unchanged");
        equalsBytes(initialFlags, flags, "all-pass occurrence masks unchanged");
        int previousState = M3StateMaskArrays.update(flags, 3, 0, 0);
        check(previousState == (initialFlags[3] & 0xff),
                "null bit-mask transition returns prior unsigned state");
        equalsBytes(initialFlags, flags, "null state update leaves every byte unchanged");
        refuses(IndexOutOfBoundsException.class,
                () -> M3StateMaskArrays.update(flags, -1, 0, 0));
        refuses(NullPointerException.class,
                () -> M3StateMaskArrays.update(null, 0, 0, 0));
        refuses(IllegalArgumentException.class,
                () -> M3StateMaskArrays.update(flags, 3, 0x01, 0x01));
        check(M3StateMaskArrays.compactWindow(slots, flags, 0, 0, 0, 0) == 0,
                "empty zero-mask interval");
        check(M3StateMaskArrays.compactWindow(slots, flags, slots.length, slots.length, 0, 0)
                == slots.length, "empty terminal interval");
        refuses(IllegalArgumentException.class,
                () -> M3StateMaskArrays.compactWindow(slots, flags, 0, 7, 1, 1));
        refuses(IllegalArgumentException.class,
                () -> M3StateMaskArrays.compactWindow(slots, flags, 0, 7, -1, 0));
        refuses(IndexOutOfBoundsException.class,
                () -> M3StateMaskArrays.compactWindow(slots, flags, -1, 7, 0, 0));
        refuses(IndexOutOfBoundsException.class,
                () -> M3StateMaskArrays.compactWindow(slots, flags, 0, 8, 0, 0));
        refuses(IllegalArgumentException.class,
                () -> M3StateMaskArrays.compactWindow(slots, new byte[1], 0, 1, 0, 0));
        equalsInt(initialSlots, slots, "refusal preserves row slots");
        equalsBytes(initialFlags, flags, "refusal preserves byte states");
    }

    private static void deterministicTwoPointerDifferential() {
        Random random = new Random(0x4d33534d41534b31L);
        for (int trial = 0; trial < 1024; trial++) {
            int n = random.nextInt(141);
            int[] slots = new int[n];
            byte[] states = new byte[n];
            long[] longValues = new long[n];
            for (int i = 0; i < n; i++) {
                slots[i] = random.nextInt(27) - 13;
                longValues[i] = switch (i % 13) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(81) - 40L;
                };
                states[i] = (byte) random.nextInt(256);
            }
            int required = switch (trial % 8) {
                case 0 -> 0;
                case 1 -> 0xff;
                case 2 -> 0x80;
                default -> random.nextInt(256);
            };
            int forbidden = random.nextInt(256) & ~required;
            int from = random.nextInt(n + 1);
            int to = from + random.nextInt(n - from + 1);
            int[] expectedSlots = slots.clone();
            byte[] expectedStates = states.clone();
            List<Integer> selectedSlots = new ArrayList<>();
            List<Byte> selectedStates = new ArrayList<>();
            for (int i = from; i < to; i++) {
                int mask = states[i] & 0xff;
                if ((mask & required) == required && (mask & forbidden) == 0) {
                    selectedSlots.add(slots[i]);
                    selectedStates.add(states[i]);
                }
            }
            int end = from + selectedSlots.size();
            for (int i = from; i < to; i++) {
                expectedSlots[i] = i < end ? selectedSlots.get(i - from) : 0;
                expectedStates[i] = i < end ? selectedStates.get(i - from) : 0;
            }
            int result = M3StateMaskArrays.compactWindow(slots, states, from, to,
                    required, forbidden);
            check(result == end, "window exclusive end trial " + trial);
            equalsInt(expectedSlots, slots, "stable physical index occurrence compaction");
            equalsBytes(expectedStates, states, "aligned mask compaction");

            byte[] wholeStates = new byte[n];
            random.nextBytes(wholeStates);
            List<Long> reference = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (((wholeStates[i] & 0xff) & required) == required) {
                    reference.add(longValues[i]);
                }
            }
            M3LongArrayList list = M3LongArrayList.copyOf(longValues);
            M3LongIterator cursor = list.iterator();
            int removed = list.retainByStateMask(wholeStates, required);
            check(removed == n - reference.size(), "removed occurrences");
            long[] expected = new long[reference.size()];
            for (int i = 0; i < expected.length; i++) expected[i] = reference.get(i);
            equalsLong(expected, list.toArray(), "stable primitive values and duplicates");
            if (removed == 0) {
                check(!cursor.hasNext() == (n == 0),
                        "no-op selection must preserve iterator epoch");
            } else {
                refuses(ConcurrentModificationException.class, cursor::hasNext);
            }
        }
    }

    private static void noOpStressContract() {
        int length = 200_000;
        int[] slots = new int[length];
        byte[] flags = new byte[length];
        long[] values = new long[length];
        for (int i = 0; i < length; i++) {
            slots[i] = i % 19 - 9;
            flags[i] = (byte) (i * 41);
            values[i] = i % 3 == 0 ? Long.MIN_VALUE : i - 100000L;
        }
        int[] before = slots.clone();
        byte[] beforeFlags = flags.clone();
        check(M3StateMaskArrays.compactWindow(slots, flags, 65537, length - 1, 0, 0)
                == length - 1, "large offset window all-pass");
        equalsInt(before, slots, "large index lane unchanged");
        equalsBytes(beforeFlags, flags, "large state-mask lane unchanged");
        M3LongArrayList list = M3LongArrayList.copyOf(values);
        M3LongIterator cursor = list.iterator();
        check(list.retainByStateMask(flags, 0) == 0, "large primitive all-pass");
        equalsLong(values, list.toArray(), "large payload unchanged");
        check(cursor.hasNext() && cursor.nextLong() == values[0], "large iterator valid");
    }

    public static void main(String[] args) {
        exactAllPassAndFailFast();
        deterministicTwoPointerDifferential();
        noOpStressContract();
        System.out.println("M3_ZERO_MASK_ARRAY_FAST_PATH_PASS checks=" + checks);
    }
}
