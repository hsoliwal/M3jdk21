/*******************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Ranged heaps, heapsort and admission-first Top-K over caller arrays; no retained entries. Not thread-safe; floating values use Java total order, not stable NaN payload order.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedPrimitiveHeapSelection.java}
 * blob {@code 780c2386c43d51a9c2e01719c3df0554fcf43491} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedPrimitiveHeapSelection {
    private M3PackedPrimitiveHeapSelection() { }

    /** Heapifies the half-open range in place. No allocation; a true flag selects a min-heap. */
    public static void heapify(boolean[] values, int from, int to, boolean minimum) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        heap(values, from, to - from, minimum);
    }

    /** In-place ascending heapsort. Equal values have no stability or NaN-payload-order guarantee. */
    public static void sort(boolean[] values, int from, int to) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        int size = to - from;
        heap(values, from, size, false);
        while (size > 1) {
            boolean value = values[from];
            values[from] = values[from + size - 1];
            values[from + size - 1] = value;
            down(values, from, --size, 0, false);
        }
    }

    /** Unfiltered one-shot Top-K, returning the retained count in heap order. */
    public static int topK(boolean[] source, int from, int to, boolean[] output,
            int outputFrom, int limit, boolean largest) {
        return topK(source, from, to, null, output, outputFrom, limit, largest);
    }

    /**
     * Bounded Top-K with optional admission bits relative to {@code from}; null admits all.
     * Filters BEFORE insertion. Duplicates count; equal boundary ties do not replace.
     * Output must hold limit slots and must not alias source or decisions, even for limit zero.
     * Invalid inputs leave output unchanged. No concurrent mutation of inputs is permitted.
     */
    public static int topK(boolean[] source, int from, int to, long[] decisions,
            boolean[] output, int outputFrom, int limit, boolean largest) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, limit, output.length);
        if (source == output || (Object) source == decisions || (Object) output == decisions) {
            throw new IllegalArgumentException("unsafe alias");
        }
        if (decisions != null) { M3PackedAdmissionMask.require(decisions, to - from); }
        if (limit == 0) { return 0; }
        int admitted = decisions == null ? to - from : M3PackedAdmissionMask.count(decisions, to - from);
        boolean all = admitted == to - from;
        boolean sparse = admitted <= (to - from) / 16;
        int size = 0;
        for (int i = from; i < to; i++) {
            if (!all && !M3PackedAdmissionMask.selected(decisions, i - from)) {
                if (!sparse) { continue; }
                int bit = M3PackedAdmissionMask.nextSelected(decisions, i - from, to - from);
                if (bit == to - from) { break; }
                i = from + bit;
            }
            boolean value = source[i];
            if (size < limit) {
                output[outputFrom + size++] = value;
                if (size == limit) { heap(output, outputFrom, size, largest); }
            } else {
                int comparison = Boolean.compare(value, output[outputFrom]);
                if (largest ? comparison > 0 : comparison < 0) {
                    output[outputFrom] = value;
                    down(output, outputFrom, size, 0, largest);
                }
            }
        }
        if (size < limit) { heap(output, outputFrom, size, largest); }
        return size;
    }

    private static void heap(boolean[] values, int from, int size, boolean minimum) {
        for (int i = size / 2; i > 0; i--) { down(values, from, size, i - 1, minimum); }
    }

    private static void down(boolean[] values, int from, int size, int index, boolean minimum) {
        boolean value = values[from + index];
        while (index < size / 2) {
            int child = index * 2 + 1;
            int right = child + 1;
            if (right < size && before(values[from + right], values[from + child], minimum)) { child = right; }
            if (!before(values[from + child], value, minimum)) { break; }
            values[from + index] = values[from + child];
            index = child;
        }
        values[from + index] = value;
    }

    private static boolean before(boolean left, boolean right, boolean minimum) {
        int comparison = Boolean.compare(left, right);
        return minimum ? comparison < 0 : comparison > 0;
    }

    /** Heapifies the half-open range in place. No allocation; a true flag selects a min-heap. */
    public static void heapify(byte[] values, int from, int to, boolean minimum) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        heap(values, from, to - from, minimum);
    }

    /** In-place ascending heapsort. Equal values have no stability or NaN-payload-order guarantee. */
    public static void sort(byte[] values, int from, int to) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        int size = to - from;
        heap(values, from, size, false);
        while (size > 1) {
            byte value = values[from];
            values[from] = values[from + size - 1];
            values[from + size - 1] = value;
            down(values, from, --size, 0, false);
        }
    }

    /** Unfiltered one-shot Top-K, returning the retained count in heap order. */
    public static int topK(byte[] source, int from, int to, byte[] output,
            int outputFrom, int limit, boolean largest) {
        return topK(source, from, to, null, output, outputFrom, limit, largest);
    }

    /**
     * Bounded Top-K with optional admission bits relative to {@code from}; null admits all.
     * Filters BEFORE insertion. Duplicates count; equal boundary ties do not replace.
     * Output must hold limit slots and must not alias source or decisions, even for limit zero.
     * Invalid inputs leave output unchanged. No concurrent mutation of inputs is permitted.
     */
    public static int topK(byte[] source, int from, int to, long[] decisions,
            byte[] output, int outputFrom, int limit, boolean largest) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, limit, output.length);
        if (source == output || (Object) source == decisions || (Object) output == decisions) {
            throw new IllegalArgumentException("unsafe alias");
        }
        if (decisions != null) { M3PackedAdmissionMask.require(decisions, to - from); }
        if (limit == 0) { return 0; }
        int admitted = decisions == null ? to - from : M3PackedAdmissionMask.count(decisions, to - from);
        boolean all = admitted == to - from;
        boolean sparse = admitted <= (to - from) / 16;
        int size = 0;
        for (int i = from; i < to; i++) {
            if (!all && !M3PackedAdmissionMask.selected(decisions, i - from)) {
                if (!sparse) { continue; }
                int bit = M3PackedAdmissionMask.nextSelected(decisions, i - from, to - from);
                if (bit == to - from) { break; }
                i = from + bit;
            }
            byte value = source[i];
            if (size < limit) {
                output[outputFrom + size++] = value;
                if (size == limit) { heap(output, outputFrom, size, largest); }
            } else {
                int comparison = Byte.compare(value, output[outputFrom]);
                if (largest ? comparison > 0 : comparison < 0) {
                    output[outputFrom] = value;
                    down(output, outputFrom, size, 0, largest);
                }
            }
        }
        if (size < limit) { heap(output, outputFrom, size, largest); }
        return size;
    }

    private static void heap(byte[] values, int from, int size, boolean minimum) {
        for (int i = size / 2; i > 0; i--) { down(values, from, size, i - 1, minimum); }
    }

    private static void down(byte[] values, int from, int size, int index, boolean minimum) {
        byte value = values[from + index];
        while (index < size / 2) {
            int child = index * 2 + 1;
            int right = child + 1;
            if (right < size && before(values[from + right], values[from + child], minimum)) { child = right; }
            if (!before(values[from + child], value, minimum)) { break; }
            values[from + index] = values[from + child];
            index = child;
        }
        values[from + index] = value;
    }

    private static boolean before(byte left, byte right, boolean minimum) {
        int comparison = Byte.compare(left, right);
        return minimum ? comparison < 0 : comparison > 0;
    }

    /** Heapifies the half-open range in place. No allocation; a true flag selects a min-heap. */
    public static void heapify(short[] values, int from, int to, boolean minimum) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        heap(values, from, to - from, minimum);
    }

    /** In-place ascending heapsort. Equal values have no stability or NaN-payload-order guarantee. */
    public static void sort(short[] values, int from, int to) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        int size = to - from;
        heap(values, from, size, false);
        while (size > 1) {
            short value = values[from];
            values[from] = values[from + size - 1];
            values[from + size - 1] = value;
            down(values, from, --size, 0, false);
        }
    }

    /** Unfiltered one-shot Top-K, returning the retained count in heap order. */
    public static int topK(short[] source, int from, int to, short[] output,
            int outputFrom, int limit, boolean largest) {
        return topK(source, from, to, null, output, outputFrom, limit, largest);
    }

    /**
     * Bounded Top-K with optional admission bits relative to {@code from}; null admits all.
     * Filters BEFORE insertion. Duplicates count; equal boundary ties do not replace.
     * Output must hold limit slots and must not alias source or decisions, even for limit zero.
     * Invalid inputs leave output unchanged. No concurrent mutation of inputs is permitted.
     */
    public static int topK(short[] source, int from, int to, long[] decisions,
            short[] output, int outputFrom, int limit, boolean largest) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, limit, output.length);
        if (source == output || (Object) source == decisions || (Object) output == decisions) {
            throw new IllegalArgumentException("unsafe alias");
        }
        if (decisions != null) { M3PackedAdmissionMask.require(decisions, to - from); }
        if (limit == 0) { return 0; }
        int admitted = decisions == null ? to - from : M3PackedAdmissionMask.count(decisions, to - from);
        boolean all = admitted == to - from;
        boolean sparse = admitted <= (to - from) / 16;
        int size = 0;
        for (int i = from; i < to; i++) {
            if (!all && !M3PackedAdmissionMask.selected(decisions, i - from)) {
                if (!sparse) { continue; }
                int bit = M3PackedAdmissionMask.nextSelected(decisions, i - from, to - from);
                if (bit == to - from) { break; }
                i = from + bit;
            }
            short value = source[i];
            if (size < limit) {
                output[outputFrom + size++] = value;
                if (size == limit) { heap(output, outputFrom, size, largest); }
            } else {
                int comparison = Short.compare(value, output[outputFrom]);
                if (largest ? comparison > 0 : comparison < 0) {
                    output[outputFrom] = value;
                    down(output, outputFrom, size, 0, largest);
                }
            }
        }
        if (size < limit) { heap(output, outputFrom, size, largest); }
        return size;
    }

    private static void heap(short[] values, int from, int size, boolean minimum) {
        for (int i = size / 2; i > 0; i--) { down(values, from, size, i - 1, minimum); }
    }

    private static void down(short[] values, int from, int size, int index, boolean minimum) {
        short value = values[from + index];
        while (index < size / 2) {
            int child = index * 2 + 1;
            int right = child + 1;
            if (right < size && before(values[from + right], values[from + child], minimum)) { child = right; }
            if (!before(values[from + child], value, minimum)) { break; }
            values[from + index] = values[from + child];
            index = child;
        }
        values[from + index] = value;
    }

    private static boolean before(short left, short right, boolean minimum) {
        int comparison = Short.compare(left, right);
        return minimum ? comparison < 0 : comparison > 0;
    }

    /** Heapifies the half-open range in place. No allocation; a true flag selects a min-heap. */
    public static void heapify(char[] values, int from, int to, boolean minimum) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        heap(values, from, to - from, minimum);
    }

    /** In-place ascending heapsort. Equal values have no stability or NaN-payload-order guarantee. */
    public static void sort(char[] values, int from, int to) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        int size = to - from;
        heap(values, from, size, false);
        while (size > 1) {
            char value = values[from];
            values[from] = values[from + size - 1];
            values[from + size - 1] = value;
            down(values, from, --size, 0, false);
        }
    }

    /** Unfiltered one-shot Top-K, returning the retained count in heap order. */
    public static int topK(char[] source, int from, int to, char[] output,
            int outputFrom, int limit, boolean largest) {
        return topK(source, from, to, null, output, outputFrom, limit, largest);
    }

    /**
     * Bounded Top-K with optional admission bits relative to {@code from}; null admits all.
     * Filters BEFORE insertion. Duplicates count; equal boundary ties do not replace.
     * Output must hold limit slots and must not alias source or decisions, even for limit zero.
     * Invalid inputs leave output unchanged. No concurrent mutation of inputs is permitted.
     */
    public static int topK(char[] source, int from, int to, long[] decisions,
            char[] output, int outputFrom, int limit, boolean largest) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, limit, output.length);
        if (source == output || (Object) source == decisions || (Object) output == decisions) {
            throw new IllegalArgumentException("unsafe alias");
        }
        if (decisions != null) { M3PackedAdmissionMask.require(decisions, to - from); }
        if (limit == 0) { return 0; }
        int admitted = decisions == null ? to - from : M3PackedAdmissionMask.count(decisions, to - from);
        boolean all = admitted == to - from;
        boolean sparse = admitted <= (to - from) / 16;
        int size = 0;
        for (int i = from; i < to; i++) {
            if (!all && !M3PackedAdmissionMask.selected(decisions, i - from)) {
                if (!sparse) { continue; }
                int bit = M3PackedAdmissionMask.nextSelected(decisions, i - from, to - from);
                if (bit == to - from) { break; }
                i = from + bit;
            }
            char value = source[i];
            if (size < limit) {
                output[outputFrom + size++] = value;
                if (size == limit) { heap(output, outputFrom, size, largest); }
            } else {
                int comparison = Character.compare(value, output[outputFrom]);
                if (largest ? comparison > 0 : comparison < 0) {
                    output[outputFrom] = value;
                    down(output, outputFrom, size, 0, largest);
                }
            }
        }
        if (size < limit) { heap(output, outputFrom, size, largest); }
        return size;
    }

    private static void heap(char[] values, int from, int size, boolean minimum) {
        for (int i = size / 2; i > 0; i--) { down(values, from, size, i - 1, minimum); }
    }

    private static void down(char[] values, int from, int size, int index, boolean minimum) {
        char value = values[from + index];
        while (index < size / 2) {
            int child = index * 2 + 1;
            int right = child + 1;
            if (right < size && before(values[from + right], values[from + child], minimum)) { child = right; }
            if (!before(values[from + child], value, minimum)) { break; }
            values[from + index] = values[from + child];
            index = child;
        }
        values[from + index] = value;
    }

    private static boolean before(char left, char right, boolean minimum) {
        int comparison = Character.compare(left, right);
        return minimum ? comparison < 0 : comparison > 0;
    }

    /** Heapifies the half-open range in place. No allocation; a true flag selects a min-heap. */
    public static void heapify(int[] values, int from, int to, boolean minimum) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        heap(values, from, to - from, minimum);
    }

    /** In-place ascending heapsort. Equal values have no stability or NaN-payload-order guarantee. */
    public static void sort(int[] values, int from, int to) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        int size = to - from;
        heap(values, from, size, false);
        while (size > 1) {
            int value = values[from];
            values[from] = values[from + size - 1];
            values[from + size - 1] = value;
            down(values, from, --size, 0, false);
        }
    }

    /** Unfiltered one-shot Top-K, returning the retained count in heap order. */
    public static int topK(int[] source, int from, int to, int[] output,
            int outputFrom, int limit, boolean largest) {
        return topK(source, from, to, null, output, outputFrom, limit, largest);
    }

    /**
     * Bounded Top-K with optional admission bits relative to {@code from}; null admits all.
     * Filters BEFORE insertion. Duplicates count; equal boundary ties do not replace.
     * Output must hold limit slots and must not alias source or decisions, even for limit zero.
     * Invalid inputs leave output unchanged. No concurrent mutation of inputs is permitted.
     */
    public static int topK(int[] source, int from, int to, long[] decisions,
            int[] output, int outputFrom, int limit, boolean largest) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, limit, output.length);
        if (source == output || (Object) source == decisions || (Object) output == decisions) {
            throw new IllegalArgumentException("unsafe alias");
        }
        if (decisions != null) { M3PackedAdmissionMask.require(decisions, to - from); }
        if (limit == 0) { return 0; }
        int admitted = decisions == null ? to - from : M3PackedAdmissionMask.count(decisions, to - from);
        boolean all = admitted == to - from;
        boolean sparse = admitted <= (to - from) / 16;
        int size = 0;
        for (int i = from; i < to; i++) {
            if (!all && !M3PackedAdmissionMask.selected(decisions, i - from)) {
                if (!sparse) { continue; }
                int bit = M3PackedAdmissionMask.nextSelected(decisions, i - from, to - from);
                if (bit == to - from) { break; }
                i = from + bit;
            }
            int value = source[i];
            if (size < limit) {
                output[outputFrom + size++] = value;
                if (size == limit) { heap(output, outputFrom, size, largest); }
            } else {
                int comparison = Integer.compare(value, output[outputFrom]);
                if (largest ? comparison > 0 : comparison < 0) {
                    output[outputFrom] = value;
                    down(output, outputFrom, size, 0, largest);
                }
            }
        }
        if (size < limit) { heap(output, outputFrom, size, largest); }
        return size;
    }

    private static void heap(int[] values, int from, int size, boolean minimum) {
        for (int i = size / 2; i > 0; i--) { down(values, from, size, i - 1, minimum); }
    }

    private static void down(int[] values, int from, int size, int index, boolean minimum) {
        int value = values[from + index];
        while (index < size / 2) {
            int child = index * 2 + 1;
            int right = child + 1;
            if (right < size && before(values[from + right], values[from + child], minimum)) { child = right; }
            if (!before(values[from + child], value, minimum)) { break; }
            values[from + index] = values[from + child];
            index = child;
        }
        values[from + index] = value;
    }

    private static boolean before(int left, int right, boolean minimum) {
        int comparison = Integer.compare(left, right);
        return minimum ? comparison < 0 : comparison > 0;
    }

    /** Heapifies the half-open range in place. No allocation; a true flag selects a min-heap. */
    public static void heapify(long[] values, int from, int to, boolean minimum) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        heap(values, from, to - from, minimum);
    }

    /** In-place ascending heapsort. Equal values have no stability or NaN-payload-order guarantee. */
    public static void sort(long[] values, int from, int to) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        int size = to - from;
        heap(values, from, size, false);
        while (size > 1) {
            long value = values[from];
            values[from] = values[from + size - 1];
            values[from + size - 1] = value;
            down(values, from, --size, 0, false);
        }
    }

    /** Unfiltered one-shot Top-K, returning the retained count in heap order. */
    public static int topK(long[] source, int from, int to, long[] output,
            int outputFrom, int limit, boolean largest) {
        return topK(source, from, to, null, output, outputFrom, limit, largest);
    }

    /**
     * Bounded Top-K with optional admission bits relative to {@code from}; null admits all.
     * Filters BEFORE insertion. Duplicates count; equal boundary ties do not replace.
     * Output must hold limit slots and must not alias source or decisions, even for limit zero.
     * Invalid inputs leave output unchanged. No concurrent mutation of inputs is permitted.
     */
    public static int topK(long[] source, int from, int to, long[] decisions,
            long[] output, int outputFrom, int limit, boolean largest) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, limit, output.length);
        if (source == output || (Object) source == decisions || (Object) output == decisions) {
            throw new IllegalArgumentException("unsafe alias");
        }
        if (decisions != null) { M3PackedAdmissionMask.require(decisions, to - from); }
        if (limit == 0) { return 0; }
        int admitted = decisions == null ? to - from : M3PackedAdmissionMask.count(decisions, to - from);
        boolean all = admitted == to - from;
        boolean sparse = admitted <= (to - from) / 16;
        int size = 0;
        for (int i = from; i < to; i++) {
            if (!all && !M3PackedAdmissionMask.selected(decisions, i - from)) {
                if (!sparse) { continue; }
                int bit = M3PackedAdmissionMask.nextSelected(decisions, i - from, to - from);
                if (bit == to - from) { break; }
                i = from + bit;
            }
            long value = source[i];
            if (size < limit) {
                output[outputFrom + size++] = value;
                if (size == limit) { heap(output, outputFrom, size, largest); }
            } else {
                int comparison = Long.compare(value, output[outputFrom]);
                if (largest ? comparison > 0 : comparison < 0) {
                    output[outputFrom] = value;
                    down(output, outputFrom, size, 0, largest);
                }
            }
        }
        if (size < limit) { heap(output, outputFrom, size, largest); }
        return size;
    }

    private static void heap(long[] values, int from, int size, boolean minimum) {
        for (int i = size / 2; i > 0; i--) { down(values, from, size, i - 1, minimum); }
    }

    private static void down(long[] values, int from, int size, int index, boolean minimum) {
        long value = values[from + index];
        while (index < size / 2) {
            int child = index * 2 + 1;
            int right = child + 1;
            if (right < size && before(values[from + right], values[from + child], minimum)) { child = right; }
            if (!before(values[from + child], value, minimum)) { break; }
            values[from + index] = values[from + child];
            index = child;
        }
        values[from + index] = value;
    }

    private static boolean before(long left, long right, boolean minimum) {
        int comparison = Long.compare(left, right);
        return minimum ? comparison < 0 : comparison > 0;
    }

    /** Heapifies the half-open range in place. No allocation; a true flag selects a min-heap. */
    public static void heapify(float[] values, int from, int to, boolean minimum) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        heap(values, from, to - from, minimum);
    }

    /** In-place ascending heapsort. Equal values have no stability or NaN-payload-order guarantee. */
    public static void sort(float[] values, int from, int to) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        int size = to - from;
        heap(values, from, size, false);
        while (size > 1) {
            float value = values[from];
            values[from] = values[from + size - 1];
            values[from + size - 1] = value;
            down(values, from, --size, 0, false);
        }
    }

    /** Unfiltered one-shot Top-K, returning the retained count in heap order. */
    public static int topK(float[] source, int from, int to, float[] output,
            int outputFrom, int limit, boolean largest) {
        return topK(source, from, to, null, output, outputFrom, limit, largest);
    }

    /**
     * Bounded Top-K with optional admission bits relative to {@code from}; null admits all.
     * Filters BEFORE insertion. Duplicates count; equal boundary ties do not replace.
     * Output must hold limit slots and must not alias source or decisions, even for limit zero.
     * Invalid inputs leave output unchanged. No concurrent mutation of inputs is permitted.
     */
    public static int topK(float[] source, int from, int to, long[] decisions,
            float[] output, int outputFrom, int limit, boolean largest) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, limit, output.length);
        if (source == output || (Object) source == decisions || (Object) output == decisions) {
            throw new IllegalArgumentException("unsafe alias");
        }
        if (decisions != null) { M3PackedAdmissionMask.require(decisions, to - from); }
        if (limit == 0) { return 0; }
        int admitted = decisions == null ? to - from : M3PackedAdmissionMask.count(decisions, to - from);
        boolean all = admitted == to - from;
        boolean sparse = admitted <= (to - from) / 16;
        int size = 0;
        for (int i = from; i < to; i++) {
            if (!all && !M3PackedAdmissionMask.selected(decisions, i - from)) {
                if (!sparse) { continue; }
                int bit = M3PackedAdmissionMask.nextSelected(decisions, i - from, to - from);
                if (bit == to - from) { break; }
                i = from + bit;
            }
            float value = source[i];
            if (size < limit) {
                output[outputFrom + size++] = value;
                if (size == limit) { heap(output, outputFrom, size, largest); }
            } else {
                int comparison = Float.compare(value, output[outputFrom]);
                if (largest ? comparison > 0 : comparison < 0) {
                    output[outputFrom] = value;
                    down(output, outputFrom, size, 0, largest);
                }
            }
        }
        if (size < limit) { heap(output, outputFrom, size, largest); }
        return size;
    }

    private static void heap(float[] values, int from, int size, boolean minimum) {
        for (int i = size / 2; i > 0; i--) { down(values, from, size, i - 1, minimum); }
    }

    private static void down(float[] values, int from, int size, int index, boolean minimum) {
        float value = values[from + index];
        while (index < size / 2) {
            int child = index * 2 + 1;
            int right = child + 1;
            if (right < size && before(values[from + right], values[from + child], minimum)) { child = right; }
            if (!before(values[from + child], value, minimum)) { break; }
            values[from + index] = values[from + child];
            index = child;
        }
        values[from + index] = value;
    }

    private static boolean before(float left, float right, boolean minimum) {
        int comparison = Float.compare(left, right);
        return minimum ? comparison < 0 : comparison > 0;
    }

    /** Heapifies the half-open range in place. No allocation; a true flag selects a min-heap. */
    public static void heapify(double[] values, int from, int to, boolean minimum) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        heap(values, from, to - from, minimum);
    }

    /** In-place ascending heapsort. Equal values have no stability or NaN-payload-order guarantee. */
    public static void sort(double[] values, int from, int to) {
        Objects.requireNonNull(values, "values");
        Objects.checkFromToIndex(from, to, values.length);
        int size = to - from;
        heap(values, from, size, false);
        while (size > 1) {
            double value = values[from];
            values[from] = values[from + size - 1];
            values[from + size - 1] = value;
            down(values, from, --size, 0, false);
        }
    }

    /** Unfiltered one-shot Top-K, returning the retained count in heap order. */
    public static int topK(double[] source, int from, int to, double[] output,
            int outputFrom, int limit, boolean largest) {
        return topK(source, from, to, null, output, outputFrom, limit, largest);
    }

    /**
     * Bounded Top-K with optional admission bits relative to {@code from}; null admits all.
     * Filters BEFORE insertion. Duplicates count; equal boundary ties do not replace.
     * Output must hold limit slots and must not alias source or decisions, even for limit zero.
     * Invalid inputs leave output unchanged. No concurrent mutation of inputs is permitted.
     */
    public static int topK(double[] source, int from, int to, long[] decisions,
            double[] output, int outputFrom, int limit, boolean largest) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, limit, output.length);
        if (source == output || (Object) source == decisions || (Object) output == decisions) {
            throw new IllegalArgumentException("unsafe alias");
        }
        if (decisions != null) { M3PackedAdmissionMask.require(decisions, to - from); }
        if (limit == 0) { return 0; }
        int admitted = decisions == null ? to - from : M3PackedAdmissionMask.count(decisions, to - from);
        boolean all = admitted == to - from;
        boolean sparse = admitted <= (to - from) / 16;
        int size = 0;
        for (int i = from; i < to; i++) {
            if (!all && !M3PackedAdmissionMask.selected(decisions, i - from)) {
                if (!sparse) { continue; }
                int bit = M3PackedAdmissionMask.nextSelected(decisions, i - from, to - from);
                if (bit == to - from) { break; }
                i = from + bit;
            }
            double value = source[i];
            if (size < limit) {
                output[outputFrom + size++] = value;
                if (size == limit) { heap(output, outputFrom, size, largest); }
            } else {
                int comparison = Double.compare(value, output[outputFrom]);
                if (largest ? comparison > 0 : comparison < 0) {
                    output[outputFrom] = value;
                    down(output, outputFrom, size, 0, largest);
                }
            }
        }
        if (size < limit) { heap(output, outputFrom, size, largest); }
        return size;
    }

    private static void heap(double[] values, int from, int size, boolean minimum) {
        for (int i = size / 2; i > 0; i--) { down(values, from, size, i - 1, minimum); }
    }

    private static void down(double[] values, int from, int size, int index, boolean minimum) {
        double value = values[from + index];
        while (index < size / 2) {
            int child = index * 2 + 1;
            int right = child + 1;
            if (right < size && before(values[from + right], values[from + child], minimum)) { child = right; }
            if (!before(values[from + child], value, minimum)) { break; }
            values[from + index] = values[from + child];
            index = child;
        }
        values[from + index] = value;
    }

    private static boolean before(double left, double right, boolean minimum) {
        int comparison = Double.compare(left, right);
        return minimum ? comparison < 0 : comparison > 0;
    }
}
