/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Separately bounded code-point navigation precompute for one canonical M3String coordinate.
 *
 * <p>The retained primitive lane stores only cumulative counts of valid UTF-16 surrogate-pair
 * starts at 64-code-unit block boundaries. It never owns spelling, byte/char arrays, String
 * wrappers, or M3String values. Exact code-point counts use block prefixes plus at most one block
 * of local UTF-16 verification per boundary. Offset navigation uses monotone exact range counts.
 * Cache absence, eviction, budget refusal, or allocation failure falls back to exact linear
 * traversal and therefore cannot change String semantics.</p>
 */
final class M3StringCodePointPrecompute {
    private static final int BLOCK_SHIFT = 6;
    private static final int BLOCK_SIZE = 1 << BLOCK_SHIFT;
    private static final int BLOCK_MASK = BLOCK_SIZE - 1;

    private static final int SLOTS = 64;
    private static final int SLOT_MASK = SLOTS - 1;
    private static final int MIN_SOURCE_UNITS = 256;
    private static final int MAX_SOURCE_UNITS = 32_768;

    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(SLOTS);

    private M3StringCodePointPrecompute() {}

    static int codePointCount(M3String source, int beginIndex, int endIndex) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromToIndex(beginIndex, endIndex, source.length());
        if (beginIndex == endIndex) return 0;
        if (source.coder() == String.LATIN1) return endIndex - beginIndex;

        Navigation navigation = prepare(source);
        return navigation == null
                ? linearCodePointCount(source, beginIndex, endIndex)
                : codePointCount(source, navigation, beginIndex, endIndex);
    }

    static int offsetByCodePoints(M3String source, int index, int codePointOffset) {
        Objects.requireNonNull(source, "source");
        int length = source.length();
        if (index < 0 || index > length) {
            throw new IndexOutOfBoundsException("index=" + index);
        }
        if (codePointOffset == 0) return index;

        M3StringFacts prepared = source.factsIfPrepared();
        if (source.coder() == String.LATIN1
                || (prepared != null && prepared.codePointCount == length)) {
            long result = (long) index + codePointOffset;
            if (result < 0L || result > length) {
                throw new IndexOutOfBoundsException("codePointOffset");
            }
            return (int) result;
        }

        Navigation navigation = prepare(source);
        if (navigation == null) {
            return linearOffsetByCodePoints(source, index, codePointOffset);
        }

        if (codePointOffset > 0) {
            int available = codePointCount(source, navigation, index, length);
            if (codePointOffset > available) {
                throw new IndexOutOfBoundsException("codePointOffset");
            }

            int low = index;
            int high = length;
            while (low < high) {
                int middle = low + ((high - low + 1) >>> 1);
                if (codePointCount(source, navigation, index, middle) <= codePointOffset) {
                    low = middle;
                } else {
                    high = middle - 1;
                }
            }
            return low;
        }

        long distance = -(long) codePointOffset;
        int available = codePointCount(source, navigation, 0, index);
        if (distance > available) {
            throw new IndexOutOfBoundsException("codePointOffset");
        }

        int low = 0;
        int high = index;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if ((long) codePointCount(source, navigation, middle, index) <= distance) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return low;
    }

    static long maximumRetainedPrimitiveBytes() {
        long blocks = ((long) MAX_SOURCE_UNITS + BLOCK_MASK) >>> BLOCK_SHIFT;
        return (long) SLOTS * (blocks + 1L) * Integer.BYTES;
    }

    private static Navigation prepare(M3String source) {
        int length = source.length();
        if (length < MIN_SOURCE_UNITS || length > MAX_SOURCE_UNITS) return null;

        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = slot(owner, coordinate);
        Entry current = CACHE.get(slot);
        if (current != null
                && current.owner.get() == owner
                && current.coordinate == coordinate
                && current.length == length) {
            return current.navigation;
        }

        try {
            int blockCount = (length + BLOCK_MASK) >>> BLOCK_SHIFT;
            int[] pairsBeforeBlock = new int[blockCount + 1];
            int pairs = 0;
            for (int block = 0; block < blockCount; block++) {
                pairsBeforeBlock[block] = pairs;
                int start = block << BLOCK_SHIFT;
                int end = Math.min(length, start + BLOCK_SIZE);
                for (int index = start; index < end; index++) {
                    if (isPairStart(source, index)) pairs++;
                }
            }
            pairsBeforeBlock[blockCount] = pairs;

            Navigation created = new Navigation(pairsBeforeBlock);
            CACHE.set(
                    slot,
                    new Entry(
                            new WeakReference<>(owner),
                            coordinate,
                            length,
                            created));
            return created;
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
    }

    private static int codePointCount(
            M3String source, Navigation navigation, int beginIndex, int endIndex) {
        if (beginIndex == endIndex) return 0;
        int pairStarts =
                pairStartsBefore(source, navigation, endIndex - 1)
                        - pairStartsBefore(source, navigation, beginIndex);
        return (endIndex - beginIndex) - pairStarts;
    }

    /**
     * Number of valid surrogate-pair starts with high-surrogate index strictly less than position.
     */
    private static int pairStartsBefore(
            M3String source, Navigation navigation, int position) {
        if (position <= 0) return 0;
        int length = source.length();
        if (position >= length) {
            return navigation.pairsBeforeBlock[navigation.pairsBeforeBlock.length - 1];
        }

        int block = position >>> BLOCK_SHIFT;
        int count = navigation.pairsBeforeBlock[block];
        int start = block << BLOCK_SHIFT;
        for (int index = start; index < position; index++) {
            if (isPairStart(source, index)) count++;
        }
        return count;
    }

    private static boolean isPairStart(M3String source, int index) {
        return index + 1 < source.length()
                && Character.isHighSurrogate(source.charAt(index))
                && Character.isLowSurrogate(source.charAt(index + 1));
    }

    private static int linearCodePointCount(M3String source, int beginIndex, int endIndex) {
        int count = endIndex - beginIndex;
        for (int index = beginIndex; index + 1 < endIndex; index++) {
            if (Character.isHighSurrogate(source.charAt(index))
                    && Character.isLowSurrogate(source.charAt(index + 1))) {
                count--;
                index++;
            }
        }
        return count;
    }

    private static int linearOffsetByCodePoints(
            M3String source, int index, int codePointOffset) {
        int length = source.length();
        int position = index;

        if (codePointOffset > 0) {
            for (int remaining = codePointOffset; remaining > 0; remaining--) {
                if (position >= length) {
                    throw new IndexOutOfBoundsException("codePointOffset");
                }
                char first = source.charAt(position++);
                if (Character.isHighSurrogate(first)
                        && position < length
                        && Character.isLowSurrogate(source.charAt(position))) {
                    position++;
                }
            }
            return position;
        }

        for (long remaining = -(long) codePointOffset; remaining > 0L; remaining--) {
            if (position <= 0) {
                throw new IndexOutOfBoundsException("codePointOffset");
            }
            char second = source.charAt(--position);
            if (Character.isLowSurrogate(second)
                    && position > 0
                    && Character.isHighSurrogate(source.charAt(position - 1))) {
                position--;
            }
        }
        return position;
    }

    private static int slot(M3StringOwner owner, long coordinate) {
        long mixed = coordinate
                ^ Long.rotateLeft(owner.structuralHash64, 23)
                ^ Integer.toUnsignedLong(System.identityHashCode(owner));
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return ((int) mixed) & SLOT_MASK;
    }

    private static final class Navigation {
        final int[] pairsBeforeBlock;

        Navigation(int[] pairsBeforeBlock) {
            this.pairsBeforeBlock = pairsBeforeBlock;
        }
    }

    private static final class Entry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final int length;
        final Navigation navigation;

        Entry(
                WeakReference<M3StringOwner> owner,
                long coordinate,
                int length,
                Navigation navigation) {
            this.owner = owner;
            this.coordinate = coordinate;
            this.length = length;
            this.navigation = navigation;
        }
    }
}
