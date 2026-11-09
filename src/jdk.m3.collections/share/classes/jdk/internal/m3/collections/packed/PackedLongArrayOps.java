/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Caller-buffer operators for existing packed collection owners, not another collection hierarchy.
 * Signed-long ordering; duplicate input values have set semantics. Not thread-safe.
 * Public checked operators never mutate output on range/order/capacity/admission failure,
 * provided callbacks obey the no-input-mutation contract. Scratch contents are unspecified.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongArrayOps.java}
 * blob {@code d3ab6dd4e6f1d6fdcb5103c3116a14f428dc28ac} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongArrayOps {
    public static final int UNION = 0;
    public static final int INTERSECTION = 1;
    public static final int DIFFERENCE = 2;

    private PackedLongArrayOps() { }

    /** Exact-size preflight; output must not alias either input. */
    public static int setOperation(long[] left, int leftFrom, int leftTo,
            long[] right, int rightFrom, int rightTo, long[] output, int outputFrom, int operation) {
        requireSorted(left, leftFrom, leftTo);
        requireSorted(right, rightFrom, rightTo);
        if (operation < UNION || operation > DIFFERENCE) {
            throw new IllegalArgumentException("operation");
        }
        Objects.requireNonNull(output, "output");
        if (output == left || output == right) {
            throw new IllegalArgumentException("output must not alias input");
        }
        long required = combine(left, leftFrom, leftTo, right, rightFrom, rightTo, null, 0, operation);
        if (required > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("result exceeds array index limit");
        }
        Objects.checkFromIndexSize(outputFrom, (int) required, output.length);
        return (int) combine(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }

    /** Internal trusted-range union; retained for the already-validated sorted-set merge atom. */
    static int unionUnchecked(long[] left, int leftTo, long[] right, int rightTo, long[] output) {
        return (int) combine(left, 0, leftTo, right, 0, rightTo, output, 0, UNION);
    }

    private static long combine(long[] left, int i, int leftTo, long[] right, int j, int rightTo,
            long[] output, int outputFrom, int operation) {
        long written = 0;
        while (i < leftTo || j < rightTo) {
            // No result can be emitted from an irrelevant exhausted-side suffix.
            if ((operation == INTERSECTION && (i == leftTo || j == rightTo))
                    || (operation == DIFFERENCE && i == leftTo)) { break; }
            long value;
            boolean emit;
            if (i == leftTo) {
                value = right[j];
                emit = operation == UNION;
                j = afterEqual(right, j, rightTo, value);
            } else if (j == rightTo) {
                value = left[i];
                emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (left[i] < right[j]) {
                if (operation == INTERSECTION) {
                    i = PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                    continue;
                }
                value = left[i];
                emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (right[j] < left[i]) {
                if (operation != UNION) {
                    j = PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                    continue;
                }
                value = right[j];
                emit = operation == UNION;
                j = afterEqual(right, j, rightTo, value);
            } else {
                value = left[i];
                emit = operation != DIFFERENCE;
                i = afterEqual(left, i, leftTo, value);
                j = afterEqual(right, j, rightTo, value);
            }
            if (emit) {
                if (output != null) { output[outputFrom + (int) written] = value; }
                written++;
            }
        }
        return written;
    }

    private static int afterEqual(long[] values, int index, int to, long value) {
        int next = index + 1;
        if (next == to || values[next] != value) { return next; }
        return PackedPrimitiveArrayOps.gallopBound(values, next, to, value, true);
    }

    /** Owned scratch prefix only. Recognize a complete natural run before invoking JDK sort. */
    static int sortUniqueUnchecked(long[] values, int size) {
        if (size < 2) { return size; }
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < size && (ascending || descending); i++) {
            ascending &= values[i - 1] <= values[i];
            descending &= values[i - 1] >= values[i];
        }
        if (!ascending) {
            if (descending) {
                for (int left = 0, right = size - 1; left < right; left++, right--) {
                    long value = values[left];
                    values[left] = values[right];
                    values[right] = value;
                }
            } else if (!PackedLongPowerSort.sortIfStructured(values, size)) {
                Arrays.sort(values, 0, size);
            }
        }
        int unique = 1;
        // The unique prefix already occupies its final positions.
        while (unique < size && values[unique] != values[unique - 1]) { unique++; }
        if (unique == size) { return size; }
        for (int read = unique + 1; read < size; read++) {
            if (values[read] != values[unique - 1]) { values[unique++] = values[read]; }
        }
        return unique;
    }

    /**
     * Trusted strictly-increasing prefixes; output is disjoint and has leftTo + rightTo capacity.
     * Only the collection owners may supply this stronger uniqueness proof. General callers keep
     * the checked setOperation path, including its sortedness scan and exact-size preflight.
     */
    static int unionUniqueUnchecked(long[] left, int leftTo, long[] right, int rightTo, long[] output) {
        int i = 0;
        int j = 0;
        int written = 0;
        while (i < leftTo && j < rightTo) {
            if (left[i] < right[j]) {
                int end = PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                System.arraycopy(left, i, output, written, end - i);
                written += end - i;
                i = end;
            } else if (right[j] < left[i]) {
                int end = PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                System.arraycopy(right, j, output, written, end - j);
                written += end - j;
                j = end;
            } else {
                output[written++] = left[i++];
                j++;
            }
        }
        System.arraycopy(left, i, output, written, leftTo - i);
        written += leftTo - i;
        System.arraycopy(right, j, output, written, rightTo - j);
        return written + rightTo - j;
    }

    /** Keeps callback-approved values in stable order, evaluating every predicate before copying. */
    public static int filter(long[] source, int from, int to, LongPredicate admission,
            long[] decisions, long[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(admission, "admission");
        Objects.requireNonNull(decisions, "decisions");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        int count = to - from;
        int words = (int) (((long) count + 63) >>> 6);
        Objects.checkFromIndexSize(0, words, decisions.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if (decisions == source || decisions == output || (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe scratch/output alias");
        }
        for (int i = 0; i < words; i++) { decisions[i] = 0; }
        int kept = 0;
        for (int i = 0; i < count; i++) {
            if (admission.test(source[from + i])) {
                decisions[i >>> 6] |= 1L << (i & 63);
                kept++;
            }
        }
        Objects.checkFromIndexSize(outputFrom, kept, output.length);
        int write = outputFrom;
        for (int i = 0; i < count; i++) {
            if ((decisions[i >>> 6] & (1L << (i & 63))) != 0) {
                output[write++] = source[from + i];
            }
        }
        return kept;
    }

    public static void heapify(long[] heap, int size, boolean minHeap) {
        Objects.requireNonNull(heap, "heap");
        Objects.checkFromIndexSize(0, size, heap.length);
        heapifyUnchecked(heap, size, minHeap);
    }

    static void heapifyUnchecked(long[] heap, int size, boolean minHeap) {
        for (int i = PackedHeapAtoms.lastParent(size); i >= 0; i--) { siftDownUnchecked(heap, size, i, minHeap); }
    }

    /* Reuses the existing PackedLongHeap hole-sifting algorithm; no entry wrappers. */
    static void siftDownUnchecked(long[] heap, int size, int index, boolean minHeap) {
        long value = heap[index];
        int half = PackedHeapAtoms.firstLeaf(size);
        while (index < half) {
            int child = PackedHeapAtoms.left(index);
            int right = PackedHeapAtoms.right(index);
            if (right < size && (minHeap ? heap[right] < heap[child] : heap[right] > heap[child])) {
                child = right;
            }
            if (!(minHeap ? heap[child] < value : heap[child] > value)) { break; }
            heap[index] = heap[child];
            index = child;
        }
        heap[index] = value;
    }

    /** One-shot Top-K. Duplicates count, equal boundary values do not replace. Output is heap order. */
    public static int topK(long[] source, int from, int to, long[] output, int limit, boolean largest) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(0, limit, output.length);
        if (source == output) { throw new IllegalArgumentException("output must not alias input"); }
        int size = Math.min(limit, to - from);
        System.arraycopy(source, from, output, 0, size);
        if (size == 0) { return 0; }
        heapifyUnchecked(output, size, largest);
        for (int i = from + size; i < to; i++) {
            long value = source[i];
            if (largest ? value > output[0] : value < output[0]) {
                output[0] = value;
                siftDownUnchecked(output, size, 0, largest);
            }
        }
        return size;
    }

    private static void requireSorted(long[] values, int from, int to) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (values[i] < values[i - 1]) { throw new IllegalArgumentException("input is not sorted"); }
        }
    }
}
