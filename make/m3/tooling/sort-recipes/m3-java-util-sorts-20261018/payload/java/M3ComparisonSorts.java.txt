/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. This particular file is
 * subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package java.util;

/**
 * Comparison kernels over one primitive permutation lane. Lane entries are
 * opaque labels: only the supplied context compares their represented values.
 * This file expresses standard abstract algorithms under target ownership;
 * the separate M3TimSort preserves its OpenJDK donor source and attribution.
 *
 * <p>All ranges below are half open. Auxiliary tree, pile, tournament and
 * linked-list state is stored in primitive arrays, without per-entry objects.
 * Algorithms may share partition, merge or heap atoms while retaining their
 * distinct scheduling, partitioning or selection strategy.
 */
final class M3ComparisonSorts {
    private static final int INSERTION_THRESHOLD = 24;

    private M3ComparisonSorts() {
    }

    static void sort(int[] a, SortAlgorithm algorithm, M3SortContext c) {
        if (a.length < 2) {
            return;
        }
        switch (algorithm) {
            case TIM -> M3TimSort.sort(a, 0, a.length, c, null, 0, 0);
            case DUAL_PIVOT_QUICK -> dualPivotQuick(a, 0, a.length,
                    depthLimit(a.length), c);
            case QUICK -> quick(a, 0, a.length, c);
            case INTRO -> intro(a, 0, a.length, depthLimit(a.length), c);
            case MERGE -> mergeSort(a, new int[a.length], 0, a.length, c);
            case BOTTOM_UP_MERGE -> bottomUpMerge(a, c);
            case NATURAL_MERGE -> naturalMerge(a, c);
            case IN_PLACE_MERGE -> inPlaceMergeSort(a, 0, a.length, c);
            case HEAP -> heap(a, 0, a.length, c);
            case SHELL -> shell(a, c);
            case INSERTION -> insertion(a, 0, a.length, c);
            case BINARY_INSERTION -> binaryInsertion(a, 0, a.length, c);
            case SELECTION -> selection(a, c);
            case DOUBLE_SELECTION -> doubleSelection(a, c);
            case BUBBLE -> bubble(a, c);
            case COCKTAIL -> cocktail(a, c);
            case COMB -> comb(a, c);
            case GNOME -> gnome(a, c);
            case ODD_EVEN -> oddEven(a, c);
            case CYCLE -> cycle(a, c);
            case PANCAKE -> pancake(a, c);
            case BINGO -> bingo(a, c);
            case TREE -> tree(a, c);
            case TOURNAMENT -> tournament(a, c);
            case PATIENCE -> patience(a, c);
            case STRAND -> strand(a, c);
            case BITONIC -> bitonicSort(a, 0, a.length, true, c);
            case ODD_EVEN_MERGE -> {
                long wires = 1;
                while (wires < a.length) {
                    wires <<= 1;
                }
                oddEvenMergeSort(a, 0, wires, c);
            }
            default -> throw new IllegalArgumentException(
                    "A comparison kernel is not available for " + algorithm);
        }
    }

    /** Median-of-three, three-way quicksort with smaller-side recursion. */
    private static void quick(int[] a, int lo, int hi, M3SortContext c) {
        while (hi - lo > INSERTION_THRESHOLD) {
            long bounds = partitionThreeWay(a, lo, hi, medianPivot(a, lo, hi, c), c);
            int equalLo = (int) (bounds >>> 32);
            int equalHi = (int) bounds;
            if (equalLo - lo < hi - equalHi) {
                quick(a, lo, equalLo, c);
                lo = equalHi;
            } else {
                quick(a, equalHi, hi, c);
                hi = equalLo;
            }
        }
        insertion(a, lo, hi, c);
    }

    /** Introsort consumes its depth budget on recursive and tail partitions. */
    private static void intro(int[] a, int lo, int hi, int depth,
                              M3SortContext c) {
        while (hi - lo > INSERTION_THRESHOLD) {
            if (depth-- == 0) {
                heap(a, lo, hi, c);
                return;
            }
            long bounds = partitionThreeWay(a, lo, hi, medianPivot(a, lo, hi, c), c);
            int equalLo = (int) (bounds >>> 32);
            int equalHi = (int) bounds;
            if (equalLo - lo < hi - equalHi) {
                intro(a, lo, equalLo, depth, c);
                lo = equalHi;
            } else {
                intro(a, equalHi, hi, depth, c);
                hi = equalLo;
            }
        }
        insertion(a, lo, hi, c);
    }

    /**
     * Dual-pivot partitioning with a depth-limited heap fallback. This is a
     * separate target-owned hybrid, not a copy of the JDK's multi-kernel
     * DualPivotQuicksort. A logarithmic budget also bounds recursion depth.
     */
    private static void dualPivotQuick(int[] a, int lo, int hi, int depth,
                                      M3SortContext c) {
        int length = hi - lo;
        if (length <= INSERTION_THRESHOLD) {
            insertion(a, lo, hi, c);
            return;
        }
        if (depth == 0) {
            heap(a, lo, hi, c);
            return;
        }
        depth--;
        int third = length / 3;
        swap(a, lo, lo + third, c);
        swap(a, hi - 1, hi - 1 - third, c);
        if (c.compare(a[lo], a[hi - 1]) > 0) {
            swap(a, lo, hi - 1, c);
        }
        int lowerPivot = a[lo];
        int upperPivot = a[hi - 1];
        if (c.compare(lowerPivot, upperPivot) == 0) {
            long bounds = partitionThreeWay(a, lo, hi, lowerPivot, c);
            dualPivotQuick(a, lo, (int) (bounds >>> 32), depth, c);
            dualPivotQuick(a, (int) bounds, hi, depth, c);
            return;
        }
        int lower = lo + 1;
        int upper = hi - 2;
        for (int current = lower; current <= upper; current++) {
            if (c.compare(a[current], lowerPivot) < 0) {
                swap(a, current, lower++, c);
            } else if (c.compare(a[current], upperPivot) > 0) {
                while (current < upper && c.compare(a[upper], upperPivot) > 0) {
                    upper--;
                }
                swap(a, current, upper--, c);
                if (c.compare(a[current], lowerPivot) < 0) {
                    swap(a, current, lower++, c);
                }
            }
        }
        swap(a, lo, --lower, c);
        swap(a, hi - 1, ++upper, c);
        dualPivotQuick(a, lo, lower, depth, c);
        dualPivotQuick(a, lower + 1, upper, depth, c);
        dualPivotQuick(a, upper + 1, hi, depth, c);
    }

    private static int depthLimit(int length) {
        return 2 * (31 - Integer.numberOfLeadingZeros(length));
    }

    private static int medianPivot(int[] a, int lo, int hi, M3SortContext c) {
        int first = a[lo];
        int middle = a[lo + ((hi - lo) >>> 1)];
        int last = a[hi - 1];
        if (c.compare(first, middle) < 0) {
            if (c.compare(middle, last) < 0) {
                return middle;
            }
            return c.compare(first, last) < 0 ? last : first;
        }
        if (c.compare(first, last) < 0) {
            return first;
        }
        return c.compare(middle, last) < 0 ? last : middle;
    }

    /** Packs the inclusive lower and exclusive upper equal-partition bounds. */
    private static long partitionThreeWay(int[] a, int lo, int hi, int pivot,
                                         M3SortContext c) {
        int lower = lo;
        int current = lo;
        int upper = hi;
        while (current < upper) {
            int order = c.compare(a[current], pivot);
            if (order < 0) {
                swap(a, lower++, current++, c);
            } else if (order > 0) {
                swap(a, current, --upper, c);
            } else {
                current++;
            }
        }
        return ((long) lower << 32) | (upper & 0xffffffffL);
    }

    private static void mergeSort(int[] a, int[] work, int lo, int hi,
                                  M3SortContext c) {
        if (hi - lo < 2) {
            return;
        }
        int mid = lo + ((hi - lo) >>> 1);
        mergeSort(a, work, lo, mid, c);
        mergeSort(a, work, mid, hi, c);
        mergeAdjacent(a, work, lo, mid, hi, c);
    }

    private static void bottomUpMerge(int[] a, M3SortContext c) {
        int length = a.length;
        int[] work = new int[length];
        for (int width = 1; width < length;
             width = width >= length - width ? length : width << 1) {
            int lo = 0;
            while (lo < length - width) {
                int mid = lo + width;
                int hi = mid + Math.min(width, length - mid);
                mergeAdjacent(a, work, lo, mid, hi, c);
                lo = hi;
            }
        }
    }

    /** Pairwise natural-run merging; strictly descending runs may reverse. */
    private static void naturalMerge(int[] a, M3SortContext c) {
        int[] work = new int[a.length];
        while (true) {
            int lo = 0;
            int merges = 0;
            while (lo < a.length) {
                int mid = ascendingRun(a, lo, a.length, c);
                if (mid == a.length) {
                    break;
                }
                int hi = ascendingRun(a, mid, a.length, c);
                mergeAdjacent(a, work, lo, mid, hi, c);
                merges++;
                lo = hi;
            }
            if (merges == 0) {
                return;
            }
        }
    }

    private static int ascendingRun(int[] a, int lo, int hi, M3SortContext c) {
        int end = lo + 1;
        if (end == hi) {
            return end;
        }
        if (c.compare(a[end++], a[lo]) < 0) {
            while (end < hi && c.compare(a[end], a[end - 1]) < 0) {
                end++;
            }
            reverse(a, lo, end, c);
        } else {
            while (end < hi && c.compare(a[end], a[end - 1]) >= 0) {
                end++;
            }
        }
        return end;
    }

    /** Copies one adjacent pair, then merges with the left run winning ties. */
    private static void mergeAdjacent(int[] a, int[] work, int lo, int mid,
                                      int hi, M3SortContext c) {
        if (lo == mid || mid == hi || c.compare(a[mid - 1], a[mid]) <= 0) {
            return;
        }
        c.step();
        System.arraycopy(a, lo, work, lo, hi - lo);
        int left = lo;
        int right = mid;
        int out = lo;
        while (left < mid && right < hi) {
            a[out++] = c.compare(work[left], work[right]) <= 0
                    ? work[left++] : work[right++];
        }
        while (left < mid) {
            a[out++] = work[left++];
            c.step();
        }
        while (right < hi) {
            a[out++] = work[right++];
            c.step();
        }
    }

    private static void inPlaceMergeSort(int[] a, int lo, int hi,
                                         M3SortContext c) {
        if (hi - lo < 2) {
            return;
        }
        int mid = lo + ((hi - lo) >>> 1);
        inPlaceMergeSort(a, lo, mid, c);
        inPlaceMergeSort(a, mid, hi, c);
        rotateMerge(a, lo, mid, hi, c);
    }

    /**
     * Stable divide-and-rotate merge. Binary cuts keep equal left-run values
     * ahead of right-run values. Work is O(n log n) per merge in the worst
     * case, with logarithmic recursive workspace and no element buffer.
     */
    private static void rotateMerge(int[] a, int lo, int mid, int hi,
                                     M3SortContext c) {
        if (lo == mid || mid == hi || c.compare(a[mid - 1], a[mid]) <= 0) {
            return;
        }
        if (hi - lo == 2) {
            swap(a, lo, mid, c);
            return;
        }
        int leftCut;
        int rightCut;
        if (mid - lo > hi - mid) {
            leftCut = lo + ((mid - lo) >>> 1);
            rightCut = lowerBound(a, mid, hi, a[leftCut], c);
        } else {
            rightCut = mid + ((hi - mid) >>> 1);
            leftCut = upperBound(a, lo, mid, a[rightCut], c);
        }
        rotate(a, leftCut, mid, rightCut, c);
        int newMid = leftCut + (rightCut - mid);
        rotateMerge(a, lo, leftCut, newMid, c);
        rotateMerge(a, newMid, rightCut, hi, c);
    }

    private static int lowerBound(int[] a, int lo, int hi, int value,
                                  M3SortContext c) {
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (c.compare(a[mid], value) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private static int upperBound(int[] a, int lo, int hi, int value,
                                  M3SortContext c) {
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (c.compare(a[mid], value) <= 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /** Heap indices are relative to lo so all child arithmetic stays bounded. */
    private static void heap(int[] a, int lo, int hi, M3SortContext c) {
        int size = hi - lo;
        for (int parent = (size >>> 1) - 1; parent >= 0; parent--) {
            siftMaximum(a, lo, size, parent, c);
        }
        for (int end = size - 1; end > 0; end--) {
            swap(a, lo, lo + end, c);
            siftMaximum(a, lo, end, 0, c);
        }
    }

    private static void siftMaximum(int[] a, int lo, int size, int root,
                                     M3SortContext c) {
        int value = a[lo + root];
        int half = size >>> 1;
        while (root < half) {
            int child = (root << 1) + 1;
            if (child + 1 < size && c.compare(a[lo + child], a[lo + child + 1]) < 0) {
                child++;
            }
            if (c.compare(value, a[lo + child]) >= 0) {
                break;
            }
            a[lo + root] = a[lo + child];
            root = child;
            c.step();
        }
        a[lo + root] = value;
    }

    /** Shell insertion passes use Knuth gaps 1, 4, 13, 40, ... . */
    private static void shell(int[] a, M3SortContext c) {
        int gap = 1;
        while (gap < a.length / 3) {
            gap = 3 * gap + 1;
        }
        for (; gap > 0; gap = (gap - 1) / 3) {
            for (int i = gap; i < a.length; i++) {
                int value = a[i];
                int hole = i;
                while (hole >= gap && c.compare(value, a[hole - gap]) < 0) {
                    a[hole] = a[hole - gap];
                    hole -= gap;
                    c.step();
                }
                a[hole] = value;
            }
        }
    }

    private static void insertion(int[] a, int lo, int hi, M3SortContext c) {
        if (hi - lo < 2) {
            return;
        }
        for (int i = lo + 1; i < hi; i++) {
            int value = a[i];
            int hole = i;
            while (hole > lo && c.compare(value, a[hole - 1]) < 0) {
                a[hole] = a[hole - 1];
                hole--;
                c.step();
            }
            a[hole] = value;
        }
    }

    private static void binaryInsertion(int[] a, int lo, int hi,
                                         M3SortContext c) {
        for (int i = lo + 1; i < hi; i++) {
            int value = a[i];
            int position = upperBound(a, lo, i, value, c);
            for (int hole = i; hole > position; hole--) {
                a[hole] = a[hole - 1];
                c.step();
            }
            a[position] = value;
        }
    }

    private static void selection(int[] a, M3SortContext c) {
        for (int lo = 0; lo < a.length - 1; lo++) {
            int minimum = lo;
            for (int i = lo + 1; i < a.length; i++) {
                if (c.compare(a[i], a[minimum]) < 0) {
                    minimum = i;
                }
            }
            swap(a, lo, minimum, c);
        }
    }

    private static void doubleSelection(int[] a, M3SortContext c) {
        for (int lo = 0, hi = a.length - 1; lo < hi; lo++, hi--) {
            int minimum = lo;
            int maximum = lo;
            for (int i = lo + 1; i <= hi; i++) {
                if (c.compare(a[i], a[minimum]) < 0) {
                    minimum = i;
                }
                if (c.compare(a[i], a[maximum]) > 0) {
                    maximum = i;
                }
            }
            swap(a, lo, minimum, c);
            if (maximum == lo) {
                maximum = minimum;
            }
            swap(a, hi, maximum, c);
        }
    }

    private static void bubble(int[] a, M3SortContext c) {
        int bound = a.length - 1;
        while (bound > 0) {
            int lastSwap = 0;
            for (int i = 0; i < bound; i++) {
                if (c.compare(a[i], a[i + 1]) > 0) {
                    swap(a, i, i + 1, c);
                    lastSwap = i;
                }
            }
            bound = lastSwap;
        }
    }

    private static void cocktail(int[] a, M3SortContext c) {
        int lo = 0;
        int hi = a.length - 1;
        while (lo < hi) {
            int lastSwap = lo;
            for (int i = lo; i < hi; i++) {
                if (c.compare(a[i], a[i + 1]) > 0) {
                    swap(a, i, i + 1, c);
                    lastSwap = i;
                }
            }
            hi = lastSwap;
            if (lo >= hi) {
                break;
            }
            lastSwap = hi;
            for (int i = hi; i > lo; i--) {
                if (c.compare(a[i - 1], a[i]) > 0) {
                    swap(a, i - 1, i, c);
                    lastSwap = i;
                }
            }
            lo = lastSwap;
        }
    }

    /** Comb 11 uses an exact rational 1.3 shrink and repairs gaps 9 and 10. */
    private static void comb(int[] a, M3SortContext c) {
        int gap = a.length;
        boolean changed = true;
        while (gap > 1 || changed) {
            gap = (int) ((long) gap * 10 / 13);
            if (gap == 9 || gap == 10) {
                gap = 11;
            }
            gap = Math.max(1, gap);
            changed = false;
            for (int i = 0; i < a.length - gap; i++) {
                if (c.compare(a[i], a[i + gap]) > 0) {
                    swap(a, i, i + gap, c);
                    changed = true;
                }
            }
        }
    }

    private static void gnome(int[] a, M3SortContext c) {
        int i = 1;
        while (i < a.length) {
            if (i == 0 || c.compare(a[i - 1], a[i]) <= 0) {
                i++;
            } else {
                swap(a, i - 1, i, c);
                i--;
            }
        }
    }

    private static void oddEven(int[] a, M3SortContext c) {
        boolean changed;
        do {
            changed = false;
            for (int parity = 0; parity < 2; parity++) {
                for (int i = parity; i < a.length - 1; i += 2) {
                    if (c.compare(a[i], a[i + 1]) > 0) {
                        swap(a, i, i + 1, c);
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    /** Cycle leader placement writes each displaced entry to its rank slot. */
    private static void cycle(int[] a, M3SortContext c) {
        for (int start = 0; start < a.length - 1; start++) {
            int item = a[start];
            int position = cycleRank(a, start, item, c);
            if (position == start) {
                continue;
            }
            position = skipCycleEquals(a, position, item, c);
            int displaced = a[position];
            a[position] = item;
            item = displaced;
            c.step();
            while (position != start) {
                position = cycleRank(a, start, item, c);
                position = skipCycleEquals(a, position, item, c);
                displaced = a[position];
                a[position] = item;
                item = displaced;
                c.step();
            }
        }
    }

    private static int cycleRank(int[] a, int start, int item, M3SortContext c) {
        int rank = start;
        for (int i = start + 1; i < a.length; i++) {
            if (c.compare(a[i], item) < 0) {
                rank++;
            }
        }
        return rank;
    }

    private static int skipCycleEquals(int[] a, int position, int item,
                                        M3SortContext c) {
        while (position < a.length && c.compare(item, a[position]) == 0) {
            position++;
        }
        if (position == a.length) {
            throw new IllegalArgumentException("Comparison method violates its general contract");
        }
        return position;
    }

    private static void pancake(int[] a, M3SortContext c) {
        for (int end = a.length; end > 1; end--) {
            int maximum = 0;
            for (int i = 1; i < end; i++) {
                if (c.compare(a[i], a[maximum]) > 0) {
                    maximum = i;
                }
            }
            if (maximum != end - 1) {
                reverse(a, 0, maximum + 1, c);
                reverse(a, 0, end, c);
            }
        }
    }

    /** Each bingo pass places every occurrence of the current minimum key. */
    private static void bingo(int[] a, M3SortContext c) {
        int currentMinimum = a[0];
        for (int i = 1; i < a.length; i++) {
            if (c.compare(a[i], currentMinimum) < 0) {
                currentMinimum = a[i];
            }
        }
        int start = 0;
        while (start < a.length) {
            int nextMinimum = 0;
            boolean foundNext = false;
            for (int i = start; i < a.length; i++) {
                if (c.compare(a[i], currentMinimum) == 0) {
                    swap(a, start++, i, c);
                } else if (!foundNext || c.compare(a[i], nextMinimum) < 0) {
                    nextMinimum = a[i];
                    foundNext = true;
                }
            }
            if (start < a.length && !foundNext) {
                throw new IllegalArgumentException("Comparison method violates its general contract");
            }
            currentMinimum = nextMinimum;
        }
    }

    /**
     * AVL tree sort. Child links encode node index plus one, so zero is empty.
     * Equal keys insert after all existing equal keys; rotations preserve that
     * in-order sequence. An AVL over any int-sized lane fits a 64-slot path.
     */
    private static void tree(int[] a, M3SortContext c) {
        int[] keys = a.clone();
        int[] left = new int[a.length];
        int[] right = new int[a.length];
        int[] height = new int[a.length];
        int[] path = new int[64];
        int root = -1;
        for (int node = 0; node < a.length; node++) {
            height[node] = 1;
            if (root < 0) {
                root = node;
                c.step();
                continue;
            }
            int current = root;
            int depth = 0;
            while (true) {
                path[depth++] = current;
                if (c.compare(keys[node], keys[current]) < 0) {
                    if (left[current] == 0) {
                        left[current] = node + 1;
                        break;
                    }
                    current = left[current] - 1;
                } else {
                    if (right[current] == 0) {
                        right[current] = node + 1;
                        break;
                    }
                    current = right[current] - 1;
                }
            }
            for (int level = depth - 1; level >= 0; level--) {
                int oldRoot = path[level];
                int newRoot = rebalance(oldRoot, left, right, height, c);
                if (level == 0) {
                    root = newRoot;
                } else {
                    int parent = path[level - 1];
                    if (left[parent] == oldRoot + 1) {
                        left[parent] = newRoot + 1;
                    } else {
                        right[parent] = newRoot + 1;
                    }
                }
            }
            c.step();
        }
        int top = 0;
        int current = root;
        int out = 0;
        while (current >= 0 || top > 0) {
            while (current >= 0) {
                path[top++] = current;
                current = left[current] - 1;
                c.step();
            }
            current = path[--top];
            a[out++] = keys[current];
            current = right[current] - 1;
            c.step();
        }
    }

    private static int rebalance(int node, int[] left, int[] right,
                                  int[] height, M3SortContext c) {
        updateHeight(node, left, right, height);
        int balance = childHeight(left[node], height) - childHeight(right[node], height);
        if (balance > 1) {
            int child = left[node] - 1;
            if (childHeight(left[child], height) < childHeight(right[child], height)) {
                left[node] = rotateTreeLeft(child, left, right, height, c) + 1;
            }
            return rotateTreeRight(node, left, right, height, c);
        }
        if (balance < -1) {
            int child = right[node] - 1;
            if (childHeight(right[child], height) < childHeight(left[child], height)) {
                right[node] = rotateTreeRight(child, left, right, height, c) + 1;
            }
            return rotateTreeLeft(node, left, right, height, c);
        }
        return node;
    }

    private static int rotateTreeLeft(int node, int[] left, int[] right,
                                       int[] height, M3SortContext c) {
        int successor = right[node] - 1;
        right[node] = left[successor];
        left[successor] = node + 1;
        updateHeight(node, left, right, height);
        updateHeight(successor, left, right, height);
        c.step();
        return successor;
    }

    private static int rotateTreeRight(int node, int[] left, int[] right,
                                        int[] height, M3SortContext c) {
        int successor = left[node] - 1;
        left[node] = right[successor];
        right[successor] = node + 1;
        updateHeight(node, left, right, height);
        updateHeight(successor, left, right, height);
        c.step();
        return successor;
    }

    private static int childHeight(int child, int[] height) {
        return child == 0 ? 0 : height[child - 1];
    }

    private static void updateHeight(int node, int[] left, int[] right, int[] height) {
        height[node] = 1 + Math.max(childHeight(left[node], height),
                childHeight(right[node], height));
    }

    /**
     * A winner tree has n leaves and n-1 internal nodes. Long wire arithmetic
     * avoids both power-of-two padding and overflow in the conceptual 2*n
     * layout. Winners store source positions; a primitive bitset marks exits.
     */
    private static void tournament(int[] a, M3SortContext c) {
        int length = a.length;
        int[] keys = a.clone();
        int[] winners = new int[length];
        long[] removed = new long[((length - 1) >>> 6) + 1];
        for (int node = length - 1; node > 0; node--) {
            winners[node] = tournamentWinner(
                    winnerAt((long) node * 2, length, winners, removed),
                    winnerAt((long) node * 2 + 1, length, winners, removed), keys, c);
        }
        for (int out = 0; out < length; out++) {
            int winner = winners[1];
            a[out] = keys[winner];
            removed[winner >>> 6] |= 1L << (winner & 63);
            for (long node = ((long) length + winner) >>> 1; node > 0; node >>>= 1) {
                winners[(int) node] = tournamentWinner(
                        winnerAt(node * 2, length, winners, removed),
                        winnerAt(node * 2 + 1, length, winners, removed), keys, c);
                c.step();
            }
            c.step();
        }
    }

    private static int winnerAt(long wire, int length, int[] winners,
                                long[] removed) {
        if (wire < length) {
            return winners[(int) wire];
        }
        int position = (int) (wire - length);
        return (removed[position >>> 6] & (1L << (position & 63))) == 0
                ? position : -1;
    }

    private static int tournamentWinner(int left, int right, int[] keys,
                                         M3SortContext c) {
        if (left < 0) {
            return right;
        }
        if (right < 0) {
            return left;
        }
        int order = c.compare(keys[left], keys[right]);
        return order < 0 || (order == 0 && left < right) ? left : right;
    }

    /**
     * Piles have strictly decreasing keys: equal keys enter different piles.
     * A heap merges pile tops, resolving equal keys by original position.
     */
    private static void patience(int[] a, M3SortContext c) {
        int[] keys = a.clone();
        int[] tops = new int[a.length];
        int[] previous = new int[a.length];
        int piles = 0;
        for (int position = 0; position < a.length; position++) {
            int lo = 0;
            int hi = piles;
            while (lo < hi) {
                int mid = lo + ((hi - lo) >>> 1);
                if (c.compare(keys[tops[mid] - 1], keys[position]) > 0) {
                    hi = mid;
                } else {
                    lo = mid + 1;
                }
            }
            previous[position] = tops[lo];
            tops[lo] = position + 1;
            if (lo == piles) {
                piles++;
            }
            c.step();
        }
        int[] heap = new int[piles];
        for (int pile = 0; pile < piles; pile++) {
            heap[pile] = pile;
            c.step();
        }
        for (int parent = (piles >>> 1) - 1; parent >= 0; parent--) {
            siftPile(heap, piles, parent, tops, keys, c);
        }
        for (int out = 0; out < a.length; out++) {
            int pile = heap[0];
            int position = tops[pile] - 1;
            a[out] = keys[position];
            tops[pile] = previous[position];
            if (tops[pile] == 0) {
                heap[0] = heap[--piles];
            }
            if (piles > 0) {
                siftPile(heap, piles, 0, tops, keys, c);
            }
            c.step();
        }
    }

    private static void siftPile(int[] heap, int size, int root, int[] tops,
                                  int[] keys, M3SortContext c) {
        int pile = heap[root];
        int half = size >>> 1;
        while (root < half) {
            int child = (root << 1) + 1;
            if (child + 1 < size && comparePiles(heap[child + 1], heap[child],
                    tops, keys, c) < 0) {
                child++;
            }
            if (comparePiles(pile, heap[child], tops, keys, c) <= 0) {
                break;
            }
            heap[root] = heap[child];
            root = child;
            c.step();
        }
        heap[root] = pile;
    }

    private static int comparePiles(int left, int right, int[] tops,
                                    int[] keys, M3SortContext c) {
        int leftPosition = tops[left] - 1;
        int rightPosition = tops[right] - 1;
        int order = c.compare(keys[leftPosition], keys[rightPosition]);
        return order != 0 ? order : Integer.compare(leftPosition, rightPosition);
    }

    /**
     * Greedily unlinks each nondecreasing strand from a primitive next lane,
     * then stably merges it into the accumulated result. Earlier occurrences
     * of equal keys enter the same or an earlier strand.
     */
    private static void strand(int[] a, M3SortContext c) {
        int[] next = new int[a.length];
        for (int i = 0; i < a.length - 1; i++) {
            next[i] = i + 2;
            c.step();
        }
        int[] extracted = new int[a.length];
        int[] sorted = new int[a.length];
        int[] merged = new int[a.length];
        int head = 1;
        int sortedLength = 0;
        while (head != 0) {
            int previous = -1;
            int current = head;
            int extractedLength = 0;
            while (current != 0) {
                int position = current - 1;
                int following = next[position];
                if (extractedLength == 0
                        || c.compare(extracted[extractedLength - 1], a[position]) <= 0) {
                    extracted[extractedLength++] = a[position];
                    if (previous < 0) {
                        head = following;
                    } else {
                        next[previous] = following;
                    }
                } else {
                    previous = position;
                }
                current = following;
                c.step();
            }
            mergeStrand(sorted, sortedLength, extracted, extractedLength, merged, c);
            int[] temporary = sorted;
            sorted = merged;
            merged = temporary;
            sortedLength += extractedLength;
        }
        c.step();
        System.arraycopy(sorted, 0, a, 0, a.length);
    }

    private static void mergeStrand(int[] sorted, int sortedLength, int[] strand,
                                    int strandLength, int[] out, M3SortContext c) {
        int left = 0;
        int right = 0;
        int position = 0;
        while (left < sortedLength && right < strandLength) {
            out[position++] = c.compare(sorted[left], strand[right]) <= 0
                    ? sorted[left++] : strand[right++];
        }
        while (left < sortedLength) {
            out[position++] = sorted[left++];
            c.step();
        }
        while (right < strandLength) {
            out[position++] = strand[right++];
            c.step();
        }
    }

    /** The arbitrary-length variant needs no physical sentinel or padding. */
    private static void bitonicSort(int[] a, int lo, int length, boolean ascending,
                                    M3SortContext c) {
        if (length < 2) {
            return;
        }
        int half = length >>> 1;
        bitonicSort(a, lo, half, !ascending, c);
        bitonicSort(a, lo + half, length - half, ascending, c);
        bitonicMerge(a, lo, length, ascending, c);
    }

    private static void bitonicMerge(int[] a, int lo, int length,
                                     boolean ascending, M3SortContext c) {
        if (length < 2) {
            return;
        }
        int distance = Integer.highestOneBit(length - 1);
        int limit = lo + (length - distance);
        for (int left = lo; left < limit; left++) {
            int order = c.compare(a[left], a[left + distance]);
            if (ascending ? order > 0 : order < 0) {
                swap(a, left, left + distance, c);
            }
        }
        bitonicMerge(a, lo, distance, ascending, c);
        bitonicMerge(a, lo + distance, length - distance, ascending, c);
    }

    /**
     * Batcher's ascending odd-even merge network on a virtual power-of-two
     * width. Its virtual +infinity suffix never crosses into the real prefix:
     * every comparator is ascending and compares a lower wire to a higher
     * wire. Consequently comparators touching that suffix can be omitted.
     */
    private static void oddEvenMergeSort(int[] a, long lo, long length,
                                         M3SortContext c) {
        if (length < 2 || lo >= a.length) {
            return;
        }
        long half = length >>> 1;
        oddEvenMergeSort(a, lo, half, c);
        oddEvenMergeSort(a, lo + half, half, c);
        oddEvenNetworkMerge(a, lo, length, 1, c);
    }

    private static void oddEvenNetworkMerge(int[] a, long lo, long length,
                                            long stride, M3SortContext c) {
        if (lo >= a.length) {
            return;
        }
        long doubled = stride << 1;
        if (doubled < length) {
            oddEvenNetworkMerge(a, lo, length, doubled, c);
            oddEvenNetworkMerge(a, lo + stride, length, doubled, c);
            for (long left = lo + stride; left + stride < lo + length;
                 left += doubled) {
                if (left + stride >= a.length) {
                    break;
                }
                compareExchange(a, (int) left, (int) (left + stride), c);
            }
        } else if (lo + stride < a.length) {
            compareExchange(a, (int) lo, (int) (lo + stride), c);
        }
    }

    private static void compareExchange(int[] a, int left, int right,
                                         M3SortContext c) {
        if (c.compare(a[left], a[right]) > 0) {
            swap(a, left, right, c);
        }
    }

    private static void rotate(int[] a, int lo, int mid, int hi,
                                M3SortContext c) {
        if (lo == mid || mid == hi) {
            return;
        }
        reverse(a, lo, mid, c);
        reverse(a, mid, hi, c);
        reverse(a, lo, hi, c);
    }

    private static void reverse(int[] a, int lo, int hi, M3SortContext c) {
        for (int left = lo, right = hi - 1; left < right; left++, right--) {
            swap(a, left, right, c);
        }
    }

    private static void swap(int[] a, int left, int right, M3SortContext c) {
        if (left != right) {
            int value = a[left];
            a[left] = a[right];
            a[right] = value;
            c.step();
        }
    }
}
