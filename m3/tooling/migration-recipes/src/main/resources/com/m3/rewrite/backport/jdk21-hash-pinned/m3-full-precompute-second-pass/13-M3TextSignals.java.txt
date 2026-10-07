// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Immutable text facts for candidate pruning, similarity ranking and exact lower bounds.
 *
 * <p>Hashes and sketches never prove equality. Exact operations must still verify original text.
 * Histograms use UTF-16 units; Unicode classification uses code points.</p>
 */
public final class M3TextSignals {
    public static final int ASCII = 1;
    public static final int LATIN1 = 1 << 1;
    public static final int BLANK = 1 << 2;
    public static final int ALL_LETTERS = 1 << 3;
    public static final int ALL_DIGITS = 1 << 4;
    public static final int HAS_LETTER = 1 << 5;
    public static final int HAS_DIGIT = 1 << 6;
    public static final int HAS_UPPER = 1 << 7;
    public static final int HAS_LOWER = 1 << 8;
    public static final int WELL_FORMED_UTF16 = 1 << 9;

    private static final long HASH_BASE = 1_000_003L;
    private static final int MIN_HASH_LANES = 16;

    private final M3TextMetrics metrics;
    private final M3Histogram characters;
    private final M3Histogram bigrams;
    private final M3Histogram trigrams;
    private final long hash64;
    private final long power64;
    private final long presence64;
    private final long prefix2;
    private final long suffix2;
    private final int letters;
    private final int digits;
    private final int whitespace;
    private final int uppercase;
    private final int lowercase;
    private final int flags;
    private final long simHash64;
    private final long[] minHash;

    private M3TextSignals(
            M3TextMetrics metrics,
            M3Histogram characters,
            M3Histogram bigrams,
            M3Histogram trigrams,
            long hash64,
            long power64,
            long presence64,
            long prefix2,
            long suffix2,
            int letters,
            int digits,
            int whitespace,
            int uppercase,
            int lowercase) {
        this.metrics = metrics;
        this.characters = characters;
        this.bigrams = bigrams;
        this.trigrams = trigrams;
        this.hash64 = hash64;
        this.power64 = power64;
        this.presence64 = presence64;
        this.prefix2 = prefix2;
        this.suffix2 = suffix2;
        this.letters = letters;
        this.digits = digits;
        this.whitespace = whitespace;
        this.uppercase = uppercase;
        this.lowercase = lowercase;
        long max = characters.keys.length == 0 ? 0 : characters.keys[characters.keys.length - 1];
        this.flags =
                (max < 128 ? ASCII : 0)
                        | (max < 256 ? LATIN1 : 0)
                        | (whitespace == metrics.codePointCount() ? BLANK : 0)
                        | (letters == metrics.codePointCount() ? ALL_LETTERS : 0)
                        | (digits == metrics.codePointCount() ? ALL_DIGITS : 0)
                        | (letters > 0 ? HAS_LETTER : 0)
                        | (digits > 0 ? HAS_DIGIT : 0)
                        | (uppercase > 0 ? HAS_UPPER : 0)
                        | (lowercase > 0 ? HAS_LOWER : 0)
                        | (metrics.isWellFormedUtf16() ? WELL_FORMED_UTF16 : 0);
        this.simHash64 = computeSimHash();
        this.minHash = computeMinHash();
    }

    public static M3TextSignals compile(CharSequence input) {
        Objects.requireNonNull(input, "input");
        String value = input instanceof String ? (String) input : input.toString();
        int length = value.length();
        long[] chars = new long[length];
        long[] grams2 = new long[Math.max(0, length - 1)];
        long[] grams3 = new long[Math.max(0, length - 2)];
        long hash = 0;
        long power = 1;
        long presence = 0;
        for (int index = 0; index < length; index++) {
            char unit = value.charAt(index);
            chars[index] = unit;
            hash = hash * HASH_BASE + unit;
            power *= HASH_BASE;
            presence = addSignal(presence, unit);
            if (index >= 1) grams2[index - 1] = bigram(value.charAt(index - 1), unit);
            if (index >= 2) {
                grams3[index - 2] =
                        trigram(value.charAt(index - 2), value.charAt(index - 1), unit);
            }
        }

        int letters = 0;
        int digits = 0;
        int whitespace = 0;
        int upper = 0;
        int lower = 0;
        for (int index = 0; index < length; ) {
            int cp = Character.codePointAt(value, index);
            index += Character.charCount(cp);
            if (Character.isLetter(cp)) letters++;
            if (Character.isDigit(cp)) digits++;
            if (Character.isWhitespace(cp)) whitespace++;
            if (Character.isUpperCase(cp)) upper++;
            if (Character.isLowerCase(cp)) lower++;
        }

        return new M3TextSignals(
                M3TextMetrics.of(value),
                M3Histogram.of(chars),
                M3Histogram.of(grams2),
                M3Histogram.of(grams3),
                hash,
                power,
                presence,
                packedPrefix2(value),
                packedSuffix2(value),
                letters,
                digits,
                whitespace,
                upper,
                lower);
    }

    /** Compose stored facts without rereading child spellings. */
    public static M3TextSignals combine(M3TextSignals left, M3TextSignals right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.metrics.isEmpty()) return right;
        if (right.metrics.isEmpty()) return left;

        int last = left.metrics.lastUtf16Unit();
        int first = right.metrics.firstUtf16Unit();
        M3Histogram grams2 =
                left.bigrams
                        .plus(right.bigrams)
                        .plus(M3Histogram.of(new long[] {bigram(last, first)}));

        ArrayList<Long> seam3 = new ArrayList<>(2);
        if (left.metrics.utf16Length() >= 2) {
            seam3.add(trigram((char) (left.suffix2 >>> 16), (char) left.suffix2, first));
        }
        if (right.metrics.utf16Length() >= 2) {
            seam3.add(trigram(last, (char) (right.prefix2 >>> 16), (char) right.prefix2));
        }
        long[] seamArray = new long[seam3.size()];
        for (int index = 0; index < seamArray.length; index++) seamArray[index] = seam3.get(index);
        M3Histogram grams3 =
                left.trigrams.plus(right.trigrams).plus(M3Histogram.of(seamArray));

        int letters = Math.addExact(left.letters, right.letters);
        int digits = Math.addExact(left.digits, right.digits);
        int whitespace = Math.addExact(left.whitespace, right.whitespace);
        int upper = Math.addExact(left.uppercase, right.uppercase);
        int lower = Math.addExact(left.lowercase, right.lowercase);

        if (Character.isHighSurrogate((char) last)
                && Character.isLowSurrogate((char) first)) {
            int cp = Character.toCodePoint((char) last, (char) first);
            if (Character.isLetter(cp)) letters++;
            if (Character.isDigit(cp)) digits++;
            if (Character.isWhitespace(cp)) whitespace++;
            if (Character.isUpperCase(cp)) upper++;
            if (Character.isLowerCase(cp)) lower++;
        }

        return new M3TextSignals(
                M3TextMetrics.combine(left.metrics, right.metrics),
                left.characters.plus(right.characters),
                grams2,
                grams3,
                left.hash64 * right.power64 + right.hash64,
                left.power64 * right.power64,
                left.presence64 | right.presence64,
                composePrefix2(left, right),
                composeSuffix2(left, right),
                letters,
                digits,
                whitespace,
                upper,
                lower);
    }

    public M3TextMetrics metrics() {
        return metrics;
    }

    public int flags() {
        return flags;
    }

    public boolean hasAll(int mask) {
        return (flags & mask) == mask;
    }

    public boolean hasAny(int mask) {
        return (flags & mask) != 0;
    }

    public long contentHash64() {
        return hash64;
    }

    public long presence64() {
        return presence64;
    }

    public int characterCount(char unit) {
        return characters.count(unit);
    }

    public int bigramCount(char first, char second) {
        return bigrams.count(bigram(first, second));
    }

    public int trigramCount(char first, char second, char third) {
        return trigrams.count(trigram(first, second, third));
    }

    public boolean mayContain(M3TextSignals needle) {
        Objects.requireNonNull(needle, "needle");
        return metrics.utf16Length() >= needle.metrics.utf16Length()
                && (presence64 & needle.presence64) == needle.presence64;
    }

    public boolean mayEqual(M3TextSignals other) {
        Objects.requireNonNull(other, "other");
        return metrics.utf16Length() == other.metrics.utf16Length()
                && metrics.javaHashCode() == other.metrics.javaHashCode()
                && hash64 == other.hash64;
    }

    public boolean isAnagramOf(M3TextSignals other) {
        return characters.same(Objects.requireNonNull(other, "other").characters);
    }

    /**
     * Safe lower bound for unit-cost UTF-16 Levenshtein.
     *
     * <p>One edit changes at most 2q q-gram multiset counts; q=2 and q=3 bounds are combined with
     * the exact length and unigram histogram bounds.</p>
     */
    public int editLowerBound(M3TextSignals other) {
        Objects.requireNonNull(other, "other");
        long length = Math.abs((long) metrics.utf16Length() - other.metrics.utf16Length());
        long histogram = (characters.l1(other.characters) + 1) / 2;
        long bigram = (bigrams.l1(other.bigrams) + 3) / 4;
        long trigram = (trigrams.l1(other.trigrams) + 5) / 6;
        return Math.toIntExact(Math.max(Math.max(length, histogram), Math.max(bigram, trigram)));
    }

    public long simHash64() {
        return simHash64;
    }

    public int simHashDistance(M3TextSignals other) {
        return Long.bitCount(simHash64 ^ Objects.requireNonNull(other, "other").simHash64);
    }

    public long[] minHash() {
        return minHash.clone();
    }

    /** Approximate feature-set Jaccard; candidate ranking only. */
    public double estimatedJaccard(M3TextSignals other) {
        Objects.requireNonNull(other, "other");
        if (metrics.isEmpty() || other.metrics.isEmpty()) {
            return metrics.isEmpty() && other.metrics.isEmpty() ? 1.0 : 0.0;
        }
        int equal = 0;
        for (int lane = 0; lane < MIN_HASH_LANES; lane++) {
            if (minHash[lane] == other.minHash[lane]) equal++;
        }
        return (double) equal / MIN_HASH_LANES;
    }

    public long histogramPayloadBytes() {
        return characters.payloadBytes() + bigrams.payloadBytes() + trigrams.payloadBytes();
    }

    public List<String> labels() {
        int[] bits = {
            ASCII, LATIN1, BLANK, ALL_LETTERS, ALL_DIGITS,
            HAS_LETTER, HAS_DIGIT, HAS_UPPER, HAS_LOWER, WELL_FORMED_UTF16
        };
        String[] names = {
            "ASCII", "LATIN1", "BLANK", "ALL_LETTERS", "ALL_DIGITS",
            "HAS_LETTER", "HAS_DIGIT", "HAS_UPPER", "HAS_LOWER", "WELL_FORMED_UTF16"
        };
        ArrayList<String> result = new ArrayList<>();
        for (int index = 0; index < bits.length; index++) {
            if (hasAll(bits[index])) result.add(names[index]);
        }
        return List.copyOf(result);
    }

    private long computeSimHash() {
        M3Histogram features =
                trigrams.keys.length != 0
                        ? trigrams
                        : bigrams.keys.length != 0 ? bigrams : characters;
        int tag = features == trigrams ? 3 : features == bigrams ? 2 : 1;
        long result = 0;
        for (int bit = 0; bit < 64; bit++) {
            long vote = 0;
            for (int index = 0; index < features.keys.length; index++) {
                long hash = mix64(features.keys[index] ^ ((long) tag << 60) ^ 0x53494dL);
                vote += ((hash >>> bit) & 1L) == 0
                        ? -(long) features.counts[index]
                        : features.counts[index];
            }
            if (vote > 0) result |= 1L << bit;
        }
        return result;
    }

    private long[] computeMinHash() {
        M3Histogram features =
                trigrams.keys.length != 0
                        ? trigrams
                        : bigrams.keys.length != 0 ? bigrams : characters;
        int tag = features == trigrams ? 3 : features == bigrams ? 2 : 1;
        long[] result = new long[MIN_HASH_LANES];
        Arrays.fill(result, -1L);
        for (int lane = 0; lane < result.length; lane++) {
            for (long key : features.keys) {
                long hash = mix64(key ^ ((long) tag << 60) ^ lane * 0x9e3779b97f4a7c15L);
                if (Long.compareUnsigned(hash, result[lane]) < 0) result[lane] = hash;
            }
        }
        return result;
    }

    private static long packedPrefix2(CharSequence value) {
        if (value.length() == 0) return 0L;
        if (value.length() == 1) return value.charAt(0);
        return bigram(value.charAt(0), value.charAt(1));
    }

    private static long packedSuffix2(CharSequence value) {
        if (value.length() == 0) return 0L;
        if (value.length() == 1) return value.charAt(0);
        return bigram(value.charAt(value.length() - 2), value.charAt(value.length() - 1));
    }

    private static long composePrefix2(M3TextSignals left, M3TextSignals right) {
        if (left.metrics.utf16Length() >= 2) return left.prefix2;
        if (left.metrics.utf16Length() == 1) {
            return bigram(left.metrics.firstUtf16Unit(), right.metrics.firstUtf16Unit());
        }
        return right.prefix2;
    }

    private static long composeSuffix2(M3TextSignals left, M3TextSignals right) {
        if (right.metrics.utf16Length() >= 2) return right.suffix2;
        if (right.metrics.utf16Length() == 1) {
            return bigram(left.metrics.lastUtf16Unit(), right.metrics.lastUtf16Unit());
        }
        return left.suffix2;
    }

    private static long bigram(int first, int second) {
        return ((long) first << 16) | (second & 0xffffL);
    }

    private static long trigram(int first, int second, int third) {
        return ((long) first << 32) | ((long) second << 16) | (third & 0xffffL);
    }

    private static long addSignal(long signal, char value) {
        int folded = value >= 'A' && value <= 'Z' ? value | 0x20 : value;
        int first = mix32(folded);
        int second = mix32(folded ^ 0x9e3779b9);
        return signal | (1L << (first & 63)) | (1L << (second & 63));
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 30;
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    private static int mix32(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        return mixed ^ (mixed >>> 16);
    }
}
