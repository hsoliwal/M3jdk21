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
 * Candidate-only full-match DP for the lowercase-ASCII {@code literal / . / *} subset.
 *
 * <p>This is an internal M3 String execution lane, not a second Java regex engine. Unsupported or
 * invalid patterns and unsupported input text return {@link #UNSUPPORTED}, and
 * {@link String#matches(String)} delegates to {@code Pattern.matches}. The cache retains only
 * packed primitive pattern metadata and weak canonical-owner keys; no regex String, source text,
 * Matcher, char[] or byte[] payload is retained.</p>
 */
final class M3StringRestrictedRegexDp {
    static final int UNSUPPORTED = -1;
    static final int NO_MATCH = 0;
    static final int MATCH = 1;

    private static final int STAR = 1 << 16;
    private static final int ATOM_MASK = 0xffff;
    private static final int CACHE_SLOTS = 64;
    private static final int CACHE_MASK = CACHE_SLOTS - 1;
    private static final int MAX_PATTERN_UNITS = 8_192;
    private static final AtomicReferenceArray<Entry> CACHE =
            new AtomicReferenceArray<>(CACHE_SLOTS);

    private M3StringRestrictedRegexDp() {}

    static int tryMatches(M3String text, String regex) {
        if (text == null || regex == null) return UNSUPPORTED;
        Plan plan = prepare(regex);
        return plan == null ? UNSUPPORTED : plan.tryMatches(text);
    }

    static long maximumRetainedPrimitiveBytes() {
        return (long) CACHE_SLOTS * MAX_PATTERN_UNITS * Integer.BYTES;
    }

    private static Plan prepare(String regex) {
        if (regex.length() > MAX_PATTERN_UNITS) return null;
        M3String storage = regex.m3();
        if (storage == null) {
            try {
                return compile(regex);
            } catch (OutOfMemoryError unavailable) {
                return null;
            }
        }

        M3StringOwner owner = storage.owner();
        long coordinate = storage.coordinate();
        int slot = slot(owner, coordinate);
        Entry entry = CACHE.get(slot);
        if (entry != null
                && entry.owner.get() == owner
                && entry.coordinate == coordinate
                && entry.patternLength == regex.length()) {
            return entry.plan;
        }

        try {
            Plan plan = compile(regex);
            if (plan != null) {
                CACHE.set(
                        slot,
                        new Entry(
                                new WeakReference<>(owner),
                                coordinate,
                                regex.length(),
                                plan));
            }
            return plan;
        } catch (OutOfMemoryError unavailable) {
            // Candidate precompute must never add a new String.matches failure mode.
            return null;
        }
    }

    private static Plan compile(CharSequence regex) {
        int length = regex.length();
        int[] scratch = new int[length];
        int tokenCount = 0;
        int minimumLength = 0;
        int nullablePrefix = 0;
        boolean allPreviousNullable = true;
        boolean hasStar = false;

        for (int index = 0; index < length; ) {
            char atom = regex.charAt(index++);
            if (atom == '*' || (atom != '.' && (atom < 'a' || atom > 'z'))) {
                return null;
            }
            boolean starred = index < length && regex.charAt(index) == '*';
            if (starred) {
                hasStar = true;
                index++;
            }
            scratch[tokenCount++] = atom | (starred ? STAR : 0);

            if (!starred) {
                minimumLength++;
                allPreviousNullable = false;
            } else if (allPreviousNullable) {
                nullablePrefix = tokenCount;
            }

            if (index < length && regex.charAt(index) == '*') return null;
        }

        return new Plan(
                Arrays.copyOf(scratch, tokenCount),
                minimumLength,
                nullablePrefix,
                hasStar);
    }

    private static int slot(M3StringOwner owner, long coordinate) {
        long mixed = Integer.toUnsignedLong(System.identityHashCode(owner))
                ^ Long.rotateLeft(coordinate, 23);
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return ((int) mixed) & CACHE_MASK;
    }

    private static final class Plan {
        final int[] tokens;
        final int minimumTextLength;
        final int nullablePrefixTokens;
        final boolean hasStar;

        Plan(int[] tokens, int minimumTextLength, int nullablePrefixTokens, boolean hasStar) {
            this.tokens = tokens;
            this.minimumTextLength = minimumTextLength;
            this.nullablePrefixTokens = nullablePrefixTokens;
            this.hasStar = hasStar;
        }

        int tryMatches(M3String text) {
            int textLength = text.length();
            if (textLength < minimumTextLength) return NO_MATCH;

            if (!hasStar) {
                if (textLength != tokens.length) return NO_MATCH;
                for (int index = 0; index < textLength; index++) {
                    char source = text.charAt(index);
                    if (source < 'a' || source > 'z') return UNSUPPORTED;
                    char atom = (char) (tokens[index] & ATOM_MASK);
                    if (atom != '.' && atom != source) return NO_MATCH;
                }
                return MATCH;
            }

            boolean[] previous = new boolean[tokens.length + 1];
            boolean[] current = new boolean[tokens.length + 1];
            previous[0] = true;
            Arrays.fill(previous, 1, nullablePrefixTokens + 1, true);

            for (int inputIndex = 0; inputIndex < textLength; inputIndex++) {
                char source = text.charAt(inputIndex);
                if (source < 'a' || source > 'z') return UNSUPPORTED;

                current[0] = false;
                for (int tokenIndex = 1; tokenIndex <= tokens.length; tokenIndex++) {
                    int token = tokens[tokenIndex - 1];
                    char atom = (char) (token & ATOM_MASK);
                    boolean matches = atom == '.' || atom == source;
                    current[tokenIndex] =
                            (token & STAR) != 0
                                    ? current[tokenIndex - 1]
                                            || (matches && previous[tokenIndex])
                                    : matches && previous[tokenIndex - 1];
                }
                boolean[] swap = previous;
                previous = current;
                current = swap;
                Arrays.fill(current, false);
            }
            return previous[tokens.length] ? MATCH : NO_MATCH;
        }
    }

    private static final class Entry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final int patternLength;
        final Plan plan;

        Entry(
                WeakReference<M3StringOwner> owner,
                long coordinate,
                int patternLength,
                Plan plan) {
            this.owner = owner;
            this.coordinate = coordinate;
            this.patternLength = patternLength;
            this.plan = plan;
        }
    }
}
