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
    /**
     * A flat needle's exact-trigram gate (A42) serves sources of this many units and up: below,
     * the stock search over the whole source costs about what the probes do.
     */
    private static final int FLAT_NEEDLE_GATE_UNITS = 2048;

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
                (long) SLOTS * (
                        (long) MAX_PATTERN_UNITS * (2L * Integer.BYTES + Long.BYTES)
                                + 256L * Integer.BYTES);
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

        M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(pattern);
        int[] prefix = new int[length];
        for (int index = 1; index < length; index++) {
            int matched = prefix[index - 1];
            char unit = units.at(index);
            while (matched > 0 && unit != units.at(matched)) {
                matched = prefix[matched - 1];
            }
            if (unit == units.at(matched)) matched++;
            prefix[index] = matched;
        }

        int[] reversePrefix = new int[length];
        for (int index = 1; index < length; index++) {
            int matched = reversePrefix[index - 1];
            char unit = units.at(length - 1 - index);
            while (matched > 0 && unit != units.at(length - 1 - matched)) {
                matched = reversePrefix[matched - 1];
            }
            if (unit == units.at(length - 1 - matched)) matched++;
            reversePrefix[index] = matched;
        }

        int[] skip256 = new int[256];
        Arrays.fill(skip256, Math.max(1, length));
        for (int index = 0; index < length - 1; index++) {
            skip256[units.at(index) & 255] = length - 1 - index;
        }

        M3TQ.Facts trigrams =
                length >= 3 ? precomputeTrigrams(pattern, MAX_PATTERN_UNITS) : null;
        Plan plan = new Plan(length, prefix, reversePrefix, skip256, trigrams);
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

    /**
     * Exact-trigram absence gate for a flat needle over {@code source} (A42): the source's
     * trigram facts, the ones of the M3 needle's lane, are probed with the needle's middle
     * trigram key and then its first (its last, for a four-unit needle), packed on the fly as
     * {@link M3TQ} packs them, without allocation; a key the source lacks proves the needle
     * absent. A source's first flat-needle search registers it without facts and its next one
     * builds them ({@link #repeatSourceFacts}), so a source searched once never pays the scan.
     * {@code true} for a needle under three units, a source outside the gate's band, a source
     * searched for the first time, and whenever the facts are unavailable: the exact search
     * remains authoritative. The facts cover the whole source, so absence from the whole proves
     * absence from any range of it.
     */
    static boolean mayContain(M3String source, String needle) {
        int needleLength = needle.length();
        int length = source.length();
        if (needleLength < 3 || length < FLAT_NEEDLE_GATE_UNITS || length > MAX_TRIGRAM_SOURCE_UNITS) {
            return true;
        }
        M3TQ.Facts facts;
        try {
            facts = repeatSourceFacts(source);
        } catch (OutOfMemoryError unavailable) {
            return true;
        }
        if (facts == null) return true;
        int last = needleLength - 3;
        int middle = last >>> 1;
        return facts.test(trigramKey(needle, middle))
                && (last == 0 || facts.test(trigramKey(needle, middle == 0 ? last : 0)));
    }

    /** The packed 48-bit key of the trigram of {@code needle} at {@code at}, as {@link M3TQ} packs it. */
    private static long trigramKey(String needle, int at) {
        return ((long) needle.charAt(at) << 32)
                | ((long) needle.charAt(at + 1) << 16)
                | needle.charAt(at + 2);
    }

    /**
     * The trigram facts of a source searched before by a flat needle, or {@code null} when this
     * search should run without them (A42, the A34 register-then-build rule): a first use
     * registers the source in its slot without facts, the next use of the same source builds
     * them through {@link #sourceFacts}, which the M3 needle's lane shares.
     */
    private static M3TQ.Facts repeatSourceFacts(M3String source) {
        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = sourceSlot(owner, coordinate);
        SourceEntry entry = SOURCE_CACHE.get(slot);
        if (entry != null
                && entry.owner.get() == owner
                && entry.coordinate == coordinate
                && entry.utf16Length == source.length()) {
            return entry.facts != null ? entry.facts : sourceFacts(source);
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
     * The trigram facts of {@code source}, built on its first use by the M3 needle's lane; an
     * entry registered without facts by the flat-needle gate (A42) is built here as well.
     */
    private static M3TQ.Facts sourceFacts(M3String source) {
        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = sourceSlot(owner, coordinate);
        SourceEntry entry = SOURCE_CACHE.get(slot);
        if (entry != null
                && entry.facts != null
                && entry.owner.get() == owner
                && entry.coordinate == coordinate
                && entry.utf16Length == source.length()) {
            return entry.facts;
        }

        M3TQ.Facts facts = precomputeTrigrams(source, MAX_TRIGRAM_SOURCE_UNITS);
        SOURCE_CACHE.set(
                slot,
                new SourceEntry(
                        new WeakReference<>(owner),
                        coordinate,
                        source.length(),
                        facts));
        return facts;
    }

    /**
     * Computes exact trigram facts with sequential reads through the current tuple leaf.
     * The cursor-backed view is temporary; M3TQ.Facts retains no source or cursor reference.
     */
    private static M3TQ.Facts precomputeTrigrams(M3String value, int maxUtf16Units) {
        return M3TQ.precompute(new CursorCharSequence(value), maxUtf16Units);
    }

    private static final class CursorCharSequence implements CharSequence {
        private final M3String.CodeUnitCursor cursor;
        private final int start;
        private final int length;

        private CursorCharSequence(M3String value) {
            this(new M3String.CodeUnitCursor(value), 0, value.length());
        }

        private CursorCharSequence(M3String.CodeUnitCursor cursor, int start, int length) {
            this.cursor = cursor;
            this.start = start;
            this.length = length;
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public char charAt(int index) {
            Objects.checkIndex(index, length);
            return cursor.at(start + index);
        }

        @Override
        public CharSequence subSequence(int beginIndex, int endIndex) {
            Objects.checkFromToIndex(beginIndex, endIndex, length);
            return new CursorCharSequence(cursor, start + beginIndex, endIndex - beginIndex);
        }

        @Override
        public String toString() {
            StringBuilder text = new StringBuilder(length);
            for (int index = 0; index < length; index++) text.append(charAt(index));
            return text.toString();
        }
    }

    static int indexOf(
            M3String source,
            M3String pattern,
            Plan plan,
            int fromIndex,
            int endIndex) {
        if (plan.patternLength >= 8) {
            return adaptiveBmh(source, pattern, plan, fromIndex, endIndex);
        }
        return kmp(source, pattern, plan, fromIndex, endIndex);
    }

    private static int kmp(
            M3String source,
            M3String pattern,
            Plan plan,
            int fromIndex,
            int endIndex) {
        M3String.CodeUnitCursor sourceUnits = new M3String.CodeUnitCursor(source);
        M3String.CodeUnitCursor patternUnits = new M3String.CodeUnitCursor(pattern);
        int matched = 0;
        for (int index = fromIndex; index < endIndex; index++) {
            char unit = sourceUnits.at(index);
            while (matched > 0 && unit != patternUnits.at(matched)) {
                matched = plan.prefix[matched - 1];
            }
            if (unit == patternUnits.at(matched)) matched++;
            if (matched == plan.patternLength) {
                return index - plan.patternLength + 1;
            }
        }
        return -1;
    }

    private static int adaptiveBmh(
            M3String source,
            M3String pattern,
            Plan plan,
            int fromIndex,
            int endIndex) {
        M3String.CodeUnitCursor sourceUnits = new M3String.CodeUnitCursor(source);
        M3String.CodeUnitCursor patternUnits = new M3String.CodeUnitCursor(pattern);
        int maximumStart = endIndex - plan.patternLength;
        int at = fromIndex;
        long failedComparisonWork = 0L;
        while (at <= maximumStart) {
            int index = plan.patternLength - 1;
            while (index >= 0 && patternUnits.at(index) == sourceUnits.at(at + index)) {
                index--;
            }
            if (index < 0) return at;

            int shift = plan.skip256[sourceUnits.at(at + plan.patternLength - 1) & 255];
            if (shift > maximumStart - at) return -1;
            at += shift;

            failedComparisonWork += plan.patternLength - index;
            if (failedComparisonWork > (long) plan.patternLength + 2L * (at - fromIndex)) {
                return kmp(source, pattern, plan, at, endIndex);
            }
        }
        return -1;
    }

    static int indexOf(
            byte[] source,
            byte sourceCoder,
            int sourceCount,
            M3String pattern,
            int fromIndex) {
        int from = Math.clamp(fromIndex, 0, sourceCount);
        int patternLength = pattern.length();
        if (patternLength == 0) return from;
        if (patternLength > sourceCount - from) return -1;
        if (patternLength == 1) {
            char wanted = pattern.charAt(0);
            for (int index = from; index < sourceCount; index++) {
                if (sourceUnit(source, sourceCoder, index) == wanted) return index;
            }
            return -1;
        }

        Plan plan = prepare(pattern);
        if (plan != null) {
            return plan.patternLength >= 8
                    ? adaptiveBmh(source, sourceCoder, sourceCount, pattern, plan, from)
                    : kmp(source, sourceCoder, sourceCount, pattern, plan, from);
        }

        int limit = sourceCount - patternLength;
        char first = pattern.charAt(0);
        for (int candidate = from; candidate <= limit; candidate++) {
            if (sourceUnit(source, sourceCoder, candidate) != first) continue;
            int index = 1;
            while (index < patternLength
                    && sourceUnit(source, sourceCoder, candidate + index) == pattern.charAt(index)) {
                index++;
            }
            if (index == patternLength) return candidate;
        }
        return -1;
    }

    private static int kmp(
            byte[] source,
            byte sourceCoder,
            int sourceCount,
            M3String pattern,
            Plan plan,
            int fromIndex) {
        int matched = 0;
        for (int index = fromIndex; index < sourceCount; index++) {
            char unit = sourceUnit(source, sourceCoder, index);
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

    private static int adaptiveBmh(
            byte[] source,
            byte sourceCoder,
            int sourceCount,
            M3String pattern,
            Plan plan,
            int fromIndex) {
        int maximumStart = sourceCount - plan.patternLength;
        int at = fromIndex;
        long failedComparisonWork = 0L;
        while (at <= maximumStart) {
            int index = plan.patternLength - 1;
            while (index >= 0
                    && pattern.charAt(index)
                            == sourceUnit(source, sourceCoder, at + index)) {
                index--;
            }
            if (index < 0) return at;

            int shift =
                    plan.skip256[
                            sourceUnit(source, sourceCoder, at + plan.patternLength - 1) & 255];
            if (shift > maximumStart - at) return -1;
            at += shift;

            failedComparisonWork += plan.patternLength - index;
            if (failedComparisonWork
                    > (long) plan.patternLength + 2L * (at - fromIndex)) {
                return kmp(source, sourceCoder, sourceCount, pattern, plan, at);
            }
        }
        return -1;
    }

    /**
     * Candidates of the reverse search (its needle's last unit found) before the scan may hand
     * the rest to the reverse KMP (A43): the hand-off fires once the units verified at the
     * candidates reach the units walked, the sign of a text whose candidates verify long and
     * often, where the stock shape degrades and the KMP never does.
     */
    private static final int DENSE_CANDIDATES = 8;

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
        char[] needle = units(pattern);
        if (needle == null) {
            return perUnitLastIndexOf(source, sourceCoder, pattern, patternLength, maximumStart);
        }
        if (sourceCoder == String.LATIN1 && !latin1(needle)) return -1;
        if (patternLength == 1) return seekBackwards(source, sourceCoder, maximumStart, 0, needle[0]);

        // The stock reverse search's shape (A43): the needle's last unit sought backwards in a
        // tight loop per coder, each candidate verified backwards, the source read raw inside the
        // bounds the arguments fix. A text whose candidates verify long and often hands the rest
        // of the scan to the reverse KMP of the plan, which never degrades; it continues below
        // the failed candidate with nothing matched, every match ending above it ruled out.
        int last = patternLength - 1;
        char lastUnit = needle[last];
        int scanStart = maximumStart + last;
        int compares = 0;
        int candidates = 0;
        boolean watching = true;
        for (int index = scanStart; ; index--) {
            index = seekBackwards(source, sourceCoder, index, last, lastUnit);
            if (index < 0) return -1;
            int matched = 1;
            while (matched < patternLength
                    && rawUnit(source, sourceCoder, index - matched) == needle[last - matched]) {
                matched++;
            }
            if (matched == patternLength) return index - last;
            if (watching) {
                compares += matched;
                if (++candidates >= DENSE_CANDIDATES && compares >= scanStart - index + 1) {
                    Plan plan = prepare(pattern);
                    if (plan == null) {
                        watching = false;
                        continue;
                    }
                    return reverseSkipSearch(source, sourceCoder, needle, plan, index - 1);
                }
            }
        }
    }

    /**
     * The needle's units read once in bulk (A43), or {@code null} when the copy cannot be
     * allocated: the per-unit search then answers, precompute is never semantic authority.
     */
    private static char[] units(M3String pattern) {
        try {
            char[] out = new char[pattern.length()];
            pattern.getChars(0, out.length, out, 0);
            return out;
        } catch (OutOfMemoryError unavailable) {
            return null;
        }
    }

    /** Whether every unit fits Latin-1: a needle with a wider unit is absent from a Latin-1 source. */
    private static boolean latin1(char[] units) {
        for (char unit : units) {
            if (unit > 0xff) return false;
        }
        return true;
    }

    /**
     * The highest index in {@code [floor, index]} holding {@code unit}, or -1 (A43): the stock
     * reverse search's seek loop, tight per coder, reading the source raw.
     */
    private static int seekBackwards(byte[] source, byte sourceCoder, int index, int floor, char unit) {
        if (sourceCoder == String.LATIN1) {
            while (index >= floor && (source[index] & 0xff) != unit) index--;
        } else {
            while (index >= floor && StringUTF16.getChar(source, index) != unit) index--;
        }
        return index >= floor ? index : -1;
    }

    /**
     * The reverse KMP of the plan over {@code [0, scanStart]} of the source: the index of the
     * rightmost match of the needle whose last unit is at or below {@code scanStart}, or -1.
     */
    private static int reverseSkipSearch(byte[] source, byte sourceCoder, char[] needle, Plan plan,
            int scanStart) {
        int patternLength = needle.length;
        int matched = 0;
        for (int index = scanStart; index >= 0; index--) {
            char unit = rawUnit(source, sourceCoder, index);
            while (matched > 0 && unit != needle[patternLength - 1 - matched]) {
                matched = plan.reversePrefix[matched - 1];
            }
            if (unit == needle[patternLength - 1 - matched]) matched++;
            if (matched == patternLength) return index;
        }
        return -1;
    }

    /**
     * A unit of the flat source read raw (A43): the reverse lane keeps {@code index} inside
     * {@code [0, sourceCount)} by construction, so the stock bounds checks of
     * {@link #sourceUnit} are not repeated per unit there.
     */
    private static char rawUnit(byte[] source, byte sourceCoder, int index) {
        return sourceCoder == String.LATIN1
                ? (char) (source[index] & 0xff)
                : StringUTF16.getChar(source, index);
    }

    /** The per-unit reverse search of master, kept for a needle whose units cannot be copied. */
    private static int perUnitLastIndexOf(byte[] source, byte sourceCoder, M3String pattern,
            int patternLength, int maximumStart) {
        if (patternLength == 1) {
            char wanted = pattern.charAt(0);
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
                while (matched > 0 && unit != reverseUnit(pattern, matched)) {
                    matched = plan.reversePrefix[matched - 1];
                }
                if (unit == reverseUnit(pattern, matched)) matched++;
                if (matched == plan.patternLength) return index;
            }
            return -1;
        }

        char first = pattern.charAt(0);
        for (int candidate = maximumStart; candidate >= 0; candidate--) {
            if (sourceUnit(source, sourceCoder, candidate) != first) continue;
            int index = 1;
            while (index < patternLength
                    && sourceUnit(source, sourceCoder, candidate + index) == pattern.charAt(index)) {
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

    static int lastIndexOf(
            M3String source,
            M3String pattern,
            Plan plan,
            int maximumStart) {
        M3String.CodeUnitCursor sourceUnits = new M3String.CodeUnitCursor(source);
        M3String.CodeUnitCursor patternUnits = new M3String.CodeUnitCursor(pattern);
        int matched = 0;
        int scanStart = maximumStart + plan.patternLength - 1;
        for (int index = scanStart; index >= 0; index--) {
            char unit = sourceUnits.at(index);
            while (matched > 0 && unit != patternUnits.at(plan.patternLength - 1 - matched)) {
                matched = plan.reversePrefix[matched - 1];
            }
            if (unit == patternUnits.at(plan.patternLength - 1 - matched)) matched++;
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
        final int[] skip256;
        final M3TQ.Facts trigrams;

        Plan(
                int patternLength,
                int[] prefix,
                int[] reversePrefix,
                int[] skip256,
                M3TQ.Facts trigrams) {
            this.patternLength = patternLength;
            this.prefix = prefix;
            this.reversePrefix = reversePrefix;
            this.skip256 = skip256;
            this.trigrams = trigrams;
        }

        long retainedPrimitiveBytes() {
            return (long) (prefix.length + reversePrefix.length + skip256.length) * Integer.BYTES
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
