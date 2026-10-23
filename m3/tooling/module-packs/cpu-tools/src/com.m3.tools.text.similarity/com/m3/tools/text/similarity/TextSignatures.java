// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.similarity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Deterministic text-candidate signals: frequency-weighted 64-bit SimHash and
 * salted 64-bit MinHash. They nominate similar text; they never prove equality.
 *
 * <p>All token identities use exact UTF-16 code units, including unpaired
 * surrogates. Tokenization itself belongs to the existing WordTokenizer.
 * A transient ordered frequency table is reduced to primitive output lanes.
 */
public final class TextSignatures {
    public static final int MAX_TOKENS = 1_000_000;
    public static final int MAX_TOKEN_UNITS = 16 * 1024 * 1024;
    public static final int MAX_LANES = 128;
    private static final long SALT = 0x9e3779b97f4a7c15L;

    private TextSignatures() { }

    public static final class Signature {
        private final long simHash;
        private final long[] minHash;
        private final int tokenCount;
        private final int distinctCount;

        private Signature(long simHash, long[] minHash, int tokenCount, int distinctCount) {
            this.simHash = simHash;
            this.minHash = minHash.clone();
            this.tokenCount = tokenCount;
            this.distinctCount = distinctCount;
        }

        public long simHash64() { return simHash; }
        public long[] minHash64() { return minHash.clone(); }
        public int tokenCount() { return tokenCount; }
        public int distinctCount() { return distinctCount; }
        public int lanes() { return minHash.length; }
    }

    /** A bounded, reproducible signal over the provided exact token sequence. */
    public static Signature compute(List<String> tokens, int lanes) {
        return compute(tokens, lanes, () -> { });
    }

    /** Checkpoints cover admission, UTF-16 hashing, weighted voting, and MinHash lanes. */
    public static Signature compute(List<String> tokens, int lanes, Runnable checkpoint) {
        Objects.requireNonNull(tokens, "tokens");
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (lanes < 1 || lanes > MAX_LANES) throw new IllegalArgumentException("signature lane budget");
        if (tokens.size() > MAX_TOKENS) throw new IllegalArgumentException("token count budget");
        checkpoint.run();
        int total = 0;
        TreeMap<String, Integer> frequencies = new TreeMap<>();
        int seen = 0;
        for (String token : tokens) {
            if ((seen++ & 4095) == 0) checkpoint.run();
            Objects.requireNonNull(token, "token");
            total = Math.addExact(total, token.length());
            if (total > MAX_TOKEN_UNITS) throw new IllegalArgumentException("token UTF-16 budget");
            frequencies.merge(token, 1, Math::addExact);
        }

        int[] votes = new int[Long.SIZE];
        long[] minima = new long[lanes];
        Arrays.fill(minima, -1L);
        int distinct = 0;
        for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
            if ((distinct++ & 1023) == 0) checkpoint.run();
            long identity = hashUtf16(entry.getKey(), checkpoint);
            int weight = entry.getValue();
            for (int bit = 0; bit < Long.SIZE; bit++) {
                votes[bit] += ((identity >>> bit) & 1L) == 0L ? -weight : weight;
            }
            for (int lane = 0; lane < lanes; lane++) {
                long candidate = mix64(identity ^ mix64(SALT * (lane + 1L)));
                if (Long.compareUnsigned(candidate, minima[lane]) < 0) minima[lane] = candidate;
            }
        }
        long simHash = 0L;
        for (int bit = 0; bit < Long.SIZE; bit++) {
            if (votes[bit] > 0) simHash |= 1L << bit;
        }
        checkpoint.run();
        return new Signature(simHash, minima, tokens.size(), frequencies.size());
    }

    /** Hamming distance is exact for the two signatures, not for their input texts. */
    public static int hamming64(Signature left, Signature right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return Long.bitCount(left.simHash ^ right.simHash);
    }

    /** MinHash agreement is an estimate; equality of the inputs is not established. */
    public static double estimatedJaccard(Signature left, Signature right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.lanes() != right.lanes()) throw new IllegalArgumentException("lane mismatch");
        if (left.distinctCount == 0 || right.distinctCount == 0) {
            return left.distinctCount == right.distinctCount ? 1.0d : 0.0d;
        }
        int matches = 0;
        for (int lane = 0; lane < left.lanes(); lane++) {
            if (left.minHash[lane] == right.minHash[lane]) matches++;
        }
        return matches / (double) left.lanes();
    }

    private static long hashUtf16(String text, Runnable checkpoint) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("m3-text-token-utf16-v1".getBytes(StandardCharsets.US_ASCII));
            int length = text.length();
            digest.update((byte) (length >>> 24));
            digest.update((byte) (length >>> 16));
            digest.update((byte) (length >>> 8));
            digest.update((byte) length);
            for (int i = 0; i < length; i++) {
                if ((i & 4095) == 0) checkpoint.run();
                char unit = text.charAt(i);
                digest.update((byte) (unit >>> 8));
                digest.update((byte) unit);
            }
            byte[] bytes = digest.digest();
            long result = 0L;
            for (int i = 0; i < Long.BYTES; i++) result = (result << 8) | (bytes[i] & 255L);
            return result;
        } catch (NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
