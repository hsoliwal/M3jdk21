/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import jdk.internal.mindex.M3TQ;

/**
 * Bounded length-proportional operation precompute for canonical M3 String patterns.
 *
 * <p>Unlike {@link M3StringFacts}, this cache may retain O(pattern length) primitive metadata.
 * It never owns pattern text: entries weakly reference the canonical owner and key the exact
 * packed coordinate. Eviction or absence changes performance only.</p>
 */
final class M3StringSearchPrecompute {
    private static final int SLOTS = 256;
    private static final int SLOT_MASK = SLOTS - 1;
    private static final int MAX_PATTERN_UNITS = 8_192;
    private static final int SOURCE_SLOTS = 64;
    private static final int SOURCE_SLOT_MASK = SOURCE_SLOTS - 1;
    private static final int MIN_TRIGRAM_SOURCE_UNITS = 256;
    private static final int MAX_TRIGRAM_SOURCE_UNITS = 32_768;

    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(SLOTS);
    private static final AtomicReferenceArray<SourceEntry> SOURCE_CACHE =
            new AtomicReferenceArray<>(SOURCE_SLOTS);

    private M3StringSearchPrecompute() {}

    /**
     * Conservative upper bound for primitive metadata retained by both direct-mapped caches.
     * Object/reference overhead is intentionally excluded; cache cardinality is separately fixed.
     */
    static long maximumRetainedPrimitiveBytes() {
        long patternBytes =
                (long) SLOTS * MAX_PATTERN_UNITS * (Integer.BYTES + Long.BYTES);
        long sourceBytes =
                (long) SOURCE_SLOTS * MAX_TRIGRAM_SOURCE_UNITS * Long.BYTES;
        return Math.addExact(patternBytes, sourceBytes);
    }

    /**
     * The needle's plan for a search over {@code source} (A36): {@code null} when the source is
     * outside the trigram band, where the plan would serve nothing.
     */
    static Plan planFor(M3String source, M3String pattern) {
        int length = source.length();
        if (length < MIN_TRIGRAM_SOURCE_UNITS || length > MAX_TRIGRAM_SOURCE_UNITS) return null;
        return prepare(pattern);
    }

    static Plan prepare(M3String pattern) {
        int length = pattern.length();
        if (length < 2 || length > MAX_PATTERN_UNITS) return null;

        try {
            return prepareAllocated(pattern, length);
        } catch (OutOfMemoryError unavailable) {
            // Precompute is never semantic authority. Under memory pressure, fall back to the
            // allocation-free exact search path instead of changing String.indexOf behavior.
            return null;
        }
    }

    private static Plan prepareAllocated(M3String pattern, int length) {
        M3StringOwner owner = pattern.owner();
        long coordinate = pattern.coordinate();
        int slot = slot(owner, coordinate);
        Entry entry = CACHE.get(slot);
        if (entry != null
                && entry.owner.get() == owner
                && entry.coordinate == coordinate
                && entry.plan.patternLength == length) {
            return entry.plan;
        }

        // The plan carries the reverse KMP table (the flat haystack's reverse search of a long
        // needle, A21) and the needle's trigram facts (the gate over an M3 source); the forward
        // tables, unused since A21 and A35, are no longer built (A36).
        int[] reversePrefix = new int[length];
        for (int index = 1; index < length; index++) {
            int matched = reversePrefix[index - 1];
            char unit = reverseUnit(pattern, index);
            while (matched > 0 && unit != reverseUnit(pattern, matched)) {
                matched = reversePrefix[matched - 1];
            }
            if (unit == reverseUnit(pattern, matched)) matched++;
            reversePrefix[index] = matched;
        }

        M3TQ.Facts trigrams = length >= 3 ? M3TQ.precompute(pattern, MAX_PATTERN_UNITS) : null;
        Plan plan = new Plan(length, reversePrefix, trigrams);
        CACHE.set(slot, new Entry(new WeakReference<>(owner), coordinate, plan));
        return plan;
    }

    static boolean mayContain(M3String source, Plan plan) {
        if (plan.trigrams == null
                || source.length() < MIN_TRIGRAM_SOURCE_UNITS
                || source.length() > MAX_TRIGRAM_SOURCE_UNITS) {
            return true;
        }
        try {
            M3TQ.Facts facts = sourceFacts(source);
            return facts == null || facts.containsAll(plan.trigrams);
        } catch (OutOfMemoryError unavailable) {
            // Exact trigram facts are optional. The exact search remains authoritative.
            return true;
        }
    }

    /**
     * The whole-String trigram facts of an M3-backed String inside the source band, computed
     * on the repeat use of a source (A34) and shared with the mixed indexOf paths (A18):
     * {@code null} for a flat String, a length outside the band, a first use, or when the facts
     * are unavailable, so the caller runs its exact search.
     */
    static M3TQ.Facts trigramFacts(String text) {
        M3String source = text.m3();
        if (source == null
                || source.length() < MIN_TRIGRAM_SOURCE_UNITS
                || source.length() > MAX_TRIGRAM_SOURCE_UNITS) {
            return null;
        }
        try {
            return sourceFacts(source);
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
    }

    /**
     * The trigram facts of a source used before, or {@code null} when this use should run its
     * exact search (A34): a first use registers the source without facts, the next use of the
     * same source builds them, so a source searched once never pays the facts and a source
     * searched again pays them once. A source that keeps losing its cache slot keeps scanning.
     */
    private static M3TQ.Facts sourceFacts(M3String source) {
        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = sourceSlot(owner, coordinate);
        SourceEntry entry = SOURCE_CACHE.get(slot);
        if (entry != null
                && entry.owner.get() == owner
                && entry.coordinate == coordinate
                && entry.utf16Length == source.length()) {
            if (entry.facts != null) return entry.facts;
            M3TQ.Facts facts = M3TQ.precompute(source, MAX_TRIGRAM_SOURCE_UNITS);
            SOURCE_CACHE.set(slot, new SourceEntry(entry.owner, coordinate, source.length(), facts));
            return facts;
        }

        SOURCE_CACHE.set(
                slot,
                new SourceEntry(
                        new WeakReference<>(owner),
                        coordinate,
                        source.length(),
                        null));
        return null;
    }

    /**
     * An M3 needle of this many units and up runs the reverse skip search for lastIndexOf over
     * a flat haystack; a shorter one, and every needle for indexOf, is read once in bulk in
     * String and takes the flat needle's search (A21).
     */
    static final int LONG_NEEDLE = 16;

    static int lastIndexOf(
            byte[] source,
            byte sourceCoder,
            int sourceCount,
            M3String pattern,
            int fromIndex) {
        int patternLength = pattern.length();
        int maximumStart = Math.min(fromIndex, sourceCount - patternLength);
        if (maximumStart < 0) return -1;
        if (patternLength == 0) return maximumStart;
        char[] needle = pattern.units();
        if (patternLength == 1) {
            char wanted = needle[0];
            for (int index = maximumStart; index >= 0; index--) {
                if (sourceUnit(source, sourceCoder, index) == wanted) return index;
            }
            return -1;
        }

        Plan plan = prepare(pattern);
        if (plan != null) {
            int matched = 0;
            int scanStart = maximumStart + plan.patternLength - 1;
            for (int index = scanStart; index >= 0; index--) {
                char unit = sourceUnit(source, sourceCoder, index);
                while (matched > 0 && unit != needle[patternLength - 1 - matched]) {
                    matched = plan.reversePrefix[matched - 1];
                }
                if (unit == needle[patternLength - 1 - matched]) matched++;
                if (matched == plan.patternLength) return index;
            }
            return -1;
        }

        char first = needle[0];
        for (int candidate = maximumStart; candidate >= 0; candidate--) {
            if (sourceUnit(source, sourceCoder, candidate) != first) continue;
            int index = 1;
            while (index < patternLength
                    && sourceUnit(source, sourceCoder, candidate + index) == needle[index]) {
                index++;
            }
            if (index == patternLength) return candidate;
        }
        return -1;
    }

    private static char sourceUnit(byte[] source, byte sourceCoder, int index) {
        return sourceCoder == String.LATIN1
                ? StringLatin1.charAt(source, index)
                : StringUTF16.charAt(source, index);
    }

    private static char reverseUnit(M3String pattern, int reverseIndex) {
        return pattern.charAt(pattern.length() - 1 - reverseIndex);
    }

    private static int slot(M3StringOwner owner, long coordinate) {
        long mixed = Integer.toUnsignedLong(System.identityHashCode(owner));
        mixed ^= Long.rotateLeft(coordinate, 19);
        mixed ^= Long.rotateLeft(owner.structuralHash64, 37);
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return ((int) mixed) & SLOT_MASK;
    }

    /** A needle's reverse KMP table and trigram facts (A36: no forward tables). */
    static final class Plan {
        final int patternLength;
        final int[] reversePrefix;
        final M3TQ.Facts trigrams;

        Plan(int patternLength, int[] reversePrefix, M3TQ.Facts trigrams) {
            this.patternLength = patternLength;
            this.reversePrefix = reversePrefix;
            this.trigrams = trigrams;
        }

        long retainedPrimitiveBytes() {
            return (long) reversePrefix.length * Integer.BYTES
                    + (trigrams == null ? 0L : (long) trigrams.keyCount() * Long.BYTES);
        }
    }

    private static int sourceSlot(M3StringOwner owner, long coordinate) {
        long mixed = Long.rotateLeft(coordinate, 11)
                ^ Long.rotateLeft(owner.structuralHash64, 29)
                ^ Integer.toUnsignedLong(System.identityHashCode(owner));
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return ((int) mixed) & SOURCE_SLOT_MASK;
    }

    private static final class SourceEntry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final int utf16Length;
        final M3TQ.Facts facts;

        SourceEntry(
                WeakReference<M3StringOwner> owner,
                long coordinate,
                int utf16Length,
                M3TQ.Facts facts) {
            this.owner = owner;
            this.coordinate = coordinate;
            this.utf16Length = utf16Length;
            this.facts = facts;
        }
    }

    private static final class Entry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final Plan plan;

        Entry(WeakReference<M3StringOwner> owner, long coordinate, Plan plan) {
            this.owner = owner;
            this.coordinate = coordinate;
            this.plan = plan;
        }
    }
}
