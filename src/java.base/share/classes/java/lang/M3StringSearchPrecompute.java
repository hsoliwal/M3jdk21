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
                (long) SLOTS * MAX_PATTERN_UNITS * (2L * Integer.BYTES + Long.BYTES);
        long sourceBytes =
                (long) SOURCE_SLOTS * MAX_TRIGRAM_SOURCE_UNITS * Long.BYTES;
        return Math.addExact(patternBytes, sourceBytes);
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

        int[] prefix = new int[length];
        for (int index = 1; index < length; index++) {
            int matched = prefix[index - 1];
            char unit = pattern.charAt(index);
            while (matched > 0 && unit != pattern.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (unit == pattern.charAt(matched)) matched++;
            prefix[index] = matched;
        }

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
        Plan plan = new Plan(length, prefix, reversePrefix, trigrams);
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
            return sourceFacts(source).containsAll(plan.trigrams);
        } catch (OutOfMemoryError unavailable) {
            // Exact trigram facts are optional. KMP remains authoritative.
            return true;
        }
    }

    private static M3TQ.Facts sourceFacts(M3String source) {
        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = sourceSlot(owner, coordinate);
        SourceEntry entry = SOURCE_CACHE.get(slot);
        if (entry != null
                && entry.owner.get() == owner
                && entry.coordinate == coordinate
                && entry.utf16Length == source.length()) {
            return entry.facts;
        }

        M3TQ.Facts facts = M3TQ.precompute(source, MAX_TRIGRAM_SOURCE_UNITS);
        SOURCE_CACHE.set(
                slot,
                new SourceEntry(
                        new WeakReference<>(owner),
                        coordinate,
                        source.length(),
                        facts));
        return facts;
    }

    static int indexOf(
            M3String source,
            M3String pattern,
            Plan plan,
            int fromIndex,
            int endIndex) {
        int matched = 0;
        for (int index = fromIndex; index < endIndex; index++) {
            char unit = source.charAt(index);
            while (matched > 0 && unit != pattern.charAt(matched)) {
                matched = plan.prefix[matched - 1];
            }
            if (unit == pattern.charAt(matched)) matched++;
            if (matched == plan.patternLength) {
                return index - plan.patternLength + 1;
            }
        }
        return -1;
    }

    static int lastIndexOf(
            M3String source,
            M3String pattern,
            Plan plan,
            int maximumStart) {
        int matched = 0;
        int scanStart = maximumStart + plan.patternLength - 1;
        for (int index = scanStart; index >= 0; index--) {
            char unit = source.charAt(index);
            while (matched > 0 && unit != reverseUnit(pattern, matched)) {
                matched = plan.reversePrefix[matched - 1];
            }
            if (unit == reverseUnit(pattern, matched)) matched++;
            if (matched == plan.patternLength) {
                return index;
            }
        }
        return -1;
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

    static final class Plan {
        final int patternLength;
        final int[] prefix;
        final int[] reversePrefix;
        final M3TQ.Facts trigrams;

        Plan(int patternLength, int[] prefix, int[] reversePrefix, M3TQ.Facts trigrams) {
            this.patternLength = patternLength;
            this.prefix = prefix;
            this.reversePrefix = reversePrefix;
            this.trigrams = trigrams;
        }

        long retainedPrimitiveBytes() {
            return (long) (prefix.length + reversePrefix.length) * Integer.BYTES
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
