// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.util.Objects;

/** Immutable primitive long-key Bloom membership prefilter.
 * False positives are possible, false negatives for inserted keys are not.
 * Never use a positive answer as an authorization or correctness proof.
 */
public final class M3BloomIndex {
    private static final long SEED_A = 0x9e3779b97f4a7c15L;
    private static final long SEED_B = 0x94d049bb133111ebL;
    private final long[] lane;
    private final int bitCount, rounds, keys;

    private M3BloomIndex(long[] lane, int bitCount, int rounds, int keys) {
        this.lane = lane;
        this.bitCount = bitCount;
        this.rounds = rounds;
        this.keys = keys;
    }

    /** Bit count must be a power of two in [64, 2^24], hashes 1..8. */
    public static M3BloomIndex build(long[] values, int bitCount, int hashes) {
        Objects.requireNonNull(values, "values");
        if (bitCount < 64 || bitCount > (1 << 24) || (bitCount & (bitCount - 1)) != 0
                || hashes < 1 || hashes > 8 || values.length > 2_000_000)
            throw new IllegalArgumentException("Bloom geometry");
        long[] bits = new long[bitCount >>> 6];
        for (long value : values) {
            long h1 = mix(value ^ SEED_A), h2 = mix(value ^ SEED_B) | 1L;
            for (int i = 0; i < hashes; i++) {
                int at = (int)(h1 + (long)i * h2) & (bitCount - 1);
                bits[at >>> 6] |= 1L << (at & 63);
            }
        }
        return new M3BloomIndex(bits, bitCount, hashes, values.length);
    }

    /** Possible membership; always check an authoritative index for positive results. */
    public boolean mightContain(long value) {
        long h1 = mix(value ^ SEED_A), h2 = mix(value ^ SEED_B) | 1L;
        for (int i = 0; i < rounds; i++) {
            int at = (int)(h1 + (long)i * h2) & (bitCount - 1);
            if ((lane[at >>> 6] & (1L << (at & 63))) == 0) return false;
        }
        return true;
    }

    public int bitCount() { return bitCount; }
    public int hashes() { return rounds; }
    public int insertedCount() { return keys; }
    public long[] snapshotBits() { return lane.clone(); }

    private static long mix(long x) {
        x ^= x >>> 30; x *= 0xbf58476d1ce4e5b9L;
        x ^= x >>> 27; x *= 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }

    /**
     * UTF-16 text prefilter using this owner's existing long-key Bloom implementation.
     * Only derived bits survive construction. Inputs are stable borrowed views; no
     * toString, subsequence, or payload buffer is used. Hash collisions and Bloom
     * collisions are allowed: a positive result is never proof of text equality.
     */
    public static M3BloomIndex buildText(CharSequence[] values, int bitCount, int hashes) {
        if (bitCount < 64 || bitCount > (1 << 24) || (bitCount & (bitCount - 1)) != 0
                || hashes < 1 || hashes > 8) throw new IllegalArgumentException("Bloom geometry");
        textBatch(values);
        long[] fingerprints = new long[values.length];
        for (int i = 0; i < values.length; i++) fingerprints[i] = textFingerprint(values[i]);
        return build(fingerprints, bitCount, hashes);
    }

    /** Same exact UTF-16 fingerprint contract as buildText; positive answers need confirmation. */
    public boolean mightContainText(CharSequence value) {
        return mightContain(textFingerprint(value));
    }

    /** Original-row bitmap of candidates; no unused high bits or retained input references. */
    public long[] screenText(CharSequence[] values) {
        textBatch(values);
        long[] result = new long[(values.length + 63) >>> 6];
        for (int i = 0; i < values.length; i++) if (mightContainText(values[i]))
            result[i >>> 6] |= 1L << (i & 63);
        return result;
    }

    private static void textBatch(CharSequence[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length > 2_000_000) throw new IllegalArgumentException("Bloom text rows");
        long units = 0;
        for (CharSequence value : values) {
            int length = textLength(value);
            units += length;
            if (units > 16_000_000L) throw new IllegalArgumentException("Bloom text work budget");
        }
    }

    private static int textLength(CharSequence value) {
        Objects.requireNonNull(value, "value");
        int length = value.length();
        if (length < 0 || length > 1_000_000) throw new IllegalArgumentException("Bloom text length");
        return length;
    }

    private static long textFingerprint(CharSequence value) {
        int length = textLength(value);
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < length; i++) {
            hash ^= value.charAt(i);
            hash *= 0x100000001b3L;
        }
        return mix(hash ^ length);
    }
}
