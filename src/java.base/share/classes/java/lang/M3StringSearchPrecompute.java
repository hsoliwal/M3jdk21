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
    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(SLOTS);

    private M3StringSearchPrecompute() {}

    static Plan prepare(M3String pattern) {
        int length = pattern.length();
        if (length < 2 || length > MAX_PATTERN_UNITS) return null;

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

        Plan plan = new Plan(length, prefix);
        CACHE.set(slot, new Entry(new WeakReference<>(owner), coordinate, plan));
        return plan;
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
        int result = -1;
        int scanEnd = maximumStart + plan.patternLength;
        for (int index = 0; index < scanEnd; index++) {
            char unit = source.charAt(index);
            while (matched > 0 && unit != pattern.charAt(matched)) {
                matched = plan.prefix[matched - 1];
            }
            if (unit == pattern.charAt(matched)) matched++;
            if (matched == plan.patternLength) {
                result = index - plan.patternLength + 1;
                matched = plan.prefix[matched - 1];
            }
        }
        return result;
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

        Plan(int patternLength, int[] prefix) {
            this.patternLength = patternLength;
            this.prefix = prefix;
        }

        long retainedPrimitiveBytes() {
            return (long) prefix.length * Integer.BYTES;
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
