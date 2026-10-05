/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

package java.lang;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Separately bounded length-proportional search precompute for M3 String.
 *
 * <p>The cache never owns pattern text. Keys weakly reference the canonical M3 String storage
 * identity and plans retain only primitive KMP failure lanes. Cache loss changes performance only;
 * exact UTF-16 comparison remains authoritative.</p>
 */
final class M3StringSearchPrecompute {
    private static final int SLOTS = 256;
    private static final int SLOT_MASK = SLOTS - 1;
    private static final int MAX_PATTERN_UNITS = 8_192;
    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(SLOTS);

    private M3StringSearchPrecompute() {}

    static Plan prepare(MIndexString pattern) {
        int length = pattern.length();
        if (length < 2 || length > MAX_PATTERN_UNITS) {
            return null;
        }
        int slot = slot(pattern.canonicalId(), pattern.structuralHash64());
        Entry entry = CACHE.get(slot);
        if (entry != null) {
            MIndexString owner = entry.owner.get();
            if (owner == pattern
                    && entry.canonicalId == pattern.canonicalId()
                    && entry.structuralHash64 == pattern.structuralHash64()
                    && entry.plan.patternLength == length) {
                return entry.plan;
            }
        }

        int[] prefix = new int[length];
        for (int index = 1; index < length; index++) {
            int matched = prefix[index - 1];
            char unit = pattern.charAt(index);
            while (matched > 0 && unit != pattern.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (unit == pattern.charAt(matched)) {
                matched++;
            }
            prefix[index] = matched;
        }

        Plan plan = new Plan(length, prefix);
        CACHE.set(
                slot,
                new Entry(
                        new WeakReference<>(pattern),
                        pattern.canonicalId(),
                        pattern.structuralHash64(),
                        plan));
        return plan;
    }

    static int indexOf(
            MIndexString source,
            MIndexString pattern,
            Plan plan,
            int fromIndex,
            int endIndex) {
        int matched = 0;
        for (int index = fromIndex; index < endIndex; index++) {
            char unit = source.charAt(index);
            while (matched > 0 && unit != pattern.charAt(matched)) {
                matched = plan.prefix[matched - 1];
            }
            if (unit == pattern.charAt(matched)) {
                matched++;
            }
            if (matched == plan.patternLength) {
                return index - plan.patternLength + 1;
            }
        }
        return -1;
    }

    static int lastIndexOf(
            MIndexString source,
            MIndexString pattern,
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
            if (unit == pattern.charAt(matched)) {
                matched++;
            }
            if (matched == plan.patternLength) {
                result = index - plan.patternLength + 1;
                matched = plan.prefix[matched - 1];
            }
        }
        return result;
    }

    private static int slot(long canonicalId, long structuralHash64) {
        long mixed = canonicalId ^ Long.rotateLeft(structuralHash64, 21);
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
        final WeakReference<MIndexString> owner;
        final long canonicalId;
        final long structuralHash64;
        final Plan plan;

        Entry(
                WeakReference<MIndexString> owner,
                long canonicalId,
                long structuralHash64,
                Plan plan) {
            this.owner = owner;
            this.canonicalId = canonicalId;
            this.structuralHash64 = structuralHash64;
            this.plan = plan;
        }
    }
}
