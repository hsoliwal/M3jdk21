// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable exact lookup plane for bounded packed lexemes.
 *
 * <p>The first two packed symbols address one of 65,536 precomputed dispatch cells. That dense
 * coordinate contributes a deterministic salt while the complete 128-bit lexeme selects a compact
 * compiled displacement bucket. Runtime lookup is therefore: encode at most sixteen UTF-16 units,
 * one dense dispatch read, one displacement read, one candidate-slot read and one exact 128-bit
 * verification. There are no runtime collision chains, probing loops, String materialization or
 * byte-array allocation.</p>
 *
 * <p>Values outside the codec/max-length domain return {@link #UNENCODABLE}; callers can then use
 * the general UTF-8/hash/trie path. An encodable but absent value returns {@link #NOT_FOUND} and
 * requires no fallback because the 128-bit representation is collision-free in the codec domain.</p>
 */
public final class MIndexPackedLexemeIndex implements MIndexPackedLexemeKeySource {
    public static final int NOT_FOUND = -1;
    public static final int UNENCODABLE = -2;
    public static final int DENSE_BUCKET_CELLS = 1 << 16;
    public static final int FIXED_RUNTIME_CANDIDATES = 1;

    private static final long LAYOUT_DOMAIN = 0x4d495041434b3136L;
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final int LAYOUT_ATTEMPTS_PER_SIZE = 32;
    private static final int MAX_SLOT_GROWTHS = 5;
    private static final int[] PREFIX_SALTS = prefixSalts();

    private final MIndexLexemeCodec codec;
    private final long[] highKeys;
    private final long[] lowKeys;
    private final int[] valueIds;
    private final long seed;
    private final int[] displacements;
    private final int[] slots;

    private MIndexPackedLexemeIndex(
            MIndexLexemeCodec codec,
            long[] highKeys,
            long[] lowKeys,
            int[] valueIds,
            long seed,
            int[] displacements,
            int[] slots) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.highKeys = Objects.requireNonNull(highKeys, "highKeys");
        this.lowKeys = Objects.requireNonNull(lowKeys, "lowKeys");
        this.valueIds = Objects.requireNonNull(valueIds, "valueIds");
        this.seed = seed;
        this.displacements = Objects.requireNonNull(displacements, "displacements");
        this.slots = Objects.requireNonNull(slots, "slots");
    }

    public static MIndexPackedLexemeIndex compile(
            List<? extends CharSequence> values, MIndexLexemeCodec codec) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(codec, "codec");

        ArrayList<Entry> entries = new ArrayList<>(values.size());
        int uniqueCapacity =
                Math.toIntExact(
                        Math.min(
                                1L << 20,
                                Math.max(16L, (long) values.size() * 2L)));
        HashSet<MIndexLexemeKey128> unique = new HashSet<>(uniqueCapacity);
        for (int valueId = 0; valueId < values.size(); valueId++) {
            CharSequence value =
                    Objects.requireNonNull(values.get(valueId), "values[" + valueId + "]");
            if (!codec.canEncode(value)) continue;
            MIndexLexemeKey128 key = codec.encode(value);
            if (!unique.add(key)) {
                throw new IllegalArgumentException("duplicate packed lexeme: " + value);
            }
            entries.add(new Entry(key.high(), key.low(), valueId));
        }

        int count = entries.size();
        long[] highs = new long[count];
        long[] lows = new long[count];
        int[] ids = new int[count];
        for (int index = 0; index < count; index++) {
            Entry entry = entries.get(index);
            highs[index] = entry.high();
            lows[index] = entry.low();
            ids[index] = entry.valueId();
        }

        int halfCountRoundedUp = (count >>> 1) + (count & 1);
        int bucketCount = powerOfTwoAtLeast(Math.max(2, halfCountRoundedUp));
        int slotCount =
                powerOfTwoAtLeast(
                        Math.max(2, Math.multiplyExact(Math.max(1, count), 2)));

        Layout layout = null;
        for (int growth = 0; growth < MAX_SLOT_GROWTHS && layout == null; growth++) {
            for (int attempt = 0;
                    attempt < LAYOUT_ATTEMPTS_PER_SIZE && layout == null;
                    attempt++) {
                long seed =
                        mix64(
                                LAYOUT_DOMAIN
                                        + GOLDEN_GAMMA
                                                * (1L
                                                        + attempt
                                                        + (long) growth
                                                                * LAYOUT_ATTEMPTS_PER_SIZE));
                layout = tryBuild(seed, bucketCount, slotCount, highs, lows);
            }
            if (layout == null) slotCount = Math.multiplyExact(slotCount, 2);
        }
        if (layout == null) {
            throw new IllegalStateException("unable to compile packed lexeme lookup plane");
        }

        return new MIndexPackedLexemeIndex(
                codec,
                highs,
                lows,
                ids,
                layout.seed(),
                layout.displacements(),
                layout.slots());
    }

    public static MIndexPackedLexemeIndex compile(
            MIndexLexemeCodec codec, CharSequence... values) {
        return compile(List.of(values), codec);
    }

    public MIndexLexemeCodec codec() {
        return codec;
    }

    @Override
    public int packedEntryCount() {
        return valueIds.length;
    }

    @Override
    public long highKeyAtEntry(int entry) {
        return highKeys[Objects.checkIndex(entry, highKeys.length)];
    }

    @Override
    public long lowKeyAtEntry(int entry) {
        return lowKeys[Objects.checkIndex(entry, lowKeys.length)];
    }

    @Override
    public int valueIdAtEntry(int entry) {
        return valueIds[Objects.checkIndex(entry, valueIds.length)];
    }

    /** Number of full-key displacement buckets retained by this immutable index. */
    public int bucketCount() {
        return displacements.length;
    }

    public int slotCount() {
        return slots.length;
    }

    /** Shared first-two-symbol coordinate cardinality. */
    public int denseBucketCells() {
        return DENSE_BUCKET_CELLS;
    }

    public int runtimeCandidateBound() {
        return FIXED_RUNTIME_CANDIDATES;
    }

    /** Compile the sparse positional/length matrix over these canonical packed key lanes. */
    public MIndexLexemeMatrixIndex compileMatrix() {
        return MIndexLexemeMatrixIndex.compile(this);
    }

    public long primitivePayloadBytes() {
        return codec.primitivePayloadBytes()
                + Long.BYTES * (long) (highKeys.length + lowKeys.length)
                + Integer.BYTES
                        * (long) (valueIds.length + displacements.length + slots.length);
    }

    /** One JVM-shared precomputed first-two-symbol dispatch table. */
    public static long sharedPrefixDispatchBytes() {
        return Integer.BYTES * (long) PREFIX_SALTS.length;
    }

    /**
     * Return the caller's value id, {@link #NOT_FOUND}, or {@link #UNENCODABLE}.
     */
    public int lookup(CharSequence value) {
        Objects.requireNonNull(value, "value");
        if (!codec.canEncode(value)) return UNENCODABLE;
        return lookup(codec.packHigh(value), codec.packLow(value));
    }

    public int lookup(MIndexLexemeKey128 key) {
        Objects.requireNonNull(key, "key");
        return lookup(key.high(), key.low());
    }

    int lookup(long high, long low) {
        int bucket = bucketIndex(high, low, seed, displacements.length - 1);
        int displacement = displacements[bucket];
        int slot = slotIndex(high, low, seed, displacement, slots.length - 1);
        int stored = slots[slot];
        if (stored == 0) return NOT_FOUND;
        int entry = stored - 1;
        return highKeys[entry] == high && lowKeys[entry] == low
                ? valueIds[entry]
                : NOT_FOUND;
    }

    long seed() {
        return seed;
    }

    long[] highKeysCopy() {
        return highKeys.clone();
    }

    long[] lowKeysCopy() {
        return lowKeys.clone();
    }

    int[] valueIdsCopy() {
        return valueIds.clone();
    }

    int[] displacementsCopy() {
        return displacements.clone();
    }

    int[] slotsCopy() {
        return slots.clone();
    }

    static int prefixCoordinate(long high) {
        return (int) ((high >>> 48) & 0xffffL);
    }

    static int bucketIndex(
            long high, long low, long seed, int bucketMask) {
        int prefixSalt = PREFIX_SALTS[prefixCoordinate(high)];
        long mixed =
                high
                        ^ Long.rotateLeft(low, 23)
                        ^ (Integer.toUnsignedLong(prefixSalt) << 32)
                        ^ seed;
        return (int) mix64(mixed) & bucketMask;
    }

    static int slotIndex(
            long high,
            long low,
            long seed,
            int displacement,
            int slotMask) {
        int prefixSalt = PREFIX_SALTS[prefixCoordinate(high)];
        long mixed =
                low
                        ^ Long.rotateLeft(high, 17)
                        ^ Long.rotateLeft(seed, 31)
                        ^ Integer.toUnsignedLong(prefixSalt)
                        ^ Integer.toUnsignedLong(displacement) * GOLDEN_GAMMA;
        return (int) mix64(mixed) & slotMask;
    }

    private static Layout tryBuild(
            long seed,
            int bucketCount,
            int slotCount,
            long[] highs,
            long[] lows) {
        int bucketMask = bucketCount - 1;
        int slotMask = slotCount - 1;
        int[] bucketSizes = new int[bucketCount];
        for (int entry = 0; entry < highs.length; entry++) {
            bucketSizes[
                    bucketIndex(highs[entry], lows[entry], seed, bucketMask)]++;
        }

        int[] bucketOffsets = new int[bucketCount + 1];
        int largestBucket = 0;
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            bucketOffsets[bucket + 1] =
                    Math.addExact(bucketOffsets[bucket], bucketSizes[bucket]);
            largestBucket = Math.max(largestBucket, bucketSizes[bucket]);
        }

        int[] bucketEntries = new int[highs.length];
        int[] cursors = Arrays.copyOf(bucketOffsets, bucketCount);
        for (int entry = 0; entry < highs.length; entry++) {
            int bucket =
                    bucketIndex(highs[entry], lows[entry], seed, bucketMask);
            bucketEntries[cursors[bucket]++] = entry;
        }

        long[] bucketOrder = new long[bucketCount];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            long descendingSize =
                    Integer.toUnsignedLong(Integer.MAX_VALUE - bucketSizes[bucket]);
            bucketOrder[bucket] =
                    (descendingSize << 32) | Integer.toUnsignedLong(bucket);
        }
        Arrays.sort(bucketOrder);

        int[] displacements = new int[bucketCount];
        int[] slots = new int[slotCount];
        int[] marks = new int[slotCount];
        int[] candidateSlots = new int[Math.max(1, largestBucket)];
        int stamp = 1;
        int displacementLimit =
                Math.max(4096, Math.min(1 << 20, slotCount));

        for (long orderKey : bucketOrder) {
            int bucket = (int) orderKey;
            int size = bucketSizes[bucket];
            if (size == 0) break;
            int from = bucketOffsets[bucket];
            int to = from + size;
            boolean placed = false;

            for (int displacement = 0;
                    displacement < displacementLimit;
                    displacement++) {
                if (++stamp == 0) {
                    Arrays.fill(marks, 0);
                    stamp = 1;
                }
                boolean collision = false;
                int candidate = 0;
                for (int at = from; at < to; at++) {
                    int entry = bucketEntries[at];
                    int slot =
                            slotIndex(
                                    highs[entry],
                                    lows[entry],
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
                if (collision) continue;

                displacements[bucket] = displacement;
                candidate = 0;
                for (int at = from; at < to; at++) {
                    slots[candidateSlots[candidate++]] =
                            bucketEntries[at] + 1;
                }
                placed = true;
                break;
            }
            if (!placed) return null;
        }
        return new Layout(seed, displacements, slots);
    }

    private static int[] prefixSalts() {
        int[] salts = new int[DENSE_BUCKET_CELLS];
        for (int coordinate = 0; coordinate < salts.length; coordinate++) {
            salts[coordinate] =
                    (int)
                            mix64(
                                    LAYOUT_DOMAIN
                                            ^ Integer.toUnsignedLong(coordinate)
                                                    * GOLDEN_GAMMA);
        }
        return salts;
    }

    private static int powerOfTwoAtLeast(int value) {
        if (value <= 1) return 1;
        int highest = Integer.highestOneBit(value - 1);
        if (highest > (1 << 29)) {
            throw new IllegalArgumentException("packed index too large");
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

    private record Entry(long high, long low, int valueId) {}
    private record Layout(long seed, int[] displacements, int[] slots) {}
}
