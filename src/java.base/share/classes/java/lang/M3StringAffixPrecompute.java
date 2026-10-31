/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded, frozen, object-free prefix and reversed-suffix trie geometry over canonical
 * M3 String coordinates. Adapted from Synexia IndexAffixIndex / IndexAffixTrie.
 *
 * <p>Two primitive lexical orderings identify contiguous prefix/suffix subtrees. The
 * parent and end lanes describe dictionary-word nodes; no trie node objects, second
 * character store, or Java String spellings survive construction. Duplicate spellings
 * retain distinct source rows, and ordinal queries return original row order.
 * Unicode comparisons are exact UTF-16 code-unit comparisons, not case-folded or
 * normalized. The caller owns a stable immutable directory throughout this lifetime.
 * Construction is optional under memory pressure; callers must retain exact fallback.
 */
final class M3StringAffixPrecompute {
    private static final long MAX_PRIMITIVE_BYTES = 32L << 20;
    private static final int LANE_COUNT = 10;

    record Range(int from, int to) {
        Range {
            if (from < 0 || to < from) throw new IllegalArgumentException("invalid affix range");
        }
        int size() { return to - from; }
    }

    private final M3String[] values;
    private final int[] prefixRows;
    private final int[] suffixRows;
    private final int[] prefixRank;
    private final int[] suffixRank;
    private final int[] prefixEnd;
    private final int[] suffixEnd;
    private final int[] prefixBegin;
    private final int[] suffixBegin;
    private final int[] prefixParent;
    private final int[] suffixParent;

    private M3StringAffixPrecompute(M3String[] values) {
        this.values = values;
        int size = values.length;
        int[] scratch = new int[size];
        prefixRows = identity(size);
        suffixRows = identity(size);
        sort(prefixRows, scratch, false);
        sort(suffixRows, scratch, true);
        prefixRank = inverse(prefixRows);
        suffixRank = inverse(suffixRows);
        prefixEnd = new int[size];
        suffixEnd = new int[size];
        prefixBegin = new int[size];
        suffixBegin = new int[size];
        prefixParent = new int[size];
        suffixParent = new int[size];
        geometry(prefixRows, prefixBegin, prefixEnd, prefixParent, scratch, false);
        geometry(suffixRows, suffixBegin, suffixEnd, suffixParent, scratch, true);
    }

    /** Admits only bounded metadata, never a second spelling owner. Null means fallback. */
    static M3StringAffixPrecompute tryPrepare(M3String[] values) {
        Objects.requireNonNull(values, "values");
        for (M3String value : values) Objects.requireNonNull(value, "M3 String coordinate");
        long bytes = (long) values.length * LANE_COUNT * Integer.BYTES;
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
        long remaining = Math.max(0L, max - used);
        long budget = Math.min(MAX_PRIMITIVE_BYTES,
                Math.min(max >>> 5, remaining >>> 2));
        // Include the one temporary primitive merge/stack lane in the construction budget.
        if (bytes + (long) values.length * Integer.BYTES > budget) return null;
        try {
            return new M3StringAffixPrecompute(values);
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
    }

    int size() { return values.length; }
    long primitiveBytes() { return (long) size() * LANE_COUNT * Integer.BYTES; }
    int prefixParent(int row) { return prefixParent[Objects.checkIndex(row, size())]; }
    int suffixParent(int row) { return suffixParent[Objects.checkIndex(row, size())]; }
    int prefixRank(int row) { return prefixRank[Objects.checkIndex(row, size())]; }
    int suffixRank(int row) { return suffixRank[Objects.checkIndex(row, size())]; }
    int prefixBegin(int row) { return prefixBegin[Objects.checkIndex(row, size())]; }
    int suffixBegin(int row) { return suffixBegin[Objects.checkIndex(row, size())]; }
    int prefixEnd(int row) { return prefixEnd[Objects.checkIndex(row, size())]; }
    int suffixEnd(int row) { return suffixEnd[Objects.checkIndex(row, size())]; }

    Range prefixRange(CharSequence prefix) { return range(prefix, false); }
    Range suffixRange(CharSequence suffix) { return range(suffix, true); }

    /** Materializes row coordinates only; the canonical text payload stays in its owner. */
    int[] rowsInSourceOrder(Range range, boolean reversed) {
        Objects.requireNonNull(range, "range");
        Objects.checkFromToIndex(range.from(), range.to(), size());
        int[] order = reversed ? suffixRows : prefixRows;
        int[] selected = Arrays.copyOfRange(order, range.from(), range.to());
        Arrays.sort(selected);
        return selected;
    }

    private Range range(CharSequence query, boolean reversed) {
        Objects.requireNonNull(query, "query");
        int[] order = reversed ? suffixRows : prefixRows;
        int low = 0, high = order.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (compareQuery(values[order[middle]], query, reversed) < 0) low = middle + 1;
            else high = middle;
        }
        int start = low;
        high = order.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (compareQuery(values[order[middle]], query, reversed) <= 0) low = middle + 1;
            else high = middle;
        }
        return new Range(start, low);
    }

    private void geometry(int[] order, int[] begin, int[] end, int[] parent,
                          int[] stack, boolean reversed) {
        int depth = 0;
        for (int rank = 0; rank < order.length; rank++) {
            int row = order[rank];
            if (rank > 0 && values[order[rank - 1]].length() == values[row].length()
                    && isAffix(values[order[rank - 1]], values[row], reversed)) {
                begin[row] = begin[order[rank - 1]];
            } else {
                begin[row] = rank;
            }
            while (depth > 0 && !isAffix(values[stack[depth - 1]], values[row], reversed)) {
                end[stack[--depth]] = rank;
            }
            if (depth == 0) {
                parent[row] = -1;
            } else {
                int previous = stack[depth - 1];
                parent[row] = values[previous].length() < values[row].length()
                        ? previous : parent[previous];
            }
            stack[depth++] = row;
        }
        while (depth > 0) end[stack[--depth]] = order.length;
    }

    private boolean isAffix(M3String prefix, M3String word, boolean reverse) {
        if (prefix.length() > word.length()) return false;
        for (int i = 0; i < prefix.length(); i++) {
            char a = prefix.charAt(reverse ? prefix.length() - 1 - i : i);
            char b = word.charAt(reverse ? word.length() - 1 - i : i);
            if (a != b) return false;
        }
        return true;
    }

    private static int compareQuery(M3String word, CharSequence query, boolean reversed) {
        int matched = Math.min(word.length(), query.length());
        for (int i = 0; i < matched; i++) {
            char a = word.charAt(reversed ? word.length() - 1 - i : i);
            char b = query.charAt(reversed ? query.length() - 1 - i : i);
            if (a != b) return Character.compare(a, b);
        }
        return word.length() < query.length() ? -1 : 0;
    }

    private int compareRows(int left, int right, boolean reversed) {
        M3String a = values[left], b = values[right];
        int matched = Math.min(a.length(), b.length());
        for (int i = 0; i < matched; i++) {
            char u = a.charAt(reversed ? a.length() - 1 - i : i);
            char v = b.charAt(reversed ? b.length() - 1 - i : i);
            if (u != v) return Character.compare(u, v);
        }
        int compared = Integer.compare(a.length(), b.length());
        return compared != 0 ? compared : Integer.compare(left, right);
    }

    private void sort(int[] order, int[] scratch, boolean reversed) {
        int[] from = order, to = scratch;
        int size = order.length;
        for (long step = 1L; step < size; step <<= 1) {
            int width = (int) step;
            for (int base = 0; base < size;) {
                int middle = (int) Math.min((long) size, (long) base + width);
                int end = (int) Math.min((long) size, (long) base + width * 2L);
                int i = base, j = middle;
                for (int k = base; k < end; k++) {
                    if (i == middle) to[k] = from[j++];
                    else if (j == end) to[k] = from[i++];
                    else if (compareRows(from[i], from[j], reversed) <= 0) to[k] = from[i++];
                    else to[k] = from[j++];
                }
                base = end;
            }
            int[] swap = from; from = to; to = swap;
        }
        if (from != order) System.arraycopy(from, 0, order, 0, size);
    }

    private static int[] identity(int size) {
        int[] rows = new int[size];
        for (int i = 0; i < size; i++) rows[i] = i;
        return rows;
    }

    private static int[] inverse(int[] order) {
        int[] result = new int[order.length];
        for (int rank = 0; rank < order.length; rank++) result[order[rank]] = rank;
        return result;
    }
}
