<<<<<<< HEAD
/*
 * Copyright (c) 2026, M3 contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 */

package java.lang;

/**
 * Precomputed one-candidate address geometry for an immutable M3 lexicon.
 *
 * <p>The production lexicon path hashes the complete UTF-16 spelling with a
 * freeze-time-selected seed and places every unique row into a two-level
 * displacement table. Runtime address selection is therefore one bounded
 * spelling scan followed by one 64-bit mix, one displacement load and one row
 * load. It has no collision chain and no hash/length ambiguity fallback.
 * Exact UTF-16 comparison remains authoritative after candidate selection.</p>
 *
 * <p>The older Java-hash-plus-length builder remains package-private because
 * historical qualification tests exercise that geometry directly. The actual
 * M3String lexicon uses {@link #buildExact(int, ExactKeySource)}.</p>
 */
final class M3FrozenLexiconIndex {
    static final int MAX_SLOTS = 1 << 22;

    private static final int TARGET_BUCKET_SIZE = 4;
    private static final int MAX_SEEDS = 64;
    private static final int MAX_DISPLACEMENT_ATTEMPTS = 1 << 20;
    private static final long SEED_BASE = 0x243f6a8885a308d3L;
    private static final long SEED_STEP = 0x9e3779b97f4a7c15L;
    private static final long EXACT_OFFSET = 0xcbf29ce484222325L;
    private static final long EXACT_PRIME = 0x100000001b3L;
    private static final long EXACT_DOMAIN = 0x4d33455841435431L; // M3EXACT1
    private static final M3FrozenLexiconIndex EMPTY =
            new M3FrozenLexiconIndex(0L, new int[0], new int[0]);

    private final long seed;
    private final int[] displacements;
    private final int[] rows;

    private M3FrozenLexiconIndex(long seed, int[] displacements, int[] rows) {
        this.seed = seed;
        this.displacements = displacements;
        this.rows = rows;
    }

    static M3FrozenLexiconIndex empty() {
        return EMPTY;
    }

    @FunctionalInterface
    interface ExactKeySource {
        long key(int row, long seed);
    }

    @FunctionalInterface
    interface ExactEquality {
        boolean same(int leftRow, int rightRow);
    }

    /**
     * Builds the production one-candidate geometry from complete spelling keys.
     *
     * <p>The key source is invoked only during immutable lexicon admission. A
     * different content-hash seed is tried when the two-level placement cannot
     * be made injective. Runtime lookup uses the retained seed to reproduce the
     * same full-content key.</p>
     */
    static M3FrozenLexiconIndex buildExact(int count, ExactKeySource source) {
        return buildExact(count, source, (leftRow, rightRow) -> false);
    }

    static M3FrozenLexiconIndex buildExact(
            int count, ExactKeySource source, ExactEquality equality) {
        if (count < 0) throw new IllegalArgumentException("negative M3 frozen index size");
        if (source == null) throw new NullPointerException("source");
        if (equality == null) throw new NullPointerException("equality");
        if (count == 0 || count > MAX_SLOTS / 2) return EMPTY;
        try {
            int slotCount = nextPowerOfTwo(Math.max(2, Math.multiplyExact(count, 2)));
            if (slotCount > MAX_SLOTS) return EMPTY;
            int bucketCount =
                    nextPowerOfTwo(
                            Math.max(
                                    1,
                                    (count + TARGET_BUCKET_SIZE - 1)
                                            / TARGET_BUCKET_SIZE));
            long[] keys = new long[count];
            for (int seedIndex = 0; seedIndex < MAX_SEEDS; seedIndex++) {
                long contentSeed = seed(seedIndex);
                for (int row = 0; row < count; row++) {
                    keys[row] = source.key(row, contentSeed);
                }
                Groups groups = groupExact(keys, equality);
                if (groups == null) continue;
                M3FrozenLexiconIndex index =
                        placePrepared(groups, contentSeed, bucketCount, slotCount);
                if (index != null) return index;
            }
            return EMPTY;
        } catch (ArithmeticException | OutOfMemoryError unavailable) {
            return EMPTY;
        }
    }

    /**
     * Starts the seeded full-content key. Length is part of the key even for an
     * empty spelling, so differently sized inputs cannot alias before exact
     * verification.
     */
    static long exactHashStart(long seed, int length) {
        long hash = EXACT_OFFSET ^ seed ^ EXACT_DOMAIN;
        hash = (hash ^ Integer.toUnsignedLong(length)) * EXACT_PRIME;
        return hash;
    }

    /** Adds one UTF-16 unit without allocating or changing representation. */
    static long exactHashUnit(long hash, char unit) {
        return (hash ^ unit) * EXACT_PRIME;
    }

    static long exactHashFinish(long hash) {
        return hash;
    }

    static M3FrozenLexiconIndex build(int[] hashes, int[] lengths) {
        if (hashes.length != lengths.length) {
            throw new IllegalArgumentException("M3 frozen index lane lengths differ");
        }
        int count = hashes.length;
        if (count == 0 || count > MAX_SLOTS / 2) return EMPTY;
        try {
            Groups groups = group(hashes, lengths);
            int groupCount = groups.keys.length;
            int slotCount = nextPowerOfTwo(Math.max(2, Math.multiplyExact(groupCount, 2)));
            if (slotCount > MAX_SLOTS) return EMPTY;
            int bucketCount =
                    nextPowerOfTwo(
                            Math.max(
                                    1,
                                    (groupCount + TARGET_BUCKET_SIZE - 1)
                                            / TARGET_BUCKET_SIZE));
            for (int seedIndex = 0; seedIndex < MAX_SEEDS; seedIndex++) {
                M3FrozenLexiconIndex index =
                        place(groups, seed(seedIndex), bucketCount, slotCount);
                if (index != null) return index;
            }
            return EMPTY;
        } catch (ArithmeticException | OutOfMemoryError unavailable) {
            return EMPTY;
        }
    }

    boolean available() {
        return rows.length != 0;
    }

    int candidate(int hash, int length) {
        if (rows.length == 0) return 0;
        long mixed = mix64(key(hash, length) ^ seed);
        int bucket = ((int) mixed) & (displacements.length - 1);
        int slot =
                (((int) (mixed >>> Integer.SIZE)) + displacements[bucket])
                        & (rows.length - 1);
        return rows[slot];
    }

    /**
     * Production lookup over a seed-specific full-content key.
     *
     * <p>Once the caller has inspected the bounded token to compute the key,
     * this is pure primitive address arithmetic plus two array reads.</p>
     */
    int candidateExact(long exactHash) {
        if (rows.length == 0) return 0;
        long mixed = mix64(exactHash);
        int bucket = ((int) mixed) & (displacements.length - 1);
        int slot =
                (((int) (mixed >>> Integer.SIZE)) + displacements[bucket])
                        & (rows.length - 1);
        return rows[slot];
    }

    int[] rows() {
        return rows;
    }

    int[] displacements() {
        return displacements;
    }

    long seed() {
        return seed;
    }

    private static Groups group(int[] hashes, int[] lengths) {
        int capacity =
                nextPowerOfTwo(Math.max(2, Math.multiplyExact(hashes.length, 2)));
        long[] tableKeys = new long[capacity];
        int[] tableRows = new int[capacity];
        int mask = capacity - 1;
        int groups = 0;

        for (int row = 0; row < hashes.length; row++) {
            long key = key(hashes[row], lengths[row]);
            int slot = ((int) mix64(key)) & mask;
            boolean grouped = false;
            while (tableRows[slot] != 0) {
                if (tableKeys[slot] == key) {
                    // The sign bit marks ambiguity; the low 31 bits still hold row + 1.
                    tableRows[slot] |= Integer.MIN_VALUE;
                    grouped = true;
                    break;
                }
                slot = (slot + 1) & mask;
            }
            if (!grouped) {
                tableKeys[slot] = key;
                tableRows[slot] = row + 1;
                groups++;
            }
        }

        long[] keys = new long[groups];
        int[] rows = new int[groups];
        int at = 0;
        for (int slot = 0; slot < tableRows.length; slot++) {
            if (tableRows[slot] == 0) continue;
            keys[at] = tableKeys[slot];
            rows[at] = tableRows[slot];
            at++;
        }
        return new Groups(keys, rows);
    }

    /**
     * Groups only byte-for-byte/UTF-16-identical rows. A seed whose 64-bit
     * content keys collide for non-identical rows is rejected and retried.
     * True duplicate spellings keep the historical negative-row marker so the
     * shared-arena compatibility path can preserve its binary row authority.
     */
    private static Groups groupExact(long[] keys, ExactEquality equality) {
        int capacity = nextPowerOfTwo(Math.max(2, Math.multiplyExact(keys.length, 2)));
        long[] tableKeys = new long[capacity];
        int[] tableRows = new int[capacity];
        int mask = capacity - 1;
        int groups = 0;
        for (int row = 0; row < keys.length; row++) {
            long key = keys[row];
            int slot = ((int) mix64(key)) & mask;
            boolean grouped = false;
            while (tableRows[slot] != 0) {
                if (tableKeys[slot] == key) {
                    int priorRow = (tableRows[slot] & Integer.MAX_VALUE) - 1;
                    if (!equality.same(priorRow, row)) return null;
                    tableRows[slot] |= Integer.MIN_VALUE;
                    grouped = true;
                    break;
                }
                slot = (slot + 1) & mask;
            }
            if (!grouped) {
                tableKeys[slot] = key;
                tableRows[slot] = row + 1;
                groups++;
            }
        }
        long[] groupedKeys = new long[groups];
        int[] groupedRows = new int[groups];
        int at = 0;
        for (int slot = 0; slot < tableRows.length; slot++) {
            if (tableRows[slot] == 0) continue;
            groupedKeys[at] = tableKeys[slot];
            groupedRows[at] = tableRows[slot];
            at++;
        }
        return new Groups(groupedKeys, groupedRows);
    }

    private static M3FrozenLexiconIndex place(
            Groups groups, long seed, int bucketCount, int slotCount) {
        int groupCount = groups.keys.length;
        int[] bucketCounts = new int[bucketCount];
        int[] bucketByGroup = new int[groupCount];
        int[] baseSlotByGroup = new int[groupCount];
        int bucketMask = bucketCount - 1;
        int slotMask = slotCount - 1;
        int maxBucket = 0;
        int nonEmptyBuckets = 0;

        for (int group = 0; group < groupCount; group++) {
            long mixed = mix64(groups.keys[group] ^ seed);
            int bucket = ((int) mixed) & bucketMask;
            int count = ++bucketCounts[bucket];
            if (count == 1) nonEmptyBuckets++;
            if (count > maxBucket) maxBucket = count;
            bucketByGroup[group] = bucket;
            baseSlotByGroup[group] = ((int) (mixed >>> Integer.SIZE)) & slotMask;
        }

        int[] offsets = new int[bucketCount + 1];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            offsets[bucket + 1] = offsets[bucket] + bucketCounts[bucket];
        }
        int[] cursors = offsets.clone();
        int[] bucketGroups = new int[groupCount];
        for (int group = 0; group < groupCount; group++) {
            int bucket = bucketByGroup[group];
            bucketGroups[cursors[bucket]++] = group;
        }

        // A common displacement cannot separate equal base slots in one bucket.
        // Reject that seed once, before trying any displacement or placing other buckets.
        if (hasAliasedBaseSlots(offsets, bucketGroups, baseSlotByGroup)) return null;

        int[] cardinalities = new int[maxBucket + 1];
        for (int count : bucketCounts) {
            if (count != 0) cardinalities[count]++;
        }
        int[] starts = new int[maxBucket + 1];
        int cursor = 0;
        for (int count = maxBucket; count >= 1; count--) {
            starts[count] = cursor;
            cursor += cardinalities[count];
        }
        int[] next = starts.clone();
        int[] order = new int[nonEmptyBuckets];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            int count = bucketCounts[bucket];
            if (count != 0) order[next[count]++] = bucket;
        }

        int[] displacements = new int[bucketCount];
        int[] rows = new int[slotCount];
        int[] candidates = new int[maxBucket];
        int attempts = Math.min(slotCount, MAX_DISPLACEMENT_ATTEMPTS);

        for (int bucket : order) {
            int count = bucketCounts[bucket];
            int from = offsets[bucket];
            boolean placed = false;
            for (int displacement = 0; displacement < attempts; displacement++) {
                boolean collision = false;
                for (int ordinal = 0; ordinal < count; ordinal++) {
                    int group = bucketGroups[from + ordinal];
                    int slot = (baseSlotByGroup[group] + displacement) & slotMask;
                    if (rows[slot] != 0) {
                        collision = true;
                        break;
                    }
                    candidates[ordinal] = slot;
                }
                if (collision) continue;
                displacements[bucket] = displacement;
                for (int ordinal = 0; ordinal < count; ordinal++) {
                    int group = bucketGroups[from + ordinal];
                    rows[candidates[ordinal]] = groups.rows[group];
                }
                placed = true;
                break;
            }
            if (!placed) return null;
        }
        return new M3FrozenLexiconIndex(seed, displacements, rows);
    }

    /**
     * Places keys that already include their content seed. Equal prepared keys
     * are unplaceable by construction and therefore cause a new content seed
     * to be tried rather than creating an ambiguity marker.
     */
    private static M3FrozenLexiconIndex placePrepared(
            Groups groups, long contentSeed, int bucketCount, int slotCount) {
        int groupCount = groups.keys.length;
        int[] bucketCounts = new int[bucketCount];
        int[] bucketByGroup = new int[groupCount];
        int[] baseSlotByGroup = new int[groupCount];
        int bucketMask = bucketCount - 1;
        int slotMask = slotCount - 1;
        int maxBucket = 0;
        int nonEmptyBuckets = 0;

        for (int group = 0; group < groupCount; group++) {
            long mixed = mix64(groups.keys[group]);
            int bucket = ((int) mixed) & bucketMask;
            int count = ++bucketCounts[bucket];
            if (count == 1) nonEmptyBuckets++;
            if (count > maxBucket) maxBucket = count;
            bucketByGroup[group] = bucket;
            baseSlotByGroup[group] = ((int) (mixed >>> Integer.SIZE)) & slotMask;
        }

        int[] offsets = new int[bucketCount + 1];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            offsets[bucket + 1] = offsets[bucket] + bucketCounts[bucket];
        }
        int[] cursors = offsets.clone();
        int[] bucketGroups = new int[groupCount];
        for (int group = 0; group < groupCount; group++) {
            int bucket = bucketByGroup[group];
            bucketGroups[cursors[bucket]++] = group;
        }
        if (hasAliasedBaseSlots(offsets, bucketGroups, baseSlotByGroup)) return null;

        int[] cardinalities = new int[maxBucket + 1];
        for (int count : bucketCounts) {
            if (count != 0) cardinalities[count]++;
        }
        int[] starts = new int[maxBucket + 1];
        int cursor = 0;
        for (int count = maxBucket; count >= 1; count--) {
            starts[count] = cursor;
            cursor += cardinalities[count];
        }
        int[] next = starts.clone();
        int[] order = new int[nonEmptyBuckets];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            int count = bucketCounts[bucket];
            if (count != 0) order[next[count]++] = bucket;
        }

        int[] displacements = new int[bucketCount];
        int[] rows = new int[slotCount];
        int[] candidates = new int[maxBucket];
        int attempts = Math.min(slotCount, MAX_DISPLACEMENT_ATTEMPTS);
        for (int bucket : order) {
            int count = bucketCounts[bucket];
            int from = offsets[bucket];
            boolean placed = false;
            for (int displacement = 0; displacement < attempts; displacement++) {
                boolean collision = false;
                for (int ordinal = 0; ordinal < count; ordinal++) {
                    int group = bucketGroups[from + ordinal];
                    int slot = (baseSlotByGroup[group] + displacement) & slotMask;
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
                for (int ordinal = 0; ordinal < count; ordinal++) {
                    int group = bucketGroups[from + ordinal];
                    rows[candidates[ordinal]] = groups.rows[group];
                }
                placed = true;
                break;
            }
            if (!placed) return null;
        }
        return new M3FrozenLexiconIndex(contentSeed, displacements, rows);
    }

    private static boolean hasAliasedBaseSlots(
            int[] offsets, int[] bucketGroups, int[] baseSlotByGroup) {
        for (int bucket = 0; bucket < offsets.length - 1; bucket++) {
            int from = offsets[bucket];
            int to = offsets[bucket + 1];
            for (int ordinal = from + 1; ordinal < to; ordinal++) {
                int baseSlot = baseSlotByGroup[bucketGroups[ordinal]];
                for (int prior = from; prior < ordinal; prior++) {
                    if (baseSlotByGroup[bucketGroups[prior]] == baseSlot) return true;
                }
            }
        }
        return false;
    }

    private static long key(int hash, int length) {
        return (Integer.toUnsignedLong(hash) << Integer.SIZE)
                | Integer.toUnsignedLong(length);
    }

    private static long seed(int seedIndex) {
        return mix64(SEED_BASE + Integer.toUnsignedLong(seedIndex) * SEED_STEP);
    }

    private static long mix64(long value) {
        long z = value;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static int nextPowerOfTwo(int value) {
        if (value <= 1) return 1;
        if (value > (1 << 30)) {
            throw new ArithmeticException("M3 frozen index capacity");
        }
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
    }

    private record Groups(long[] keys, int[] rows) { }
}
||||||| merged common ancestors
=======
/*
 * Copyright (c) 2026, M3 contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 */

package java.lang;

/**
 * Precomputed one-candidate address geometry for an immutable M3 lexicon.
 *
 * <p>The runtime key is the Java String hash plus UTF-16 length that the
 * lexicon lookup already computes. Admission groups equal keys once, places
 * each distinct key into a two-level displacement table, and retains a
 * negative row marker for the rare hash/length-equivalent group. A normal
 * lookup therefore performs one 64-bit mix, one displacement load and one row
 * load; it never walks a collision chain. Exact UTF-16 comparison remains
 * authoritative after candidate selection.</p>
 */
final class M3FrozenLexiconIndex {
    static final int MAX_SLOTS = 1 << 22;

    private static final int TARGET_BUCKET_SIZE = 4;
    private static final int MAX_SEEDS = 64;
    private static final int MAX_DISPLACEMENT_ATTEMPTS = 1 << 20;
    private static final long SEED_BASE = 0x243f6a8885a308d3L;
    private static final long SEED_STEP = 0x9e3779b97f4a7c15L;
    private static final M3FrozenLexiconIndex EMPTY =
            new M3FrozenLexiconIndex(0L, new int[0], new int[0]);

    private final long seed;
    private final int[] displacements;
    private final int[] rows;

    private M3FrozenLexiconIndex(long seed, int[] displacements, int[] rows) {
        this.seed = seed;
        this.displacements = displacements;
        this.rows = rows;
    }

    static M3FrozenLexiconIndex empty() {
        return EMPTY;
    }

    static M3FrozenLexiconIndex build(int[] hashes, int[] lengths) {
        if (hashes.length != lengths.length) {
            throw new IllegalArgumentException("M3 frozen index lane lengths differ");
        }
        int count = hashes.length;
        if (count == 0 || count > MAX_SLOTS / 2) return EMPTY;
        try {
            Groups groups = group(hashes, lengths);
            int groupCount = groups.keys.length;
            int slotCount = nextPowerOfTwo(Math.max(2, Math.multiplyExact(groupCount, 2)));
            if (slotCount > MAX_SLOTS) return EMPTY;
            int bucketCount =
                    nextPowerOfTwo(
                            Math.max(
                                    1,
                                    (groupCount + TARGET_BUCKET_SIZE - 1)
                                            / TARGET_BUCKET_SIZE));
            for (int seedIndex = 0; seedIndex < MAX_SEEDS; seedIndex++) {
                M3FrozenLexiconIndex index =
                        place(groups, seed(seedIndex), bucketCount, slotCount);
                if (index != null) return index;
            }
            return EMPTY;
        } catch (ArithmeticException | OutOfMemoryError unavailable) {
            return EMPTY;
        }
    }

    boolean available() {
        return rows.length != 0;
    }

    int candidate(int hash, int length) {
        if (rows.length == 0) return 0;
        long mixed = mix64(key(hash, length) ^ seed);
        int bucket = ((int) mixed) & (displacements.length - 1);
        int slot =
                (((int) (mixed >>> Integer.SIZE)) + displacements[bucket])
                        & (rows.length - 1);
        return rows[slot];
    }

    int[] rows() {
        return rows;
    }

    int[] displacements() {
        return displacements;
    }

    long seed() {
        return seed;
    }

    private static Groups group(int[] hashes, int[] lengths) {
        int capacity =
                nextPowerOfTwo(Math.max(2, Math.multiplyExact(hashes.length, 2)));
        long[] tableKeys = new long[capacity];
        int[] tableRows = new int[capacity];
        int mask = capacity - 1;
        int groups = 0;

        for (int row = 0; row < hashes.length; row++) {
            long key = key(hashes[row], lengths[row]);
            int slot = ((int) mix64(key)) & mask;
            boolean grouped = false;
            while (tableRows[slot] != 0) {
                if (tableKeys[slot] == key) {
                    if (tableRows[slot] > 0) tableRows[slot] = -tableRows[slot];
                    grouped = true;
                    break;
                }
                slot = (slot + 1) & mask;
            }
            if (!grouped) {
                tableKeys[slot] = key;
                tableRows[slot] = row + 1;
                groups++;
            }
        }

        long[] keys = new long[groups];
        int[] rows = new int[groups];
        int at = 0;
        for (int slot = 0; slot < tableRows.length; slot++) {
            if (tableRows[slot] == 0) continue;
            keys[at] = tableKeys[slot];
            rows[at] = tableRows[slot];
            at++;
        }
        return new Groups(keys, rows);
    }

    private static M3FrozenLexiconIndex place(
            Groups groups, long seed, int bucketCount, int slotCount) {
        int groupCount = groups.keys.length;
        int[] bucketCounts = new int[bucketCount];
        int[] bucketByGroup = new int[groupCount];
        int[] baseSlotByGroup = new int[groupCount];
        int bucketMask = bucketCount - 1;
        int slotMask = slotCount - 1;
        int maxBucket = 0;
        int nonEmptyBuckets = 0;

        for (int group = 0; group < groupCount; group++) {
            long mixed = mix64(groups.keys[group] ^ seed);
            int bucket = ((int) mixed) & bucketMask;
            int count = ++bucketCounts[bucket];
            if (count == 1) nonEmptyBuckets++;
            if (count > maxBucket) maxBucket = count;
            bucketByGroup[group] = bucket;
            baseSlotByGroup[group] = ((int) (mixed >>> Integer.SIZE)) & slotMask;
        }

        int[] offsets = new int[bucketCount + 1];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            offsets[bucket + 1] = offsets[bucket] + bucketCounts[bucket];
        }
        int[] cursors = offsets.clone();
        int[] bucketGroups = new int[groupCount];
        for (int group = 0; group < groupCount; group++) {
            int bucket = bucketByGroup[group];
            bucketGroups[cursors[bucket]++] = group;
        }

        int[] cardinalities = new int[maxBucket + 1];
        for (int count : bucketCounts) {
            if (count != 0) cardinalities[count]++;
        }
        int[] starts = new int[maxBucket + 1];
        int cursor = 0;
        for (int count = maxBucket; count >= 1; count--) {
            starts[count] = cursor;
            cursor += cardinalities[count];
        }
        int[] next = starts.clone();
        int[] order = new int[nonEmptyBuckets];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            int count = bucketCounts[bucket];
            if (count != 0) order[next[count]++] = bucket;
        }

        int[] displacements = new int[bucketCount];
        int[] rows = new int[slotCount];
        int[] candidates = new int[maxBucket];
        int attempts = Math.min(slotCount, MAX_DISPLACEMENT_ATTEMPTS);

        for (int bucket : order) {
            int count = bucketCounts[bucket];
            int from = offsets[bucket];
            boolean placed = false;
            for (int displacement = 0; displacement < attempts; displacement++) {
                boolean collision = false;
                for (int ordinal = 0; ordinal < count; ordinal++) {
                    int group = bucketGroups[from + ordinal];
                    int slot = (baseSlotByGroup[group] + displacement) & slotMask;
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
                for (int ordinal = 0; ordinal < count; ordinal++) {
                    int group = bucketGroups[from + ordinal];
                    rows[candidates[ordinal]] = groups.rows[group];
                }
                placed = true;
                break;
            }
            if (!placed) return null;
        }
        return new M3FrozenLexiconIndex(seed, displacements, rows);
    }

    private static long key(int hash, int length) {
        return (Integer.toUnsignedLong(hash) << Integer.SIZE)
                | Integer.toUnsignedLong(length);
    }

    private static long seed(int seedIndex) {
        return mix64(SEED_BASE + Integer.toUnsignedLong(seedIndex) * SEED_STEP);
    }

    private static long mix64(long value) {
        long z = value;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static int nextPowerOfTwo(int value) {
        if (value <= 1) return 1;
        if (value > (1 << 30)) {
            throw new ArithmeticException("M3 frozen index capacity");
        }
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
    }

    private record Groups(long[] keys, int[] rows) { }
}
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
