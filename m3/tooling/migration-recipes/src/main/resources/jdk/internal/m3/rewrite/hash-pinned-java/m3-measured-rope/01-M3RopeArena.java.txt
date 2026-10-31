/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.precompute;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

/**
 * Append-only node arena for {@link M3MeasuredRope} values.
 *
 * <p>Nodes live in parallel primitive lanes (no stored node objects): {@code left}/{@code right} child ids
 * ({@code -1} marks a leaf), {@code units} (UTF-16 length), {@code height}, and for a leaf the immutable
 * {@code String} it slices plus the slice start. Each registered {@link M3RopeMeasure} owns one {@code long}
 * lane holding {@code width()} slots per node, computed once when the node is created: a leaf by
 * {@link M3RopeMeasure#leaf}, a branch by {@link M3RopeMeasure#combine} of its children.</p>
 *
 * <p>Nodes are never mutated, so ropes are persistent and share structure; an edit path-copies
 * {@code O(log n)} nodes. Balance follows the AVL height rule of {@code M3StringPool.concatBalanced}.
 * Admitted text is cut into coordinate slices of at most {@code maxLeaf} units (no character copy), so a
 * split re-measures at most {@code maxLeaf} units. The arena never frees nodes; rebuild a rope into a new
 * arena to compact. An arena and its ropes are confined to one thread (queries use a shared scratch lane).</p>
 */
public final class M3RopeArena {
    static final int EMPTY = -1;
    private static final int MAX_DEPTH = 96;

    private final int maxLeaf;
    private final M3RopeMeasure[] measures;
    private final int[] widths;
    private final M3MeasuredRope empty;
    private int count;
    private int[] left;
    private int[] right;
    private int[] units;
    private byte[] height;
    private String[] text;
    private int[] start;
    private long[][] lanes;
    private final long[] scratch;
    private int top;

    /** Creates an arena whose nodes carry one summary per measure, with leaves of at most {@code maxLeaf}. */
    public M3RopeArena(int maxLeaf, M3RopeMeasure... measures) {
        if (maxLeaf < 1) throw new IllegalArgumentException("maxLeaf < 1: " + maxLeaf);
        this.maxLeaf = maxLeaf;
        this.measures = measures.clone();
        this.widths = new int[this.measures.length];
        int widest = 1;
        for (int m = 0; m < this.measures.length; m++) {
            M3RopeMeasure measure = Objects.requireNonNull(this.measures[m], "measure");
            for (int k = 0; k < m; k++) {
                if (this.measures[k] == measure) throw new IllegalArgumentException("duplicate measure");
            }
            if (measure.width() < 1) throw new IllegalArgumentException("measure width < 1");
            widths[m] = measure.width();
            widest = Math.max(widest, widths[m]);
        }
        int capacity = 16;
        left = new int[capacity];
        right = new int[capacity];
        units = new int[capacity];
        height = new byte[capacity];
        text = new String[capacity];
        start = new int[capacity];
        lanes = new long[this.measures.length][];
        for (int m = 0; m < lanes.length; m++) lanes[m] = new long[capacity * widths[m]];
        scratch = new long[widest * (2 * MAX_DEPTH + 4)];
        empty = new M3MeasuredRope(this, EMPTY);
    }

    /** Leaf size cap in UTF-16 units. */
    public int maxLeaf() { return maxLeaf; }

    /** Number of nodes ever created in this arena (append-only). */
    public int nodeCount() { return count; }

    /** The empty rope of this arena. */
    public M3MeasuredRope empty() { return empty; }

    /** A rope over an immutable snapshot of {@code value} (a {@code String} is used as is, never copied). */
    public M3MeasuredRope of(CharSequence value) {
        return wrap(admit(value));
    }

    /** A balanced rope over the chunks in order, each admitted like {@link #of} (Swift {@code Rope.Builder}). */
    public M3MeasuredRope ofChunks(CharSequence... chunks) {
        int[] roots = new int[chunks.length];
        for (int i = 0; i < chunks.length; i++) roots[i] = admit(chunks[i]);
        return wrap(joinAll(roots, 0, roots.length));
    }

    M3MeasuredRope wrap(int root) {
        return root == EMPTY ? empty : new M3MeasuredRope(this, root);
    }

    int measureIndex(M3RopeMeasure measure) {
        for (int m = 0; m < measures.length; m++) if (measures[m] == measure) return m;
        throw new IllegalArgumentException("measure not registered in this arena");
    }

    int units(int n) { return n == EMPTY ? 0 : units[n]; }

    int height(int n) { return n == EMPTY ? 0 : height[n]; }

    int admit(CharSequence value) {
        String s = Objects.requireNonNull(value, "text").toString();
        int n = s.length();
        if (n == 0) return EMPTY;
        int pieces = n / maxLeaf + (n % maxLeaf == 0 ? 0 : 1);
        return build(s, 0, pieces);
    }

    private int build(String s, int firstPiece, int pieceCount) {
        if (pieceCount == 1) {
            int from = (int) Math.min(s.length(), (long) firstPiece * maxLeaf);
            return leaf(s, from, Math.min(s.length() - from, maxLeaf));
        }
        int half = pieceCount >>> 1;
        int l = build(s, firstPiece, half);
        return branch(l, build(s, firstPiece + half, pieceCount - half));
    }

    private int joinAll(int[] roots, int from, int to) {
        if (to - from == 0) return EMPTY;
        if (to - from == 1) return roots[from];
        int mid = (from + to) >>> 1;
        return join(joinAll(roots, from, mid), joinAll(roots, mid, to));
    }

    private int newNode() {
        if (count == left.length) {
            int capacity = Math.addExact(count, (count >> 1) + 16);
            left = Arrays.copyOf(left, capacity);
            right = Arrays.copyOf(right, capacity);
            units = Arrays.copyOf(units, capacity);
            height = Arrays.copyOf(height, capacity);
            text = Arrays.copyOf(text, capacity);
            start = Arrays.copyOf(start, capacity);
            for (int m = 0; m < lanes.length; m++) {
                lanes[m] = Arrays.copyOf(lanes[m], Math.multiplyExact(capacity, widths[m]));
            }
        }
        return count++;
    }

    private int leaf(String s, int from, int length) {
        int n = newNode();
        left[n] = EMPTY;
        right[n] = EMPTY;
        units[n] = length;
        height[n] = 1;
        text[n] = s;
        start[n] = from;
        for (int m = 0; m < measures.length; m++) {
            measures[m].leaf(s, from, from + length, lanes[m], n * widths[m]);
        }
        return n;
    }

    private int branch(int l, int r) {
        int n = newNode();
        left[n] = l;
        right[n] = r;
        units[n] = Math.addExact(units[l], units[r]);
        height[n] = (byte) (1 + Math.max(height[l], height[r]));
        for (int m = 0; m < measures.length; m++) {
            int w = widths[m];
            long[] lane = lanes[m];
            measures[m].combine(lane, l * w, lane, r * w, lane, n * w);
        }
        return n;
    }

    /** AVL concatenation (same height rule as {@code M3StringPool.concatBalanced}). */
    int join(int a, int b) {
        if (a == EMPTY) return b;
        if (b == EMPTY) return a;
        int ha = height[a];
        int hb = height[b];
        if (ha > hb + 1) return balance(left[a], join(right[a], b));
        if (hb > ha + 1) return balance(join(a, left[b]), right[b]);
        return branch(a, b);
    }

    private int balance(int l, int r) {
        int hl = height(l);
        int hr = height(r);
        if (hl > hr + 1) {
            int ll = left[l];
            int lr = right[l];
            if (height(ll) >= height(lr)) return branch(ll, branch(lr, r));
            return branch(branch(ll, left[lr]), branch(right[lr], r));
        }
        if (hr > hl + 1) {
            int rl = left[r];
            int rr = right[r];
            if (height(rr) >= height(rl)) return branch(branch(l, rl), rr);
            return branch(branch(l, left[rl]), branch(right[rl], rr));
        }
        return branch(l, r);
    }

    /** Splits node {@code n} at UTF-16 offset {@code at}; returns {@code (left << 32) | right} node ids. */
    long split(int n, int at) {
        if (n == EMPTY || at <= 0) return pack(EMPTY, n);
        if (at >= units[n]) return pack(n, EMPTY);
        if (left[n] == EMPTY) {
            return pack(leaf(text[n], start[n], at), leaf(text[n], start[n] + at, units[n] - at));
        }
        int l = left[n];
        int r = right[n];
        int lu = units[l];
        if (at < lu) {
            long s = split(l, at);
            return pack(first(s), join(second(s), r));
        }
        if (at > lu) {
            long s = split(r, at - lu);
            return pack(join(l, first(s)), second(s));
        }
        return pack(l, r);
    }

    int slice(int n, int from, int to) {
        return first(split(second(split(n, from)), to - from));
    }

    static long pack(int a, int b) { return ((long) a << 32) | (b & 0xFFFF_FFFFL); }

    static int first(long pair) { return (int) (pair >> 32); }

    static int second(long pair) { return (int) pair; }

    char charAt(int n, int index) {
        int node = n;
        int offset = index;
        while (left[node] != EMPTY) {
            int lu = units[left[node]];
            if (offset < lu) {
                node = left[node];
            } else {
                offset -= lu;
                node = right[node];
            }
        }
        return text[node].charAt(start[node] + offset);
    }

    void getChars(int n, int from, int to, char[] destination, int at) {
        if (from >= to) return;
        if (left[n] == EMPTY) {
            text[n].getChars(start[n] + from, start[n] + to, destination, at);
            return;
        }
        int lu = units[left[n]];
        if (to <= lu) {
            getChars(left[n], from, to, destination, at);
        } else if (from >= lu) {
            getChars(right[n], from - lu, to - lu, destination, at);
        } else {
            getChars(left[n], from, lu, destination, at);
            getChars(right[n], 0, to - lu, destination, at + lu - from);
        }
    }

    void forEachLeaf(int n, boolean reverse, M3MeasuredRope.LeafVisitor visitor) {
        if (n == EMPTY) return;
        if (left[n] == EMPTY) {
            visitor.leaf(text[n], start[n], start[n] + units[n]);
            return;
        }
        forEachLeaf(reverse ? right[n] : left[n], reverse, visitor);
        forEachLeaf(reverse ? left[n] : right[n], reverse, visitor);
    }

    int leafCount(int n) {
        if (n == EMPTY) return 0;
        return left[n] == EMPTY ? 1 : leafCount(left[n]) + leafCount(right[n]);
    }

    void copySummary(int n, int m, long[] out, int at) {
        if (n == EMPTY) {
            measures[m].identity(out, at);
        } else {
            System.arraycopy(lanes[m], n * widths[m], out, at, widths[m]);
        }
    }

    void rangeSummary(int n, int m, int from, int to, long[] out, int at) {
        if (n == EMPTY || from == to) {
            measures[m].identity(out, at);
            return;
        }
        top = 0;
        int dst = push(widths[m]);
        rangeInto(n, m, from, to, dst);
        System.arraycopy(scratch, dst, out, at, widths[m]);
    }

    private void rangeInto(int n, int m, int from, int to, int dst) {
        int w = widths[m];
        if (from == 0 && to == units[n]) {
            System.arraycopy(lanes[m], n * w, scratch, dst, w);
        } else if (left[n] == EMPTY) {
            measures[m].leaf(text[n], start[n] + from, start[n] + to, scratch, dst);
        } else {
            int lu = units[left[n]];
            if (to <= lu) {
                rangeInto(left[n], m, from, to, dst);
            } else if (from >= lu) {
                rangeInto(right[n], m, from - lu, to - lu, dst);
            } else {
                int a = push(w);
                int b = push(w);
                rangeInto(left[n], m, from, lu, a);
                rangeInto(right[n], m, 0, to - lu, b);
                measures[m].combine(scratch, a, scratch, b, scratch, dst);
                top -= 2 * w;
            }
        }
    }

    int seek(int n, int m, long target) {
        if (n == EMPTY) return 0;
        M3RopeMetric metric = (M3RopeMetric) measures[m];
        int w = widths[m];
        long[] lane = lanes[m];
        if (metric.size(lane, n * w) <= target) return units[n];
        top = 0;
        int acc = push(w);
        int candidate = push(w);
        int unit = push(w);
        metric.identity(scratch, acc);
        int node = n;
        int base = 0;
        while (left[node] != EMPTY) {
            int l = left[node];
            metric.combine(scratch, acc, lane, l * w, scratch, candidate);
            if (metric.size(scratch, candidate) > target) {
                node = l;
            } else {
                int swap = acc;
                acc = candidate;
                candidate = swap;
                base += units[l];
                node = right[node];
            }
        }
        for (int j = 0, f = start[node]; j < units[node]; j++) {
            metric.leaf(text[node], f + j, f + j + 1, scratch, unit);
            metric.combine(scratch, acc, scratch, unit, scratch, candidate);
            if (metric.size(scratch, candidate) > target) return base + j + 1;
            int swap = acc;
            acc = candidate;
            candidate = swap;
        }
        return base + units[node];
    }

    private int push(int w) {
        if (top + w > scratch.length) throw new IllegalStateException("rope deeper than " + MAX_DEPTH);
        int at = top;
        top += w;
        return at;
    }

    int measureCount() { return measures.length; }

    /** Checks that the root summary of measure {@code m} equals the measure of {@code flat} as one leaf. */
    void verifyRootFold(int n, int m, String flat) {
        int w = widths[m];
        long[] whole = new long[w];
        long[] stored = new long[w];
        measures[m].leaf(flat, 0, flat.length(), whole, 0);
        copySummary(n, m, stored, 0);
        if (!Arrays.equals(whole, stored)) {
            throw new IllegalStateException("measure " + m + " root summary != fold of the flat text");
        }
    }

    /** Re-derives every reachable node from its leaves and children; throws on the first mismatch. */
    void verify(int n) {
        verify(n, new BitSet());
    }

    private void verify(int n, BitSet seen) {
        if (n == EMPTY || seen.get(n)) return;
        if (n < 0 || n >= count) throw new IllegalStateException("node id out of arena: " + n);
        seen.set(n);
        boolean leafNode = left[n] == EMPTY;
        if (leafNode) {
            String s = text[n];
            if (right[n] != EMPTY || s == null || height[n] != 1 || units[n] < 1 || units[n] > maxLeaf
                    || start[n] < 0 || start[n] + units[n] > s.length()) {
                throw new IllegalStateException("bad leaf " + n);
            }
        } else {
            int l = left[n];
            int r = right[n];
            if (r == EMPTY) throw new IllegalStateException("branch without right child " + n);
            verify(l, seen);
            verify(r, seen);
            if (units[n] != units[l] + units[r] || height[n] != 1 + Math.max(height[l], height[r])
                    || Math.abs(height[l] - height[r]) > 1) {
                throw new IllegalStateException("bad branch shape " + n);
            }
        }
        for (int m = 0; m < measures.length; m++) {
            int w = widths[m];
            long[] expected = new long[w];
            if (leafNode) {
                measures[m].leaf(text[n], start[n], start[n] + units[n], expected, 0);
            } else {
                measures[m].combine(lanes[m], left[n] * w, lanes[m], right[n] * w, expected, 0);
            }
            if (!Arrays.equals(expected, 0, w, lanes[m], n * w, n * w + w)) {
                throw new IllegalStateException("measure " + m + " fold != combine at node " + n);
            }
        }
    }
}
