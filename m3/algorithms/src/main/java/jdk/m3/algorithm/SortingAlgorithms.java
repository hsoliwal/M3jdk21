// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.Arrays;
import java.util.Objects;

/** Deterministic primitive sorting algorithms without comparator boxing. */
//FLLDCIM: RELOCATE - donor blob a42bd2e406a1 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class SortingAlgorithms {
    private SortingAlgorithms() {}

    public static void mergeSort(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length < 2) return;
        int[] source = values.clone();
        int[] destination = values;
        for (int width = 1; width < values.length; width = nextWidth(width, values.length)) {
            for (int start = 0; start < values.length; start += width << 1) {
                int middle = Math.min(start + width, values.length);
                int end = Math.min(start + (width << 1), values.length);
                merge(source, destination, start, middle, end);
            }
            int[] swap = source;
            source = destination;
            destination = swap;
        }
        if (source != values) System.arraycopy(source, 0, values, 0, values.length);
    }

    public static void heapSort(int[] values) {
        Objects.requireNonNull(values, "values");
        for (int parent = (values.length >>> 1) - 1; parent >= 0; parent--) {
            siftDown(values, parent, values.length);
        }
        for (int end = values.length - 1; end > 0; end--) {
            swap(values, 0, end);
            siftDown(values, 0, end);
        }
    }

    /**
     * Stable four-pass LSD radix sort for all signed int values.
     * Signed order is achieved by toggling the sign bit before byte extraction.
     */
    public static void radixSortSigned(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length < 2) return;
        int[] source = values;
        int[] target = new int[values.length];
        int[] counts = new int[256];
        for (int shift = 0; shift < 32; shift += 8) {
            Arrays.fill(counts, 0);
            for (int value : source) {
                int transformed = value ^ Integer.MIN_VALUE;
                counts[(transformed >>> shift) & 0xff]++;
            }
            int total = 0;
            for (int bucket = 0; bucket < counts.length; bucket++) {
                int count = counts[bucket];
                counts[bucket] = total;
                total += count;
            }
            for (int value : source) {
                int transformed = value ^ Integer.MIN_VALUE;
                int bucket = (transformed >>> shift) & 0xff;
                target[counts[bucket]++] = value;
            }
            int[] swap = source;
            source = target;
            target = swap;
        }
        if (source != values) System.arraycopy(source, 0, values, 0, values.length);
    }

    /** Counting sort for values in the caller-supplied inclusive range. */
    public static void countingSort(int[] values, int minimum, int maximum) {
        Objects.requireNonNull(values, "values");
        if (minimum > maximum) throw new IllegalArgumentException("minimum > maximum");
        long range = (long) maximum - minimum + 1L;
        if (range > 16_777_216L) {
            throw new IllegalArgumentException("counting range too large: " + range);
        }
        int[] counts = new int[(int) range];
        for (int value : values) {
            if (value < minimum || value > maximum) {
                throw new IllegalArgumentException("value outside declared counting range: " + value);
            }
            counts[value - minimum]++;
        }
        int cursor = 0;
        for (int offset = 0; offset < counts.length; offset++) {
            int value = minimum + offset;
            for (int count = counts[offset]; count > 0; count--) values[cursor++] = value;
        }
    }

    private static void merge(
            int[] source,
            int[] destination,
            int start,
            int middle,
            int end) {
        int left = start;
        int right = middle;
        int cursor = start;
        while (left < middle && right < end) {
            destination[cursor++] = source[left] <= source[right]
                    ? source[left++]
                    : source[right++];
        }
        while (left < middle) destination[cursor++] = source[left++];
        while (right < end) destination[cursor++] = source[right++];
    }

    private static int nextWidth(int width, int length) {
        if (width > length / 2) return length;
        return width << 1;
    }

    private static void siftDown(int[] values, int root, int size) {
        int value = values[root];
        int index = root;
        while (true) {
            int left = index * 2 + 1;
            if (left >= size) break;
            int right = left + 1;
            int child = right < size && values[right] > values[left] ? right : left;
            if (values[child] <= value) break;
            values[index] = values[child];
            index = child;
        }
        values[index] = value;
    }

    private static void swap(int[] values, int left, int right) {
        int value = values[left];
        values[left] = values[right];
        values[right] = value;
    }
}
