/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Bounded repeated-source search precompute for canonical M3 String ranges.
 *
 * <p>This is the M3JDK adaptation of Synexia's MIndexStringSearchIndex lineage. It retains only
 * primitive UTF-16 character postings and String-compatible prefix hashes. The cache weakly keys
 * the exact canonical owner plus packed coordinate. It never owns String/M3String objects or a
 * source spelling payload, and it builds only after a source range is observed repeatedly.</p>
 *
 * <p>Search chooses the rarest pattern code unit, seeks its ordered postings, rejects candidates
 * with an O(1) Java String region hash, and then performs exact UTF-16 verification. Hash equality
 * is never semantic authority. Dense-posting cases fall back to the existing prepared KMP/BMH
 * path.</p>
 */
final class M3StringSourcePrecompute {
    static final int FALLBACK = Integer.MIN_VALUE;

    private static final int SLOTS = 16;
    private static final int SLOT_MASK = SLOTS - 1;
    private static final int MIN_SOURCE_UNITS = 512;
    private static final int MAX_SOURCE_UNITS = 16_384;
    private static final int MIN_PATTERN_UNITS = 3;
    private static final int HOT_REQUESTS = 2;

    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(SLOTS);

    private M3StringSourcePrecompute() {}

    static int indexOf(
            M3String source,
            M3String pattern,
            int fromIndex,
            int endIndex) {
        if (pattern.length() < MIN_PATTERN_UNITS) return FALLBACK;
        SourceIndex index = prepareIfHot(source);
        if (index == null) return FALLBACK;
        return index.indexOf(source, pattern, fromIndex, endIndex);
    }

    static int lastIndexOf(
            M3String source,
            M3String pattern,
            int maximumStart) {
        if (pattern.length() < MIN_PATTERN_UNITS) return FALLBACK;
        SourceIndex index = prepareIfHot(source);
        if (index == null) return FALLBACK;
        return index.lastIndexOf(source, pattern, maximumStart);
    }

    static long maximumRetainedPrimitiveBytes() {
        // Worst case: every UTF-16 unit is distinct until the 65,536-unit alphabet saturates.
        // Per cached source: positions[N] + prefix[N+1] + chars[U] + postingStart[U+1]
        // + open-address slots[<=4U]. The bound intentionally overestimates U with N.
        long perEntry =
                Math.addExact(
                        2L * MAX_SOURCE_UNITS * Integer.BYTES,
                        Math.addExact(
                                (long) MAX_SOURCE_UNITS * Character.BYTES,
                                Math.addExact(
                                        (long) (MAX_SOURCE_UNITS + 1) * Integer.BYTES,
                                        4L * MAX_SOURCE_UNITS * Integer.BYTES)));
        return Math.multiplyExact(SLOTS, perEntry);
    }

    static boolean isPrepared(M3String source) {
        int length = source.length();
        if (length < MIN_SOURCE_UNITS || length > MAX_SOURCE_UNITS) return false;
        Entry entry = CACHE.get(slot(source.owner(), source.coordinate()));
        return matches(entry, source) && entry.index != null;
    }

    private static SourceIndex prepareIfHot(M3String source) {
        int length = source.length();
        if (length < MIN_SOURCE_UNITS || length > MAX_SOURCE_UNITS) return null;

        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = slot(owner, coordinate);
        Entry entry = CACHE.get(slot);

        if (!matches(entry, source)) {
            CACHE.set(
                    slot,
                    new Entry(
                            new WeakReference<>(owner),
                            coordinate,
                            length));
            return null;
        }

        SourceIndex current = entry.index;
        if (current != null) return current;

        synchronized (entry) {
            current = entry.index;
            if (current != null) return current;
            if (++entry.requests < HOT_REQUESTS) return null;
            try {
                current = SourceIndex.build(source);
            } catch (OutOfMemoryError unavailable) {
                return null;
            }
            entry.index = current;
            return current;
        }
    }

    private static boolean matches(Entry entry, M3String source) {
        return entry != null
                && entry.owner.get() == source.owner()
                && entry.coordinate == source.coordinate()
                && entry.length == source.length();
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

    private static final class SourceIndex {
        final char[] characters;
        final int[] postingStart;
        final int[] positions;
        final int[] slots;
        final int[] prefixHashes;

        SourceIndex(
                char[] characters,
                int[] postingStart,
                int[] positions,
                int[] slots,
                int[] prefixHashes) {
            this.characters = characters;
            this.postingStart = postingStart;
            this.positions = positions;
            this.slots = slots;
            this.prefixHashes = prefixHashes;
        }

        static SourceIndex build(M3String source) {
            int length = source.length();
            int[] frequencies = new int[Character.MAX_VALUE + 1];
            int[] prefixHashes = new int[length + 1];
            for (int index = 0; index < length; index++) {
                char unit = source.charAt(index);
                frequencies[unit]++;
                prefixHashes[index + 1] = 31 * prefixHashes[index] + unit;
            }

            int unique = 0;
            for (int count : frequencies) {
                if (count != 0) unique++;
            }

            char[] characters = new char[unique];
            int[] postingStart = new int[unique + 1];
            int row = 0;
            int cumulative = 0;
            for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
                int count = frequencies[unit];
                if (count == 0) continue;
                characters[row] = (char) unit;
                postingStart[row] = cumulative;
                cumulative += count;
                row++;
            }
            postingStart[unique] = cumulative;

            int capacity = 2;
            while (capacity < Math.max(2, unique << 1)) {
                capacity <<= 1;
            }
            int[] slots = new int[capacity];
            int mask = capacity - 1;
            for (row = 0; row < unique; row++) {
                int at = mix(characters[row]) & mask;
                while (slots[at] != 0) at = (at + 1) & mask;
                slots[at] = row + 1;
            }

            int[] positions = new int[length];
            int[] cursor = postingStart.clone();
            for (int index = 0; index < length; index++) {
                int characterRow = row(characters, slots, source.charAt(index));
                positions[cursor[characterRow]++] = index;
            }

            return new SourceIndex(
                    characters, postingStart, positions, slots, prefixHashes);
        }

        int indexOf(
                M3String source,
                M3String pattern,
                int fromIndex,
                int endIndex) {
            int from = Math.max(0, fromIndex);
            int end = Math.min(source.length(), endIndex);
            int patternLength = pattern.length();
            int maximumStart = end - patternLength;
            if (from > maximumStart) return -1;

            Anchor anchor = rarestAnchor(pattern);
            if (anchor.row < 0) return -1;
            int first = postingStart[anchor.row];
            int last = postingStart[anchor.row + 1];
            int candidates = last - first;
            int searchUnits = end - from;
            if (candidates > Math.max(64, searchUnits >>> 2)) return FALLBACK;

            int patternHash = pattern.hashCodeValue();
            int power = M3String.pow31(patternLength);
            first = lowerBound(positions, first, last, from + anchor.offset);
            last = upperBound(
                    positions, first, last, maximumStart + anchor.offset);

            for (int posting = first; posting < last; posting++) {
                int start = positions[posting] - anchor.offset;
                if (regionHash(start, start + patternLength, power) != patternHash) continue;
                if (regionEquals(source, start, pattern)) return start;
            }
            return -1;
        }

        int lastIndexOf(
                M3String source,
                M3String pattern,
                int maximumStart) {
            int patternLength = pattern.length();
            int maxStart = Math.min(maximumStart, source.length() - patternLength);
            if (maxStart < 0) return -1;

            Anchor anchor = rarestAnchor(pattern);
            if (anchor.row < 0) return -1;
            int first = postingStart[anchor.row];
            int last = postingStart[anchor.row + 1];
            int candidates = last - first;
            if (candidates > Math.max(64, (maxStart + patternLength) >>> 2)) {
                return FALLBACK;
            }

            int patternHash = pattern.hashCodeValue();
            int power = M3String.pow31(patternLength);
            int posting =
                    upperBound(
                                    positions,
                                    first,
                                    last,
                                    maxStart + anchor.offset)
                            - 1;
            for (; posting >= first; posting--) {
                int start = positions[posting] - anchor.offset;
                if (start < 0) break;
                if (regionHash(start, start + patternLength, power) != patternHash) continue;
                if (regionEquals(source, start, pattern)) return start;
            }
            return -1;
        }

        private Anchor rarestAnchor(M3String pattern) {
            int bestRow = -1;
            int bestOffset = 0;
            int bestCount = Integer.MAX_VALUE;
            for (int offset = 0; offset < pattern.length(); offset++) {
                int row = row(characters, slots, pattern.charAt(offset));
                if (row < 0) return new Anchor(-1, offset);
                int count = postingStart[row + 1] - postingStart[row];
                if (count < bestCount) {
                    bestCount = count;
                    bestRow = row;
                    bestOffset = offset;
                    if (count == 1) break;
                }
            }
            return new Anchor(bestRow, bestOffset);
        }

        private int regionHash(int beginIndex, int endIndex, int power) {
            return prefixHashes[endIndex] - prefixHashes[beginIndex] * power;
        }

        private static boolean regionEquals(
                M3String source, int sourceStart, M3String pattern) {
            for (int index = 0; index < pattern.length(); index++) {
                if (source.charAt(sourceStart + index) != pattern.charAt(index)) return false;
            }
            return true;
        }
    }

    private static int row(char[] characters, int[] slots, char unit) {
        if (characters.length == 0) return -1;
        int mask = slots.length - 1;
        int at = mix(unit) & mask;
        for (int probe = 0; probe < slots.length; probe++) {
            int encoded = slots[at];
            if (encoded == 0) return -1;
            int row = encoded - 1;
            if (characters[row] == unit) return row;
            at = (at + 1) & mask;
        }
        return -1;
    }

    private static int lowerBound(int[] values, int from, int to, int sought) {
        while (from < to) {
            int middle = (from + to) >>> 1;
            if (values[middle] < sought) from = middle + 1;
            else to = middle;
        }
        return from;
    }

    private static int upperBound(int[] values, int from, int to, int sought) {
        while (from < to) {
            int middle = (from + to) >>> 1;
            if (values[middle] <= sought) from = middle + 1;
            else to = middle;
        }
        return from;
    }

    private static int mix(char value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        return mixed ^ (mixed >>> 16);
    }

    private record Anchor(int row, int offset) {}
    }

    private static final class Entry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final int length;
        int requests = 1;
        volatile SourceIndex index;

        Entry(
                WeakReference<M3StringOwner> owner,
                long coordinate,
                int length) {
            this.owner = owner;
            this.coordinate = coordinate;
            this.length = length;
        }
    }
}
