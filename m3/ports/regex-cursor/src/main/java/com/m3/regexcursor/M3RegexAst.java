// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;

/**
 * Regex syntax tree in int lanes, shared by the UTF-16 and the token parsers.
 *
 * <p>Node lanes: {@code op}, {@code a} (first child, class id or assertion kind), {@code next}
 * (sibling), {@code min}/{@code max} (repeat bounds, {@code max = -1} unbounded),
 * {@code greedy}. Symbol classes live in one {@code ranges} pool of sorted, merged inclusive
 * {@code (lo, hi)} pairs addressed by {@code classStart}/{@code classPairs}.</p>
 */
final class M3RegexAst {
    static final int CLASS = 1;
    static final int EMPTY = 2;
    static final int CONCAT = 3;
    static final int ALT = 4;
    static final int REPEAT = 5;
    static final int GROUP = 6;
    static final int ASSERT = 7;

    static final int BEGIN = 1;
    static final int END_Z = 2;
    static final int END = 3;

    final int alphabetMax;
    int[] op = new int[16];
    int[] a = new int[16];
    int[] next = new int[16];
    int[] min = new int[16];
    int[] max = new int[16];
    boolean[] greedy = new boolean[16];
    int size;
    int[] ranges = new int[32];
    int rangeSize;
    int[] classStart = new int[8];
    int[] classPairs = new int[8];
    int classCount;
    int groupCount;
    int root;
    int anyClass;
    boolean assertions;

    M3RegexAst(int alphabetMax) {
        this.alphabetMax = alphabetMax;
    }

    int node(int kind, int arg) {
        if (size == op.length) grow();
        op[size] = kind;
        a[size] = arg;
        next[size] = -1;
        return size++;
    }

    private void grow() {
        int capacity = size * 2;
        op = Arrays.copyOf(op, capacity);
        a = Arrays.copyOf(a, capacity);
        next = Arrays.copyOf(next, capacity);
        min = Arrays.copyOf(min, capacity);
        max = Arrays.copyOf(max, capacity);
        greedy = Arrays.copyOf(greedy, capacity);
    }

    /** CONCAT or ALT over {@code children[0..count)}; one child collapses, none is EMPTY. */
    int list(int kind, int[] children, int count) {
        if (count == 0) return node(EMPTY, 0);
        if (count == 1) return children[0];
        int parent = node(kind, children[0]);
        for (int index = 1; index < count; index++) {
            next[children[index - 1]] = children[index];
        }
        next[children[count - 1]] = -1;
        return parent;
    }

    /** Capturing group {@code index} (stored in the {@code min} lane) around {@code child}. */
    int group(int child, int index) {
        int id = node(GROUP, child);
        min[id] = index;
        return id;
    }

    int repeat(int child, int lower, int upper, boolean isGreedy) {
        int id = node(REPEAT, child);
        min[id] = lower;
        max[id] = upper;
        greedy[id] = isGreedy;
        return id;
    }

    /** Class from unsorted pairs {@code (lo, hi)}; sorted, merged, optionally complemented. */
    int addClass(int[] pairs, int pairCount, boolean negate) {
        long[] keyed = new long[pairCount];
        for (int index = 0; index < pairCount; index++) {
            keyed[index] = ((long) pairs[2 * index] << 32) | (pairs[2 * index + 1] & 0xFFFF_FFFFL);
        }
        Arrays.sort(keyed);
        int[] merged = new int[2 * pairCount + 2];
        int count = merge(keyed, merged);
        return negate ? store(complement(merged, count)) : store(Arrays.copyOf(merged, 2 * count));
    }

    private static int merge(long[] keyed, int[] out) {
        int count = 0;
        for (long key : keyed) {
            int lo = (int) (key >>> 32);
            int hi = (int) key;
            if (count > 0 && (long) lo <= (long) out[2 * count - 1] + 1) {
                out[2 * count - 1] = Math.max(out[2 * count - 1], hi);
            } else {
                out[2 * count] = lo;
                out[2 * count + 1] = hi;
                count++;
            }
        }
        return count;
    }

    private int[] complement(int[] merged, int count) {
        int[] out = new int[2 * count + 2];
        int size2 = 0;
        long cursor = 0;
        for (int index = 0; index < count; index++) {
            if (merged[2 * index] > cursor) {
                out[size2++] = (int) cursor;
                out[size2++] = merged[2 * index] - 1;
            }
            cursor = (long) merged[2 * index + 1] + 1;
        }
        if (cursor <= alphabetMax) {
            out[size2++] = (int) cursor;
            out[size2++] = alphabetMax;
        }
        return Arrays.copyOf(out, size2);
    }

    private int store(int[] pairs) {
        if (classCount == classStart.length) {
            classStart = Arrays.copyOf(classStart, classCount * 2);
            classPairs = Arrays.copyOf(classPairs, classCount * 2);
        }
        if (rangeSize + pairs.length > ranges.length) {
            ranges = Arrays.copyOf(ranges, Math.max(ranges.length * 2, rangeSize + pairs.length));
        }
        System.arraycopy(pairs, 0, ranges, rangeSize, pairs.length);
        classStart[classCount] = rangeSize;
        classPairs[classCount] = pairs.length / 2;
        rangeSize += pairs.length;
        return classCount++;
    }

    /** CLASS node over {@code pairs} (all pairs used). */
    int addClassNode(int[] pairs, boolean negate) {
        return node(CLASS, addClass(pairs, pairs.length / 2, negate));
    }

    /** Whether class {@code id} contains {@code unit} (binary search over its pairs). */
    boolean contains(int id, int unit) {
        int low = 0;
        int high = classPairs[id] - 1;
        int base = classStart[id];
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (unit < ranges[base + 2 * mid]) {
                high = mid - 1;
            } else if (unit > ranges[base + 2 * mid + 1]) {
                low = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /** The single symbol of a one-symbol class, else {@code -1}. */
    int singleton(int node) {
        if (op[node] != CLASS) return -1;
        int id = a[node];
        int base = classStart[id];
        return classPairs[id] == 1 && ranges[base] == ranges[base + 1] ? ranges[base] : -1;
    }

    boolean nullable(int node) {
        switch (op[node]) {
            case CLASS:
                return false;
            case CONCAT:
                for (int child = a[node]; child >= 0; child = next[child]) {
                    if (!nullable(child)) return false;
                }
                return true;
            case ALT:
                for (int child = a[node]; child >= 0; child = next[child]) {
                    if (nullable(child)) return true;
                }
                return false;
            case REPEAT:
                return min[node] == 0 || nullable(a[node]);
            case GROUP:
                return nullable(a[node]);
            default:
                return true;
        }
    }
}
