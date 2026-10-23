/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Actual-owner Java 21 differential of historical ascending/descending index logic.
 *
 * <p>In particular, a comparator's complete invocation sequence, comparisons that
 * throw, and the original public/physical occurrence mapping must not change.
 * Optional NATIVE_REQUIRED mode exercises the original JNI path-mask selector,
 * not a new comparator implementation in C.</p>
 */
public final class M3IndexedDescendingGroupMaskSelfTest {
    private static final long SEED = 0x4d334d41534b2026L;
    private static final int SIGN = Integer.MIN_VALUE;
    private static long checks;

    private record Item(int ordinal, int key) { }

    private static final class Tape implements Comparator<Item> {
        private final int failAt;
        private int calls;
        private long digest = 0x9e3779b97f4a7c15L;

        private Tape(int failAt) { this.failAt = failAt; }

        @Override
        public int compare(Item left, Item right) {
            calls++;
            digest = Long.rotateLeft(digest ^ left.ordinal(), 11) * 0x100000001b3L;
            digest = Long.rotateLeft(digest ^ right.ordinal(), 13) * 0x100000001b3L;
            if (calls == failAt) throw new DeliberateComparatorFailure();
            return left.key() < right.key() ? Integer.MIN_VALUE
                    : left.key() > right.key() ? Integer.MAX_VALUE : 0;
        }
    }

    private static final class DeliberateComparatorFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private M3IndexedDescendingGroupMaskSelfTest() { }

    public static void main(String[] arguments) {
        boolean requireNative = arguments.length != 0 && "native".equals(arguments[0]);
        if (requireNative && !M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("required existing native path-mask selector is unavailable");
        }
        boundaryShapes(requireNative);
        Random random = new Random(SEED);
        for (int trial = 0; trial < 2_048; trial++) {
            int length = trial < 20 ? trial : random.nextInt(513);
            Item[] values = new Item[length];
            int domain = switch (trial & 3) {
                case 0 -> 1;
                case 1 -> 2;
                case 2 -> 17;
                default -> 1_024;
            };
            for (int index = 0; index < length; index++) {
                values[index] = new Item(index, random.nextInt(domain) - domain / 2);
            }
            verifyRun(values, requireNative, random);
        }
        System.out.println("PASS M3IndexedDescendingGroupMaskSelfTest: checks=" + checks
                + ", nativeRequired=" + requireNative);
    }

    private static void boundaryShapes(boolean requireNative) {
        for (int size : new int[] {0, 1, 2, 3, 31, 32, 33, 63, 64, 65, 66,
                127, 128, 129, 255, 256, 257, 511, 512, 513, 1025}) {
            for (int shape = 0; shape < 4; shape++) {
                Item[] values = new Item[size];
                for (int index = 0; index < size; index++) {
                    int key = switch (shape) {
                        case 0 -> 0;
                        case 1 -> index;
                        case 2 -> (index / 7) % 3;
                        default -> size - index;
                    };
                    values[index] = new Item(index, key);
                }
                verifyRun(values, requireNative, new Random(SEED + size * 31L + shape));
            }
        }
    }

    private static void verifyRun(Item[] items, boolean requireNative, Random random) {
        M3IndexedListSnapshot<Item> source = M3IndexedListSnapshot.copyOf(items);
        Comparator<Item> keyOrder = Comparator.comparingInt(Item::key);
        M3IndexedListSnapshot<Item> ascending = source.sorted(keyOrder);
        compareHistoricalAndActual(ascending, items, "sorted-descending");
        compareHistoricalAndActual(source, items, "unsorted-fallback");
        check(Arrays.equals(ids(items), source.physicalIndices()), "source index isolation");

        int[][] states = new int[2][items.length];
        for (int index = 0; index < items.length; index++) {
            states[0][index] = random.nextInt();
            states[1][index] = random.nextInt();
        }
        int[] required = new int[] {SIGN, 0};
        int[] forbidden = new int[] {0, 0};
        verifyMasked(ascending, items, states, required, forbidden,
                M3IndexPathMasks.Backend.JAVA);
        if (requireNative) {
            verifyMasked(ascending, items, states, required, forbidden,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED);
        }
        // A full-mask zero selection is still a valid empty projection.
        if (items.length > 0) {
            int[][] absent = {{0}, {0}};
            int[][] expanded = new int[][] {
                    Arrays.copyOf(absent[0], items.length),
                    Arrays.copyOf(absent[1], items.length)
            };
            verifyMasked(ascending, items, expanded,
                    new int[] {SIGN, SIGN}, forbidden, M3IndexPathMasks.Backend.JAVA);
        }
    }

    private static void verifyMasked(M3IndexedListSnapshot<Item> source, Item[] items,
            int[][] states, int[] required, int[] forbidden,
            M3IndexPathMasks.Backend backend) {
        M3IndexedListSnapshot<Item> selected = source.maskedByPhysicalStates(
                states, required, forbidden, backend);
        int[] selectedBefore = selected.physicalIndices();
        Tape historical = new Tape(0);
        Tape current = new Tape(0);
        int[] expected = historicalOrder(selectedBefore, items, historical);
        var actual = source.sortedMaskedByPhysicalStatesDescending(
                states, required, forbidden, current, backend);
        check(Arrays.equals(expected, actual.physicalIndices()),
                "masked stable descending physical occurrence order");
        sameTape(historical, current, "masked order comparator tape");
        check(Arrays.equals(selectedBefore, selected.physicalIndices()),
                "masked selection remains isolated");
        check(Arrays.equals(source.toArray(), originalToArray(source)),
                "unmodified parent values");
    }

    private static Object[] originalToArray(M3IndexedListSnapshot<Item> source) {
        Object[] values = new Object[source.size()];
        for (int index = 0; index < source.size(); index++) values[index] = source.get(index);
        return values;
    }

    private static void compareHistoricalAndActual(
            M3IndexedListSnapshot<Item> source, Item[] items, String description) {
        int[] before = source.physicalIndices();
        Tape historical = new Tape(0);
        Tape current = new Tape(0);
        int[] expected = historicalOrder(before, items, historical);
        M3IndexedListSnapshot<Item> actual = source.sortedDescending(current);
        check(Arrays.equals(expected, actual.physicalIndices()),
                description + " stable physical occurrence order");
        sameTape(historical, current, description + " comparator tape");
        check(Arrays.equals(before, source.physicalIndices()),
                description + " original positions unchanged");
        for (int index = 0; index < expected.length; index++) {
            check(actual.get(index) == items[expected[index]],
                    description + " shared canonical item payload");
        }

        // Exception injection verifies no early writes and precisely the previous
        // comparison timing. User callbacks are never moved into a native lane.
        for (int failAt : new int[] {1, 2, 3, 5, 7, 11}) {
            Tape beforeTape = new Tape(failAt);
            Tape afterTape = new Tape(failAt);
            Throwable beforeError = errorOf(
                    () -> historicalOrder(before, items, beforeTape));
            Throwable afterError = errorOf(
                    () -> source.sortedDescending(afterTape));
            check((beforeError == null) == (afterError == null), "exception occurrence");
            check(beforeError == null || beforeError.getClass() == afterError.getClass(),
                    "exception identity");
            sameTape(beforeTape, afterTape, "exception comparator tape");
            check(Arrays.equals(before, source.physicalIndices()),
                    "exception must not mutate source owner");
        }
    }

    /** Preimage behavior, deliberately independent of the candidate group-bit algorithm. */
    private static int[] historicalOrder(int[] source, Item[] payload, Tape comparator) {
        int[] indices = source.clone();
        int length = indices.length;
        if (length < 2) return indices;
        boolean ascending = true;
        for (int index = 1; index < length; index++) {
            if (comparator.compare(payload[indices[index - 1]],
                    payload[indices[index]]) > 0) {
                ascending = false;
                break;
            }
        }
        if (ascending) {
            int[] reverseGroups = new int[length];
            int output = 0;
            for (int end = length; end > 0;) {
                int begin = end - 1;
                while (begin > 0
                        && comparator.compare(payload[indices[begin - 1]],
                                payload[indices[begin]]) == 0) {
                    begin--;
                }
                System.arraycopy(indices, begin, reverseGroups, output, end - begin);
                output += end - begin;
                end = begin;
            }
            System.arraycopy(reverseGroups, 0, indices, 0, length);
        } else {
            M3IndexSort.stable(indices, index -> payload[index],
                    (left, right) -> comparator.compare(right, left));
        }
        return indices;
    }

    private static int[] ids(Item[] items) {
        int[] ids = new int[items.length];
        for (int index = 0; index < ids.length; index++) ids[index] = index;
        return ids;
    }

    private static Throwable errorOf(Runnable action) {
        try {
            action.run();
            return null;
        } catch (DeliberateComparatorFailure failure) {
            return failure;
        }
    }

    private static void sameTape(Tape left, Tape right, String label) {
        check(left.calls == right.calls && left.digest == right.digest, label);
    }

    private static void check(boolean truth, String label) {
        checks++;
        if (!truth) throw new AssertionError(label + " check=" + checks);
    }
}
