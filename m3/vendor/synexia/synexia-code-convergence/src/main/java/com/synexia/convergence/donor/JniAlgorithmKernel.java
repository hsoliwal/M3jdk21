// SPDX-License-Identifier: Apache-2.0
package com.synexia.convergence.donor;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class JniAlgorithmKernel implements AlgorithmKernel {
    private static final Set<String> LOADED_LIBRARIES = ConcurrentHashMap.newKeySet();

    @FunctionalInterface
    interface NativeLibraryLoader {
        void load(String library);
    }

    private JniAlgorithmKernel() {}

    public static Optional<AlgorithmKernel> tryLoad(final String library) {
        return tryLoad(library, System::loadLibrary);
    }

    static Optional<AlgorithmKernel> tryLoad(
            final String library,
            final NativeLibraryLoader loader) {
        if (library == null || !library.matches("[A-Za-z0-9_.-]{1,128}")) {
            throw new IllegalArgumentException("INVALID_NATIVE_LIBRARY");
        }
        Objects.requireNonNull(loader, "loader");
        if (!LOADED_LIBRARIES.contains(library)) {
            synchronized (LOADED_LIBRARIES) {
                if (!LOADED_LIBRARIES.contains(library)) {
                    try {
                        loader.load(library);
                    } catch (UnsatisfiedLinkError | SecurityException unavailable) {
                        return Optional.empty();
                    }
                    LOADED_LIBRARIES.add(library);
                }
            }
        }
        return Optional.of(new JniAlgorithmKernel());
    }

    @Override
    public int jumpSearchCanonical(final long[] sorted, final long value) {
        Objects.requireNonNull(sorted, "sorted");
        return jumpSearchCanonical0(sorted, value);
    }

    @Override
    public int ternarySearchCanonical(final long[] sorted, final long value) {
        Objects.requireNonNull(sorted, "sorted");
        return ternarySearchCanonical0(sorted, value);
    }

    @Override
    public int lowerBound(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        return lowerBound0(sorted, value);
    }

    @Override
    public int upperBound(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        return upperBound0(sorted, value);
    }

    @Override
    public int binarySearchExact(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        return binarySearchExact0(sorted, value);
    }

    @Override
    public int binarySearchCanonical(final long[] sorted, final long value) {
        Objects.requireNonNull(sorted, "sorted");
        return binarySearchCanonical0(sorted, value);
    }

    @Override
    public int exponentialSearchCanonical(final long[] sorted, final long value) {
        Objects.requireNonNull(sorted, "sorted");
        return exponentialSearchCanonical0(sorted, value);
    }

    @Override
    public long equalRangePacked(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        return equalRangePacked0(sorted, value);
    }

    @Override
    public int equalRangesPacked(
            final int[] sorted,
            final int[] values,
            final int valueFrom,
            final int valueTo,
            final long[] destination,
            final int destinationFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromToIndex(valueFrom, valueTo, values.length);
        final int count = valueTo - valueFrom;
        Objects.checkFromIndexSize(destinationFrom, count, destination.length);
        return equalRangesPacked0(
                sorted, values, valueFrom, valueTo, destination, destinationFrom);
    }

    @Override
    public int gallopLowerBound(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return gallopLowerBound0(sorted, from, to, value);
    }

    @Override
    public int gallopUpperBound(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return gallopUpperBound0(sorted, from, to, value);
    }

    @Override
    public int exponentialSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return exponentialSearchExact0(sorted, from, to, value);
    }

    @Override
    public int selectKthSmallestInPlace(
            final int[] values, final int from, final int to, final int rank) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        Objects.checkIndex(rank, to - from);
        return selectKthSmallestInPlace0(values, from, to, rank);
    }

    @Override
    public int fibonacciSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return fibonacciSearchExact0(sorted, from, to, value);
    }

    @Override
    public int ternarySearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return ternarySearchExact0(sorted, from, to, value);
    }

    @Override
    public int jumpSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return jumpSearchExact0(sorted, from, to, value);
    }

    @Override
    public int interpolationSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return interpolationSearchExact0(sorted, from, to, value);
    }


    @Override
    public int[] intersectSortedUnique(final int[] left, final int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return intersectSortedUnique0(left, right);
    }

    @Override
    public int intersectCountSortedUnique(final int[] left, final int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return intersectCountSortedUnique0(left, right);
    }

    @Override
    public int[] unionSortedUnique(final int[] left, final int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return unionSortedUnique0(left, right);
    }

    @Override
    public int indexOf(final byte[] haystack, final byte[] needle) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        return indexOf0(haystack, needle);
    }

    @Override
    public int indexOfKmp(final byte[] haystack, final byte[] needle) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        return indexOfKmp0(haystack, needle);
    }

    @Override
    public int longestCommonPrefix(final byte[] left, final byte[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return longestCommonPrefix0(left, right);
    }

    private static native int lowerBound0(int[] sorted, int value);

    private static native int upperBound0(int[] sorted, int value);

    private static native int binarySearchExact0(int[] sorted, int value);

    private static native int binarySearchCanonical0(long[] sorted, long value);

    private static native int exponentialSearchCanonical0(long[] sorted, long value);

    private static native int jumpSearchCanonical0(long[] sorted, long value);

    private static native int ternarySearchCanonical0(long[] sorted, long value);

    private static native long equalRangePacked0(int[] sorted, int value);

    private static native int equalRangesPacked0(
            int[] sorted,
            int[] values,
            int valueFrom,
            int valueTo,
            long[] destination,
            int destinationFrom);

    private static native int gallopLowerBound0(int[] sorted, int from, int to, int value);

    private static native int gallopUpperBound0(int[] sorted, int from, int to, int value);

    private static native int exponentialSearchExact0(int[] sorted, int from, int to, int value);

    private static native int selectKthSmallestInPlace0(
            int[] values, int from, int to, int rank);

    private static native int fibonacciSearchExact0(
            int[] sorted, int from, int to, int value);

    private static native int ternarySearchExact0(
            int[] sorted, int from, int to, int value);

    private static native int jumpSearchExact0(int[] sorted, int from, int to, int value);

    private static native int interpolationSearchExact0(int[] sorted, int from, int to, int value);


    private static native int[] intersectSortedUnique0(int[] left, int[] right);

    private static native int intersectCountSortedUnique0(int[] left, int[] right);

    private static native int[] unionSortedUnique0(int[] left, int[] right);

    private static native int indexOf0(byte[] haystack, byte[] needle);

    private static native int indexOfKmp0(byte[] haystack, byte[] needle);

    private static native int longestCommonPrefix0(byte[] left, byte[] right);
}
