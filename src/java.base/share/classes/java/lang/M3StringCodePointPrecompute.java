/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Separately bounded exact code-point geometry for one immutable M3 String range.
 *
 * <p>The retained lane stores only relative UTF-16 offsets of low surrogates that are paired
 * with an immediately preceding high surrogate inside the exact source range. Therefore:</p>
 *
 * <pre>
 * codePointCount(begin,end)
 *   = (end-begin) - pairedLowCount(begin+1,end)
 * </pre>
 *
 * <p>No text, char[], byte[], canonical IDs, or per-String fields are introduced. Cache loss or
 * budget refusal changes performance only; callers fall back to the exact JDK traversal.</p>
 */
final class M3StringCodePointPrecompute {
    private static final int SLOTS = 64;
    private static final int SLOT_MASK = SLOTS - 1;
    private static final int MAX_SOURCE_UNITS = 32_768;
    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(SLOTS);

    private M3StringCodePointPrecompute() {}

    static Geometry prepare(M3String source) {
        int length = source.length();
        if (length == 0 || length > MAX_SOURCE_UNITS) return null;
        try {
            M3StringOwner owner = source.owner();
            long coordinate = source.coordinate();
            int slot = slot(owner, coordinate);
            Entry entry = CACHE.get(slot);
            if (entry != null
                    && entry.owner.get() == owner
                    && entry.coordinate == coordinate
                    && entry.geometry.utf16Length == length) {
                return entry.geometry;
            }

            int[] scratch = new int[Math.min(length >>> 1, 256)];
            int count = 0;
            // Reuse one tuple-aware cursor: each scalar leaf is resolved once per run.
            M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(source);
            char previous = units.at(0);
            for (int index = 1; index < length; index++) {
                char current = units.at(index);
                if (Character.isHighSurrogate(previous)
                        && Character.isLowSurrogate(current)) {
                    if (count == scratch.length) {
                        int next = Math.min(length >>> 1, Math.max(1, scratch.length << 1));
                        scratch = Arrays.copyOf(scratch, next);
                    }
                    scratch[count++] = index;
                }
                previous = current;
            }

            Geometry geometry =
                    new Geometry(length, count == scratch.length ? scratch : Arrays.copyOf(scratch, count));
            CACHE.set(slot, new Entry(new WeakReference<>(owner), coordinate, geometry));
            return geometry;
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
    }

    static long maximumRetainedPrimitiveBytes() {
        return (long) SLOTS * (MAX_SOURCE_UNITS >>> 1) * Integer.BYTES;
    }

    private static int slot(M3StringOwner owner, long coordinate) {
        long mixed = Integer.toUnsignedLong(System.identityHashCode(owner))
                ^ Long.rotateLeft(coordinate, 17)
                ^ Long.rotateLeft(owner.structuralHash64, 31);
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return ((int) mixed) & SLOT_MASK;
    }

    static final class Geometry {
        final int utf16Length;
        private final int[] pairedLowOffsets;

        Geometry(int utf16Length, int[] pairedLowOffsets) {
            this.utf16Length = utf16Length;
            this.pairedLowOffsets = pairedLowOffsets;
        }

        int codePointCount(int beginIndex, int endIndex) {
            Objects.checkFromToIndex(beginIndex, endIndex, utf16Length);
            if (beginIndex == endIndex) return 0;
            int firstPair = lowerBound(pairedLowOffsets, beginIndex + 1);
            int afterLastPair = lowerBound(pairedLowOffsets, endIndex);
            return (endIndex - beginIndex) - (afterLastPair - firstPair);
        }

        /**
         * Rank/select over paired-low UTF-16 coordinates. A paired low at q consumes
         * one extra code unit; q - its ordinal is strictly increasing because
         * non-overlapping surrogate pairs cannot have adjacent low positions.
         *
         * Both directions use one binary search over transformed pair offsets,
         * not a binary search whose predicate repeatedly binary-searches the lane.
         * At a boundary inside a pair, the initial low/high unit is independent
         * in the traversal direction, exactly as Character.offsetByCodePoints.
         */
        int offsetByCodePoints(int index, int codePointOffset) {
            Objects.checkIndex(index, utf16Length + 1);
            if (codePointOffset == 0) return index;

            if (codePointOffset > 0) {
                if (codePointCount(index, utf16Length) < codePointOffset) {
                    throw new IndexOutOfBoundsException();
                }
                // Pair lows at index are not included: starting on a low surrogate
                // must consume it as an independent code point.
                int first = lowerBound(pairedLowOffsets, index + 1);
                long threshold = (long) index + codePointOffset - first;
                int low = first;
                int high = pairedLowOffsets.length;
                while (low < high) {
                    int middle = (low + high) >>> 1;
                    if ((long) pairedLowOffsets[middle] - middle <= threshold) {
                        low = middle + 1;
                    } else {
                        high = middle;
                    }
                }
                return index + codePointOffset + (low - first);
            }

            long backward = -(long) codePointOffset;
            if (backward > Integer.MAX_VALUE || codePointCount(0, index) < backward) {
                throw new IndexOutOfBoundsException();
            }
            int steps = (int) backward;
            // Pair lows at index are excluded when traversing backward, too.
            int limit = lowerBound(pairedLowOffsets, index);
            long threshold = (long) index - steps - (limit - 1L);
            int low = 0;
            int high = limit;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if ((long) pairedLowOffsets[middle] - middle < threshold) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return index - steps - (limit - low);
        }

        int pairedLowCount() {
            return pairedLowOffsets.length;
        }

        long retainedPrimitiveBytes() {
            return (long) pairedLowOffsets.length * Integer.BYTES;
        }
    }

    private static int lowerBound(int[] values, int key) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (values[middle] < key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static final class Entry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final Geometry geometry;

        Entry(WeakReference<M3StringOwner> owner, long coordinate, Geometry geometry) {
            this.owner = owner;
            this.coordinate = coordinate;
            this.geometry = geometry;
        }
    }
}
