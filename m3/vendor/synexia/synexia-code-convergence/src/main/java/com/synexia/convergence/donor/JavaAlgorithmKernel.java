// SPDX-License-Identifier: Apache-2.0
package com.synexia.convergence.donor;

import java.util.Arrays;
import java.util.Objects;

public final class JavaAlgorithmKernel implements AlgorithmKernel {
    @Override
    public int ternarySearchCanonical(final long[] sorted, final long value) {
        return AlgorithmKernel.super.ternarySearchCanonical(sorted, value);
    }

    @Override
    public AhoCorasickProgram compileAhoCorasick(final byte[][] patterns) {
        return JavaAhoCorasickProgram.compile(patterns);
    }

    @Override
    public int lowerBound(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        int low = 0;
        int high = sorted.length;
        while (low < high) {
            final int mid = (low + high) >>> 1;
            if (sorted[mid] < value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    @Override
    public int upperBound(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        int low = 0;
        int high = sorted.length;
        while (low < high) {
            final int mid = (low + high) >>> 1;
            if (sorted[mid] <= value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    @Override
    public int binarySearchExact(final int[] sorted, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        final int index = lowerBound(sorted, value);
        return index < sorted.length && sorted[index] == value ? index : -1;
    }

    @Override
    public int gallopLowerBound(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return gallopBound(sorted, from, to, value, false);
    }

    @Override
    public int gallopUpperBound(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return gallopBound(sorted, from, to, value, true);
    }

    @Override
    public int exponentialSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        final int index = gallopBound(sorted, from, to, value, false);
        return index < to && sorted[index] == value ? index : -1;
    }

    @Override
    public int fibonacciSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        return AlgorithmKernel.super.fibonacciSearchExact(sorted, from, to, value);
    }

    @Override
    public int jumpSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return jumpSearchExactRange(sorted, from, to, value);
    }

    @Override
    public int interpolationSearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return interpolationSearchExactRange(sorted, from, to, value);
    }


    /**
     * Returns the first equal value in the validated sorted subrange using ternary probes.
     *
     * <p>M3 atom: {@code M3_SEARCH_TERNARY_CONTRACT_V1}. Equal probes lower through the
     * shared first-equal bound atom, preserving duplicate semantics across the ordered-search
     * Strategy family. Java remains the semantic oracle; JNI is an optional parity-checked shadow.
     *
     * @param sorted ascending caller-owned primitive lane
     * @param from inclusive range start
     * @param to exclusive range end
     * @param value target value
     * @return first equal index in {@code [from,to)}, or {@code -1}
     */
    @Override
    public int ternarySearchExact(
            final int[] sorted, final int from, final int to, final int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return ternarySearchExactRange(sorted, from, to, value);
    }

    private static int jumpSearchExactRange(
            final int[] sorted, final int from, final int to, final int value) {
        final int length = to - from;
        if (length == 0) {
            return -1;
        }
        final int block = Math.max(1, (int) Math.sqrt(length));
        int start = from;
        int end = (int) Math.min((long) to, (long) start + block);
        while (end < to && sorted[end - 1] < value) {
            start = end;
            end = (int) Math.min((long) to, (long) start + block);
        }
        for (int index = start; index < end && sorted[index] <= value; index++) {
            if (sorted[index] == value) {
                return index;
            }
        }
        return -1;
    }

    private static int interpolationSearchExactRange(
            final int[] sorted, final int from, final int to, final int value) {
        if (from == to) {
            return -1;
        }
        int low = from;
        int high = to - 1;
        while (low <= high && value >= sorted[low] && value <= sorted[high]) {
            final int lowValue = sorted[low];
            final int highValue = sorted[high];
            if (lowValue == highValue) {
                return lowValue == value ? low : -1;
            }
            final long delta = (long) value - lowValue;
            final long denominator = (long) highValue - lowValue;
            final long width = (long) high - low;
            final long quotient = delta / denominator;
            final long remainder = delta % denominator;
            final long relative = quotient * width + (remainder * width) / denominator;
            final int probe = low + (int) relative;
            final int current = sorted[probe];
            if (current < value) {
                low = probe + 1;
            } else if (current > value) {
                high = probe - 1;
            } else {
                return boundRange(sorted, low, probe + 1, value, false);
            }
        }
        return -1;
    }

    private static int ternarySearchExactRange(
            final int[] sorted, final int from, final int to, final int value) {
        if (from == to) {
            return -1;
        }
        int low = from;
        int high = to - 1;
        while (low <= high) {
            final int third = (high - low) / 3;
            final int leftProbe = low + third;
            final int rightProbe = high - third;
            final int leftValue = sorted[leftProbe];
            if (leftValue == value) {
                return boundRange(sorted, from, leftProbe + 1, value, false);
            }
            final int rightValue = sorted[rightProbe];
            if (rightValue == value) {
                return boundRange(sorted, from, rightProbe + 1, value, false);
            }
            if (value < leftValue) {
                high = leftProbe - 1;
            } else if (value > rightValue) {
                low = rightProbe + 1;
            } else {
                low = leftProbe + 1;
                high = rightProbe - 1;
            }
        }
        return -1;
    }

    private static int gallopBound(
            final int[] sorted,
            final int from,
            final int to,
            final int value,
            final boolean upper) {
        if (from == to) {
            return to;
        }
        int comparison = Integer.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) {
            return from;
        }
        int low = from + 1;
        for (long offset = 1L;
                (long) from + offset < to;
                offset = (offset << 1) + 1) {
            final int probe = (int) ((long) from + offset);
            comparison = Integer.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return boundRange(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return boundRange(sorted, low, to, value, upper);
    }

    private static int boundRange(
            final int[] sorted,
            final int from,
            final int to,
            final int value,
            final boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            final int mid = low + ((high - low) >>> 1);
            final int comparison = Integer.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    @Override
    public int[] intersectSortedUnique(final int[] left, final int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        final int[] out = new int[Math.min(left.length, right.length)];
        int i = 0;
        int j = 0;
        int size = 0;
        int previous = 0;
        boolean hasPrevious = false;
        while (i < left.length && j < right.length) {
            final int a = left[i];
            final int b = right[j];
            if (a < b) {
                i++;
            } else if (a > b) {
                j++;
            } else {
                if (!hasPrevious || previous != a) {
                    out[size++] = a;
                    previous = a;
                    hasPrevious = true;
                }
                while (i < left.length && left[i] == a) {
                    i++;
                }
                while (j < right.length && right[j] == b) {
                    j++;
                }
            }
        }
        return Arrays.copyOf(out, size);
    }

    @Override
    public int intersectCountSortedUnique(final int[] left, final int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        int i = 0;
        int j = 0;
        int count = 0;
        int previous = 0;
        boolean hasPrevious = false;
        while (i < left.length && j < right.length) {
            final int a = left[i];
            final int b = right[j];
            if (a < b) {
                i++;
            } else if (a > b) {
                j++;
            } else {
                if (!hasPrevious || previous != a) {
                    count++;
                    previous = a;
                    hasPrevious = true;
                }
                while (i < left.length && left[i] == a) {
                    i++;
                }
                while (j < right.length && right[j] == b) {
                    j++;
                }
            }
        }
        return count;
    }

    @Override
    public int[] unionSortedUnique(final int[] left, final int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        final int[] out = new int[Math.addExact(left.length, right.length)];
        int i = 0;
        int j = 0;
        int size = 0;
        int previous = 0;
        boolean hasPrevious = false;
        while (i < left.length || j < right.length) {
            final int value;
            if (j >= right.length || (i < left.length && left[i] <= right[j])) {
                value = left[i++];
                while (i < left.length && left[i] == value) {
                    i++;
                }
                while (j < right.length && right[j] == value) {
                    j++;
                }
            } else {
                value = right[j++];
                while (j < right.length && right[j] == value) {
                    j++;
                }
            }
            if (!hasPrevious || previous != value) {
                out[size++] = value;
                previous = value;
                hasPrevious = true;
            }
        }
        return Arrays.copyOf(out, size);
    }

    @Override
    public int indexOf(final byte[] haystack, final byte[] needle) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        if (needle.length == 0) {
            return 0;
        }
        if (needle.length > haystack.length) {
            return -1;
        }
        for (int start = 0; start <= haystack.length - needle.length; start++) {
            int offset = 0;
            while (offset < needle.length && haystack[start + offset] == needle[offset]) {
                offset++;
            }
            if (offset == needle.length) {
                return start;
            }
        }
        return -1;
    }

    @Override
    public int indexOfKmp(final byte[] haystack, final byte[] needle) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        if (needle.length == 0) {
            return 0;
        }
        if (needle.length > haystack.length) {
            return -1;
        }
        final int[] failure = new int[needle.length];
        for (int i = 1, prefix = 0; i < needle.length; ) {
            if (needle[i] == needle[prefix]) {
                failure[i++] = ++prefix;
            } else if (prefix > 0) {
                prefix = failure[prefix - 1];
            } else {
                failure[i++] = 0;
            }
        }
        for (int i = 0, matched = 0; i < haystack.length; ) {
            if (haystack[i] == needle[matched]) {
                i++;
                matched++;
                if (matched == needle.length) {
                    return i - matched;
                }
            } else if (matched > 0) {
                matched = failure[matched - 1];
            } else {
                i++;
            }
        }
        return -1;
    }

    @Override
    public int longestCommonPrefix(final byte[] left, final byte[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        final int limit = Math.min(left.length, right.length);
        int index = 0;
        while (index < limit && left[index] == right[index]) {
            index++;
        }
        return index;
    }
}
