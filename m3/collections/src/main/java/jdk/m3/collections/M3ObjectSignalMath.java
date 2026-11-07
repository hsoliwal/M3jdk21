// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Objects;

/** Allocation-free hashing and lossy feature-bit extraction used only for first-pass rejection. */
//FLLDCIM: RELOCATE - donor blob 3116563a7230 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public final class M3ObjectSignalMath {
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private M3ObjectSignalMath() { }

    /** Exact hash over Java UTF-16 code units; equality still requires exact fallback verification. */
    public static long hashString(CharSequence value) {
        Objects.requireNonNull(value, "value");
        long h = FNV_OFFSET;
        for (int i = 0; i < value.length(); i++) {
            h ^= value.charAt(i);
            h *= FNV_PRIME;
        }
        return mix64(h ^ Integer.toUnsignedLong(value.length()));
    }

    /**
     * 64-bit Bloom-like signature of UTF-16 unigrams and adjacent bigrams.
     * If needle is a literal substring of candidate then every needle bit is necessarily present
     * in candidate. Collisions may create false positives, never false negatives for that relation.
     */
    public static long stringFeatures(CharSequence value) {
        Objects.requireNonNull(value, "value");
        long bits = 0L;
        int previous = -1;
        for (int i = 0; i < value.length(); i++) {
            int current = value.charAt(i);
            bits |= bit(mix64(0x51L ^ current));
            if (previous >= 0) {
                long pair = ((long) previous << 16) | current;
                bits |= bit(mix64(0xa7L ^ pair));
            }
            previous = current;
        }
        return bits;
    }

    public static long hashBytes(byte[] value) {
        Objects.requireNonNull(value, "value");
        long h = FNV_OFFSET;
        for (byte b : value) {
            h ^= b & 0xffL;
            h *= FNV_PRIME;
        }
        return mix64(h ^ Integer.toUnsignedLong(value.length));
    }

    public static long byteFeatures(byte[] value) {
        Objects.requireNonNull(value, "value");
        long bits = 0L;
        int previous = -1;
        for (byte b : value) {
            int current = b & 0xff;
            bits |= bit(mix64(0x63L ^ current));
            if (previous >= 0) {
                long pair = ((long) previous << 8) | current;
                bits |= bit(mix64(0xb9L ^ pair));
            }
            previous = current;
        }
        return bits;
    }

    public static long indexFeatures(long value) {
        long mixed = mix64(value);
        return bit(mixed) | bit(Long.rotateLeft(mixed, 21)) | bit(Long.rotateLeft(mixed, 43));
    }

    public static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ (value >>> 33);
    }

    private static long bit(long mixed) {
        return 1L << ((int) mixed & 63);
    }
}
