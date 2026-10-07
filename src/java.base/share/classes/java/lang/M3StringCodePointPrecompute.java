/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Separately bounded UTF-16 code-point boundary precompute for M3 String.
 *
 * <p>The cache retains no text. Each entry weakly keys one exact owner+coordinate and stores only
 * a bit per low-surrogate continuation plus block-prefix counts. A set bit at UTF-16 position
 * {@code i} means {@code charAt(i)} is a low surrogate paired with a high surrogate at
 * {@code i - 1}. Exact String/Character semantics remain authoritative when a source is outside
 * the bounded cache or precompute allocation is unavailable.</p>
 */
final class M3StringCodePointPrecompute {
    private static final int SLOTS = 64;
    private static final int SLOT_MASK = SLOTS - 1;
    private static final int BLOCK_SHIFT = 6;
    private static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;
    private static final int BLOCK_MASK = BLOCK_SIZE - 1;
    private static final int MIN_SOURCE_UNITS = 128;
    private static final int MAX_SOURCE_UNITS = 32_768;
    private static final int UNAVAILABLE = Integer.MIN_VALUE;

    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(SLOTS);

    private M3StringCodePointPrecompute() {}

    static int codePointCount(M3String source, int beginIndex, int endIndex) {
        if (source.length() < MIN_SOURCE_UNITS || source.length() > MAX_SOURCE_UNITS) {
            return UNAVAILABLE;
        }
        Entry entry = prepare(source);
        if (entry == null) return UNAVAILABLE;

        // A low surrogate at beginIndex pairs with a high unit outside the requested range and
        // therefore counts as one independent code point. Only continuations strictly after the
        // range start collapse two UTF-16 units into one code point.
        int continuations = entry.countContinuations(beginIndex + 1, endIndex);
        return (endIndex - beginIndex) - continuations;
    }

    static int offsetByCodePoints(M3String source, int index, int codePointOffset) {
        if (source.length() < MIN_SOURCE_UNITS || source.length() > MAX_SOURCE_UNITS) {
            return UNAVAILABLE;
        }
        if (codePointOffset == 0) return index;

        Entry entry = prepare(source);
        if (entry == null) return UNAVAILABLE;

        if (codePointOffset > 0) {
            int available =
                    (source.length() - index)
                            - entry.countContinuations(index + 1, source.length());
            if (available < codePointOffset) return UNAVAILABLE;

            int low = index + 1;
            int high = source.length();
            int result = high;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                int continuationEnd = Math.min(source.length(), middle + 1);
                int traversed =
                        (middle - index)
                                - entry.countContinuations(index + 1, continuationEnd);
                if (traversed >= codePointOffset) {
                    result = middle;
                    high = middle - 1;
                } else {
                    low = middle + 1;
                }
            }
            return result;
        }

        long requested = -(long) codePointOffset;
        int available = index - entry.countContinuations(0, index);
        if (available < requested) return UNAVAILABLE;

        int low = 0;
        int high = index - 1;
        int result = 0;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int traversed =
                    (index - middle) - entry.countContinuations(middle, index);
            if (traversed >= requested) {
                result = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return result;
    }

    static long maximumRetainedPrimitiveBytes() {
        long blocks = (MAX_SOURCE_UNITS + BLOCK_MASK) >>> BLOCK_SHIFT;
        long masks = (long) SLOTS * blocks * Long.BYTES;
        long prefixes = (long) SLOTS * (blocks + 1L) * Integer.BYTES;
        return Math.addExact(masks, prefixes);
    }

    private static Entry prepare(M3String source) {
        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = slot(owner, coordinate);
        Entry current = CACHE.get(slot);
        if (current != null
                && current.owner.get() == owner
                && current.coordinate == coordinate
                && current.utf16Length == source.length()) {
            return current;
        }

        try {
            int blockCount = (source.length() + BLOCK_MASK) >>> BLOCK_SHIFT;
            long[] continuations = new long[blockCount];
            int[] prefixCounts = new int[blockCount + 1];

            char previous = 0;
            for (int index = 0; index < source.length(); index++) {
                char unit = source.charAt(index);
                if (index != 0
                        && Character.isHighSurrogate(previous)
                        && Character.isLowSurrogate(unit)) {
                    continuations[index >>> BLOCK_SHIFT] |= 1L << (index & BLOCK_MASK);
                }
                previous = unit;
            }
            for (int block = 0; block < blockCount; block++) {
                prefixCounts[block + 1] =
                        Math.addExact(prefixCounts[block], Long.bitCount(continuations[block]));
            }

            Entry computed =
                    new Entry(
                            new WeakReference<>(owner),
                            coordinate,
                            source.length(),
                            continuations,
                            prefixCounts);
            CACHE.set(slot, computed);
            return computed;
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
    }

    private static int slot(M3StringOwner owner, long coordinate) {
        long mixed = Integer.toUnsignedLong(System.identityHashCode(owner));
        mixed ^= Long.rotateLeft(coordinate, 23);
        mixed ^= Long.rotateLeft(owner.structuralHash64, 41);
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return ((int) mixed) & SLOT_MASK;
    }

    private static final class Entry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final int utf16Length;
        final long[] continuationMasks;
        final int[] continuationPrefixCounts;

        Entry(
                WeakReference<M3StringOwner> owner,
                long coordinate,
                int utf16Length,
                long[] continuationMasks,
                int[] continuationPrefixCounts) {
            this.owner = owner;
            this.coordinate = coordinate;
            this.utf16Length = utf16Length;
            this.continuationMasks = continuationMasks;
            this.continuationPrefixCounts = continuationPrefixCounts;
        }

        int countContinuations(int fromIndex, int toIndex) {
            if (fromIndex >= toIndex) return 0;
            return countBefore(toIndex) - countBefore(fromIndex);
        }

        private int countBefore(int index) {
            if (index <= 0) return 0;
            if (index >= utf16Length) {
                return continuationPrefixCounts[continuationMasks.length];
            }
            int block = index >>> BLOCK_SHIFT;
            int offset = index & BLOCK_MASK;
            int count = continuationPrefixCounts[block];
            if (offset != 0) {
                long mask = (1L << offset) - 1L;
                count += Long.bitCount(continuationMasks[block] & mask);
            }
            return count;
        }
    }
}
