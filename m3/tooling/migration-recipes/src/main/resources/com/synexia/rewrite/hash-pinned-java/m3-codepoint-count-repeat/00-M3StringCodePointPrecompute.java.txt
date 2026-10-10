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
    /** Under this many units a range is not registered: its bulk count costs less (A39). */
    private static final int MIN_REPEAT_UNITS = 1024;
    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(SLOTS);

    private M3StringCodePointPrecompute() {}

    /**
     * The geometry cached for this range, built on the range's repeat use (A39): a first use
     * registers the range and returns null, so the caller counts in bulk once; the next use
     * builds the geometry (A24), which every later use answers from. Outside the band, null; a
     * range under {@link #MIN_REPEAT_UNITS} is counted in bulk without registering, the bulk
     * count costing less than the registration.
     */
    static Geometry prepared(M3String source) {
        int length = source.length();
        if (length == 0 || length > MAX_SOURCE_UNITS) return null;
        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = slot(owner, coordinate);
        Entry entry = CACHE.get(slot);
        if (entry != null
                && entry.owner.get() == owner
                && entry.coordinate == coordinate
                && entry.utf16Length == length) {
            return entry.geometry != null ? entry.geometry : prepare(source);
        }
        if (length >= MIN_REPEAT_UNITS) {
            CACHE.set(slot, new Entry(new WeakReference<>(owner), coordinate, length, null));
        }
        return null;
    }

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
                    && entry.utf16Length == length
                    && entry.geometry != null) {
                return entry.geometry;
            }

            int[] scratch = new int[Math.min(length >>> 1, 256)];
            int count = 0;
            // The units are read once in bulk (A24): the pair scan reads an array.
            char[] units = source.units();
            char previous = units[0];
            for (int index = 1; index < length; index++) {
                char current = units[index];
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
            CACHE.set(slot, new Entry(new WeakReference<>(owner), coordinate, length, geometry));
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

        int offsetByCodePoints(int index, int codePointOffset) {
            Objects.checkIndex(index, utf16Length + 1);
            if (codePointOffset == 0) return index;

            if (codePointOffset > 0) {
                int target = codePointOffset;
                if (codePointCount(index, utf16Length) < target) {
                    throw new IndexOutOfBoundsException();
                }
                int low = index;
                int high = utf16Length;
                while (low < high) {
                    int middle = (low + high + 1) >>> 1;
                    if (codePointCount(index, middle) <= target) low = middle;
                    else high = middle - 1;
                }
                return low;
            }

            long targetLong = -(long) codePointOffset;
            if (targetLong > Integer.MAX_VALUE
                    || codePointCount(0, index) < targetLong) {
                throw new IndexOutOfBoundsException();
            }
            int target = (int) targetLong;
            int low = 0;
            int high = index;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (codePointCount(middle, index) <= target) high = middle;
                else low = middle + 1;
            }
            return low;
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

    /** A registered range; {@code geometry} is null until its repeat use builds it (A39). */
    private static final class Entry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final int utf16Length;
        final Geometry geometry;

        Entry(WeakReference<M3StringOwner> owner, long coordinate, int utf16Length, Geometry geometry) {
            this.owner = owner;
            this.coordinate = coordinate;
            this.utf16Length = utf16Length;
            this.geometry = geometry;
        }
    }
}
