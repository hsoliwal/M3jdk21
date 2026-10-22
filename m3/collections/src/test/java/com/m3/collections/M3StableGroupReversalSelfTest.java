// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;

/**
 * Contract oracle for the recipe-owned private int[] stable-run reversal.
 * Runs on the original M3IndexedListSnapshot API; no test-only runtime owner.
 */
public final class M3StableGroupReversalSelfTest {
    private static final long SEED = 0x4d334a444b3231L;
    private static int cases;
    private static long checks;

    private M3StableGroupReversalSelfTest() { }

    private static void check(boolean ok, String detail) {
        checks++;
        if (!ok) throw new AssertionError(detail);
    }

    /** Independent original array-copy oracle with the same callback order. */
    private static int[] original(int[] source, Integer[] payload, Comparator<Integer> comparator) {
        int[] indices = source.clone();
        for (int i = 1; i < indices.length; i++) {
            if (comparator.compare(payload[indices[i - 1]], payload[indices[i]]) > 0) {
                throw new AssertionError("input is not ascending");
            }
        }
        int[] reordered = new int[indices.length];
        int write = 0;
        for (int end = indices.length; end > 0;) {
            int begin = end - 1;
            while (begin > 0 && comparator.compare(
                    payload[indices[begin - 1]], payload[indices[begin]]) == 0) begin--;
            System.arraycopy(indices, begin, reordered, write, end - begin);
            write += end - begin;
            end = begin;
        }
        return reordered;
    }

    private static final class Traced implements Comparator<Integer> {
        private final Comparator<Integer> delegate;
        private final StringBuilder trace = new StringBuilder();
        private Traced(Comparator<Integer> delegate) { this.delegate = delegate; }
        @Override public int compare(Integer left, Integer right) {
            trace.append(Objects.toString(left)).append('/')
                    .append(Objects.toString(right)).append(';');
            return delegate.compare(left, right);
        }
        String transcript() { return trace.toString(); }
    }

    private static void oneCase(Integer[] input, Comparator<Integer> comparator) {
        M3IndexedListSnapshot<Integer> owner = M3IndexedListSnapshot.copyOf(input);
        M3IndexedListSnapshot<Integer> ascending = owner.sorted(comparator);
        int[] originalIndices = ascending.physicalIndices();
        Traced expectedCalls = new Traced(comparator);
        Traced actualCalls = new Traced(comparator);
        int[] expected = original(originalIndices, input, expectedCalls);
        M3IndexedListSnapshot<Integer> descending = ascending.sortedDescending(actualCalls);
        check(Arrays.equals(expected, descending.physicalIndices()), "physical index stability");
        check(expectedCalls.transcript().equals(actualCalls.transcript()), "comparator call ordering");
        check(Arrays.equals(originalIndices, ascending.physicalIndices()), "ascending parent unchanged");
        check(Arrays.equals(input, owner.toArray()), "original payload unchanged");
        for (int i = 0; i < expected.length; i++) {
            check(Objects.equals(input[expected[i]], descending.get(i)), "public List get");
        }
        if (input.length > 1) {
            M3IndexedListSnapshot<Integer> view = descending.subList(1, descending.size());
            check(view.size() == descending.size() - 1, "shared index-span slice");
            check(view.physicalIndex(0) == expected[1], "sliced occurrence index");
        }
        try {
            descending.add(42);
            throw new AssertionError("immutable result accepted write");
        } catch (UnsupportedOperationException expectedRefusal) {
            checks++;
        }
        cases++;
    }

    private static void maskAndNativeBoundary() {
        Integer[] input = {4, 4, 1, 3, 3, null, -5};
        Comparator<Integer> c = Comparator.nullsFirst(Integer::compare);
        M3IndexedListSnapshot<Integer> owner = M3IndexedListSnapshot.copyOf(input);
        int[] states = {1, 0, 1, 1, 0, 1, 1};
        M3IndexedListSnapshot<Integer> javaSelection = owner.maskedByPhysicalState(
                states, 1, 0, M3IndexPathMasks.Backend.JAVA);
        M3IndexedListSnapshot<Integer> javaOrdered = javaSelection.sortedDescending(c);
        check(javaOrdered.size() == 5, "precomputed physical-mask admission");
        if (M3IndexPathMasks.nativeAvailable()) {
            M3IndexedListSnapshot<Integer> nativeSelection = owner.maskedByPhysicalState(
                    states, 1, 0, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            M3IndexedListSnapshot<Integer> nativeOrdered = nativeSelection.sortedDescending(c);
            check(Arrays.equals(javaOrdered.physicalIndices(), nativeOrdered.physicalIndices()),
                    "existing Java/JNI mask selection and stable sorting parity");
        }
    }

    private static void exceptionAndExtremeCompare() {
        Integer[] input = {Integer.MIN_VALUE, -1, -1, 0, 9, Integer.MAX_VALUE};
        Comparator<Integer> extreme = (left, right) -> {
            int sign = Integer.compare(left, right);
            return sign < 0 ? Integer.MIN_VALUE : sign > 0 ? Integer.MAX_VALUE : 0;
        };
        oneCase(input, extreme);
        M3IndexedListSnapshot<Integer> original = M3IndexedListSnapshot.copyOf(input);
        int[] positions = original.physicalIndices();
        try {
            original.sortedDescending((left, right) -> {
                throw new IllegalStateException("sentinel");
            });
            throw new AssertionError("comparator exception suppressed");
        } catch (IllegalStateException expected) {
            check("sentinel".equals(expected.getMessage()), "original exception contract");
            check(Arrays.equals(positions, original.physicalIndices()), "unpublished failure state");
        }
    }

    public static void main(String[] args) {
        Random generator = new Random(SEED);
        Comparator<Integer> nullable = Comparator.nullsFirst(Integer::compare);
        for (int length = 0; length <= 192; length++) {
            for (int sample = 0; sample < 48; sample++) {
                Integer[] input = new Integer[length];
                for (int i = 0; i < length; i++) {
                    input[i] = generator.nextInt(31) == 0 ? null : generator.nextInt(13) - 6;
                }
                oneCase(input, nullable);
            }
        }
        exceptionAndExtremeCompare();
        maskAndNativeBoundary();
        System.out.println("M3_STABLE_GROUP_REVERSE_PASS cases=" + cases + " checks=" + checks);
    }
}
