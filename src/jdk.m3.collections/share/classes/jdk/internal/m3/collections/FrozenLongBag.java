/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Immutable primitive long counts in exact signed-sorted element and count lanes.
 *
 * <p>The original elementAt contract and lane layout specify sorted distinct
 * ordinals. That order is preserved independently of Guava ImmutableMultiset's
 * first-encounter order. The provider's count semantics are projected onto this
 * existing M3 API; the class does not implement a provider interface.
 *
 * <p>Construction clones its input, sorts the private lane, and packs equal
 * runs. Exports are independent copies, mutations always throw, and no entry
 * object or boxed long is retained for an element or occurrence.
 */
public final class FrozenLongBag {
    static final long STATE_FROZEN = 1L << 0;

    private final long[] elementLane;
    private final int[] countLane;
    private final long[] stateMaskWords;
    private final int size;

    /** Creates a valid immutable empty owner. */
    public FrozenLongBag() {
        this(new long[0], new int[0], 0);
    }

    private FrozenLongBag(long[] elements, int[] counts, int total) {
        elementLane = elements;
        countLane = counts;
        size = total;
        stateMaskWords = new long[(int) (((long) elements.length + 63) >>> 6)];
        Arrays.fill(stateMaskWords, -1L);
        if ((elements.length & 63) != 0) {
            stateMaskWords[stateMaskWords.length - 1] = (1L << (elements.length & 63)) - 1;
        }
    }

    /** Retained compatibility descriptor; it does not describe admission status. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongBag|FROZEN_LONG_BAG|LONG|FROZEN|long[] elementLane;int[] countLane";
    }

    /** Copies input values and freezes counts in signed-sorted distinct order. */
    public static FrozenLongBag copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) {
            return new FrozenLongBag();
        }
        if (source.length >= 512) {
            FrozenLongBag dense = copyDenseIfNarrow(source);
            if (dense != null) return dense;
        }
        long[] elements = source.clone();
        Arrays.sort(elements);
        int distinct = 1;
        for (int index = 1; index < elements.length; index++) {
            if (elements[index] != elements[index - 1]) {
                distinct++;
            }
        }
        int[] counts = countsOf(elements, distinct);
        return new FrozenLongBag(distinct == elements.length
                ? elements : Arrays.copyOf(elements, distinct), counts, source.length);
    }

    private static int[] countsOf(long[] elements, int distinct) {
        int[] counts = new int[distinct];
        int ordinal = 0;
        long previous = elements[0];
        counts[0] = 1;
        for (int index = 1; index < elements.length; index++) {
            long element = elements[index];
            if (element == previous) {
                counts[ordinal]++;
            } else {
                previous = element;
                elements[++ordinal] = element;
                counts[ordinal] = 1;
            }
        }
        return counts;
    }

    /**
     * Counting-sort/frequency shape over a bounded signed-long domain. A wide
     * domain keeps the original clone/sort atom; range overflow is a refusal,
     * not an accidental small histogram. The returned owner retains no input.
     */
    private static FrozenLongBag copyDenseIfNarrow(long[] source) {
        int widthLimit = Math.min(65_535, source.length / 2 - 1);
        long minimum = Long.MAX_VALUE;
        long maximum = Long.MIN_VALUE;
        for (int index = 0; index < source.length; index++) {
            long element = source[index];
            if (element < minimum) minimum = element;
            if (element > maximum) maximum = element;
            // A span only grows; reject a wide domain every 64 observations.
            if ((index & 63) == 63) {
                long observedWidth = maximum - minimum;
                if (observedWidth < 0 || observedWidth > widthLimit) return null;
            }
        }
        long width = maximum - minimum;
        if (width < 0 || width > widthLimit) return null;
        int[] histogram = new int[(int) width + 1];
        for (long element : source) {
            histogram[(int) (element - minimum)]++;
        }
        int distinct = 0;
        for (int count : histogram) {
            if (count != 0) distinct++;
        }
        long[] elements = new long[distinct];
        int[] counts = new int[distinct];
        int ordinal = 0;
        for (int index = 0; index < histogram.length; index++) {
            int count = histogram[index];
            if (count != 0) {
                elements[ordinal] = minimum + index;
                counts[ordinal++] = count;
            }
        }
        return new FrozenLongBag(elements, counts, source.length);
    }

    public int size() {
        return size;
    }

    public int distinctSize() {
        return elementLane.length;
    }

    public int occurrencesOf(long element) {
        int ordinal = LaneSearch.lowerBound(elementLane, 0, elementLane.length, element);
        return ordinal < elementLane.length && elementLane[ordinal] == element
                ? countLane[ordinal] : 0;
    }

    public boolean contains(long element) {
        return occurrencesOf(element) != 0;
    }

    /** Signed-sorted distinct ordinal, as required by the original method contract. */
    public long elementAt(int ordinal) {
        return elementLane[Objects.checkIndex(ordinal, elementLane.length)];
    }

    public int countAt(int ordinal) {
        return countLane[Objects.checkIndex(ordinal, countLane.length)];
    }

    public long[] distinctToArray() {
        return elementLane.clone();
    }

    /** Calls the consumer once per distinct element in signed-sorted order. */
    public void forEachDistinct(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (long element : elementLane) {
            action.accept(element);
        }
    }

    public int addOccurrences(long element, int occurrences) {
        throw new UnsupportedOperationException("immutable bag");
    }

    public boolean removeOccurrences(long element, int occurrences) {
        throw new UnsupportedOperationException("immutable bag");
    }
}
