// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.regex.Pattern;

/** Independent ArrayDeque model for both-end ring operations, exceptions and snapshots. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipe-crates/m3-mat-ring-20261030/src/test/java/com/synexia/matcollections/MatIntRingBufferAtomOracle.java blob 842dff4a9847; recipe m3/arrays/recipe/int-ring-buffer-20261106/relocate.py v3
public final class IntRingBufferContract {
    private static long checks;
    private static long digest = 0x428a2f98d0c4b3a1L;
    private IntRingBufferContract() { }

    public static void main(String[] args) {
        codeCarrierDecoys();
        for (int invalid : new int[] { 0, -1, Integer.MIN_VALUE }) {
            requireThrows(IllegalArgumentException.class,
                    () -> new IntRingBuffer(invalid), "invalid capacity");
        }
        int[] capacities = { 1, 2, 3, 4, 5, 7, 8, 9, 16, 17, 31, 32, 33, 63, 64, 65,
                127, 128, 129, 255, 256, 257, 511, 512, 1023, 1024 };
        for (int cap : capacities) run(cap, 10000);
        System.out.println("M3_RING_ORACLE_PASS checks=" + checks + " digest="
                + Long.toUnsignedString(digest, 16));
    }

    private static void codeCarrierDecoys() {
        String sourceLooking = "class Fake { int index=(head+size)%capacity; }";
        String regexLooking = "(?:offerLast|removeFirst)\\(.*\\)";
        String textBlock = """
                void pretend() { while (true) {} }
                //FLLDCIM: RECIPE pretend body
                Pattern.compile("(?:\\\\w+)");
                """;
        check(sourceLooking.contains("%"), "source-looking literal");
        check(Pattern.compile(regexLooking).matcher("offerLast(7)").matches(), "regex decoy");
        check(textBlock.contains("pretend"), "text block decoy");
    }

    private static void run(int capacity, int operations) {
        IntRingBuffer actual = new IntRingBuffer(capacity);
        ArrayDeque<Integer> expected = new ArrayDeque<>();
        Random random = new Random(0x22d3b192L ^ capacity * 0x9e3779b9L);
        equal(capacity, actual.capacity(), "capacity");
        equal(4L * capacity, actual.primitivePayloadBytes(), "payload bytes");
        for (int step = 0; step < operations; step++) {
            int value = switch (step % 17) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> 0;
                case 3 -> -1;
                default -> random.nextInt();
            };
            int action = random.nextInt(12);
            switch (action) {
                case 0, 1, 2 -> {
                    boolean accepted = expected.size() < capacity;
                    equal(accepted, actual.offerLast(value), "offerLast");
                    if (accepted) expected.addLast(value);
                }
                case 3, 4 -> {
                    boolean accepted = expected.size() < capacity;
                    equal(accepted, actual.offerFirst(value), "offerFirst");
                    if (accepted) expected.addFirst(value);
                }
                case 5, 6 -> {
                    if (expected.isEmpty()) {
                        requireThrows(NoSuchElementException.class, actual::removeFirst, "removeFirst empty");
                    } else equal(expected.removeFirst(), actual.removeFirst(), "removeFirst");
                }
                case 7 -> {
                    if (expected.isEmpty()) {
                        requireThrows(NoSuchElementException.class, actual::removeLast, "removeLast empty");
                    } else equal(expected.removeLast(), actual.removeLast(), "removeLast");
                }
                case 8 -> {
                    if (expected.isEmpty()) {
                        requireThrows(NoSuchElementException.class, actual::peekFirst, "peekFirst empty");
                    } else equal(expected.peekFirst(), actual.peekFirst(), "peekFirst");
                }
                case 9 -> {
                    if (expected.isEmpty()) {
                        requireThrows(NoSuchElementException.class, actual::peekLast, "peekLast empty");
                    } else equal(expected.peekLast(), actual.peekLast(), "peekLast");
                }
                case 10, 11 -> checkSnapshot(expected, actual);
                default -> throw new AssertionError("action");
            }
            equal(expected.size(), actual.size(), "size");
            equal(expected.isEmpty(), actual.isEmpty(), "empty");
            equal(expected.size() == capacity, actual.isFull(), "full");
            if ((step & 31) == 0) checkSnapshot(expected, actual);
        }
        while (!expected.isEmpty()) equal(expected.removeFirst(), actual.removeFirst(), "drain");
        checkSnapshot(expected, actual);
        requireThrows(NoSuchElementException.class, actual::peekLast, "empty last after drain");
        for (int i = 0; i < capacity; i++) {
            check(actual.offerLast(i), "refill");
        }
        check(!actual.offerLast(77), "full reject");
        for (int i = 0; i < capacity; i++) equal(i, actual.removeFirst(), "refill FIFO");
    }

    private static void checkSnapshot(ArrayDeque<Integer> expected, IntRingBuffer actual) {
        int[] array = actual.snapshot();
        int[] snapshot = expected.stream().mapToInt(Integer::intValue).toArray();
        check(Arrays.equals(snapshot, array), "snapshot parity");
        if (array.length > 0) {
            int old = array[0];
            array[0] ^= 0x5a5a5a5a;
            check(actual.peekFirst() == old, "snapshot independent");
        }
        digest = (digest ^ array.length) * 0x100000001b3L;
        for (int number : array) digest = (digest ^ number) * 0x100000001b3L;
    }

    private static void equal(long a, long b, String message) {
        check(a == b, message + ": " + a + " != " + b);
        digest = (digest ^ a) * 0x100000001b3L;
    }
    private static void equal(boolean a, boolean b, String message) {
        check(a == b, message);
        digest = (digest ^ (a ? 1 : 0)) * 0x100000001b3L;
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " at check " + checks);
    }
    private static void requireThrows(Class<? extends Throwable> type, Runnable work, String message) {
        try { work.run(); } catch (Throwable thrown) {
            check(type.isInstance(thrown), message + ": unexpected " + thrown);
            return;
        }
        throw new AssertionError(message + " did not throw");
    }
}
