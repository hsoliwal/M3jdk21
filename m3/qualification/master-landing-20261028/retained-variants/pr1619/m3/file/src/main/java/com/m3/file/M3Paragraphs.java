// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

/**
 * Immutable grammar metadata in parallel primitive arrays, with offset/index views.
 * No paragraph text or per-row structural objects are retained. Filter predicates
 * receive logical ordinals; rowIds() exposes stable physical metadata row numbers.
 * Result views are safe for concurrent reads; callbacks remain caller-owned.
 */
public final class M3Paragraphs {
    public static final int NON_ASCII = 1;
    public static final int SURROGATE = 2;
    public static final int TAB = 4;
    public static final int MULTILINE = 8;
    private final Data data;
    private final int[] order;
    private final int from;
    private final int to;
    private final int direction;

    private M3Paragraphs(Data data, int[] order, int from, int to, int direction) {
        this.data = data;
        this.order = order;
        this.from = from;
        this.to = to;
        this.direction = direction;
    }

    public int size() { return to - from; }
    public long observedUtf16Units() { return data.units; }
    public int physicalRow(int ordinal) {
        int index = from + Objects.checkIndex(ordinal, size());
        return order == null ? index : order[index];
    }
    public long start(int ordinal) { return data.starts[physicalRow(ordinal)]; }
    public long end(int ordinal) { return data.ends[physicalRow(ordinal)]; }
    public long length(int ordinal) { return end(ordinal) - start(ordinal); }
    public long firstLine(int ordinal) { return data.firstLines[physicalRow(ordinal)]; }
    public long lastLine(int ordinal) { return data.lastLines[physicalRow(ordinal)]; }
    public long whitespaceTokens(int ordinal) { return data.tokens[physicalRow(ordinal)]; }
    public int flags(int ordinal) { return data.flags[physicalRow(ordinal)] & 0xff; }
    public IntStream rowIds() { return IntStream.range(0, size()).map(this::physicalRow); }

    /** Shares the same immutable metadata and order; arguments are half-open ordinals. */
    public M3Paragraphs slice(int first, int end) {
        Objects.checkFromToIndex(first, end, size());
        return new M3Paragraphs(data, order, from + first, from + end, direction);
    }

    /** Evaluates each logical ordinal exactly once, serially in encounter order. */
    public M3Paragraphs filter(IntPredicate keepOrdinal) {
        Objects.requireNonNull(keepOrdinal, "keepOrdinal");
        int[] selected = new int[size()];
        int count = 0;
        for (int i = 0; i < size(); i++) {
            if (keepOrdinal.test(i)) selected[count++] = physicalRow(i);
        }
        if (count == size()) return this;
        return new M3Paragraphs(data, Arrays.copyOf(selected, count), 0, count, direction);
    }

    /** Required bits are all present and excluded bits are all absent. */
    public M3Paragraphs filterMask(int required, int excluded) {
        if (((required | excluded) & ~0xff) != 0) {
            throw new IllegalArgumentException("masks must fit an unsigned byte");
        }
        if ((required & excluded) != 0) return slice(0, 0);
        if ((required | excluded) == 0) return this;
        return filter(i -> (flags(i) & required) == required && (flags(i) & excluded) == 0);
    }

    /** Stable length ordering, permuting only int row IDs, never metadata/payload. */
    public M3Paragraphs sortedByLength(boolean descending) {
        int requested = descending ? -1 : 1;
        if (direction == requested) return this;
        int n = size();
        if (n < 2) return new M3Paragraphs(data, order, from, to, requested);
        int[] current = rowIds().toArray();
        int[] scratch = new int[n];
        // Long geometry prevents doubling/index overflow near the maximum array size.
        for (long width = 1; width < n; width *= 2) {
            for (long base = 0; base < n; base += 2 * width) {
                int a = (int) base;
                int middle = (int) Math.min(base + width, n);
                int b = middle;
                int end = (int) Math.min(base + 2 * width, n);
                int write = a;
                while (a < middle && b < end) {
                    int cmp = Long.compare(rowLength(current[a]), rowLength(current[b]));
                    boolean left = descending ? cmp >= 0 : cmp <= 0;
                    scratch[write++] = left ? current[a++] : current[b++];
                }
                while (a < middle) scratch[write++] = current[a++];
                while (b < end) scratch[write++] = current[b++];
            }
            int[] swap = current;
            current = scratch;
            scratch = swap;
        }
        return new M3Paragraphs(data, current, 0, n, requested);
    }

    private long rowLength(int row) { return data.ends[row] - data.starts[row]; }

    private static final class Data {
        final long[] starts;
        final long[] ends;
        final long[] firstLines;
        final long[] lastLines;
        final long[] tokens;
        final byte[] flags;
        final long units;

        Data(Builder source, long units) {
            starts = Arrays.copyOf(source.starts, source.size);
            ends = Arrays.copyOf(source.ends, source.size);
            firstLines = Arrays.copyOf(source.firstLines, source.size);
            lastLines = Arrays.copyOf(source.lastLines, source.size);
            tokens = Arrays.copyOf(source.tokens, source.size);
            flags = Arrays.copyOf(source.flags, source.size);
            this.units = units;
        }
    }

    static final class Builder {
        private static final int MAX_ROWS = Integer.MAX_VALUE - 8;
        private final int limit;
        private long[] starts;
        private long[] ends;
        private long[] firstLines;
        private long[] lastLines;
        private long[] tokens;
        private byte[] flags;
        private int size;

        Builder(int limit) {
            if (limit < 0 || limit > MAX_ROWS) throw new IllegalArgumentException("invalid paragraph budget");
            this.limit = limit;
            int capacity = Math.min(16, limit);
            starts = new long[capacity];
            ends = new long[capacity];
            firstLines = new long[capacity];
            lastLines = new long[capacity];
            tokens = new long[capacity];
            flags = new byte[capacity];
        }

        void add(long start, long end, long first, long last, long count, int state) {
            if (size == limit) throw new IllegalStateException("grammar paragraph budget exceeded");
            if (size == starts.length) grow();
            starts[size] = start;
            ends[size] = end;
            firstLines[size] = first;
            lastLines[size] = last;
            tokens[size] = count;
            flags[size++] = (byte) state;
        }

        M3Paragraphs freeze(long units) {
            M3Paragraphs result = new M3Paragraphs(new Data(this, units), null, 0, size, 0);
            // Transfer to exact immutable arrays, releasing the observer's growth capacity.
            starts = ends = firstLines = lastLines = tokens = new long[0];
            flags = new byte[0];
            return result;
        }

        private void grow() {
            int capacity = (int) Math.min(limit, Math.max(1L, starts.length * 2L));
            starts = Arrays.copyOf(starts, capacity);
            ends = Arrays.copyOf(ends, capacity);
            firstLines = Arrays.copyOf(firstLines, capacity);
            lastLines = Arrays.copyOf(lastLines, capacity);
            tokens = Arrays.copyOf(tokens, capacity);
            flags = Arrays.copyOf(flags, capacity);
        }
    }
}
