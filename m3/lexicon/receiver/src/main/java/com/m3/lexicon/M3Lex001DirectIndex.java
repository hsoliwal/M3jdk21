// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.util.List;
import java.util.Objects;

/**
 * Cold-path builder/verifier for the M3LEX001 version-3 direct-address trailer.
 *
 * <p>The VM owns the corresponding hot-path reader in java.lang.M3FrozenLexiconIndex. This class
 * deliberately contains only image-build mechanics: complete UTF-16 spellings are hashed with a
 * selected seed, then placed into a two-level displacement table. The published rows are positive
 * {@code row + 1} values because M3LEX001 itself is duplicate-free.</p>
 */
final class M3Lex001DirectIndex {
    static final long MAGIC = 0x4d334c5849445831L; // M3LXIDX1
    static final int HEADER = 32;
    static final int MAX_SLOTS = 1 << 22;

    private static final int TARGET_BUCKET_SIZE = 4;
    private static final int MAX_SEEDS = 64;
    private static final int MAX_DISPLACEMENT_ATTEMPTS = 1 << 20;
    private static final long SEED_BASE = 0x243f6a8885a308d3L;
    private static final long SEED_STEP = 0x9e3779b97f4a7c15L;
    private static final long EXACT_OFFSET = 0xcbf29ce484222325L;
    private static final long EXACT_PRIME = 0x100000001b3L;
    private static final long EXACT_DOMAIN = 0x4d33455841435431L; // M3EXACT1

    record Geometry(long seed, int[] displacements, int[] rows) {
        Geometry {
            displacements = Objects.requireNonNull(displacements, "displacements").clone();
            rows = Objects.requireNonNull(rows, "rows").clone();
            if (displacements.length == 0 || Integer.bitCount(displacements.length) != 1) {
                throw new IllegalArgumentException("direct-index displacement count");
            }
            if (rows.length < 2 || Integer.bitCount(rows.length) != 1 || rows.length > MAX_SLOTS) {
                throw new IllegalArgumentException("direct-index row slot count");
            }
        }

        @Override public int[] displacements() { return displacements.clone(); }
        @Override public int[] rows() { return rows.clone(); }

        int candidate(CharSequence spelling) {
            long key = exactHash(spelling, seed);
            long mixed = mix64(key);
            int bucket = ((int) mixed) & (displacements.length - 1);
            int slot =
                    (((int) (mixed >>> Integer.SIZE)) + displacements[bucket])
                            & (rows.length - 1);
            return rows[slot];
        }
    }

    private M3Lex001DirectIndex() {}

    static Geometry build(List<? extends CharSequence> words) {
        Objects.requireNonNull(words, "words");
        int count = words.size();
        if (count == 0) {
            // Keep a valid power-of-two geometry for the empty dictionary.
            return new Geometry(seed(0), new int[] {0}, new int[] {0, 0});
        }
        if (count > MAX_SLOTS / 2) {
            throw new IllegalArgumentException("M3LEX001 direct index exceeds slot capacity");
        }
        int slotCount = nextPowerOfTwo(Math.multiplyExact(count, 2));
        int bucketCount =
                nextPowerOfTwo(
                        Math.max(1, (count + TARGET_BUCKET_SIZE - 1) / TARGET_BUCKET_SIZE));
        long[] keys = new long[count];

        for (int seedIndex = 0; seedIndex < MAX_SEEDS; seedIndex++) {
            long selectedSeed = seed(seedIndex);
            for (int row = 0; row < count; row++) {
                keys[row] = exactHash(words.get(row), selectedSeed);
            }
            Geometry result = place(keys, selectedSeed, bucketCount, slotCount);
            if (result != null) return result;
        }
        throw new IllegalStateException("cannot construct M3LEX001 direct index");
    }

    static long exactHash(CharSequence spelling, long seed) {
        Objects.requireNonNull(spelling, "spelling");
        long hash = EXACT_OFFSET ^ seed ^ EXACT_DOMAIN;
        hash = (hash ^ Integer.toUnsignedLong(spelling.length())) * EXACT_PRIME;
        for (int index = 0; index < spelling.length(); index++) {
            hash = (hash ^ spelling.charAt(index)) * EXACT_PRIME;
        }
        return hash;
    }

    static long mix64(long value) {
        long z = value;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static Geometry place(
            long[] keys, long selectedSeed, int bucketCount, int slotCount) {
        int count = keys.length;
        int[] bucketCounts = new int[bucketCount];
        int[] bucketByRow = new int[count];
        int[] baseSlotByRow = new int[count];
        int bucketMask = bucketCount - 1;
        int slotMask = slotCount - 1;
        int maxBucket = 0;
        int nonEmptyBuckets = 0;

        for (int row = 0; row < count; row++) {
            long mixed = mix64(keys[row]);
            int bucket = ((int) mixed) & bucketMask;
            int cardinality = ++bucketCounts[bucket];
            if (cardinality == 1) nonEmptyBuckets++;
            if (cardinality > maxBucket) maxBucket = cardinality;
            bucketByRow[row] = bucket;
            baseSlotByRow[row] = ((int) (mixed >>> Integer.SIZE)) & slotMask;
        }

        int[] offsets = new int[bucketCount + 1];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            offsets[bucket + 1] = offsets[bucket] + bucketCounts[bucket];
        }
        int[] cursors = offsets.clone();
        int[] bucketRows = new int[count];
        for (int row = 0; row < count; row++) {
            bucketRows[cursors[bucketByRow[row]]++] = row;
        }
        if (hasAliasedBaseSlots(offsets, bucketRows, baseSlotByRow)) return null;

        int[] cardinalities = new int[maxBucket + 1];
        for (int cardinality : bucketCounts) if (cardinality != 0) cardinalities[cardinality]++;
        int[] starts = new int[maxBucket + 1];
        int cursor = 0;
        for (int cardinality = maxBucket; cardinality >= 1; cardinality--) {
            starts[cardinality] = cursor;
            cursor += cardinalities[cardinality];
        }
        int[] next = starts.clone();
        int[] order = new int[nonEmptyBuckets];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            int cardinality = bucketCounts[bucket];
            if (cardinality != 0) order[next[cardinality]++] = bucket;
        }

        int[] displacements = new int[bucketCount];
        int[] rows = new int[slotCount];
        int[] candidates = new int[maxBucket];
        int attempts = Math.min(slotCount, MAX_DISPLACEMENT_ATTEMPTS);
        for (int bucket : order) {
            int cardinality = bucketCounts[bucket];
            int from = offsets[bucket];
            boolean placed = false;
            for (int displacement = 0; displacement < attempts; displacement++) {
                boolean collision = false;
                for (int ordinal = 0; ordinal < cardinality; ordinal++) {
                    int row = bucketRows[from + ordinal];
                    int slot = (baseSlotByRow[row] + displacement) & slotMask;
                    if (rows[slot] != 0) {
                        collision = true;
                        break;
                    }
                    for (int prior = 0; prior < ordinal; prior++) {
                        if (candidates[prior] == slot) {
                            collision = true;
                            break;
                        }
                    }
                    if (collision) break;
                    candidates[ordinal] = slot;
                }
                if (collision) continue;
                displacements[bucket] = displacement;
                for (int ordinal = 0; ordinal < cardinality; ordinal++) {
                    int row = bucketRows[from + ordinal];
                    rows[candidates[ordinal]] = row + 1;
                }
                placed = true;
                break;
            }
            if (!placed) return null;
        }
        return new Geometry(selectedSeed, displacements, rows);
    }

    private static boolean hasAliasedBaseSlots(
            int[] offsets, int[] bucketRows, int[] baseSlotByRow) {
        for (int bucket = 0; bucket < offsets.length - 1; bucket++) {
            int from = offsets[bucket];
            int to = offsets[bucket + 1];
            for (int ordinal = from + 1; ordinal < to; ordinal++) {
                int slot = baseSlotByRow[bucketRows[ordinal]];
                for (int prior = from; prior < ordinal; prior++) {
                    if (baseSlotByRow[bucketRows[prior]] == slot) return true;
                }
            }
        }
        return false;
    }

    private static long seed(int seedIndex) {
        return mix64(SEED_BASE + Integer.toUnsignedLong(seedIndex) * SEED_STEP);
    }

    private static int nextPowerOfTwo(int value) {
        if (value <= 1) return 1;
        if (value > (1 << 30)) throw new ArithmeticException("M3LEX001 direct-index capacity");
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
    }
}
