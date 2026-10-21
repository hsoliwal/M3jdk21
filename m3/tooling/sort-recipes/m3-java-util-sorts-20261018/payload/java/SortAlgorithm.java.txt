/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util;

/**
 * Explicit algorithm selection for {@link Sorts}. Each constant names an
 * implemented variant; no memory-budget fallback silently changes that choice.
 * Existing {@link Arrays#sort(int[])} and collection sorting policies are unchanged.
 *
 * <p>Stability means equal keys retain their input order. It applies to object
 * identities and indirect index order as well as primitive values. Algorithms
 * requiring numeric keys reject comparator-only entry points. Counting and
 * pigeonhole sort require a key span no greater than {@link Sorts#MAX_HISTOGRAM_SPAN}.
 *
 * <p>The complete call uses an O(n) primitive permutation even when the selected
 * kernel sorts its index lane in place. The catalogue documents additional
 * per-algorithm workspace and cost. This finite registry covers named in-memory
 * algorithms; aliases, external-I/O protocols and non-sorting operations are not
 * counted as additional algorithms.
 */
public enum SortAlgorithm {
    /** Adaptive OpenJDK TimSort, specialized to primitive indices. */
    TIM(Family.MERGE, true),
    /** Deterministic dual-pivot quicksort with a depth-triggered heap fallback. */
    DUAL_PIVOT_QUICK(Family.PARTITION, false),
    /** Deterministic three-way quicksort. */
    QUICK(Family.PARTITION, false),
    /** Introspective quicksort with a heap fallback. */
    INTRO(Family.PARTITION, false),
    /** Top-down buffered merge sort. */
    MERGE(Family.MERGE, true),
    /** Iterative bottom-up buffered merge sort. */
    BOTTOM_UP_MERGE(Family.MERGE, true),
    /** Natural-run merge sort. */
    NATURAL_MERGE(Family.MERGE, true),
    /** Stable rotation merge sort over the index lane. */
    IN_PLACE_MERGE(Family.MERGE, true),
    /** Binary heap sort. */
    HEAP(Family.HEAP, false),
    /** Shell gap insertion sort. */
    SHELL(Family.INSERTION, false),
    /** Straight insertion sort. */
    INSERTION(Family.INSERTION, true),
    /** Insertion sort using upper-bound binary search. */
    BINARY_INSERTION(Family.INSERTION, true),
    /** Minimum selection sort. */
    SELECTION(Family.SELECTION, false),
    /** Bidirectional minimum/maximum selection sort. */
    DOUBLE_SELECTION(Family.SELECTION, false),
    /** Adjacent-exchange bubble sort. */
    BUBBLE(Family.EXCHANGE, true),
    /** Bidirectional adjacent-exchange cocktail sort. */
    COCKTAIL(Family.EXCHANGE, true),
    /** Shrinking-gap comb sort. */
    COMB(Family.EXCHANGE, false),
    /** Adjacent-exchange gnome sort. */
    GNOME(Family.EXCHANGE, true),
    /** Odd-even transposition sort. */
    ODD_EVEN(Family.NETWORK, true),
    /** Cycle sort with duplicate-aware placement. */
    CYCLE(Family.SELECTION, false),
    /** Prefix-reversal pancake sort. */
    PANCAKE(Family.SELECTION, false),
    /** Duplicate-group bingo sort. */
    BINGO(Family.SELECTION, false),
    /** Array-backed balanced tree sort retaining equal-key occurrences. */
    TREE(Family.TREE, true),
    /** Array-backed winner tournament sort. */
    TOURNAMENT(Family.TREE, true),
    /** Patience piles followed by ordered extraction. */
    PATIENCE(Family.MERGE, true),
    /** Stable strand extraction and merging. */
    STRAND(Family.MERGE, true),
    /** Bitonic sorting network. */
    BITONIC(Family.NETWORK, false),
    /** Batcher odd-even merge sorting network. */
    ODD_EVEN_MERGE(Family.NETWORK, false),
    /** Stable histogram counting sort; a bounded key span is required. */
    COUNTING(Family.DISTRIBUTION, true),
    /** Stable linked index holes; a bounded key span is required. */
    PIGEONHOLE(Family.DISTRIBUTION, true),
    /** Ordered numeric buckets with stable per-bucket insertion. */
    BUCKET(Family.DISTRIBUTION, true),
    /** Stable least-significant-byte radix sort. */
    LSD_RADIX(Family.RADIX, true),
    /** Stable most-significant-byte radix sort. */
    MSD_RADIX(Family.RADIX, true),
    /** In-place most-significant-byte distribution of the index lane. */
    AMERICAN_FLAG(Family.RADIX, false);

    /** Broad problem category, independent of an implementation's variant. */
    public enum Family {
        /** Run detection and merging. */ MERGE,
        /** Pivot partitioning. */ PARTITION,
        /** Heap construction and extraction. */ HEAP,
        /** Inserting elements into ordered positions. */ INSERTION,
        /** Selecting values or placement positions. */ SELECTION,
        /** Comparing and exchanging values. */ EXCHANGE,
        /** Ordered tree or tournament extraction. */ TREE,
        /** Fixed compare-exchange structures. */ NETWORK,
        /** Distribution by a numeric key. */ DISTRIBUTION,
        /** Digit distribution of numeric keys. */ RADIX
    }

    private final Family family;
    private final boolean stable;

    SortAlgorithm(Family family, boolean stable) {
        this.family = family;
        this.stable = stable;
    }

    /** Returns this algorithm's problem category. */
    public Family family() { return family; }

    /** Returns whether equal keys are guaranteed to retain their input order. */
    public boolean isStable() { return stable; }

    /** Returns whether a numeric ordering key is required. */
    public boolean requiresNumericKeys() {
        return family == Family.DISTRIBUTION || family == Family.RADIX;
    }
}
