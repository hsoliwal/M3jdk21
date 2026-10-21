// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable collision-free exact lookup plane for a frozen dictionary.
 *
 * <p>Build time is deliberately allowed to do substantially more work than lookup time. Runtime
 * resolution performs a fixed sequence: bounded token hash, flattened
 * {@code [byte-length-class][first-byte]} dispatch, one bucket displacement read, one candidate
 * slot read, then exact hash/byte verification by the owning dictionary. There is no collision
 * chain, linear probing, tree traversal or dictionary-cardinality-dependent loop on lookup.</p>
 *
 * <p>The displacement layout is deterministic for one frozen spelling set. Buckets are laid out
 * largest first and build-time reseeding/growth is allowed until every spelling has one unique
 * runtime slot. None of that retry work survives into the runtime path.</p>
 */
final class MIndexExactLookup {
    static final int MAX_DIRECT_BYTE_LENGTH = 32;
    static final int LENGTH_CLASS_COUNT = MAX_DIRECT_BYTE_LENGTH + 2;
    static final int BYTE_CARDINALITY = 256;
    static final int FIXED_RUNTIME_CANDIDATES = 1;

    private static final long LAYOUT_DOMAIN = 0x4d4944585f455841L;
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final int LAYOUT_ATTEMPTS_PER_SIZE = 32;
    private static final int MAX_SLOT_GROWTHS = 4;
    private static final int[] DIMENSION_SALTS = dimensionSalts();

    private final long seed;
    private final int[] displacements;
    private final int[] slots;

    private MIndexExactLookup(long seed, int[] displacements, int[] slots) {
        if (displacements.length == 0 || Integer.bitCount(displacements.length) != 1) {
            throw new IllegalArgumentException("bucket count must be a positive power of two");
        }
        if (slots.length == 0 || Integer.bitCount(slots.length) != 1) {
            throw new IllegalArgumentException("slot count must be a positive power of two");
        }
        this.seed = seed;
        this.displacements = displacements;
        this.slots = slots;
    }

    static MIndexExactLookup build(long[] hashes, byte[] arena, int[] offsets) {
        Objects.requireNonNull(hashes, "hashes");
        Objects.requireNonNull(arena, "arena");
        Objects.requireNonNull(offsets, "offsets");
        if (offsets.length != hashes.length + 1) {
            throw new IllegalArgumentException("hash/offset cardinality mismatch");
        }
        if (hashes.length == 0) {
            throw new IllegalArgumentException("dictionary must contain at least one spelling");
        }

        int count = hashes.length;
        int bucketCount = powerOfTwoAtLeast(Math.max(2, (count + 1) >>> 1));
        int slotCount = powerOfTwoAtLeast(Math.max(2, Math.multiplyExact(count, 2)));

        for (int growth = 0; growth < MAX_SLOT_GROWTHS; growth++) {
            for (int attempt = 0; attempt < LAYOUT_ATTEMPTS_PER_SIZE; attempt++) {
                long seed = mix64(
                        LAYOUT_DOMAIN
                                + GOLDEN_GAMMA
                                        * (1L + attempt
                                                + (long) growth * LAYOUT_ATTEMPTS_PER_SIZE));
                MIndexExactLookup layout =
                        tryBuild(seed, bucketCount, slotCount, hashes, arena, offsets);
                if (layout != null) {
                    return layout;
                }
            }
            slotCount = Math.multiplyExact(slotCount, 2);
        }
        throw new IllegalStateException(
                "unable to build collision-free MIndex dictionary lookup plane");
    }

    static MIndexExactLookup restore(long seed, int[] displacements, int[] slots) {
        return new MIndexExactLookup(
                seed,
                Objects.requireNonNull(displacements, "displacements").clone(),
                Objects.requireNonNull(slots, "slots").clone());
    }

    int candidateRank(byte[] bytes, int offset, int length, long hash) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.checkFromIndexSize(offset, length, bytes.length);
        int bucket = bucketIndex(
                hash, bytes, offset, length, seed, displacements.length - 1);
        int displacement = displacements[bucket];
        int slot = slotIndex(
                hash, bytes, offset, length, seed, displacement, slots.length - 1);
        return slots[slot] - 1;
    }

    long seed() {
        return seed;
    }

    int bucketCount() {
        return displacements.length;
    }

    int slotCount() {
        return slots.length;
    }

    int[] displacementsCopy() {
        return displacements.clone();
    }

    int[] slotsCopy() {
        return slots.clone();
    }

    long retainedBytes() {
        return Long.BYTES
                + Integer.BYTES * (long) (displacements.length + slots.length);
    }

    static int bucketIndex(
            long hash,
            byte[] bytes,
            int offset,
            int length,
            long seed,
            int bucketMask) {
        int salt = dispatchSalt(bytes, offset, length);
        long mixed = hash ^ (Integer.toUnsignedLong(salt) << 32) ^ seed;
        return (int) mix64(mixed) & bucketMask;
    }

    static int slotIndex(
            long hash,
            byte[] bytes,
            int offset,
            int length,
            long seed,
            int displacement,
            int slotMask) {
        int salt = dispatchSalt(bytes, offset, length);
        long mixed =
                hash
                        ^ (Integer.toUnsignedLong(salt) << 32)
                        ^ Long.rotateLeft(seed, 17)
                        ^ Integer.toUnsignedLong(displacement) * GOLDEN_GAMMA;
        return (int) mix64(mixed) & slotMask;
    }

    private static MIndexExactLookup tryBuild(
            long seed,
            int bucketCount,
            int slotCount,
            long[] hashes,
            byte[] arena,
            int[] offsets) {
        int bucketMask = bucketCount - 1;
        int slotMask = slotCount - 1;
        int[] bucketSizes = new int[bucketCount];

        for (int rank = 0; rank < hashes.length; rank++) {
            int start = offsets[rank];
            int length = offsets[rank + 1] - start;
            bucketSizes[bucketIndex(
                    hashes[rank], arena, start, length, seed, bucketMask)]++;
        }

        int[] bucketOffsets = new int[bucketCount + 1];
        int largestBucket = 0;
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            bucketOffsets[bucket + 1] = Math.addExact(bucketOffsets[bucket], bucketSizes[bucket]);
            largestBucket = Math.max(largestBucket, bucketSizes[bucket]);
        }

        int[] bucketRanks = new int[hashes.length];
        int[] cursors = Arrays.copyOf(bucketOffsets, bucketCount);
        for (int rank = 0; rank < hashes.length; rank++) {
            int start = offsets[rank];
            int length = offsets[rank + 1] - start;
            int bucket = bucketIndex(
                    hashes[rank], arena, start, length, seed, bucketMask);
            bucketRanks[cursors[bucket]++] = rank;
        }

        long[] bucketOrder = new long[bucketCount];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            long descendingSize = Integer.toUnsignedLong(Integer.MAX_VALUE - bucketSizes[bucket]);
            bucketOrder[bucket] = (descendingSize << 32) | Integer.toUnsignedLong(bucket);
        }
        Arrays.sort(bucketOrder);

        int[] displacements = new int[bucketCount];
        int[] slots = new int[slotCount];
        int[] marks = new int[slotCount];
        int[] candidateSlots = new int[Math.max(1, largestBucket)];
        int stamp = 1;
        int displacementLimit = Math.max(4096, Math.min(1 << 20, slotCount));

        for (long orderKey : bucketOrder) {
            int bucket = (int) orderKey;
            int size = bucketSizes[bucket];
            if (size == 0) {
                break;
            }
            int from = bucketOffsets[bucket];
            int to = from + size;
            boolean placed = false;

            for (int displacement = 0; displacement < displacementLimit; displacement++) {
                if (++stamp == 0) {
                    Arrays.fill(marks, 0);
                    stamp = 1;
                }
                boolean collision = false;
                int candidate = 0;
                for (int at = from; at < to; at++) {
                    int rank = bucketRanks[at];
                    int start = offsets[rank];
                    int length = offsets[rank + 1] - start;
                    int slot = slotIndex(
                            hashes[rank],
                            arena,
                            start,
                            length,
                            seed,
                            displacement,
                            slotMask);
                    if (slots[slot] != 0 || marks[slot] == stamp) {
                        collision = true;
                        break;
                    }
                    marks[slot] = stamp;
                    candidateSlots[candidate++] = slot;
                }
                if (collision) {
                    continue;
                }

                displacements[bucket] = displacement;
                candidate = 0;
                for (int at = from; at < to; at++) {
                    slots[candidateSlots[candidate++]] = bucketRanks[at] + 1;
                }
                placed = true;
                break;
            }
            if (!placed) {
                return null;
            }
        }

        return new MIndexExactLookup(seed, displacements, slots);
    }

    private static int dispatchSalt(byte[] bytes, int offset, int length) {
        int lengthClass = Math.min(length, MAX_DIRECT_BYTE_LENGTH + 1);
        int first = length == 0 ? 0 : Byte.toUnsignedInt(bytes[offset]);
        int second = length <= 1 ? 0 : Byte.toUnsignedInt(bytes[offset + 1]);
        int coordinate = (lengthClass << 8) | first;
        return DIMENSION_SALTS[coordinate]
                ^ Integer.rotateLeft(second * 0x85ebca6b, 11);
    }

    private static int[] dimensionSalts() {
        int[] salts = new int[LENGTH_CLASS_COUNT * BYTE_CARDINALITY];
        for (int coordinate = 0; coordinate < salts.length; coordinate++) {
            salts[coordinate] = (int) mix64(
                    LAYOUT_DOMAIN ^ Integer.toUnsignedLong(coordinate) * GOLDEN_GAMMA);
        }
        return salts;
    }

    private static int powerOfTwoAtLeast(int value) {
        if (value <= 1) {
            return 1;
        }
        int highest = Integer.highestOneBit(value - 1);
        if (highest > (1 << 29)) {
            throw new IllegalArgumentException("lookup plane too large");
        }
        return highest << 1;
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        return mixed ^ (mixed >>> 33);
    }
}
