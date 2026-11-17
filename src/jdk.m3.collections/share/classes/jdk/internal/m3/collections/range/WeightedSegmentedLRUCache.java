/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Thread-safe weighted segmented LRU (SLRU) cache.
 *
 * <p>New entries enter the probation segment. A probation hit promotes the entry to the protected
 * segment. Protected entries keep ordinary LRU recency. When the protected target is exceeded, its
 * LRU entry is demoted to probation instead of discarded. Hard entry/weight pressure evicts
 * probation LRU entries first and protected LRU entries only when probation is empty.</p>
 *
 * <p>This preserves LRU semantics inside each segment while resisting one-pass scan pollution:
 * entries observed only once compete primarily with other probation entries instead of immediately
 * displacing repeatedly reused protected entries. The cache bounds both total entry count and
 * caller-defined retained weight. Weight remains an application estimate, not a JVM object-layout
 * claim.</p>
 *
 * <p>The residents live in array lanes, one slot each (key, value, weight, hash, segment), with
 * each segment's recency list as two {@code int} lanes of slot indices and an open-addressing
 * {@code int} index from a key's hash to its slot; there is no node or map entry object per
 * resident. Freed slots are reused, the lanes double up to {@code maxEntries + 1} slots (at most
 * 2<sup>29</sup>) and are dropped when the cache empties (clear, trim, removeIf or the removal of
 * the last resident), and a key's {@code hashCode} is not called again after it was admitted.</p>
 *
 * <p>Expected complexity is O(1) for get/put/remove and O(evicted) for trims. Predicate removal and
 * ordered snapshots are O(n).</p>
 *
 * <p>2026-11-17: storage rewritten onto array lanes (arrayify, leaf 2). Every method keeps its
 * results, segment order, counters and exceptions for keys that obey {@code equals} and
 * {@code hashCode}; {@code compareTo} is never consulted. Lookups call {@code hashCode} as
 * {@code HashMap} did; evictions and {@code removeIf} no longer call {@code hashCode} or
 * {@code equals} on the keys they remove. A key whose {@code equals} changes the cache during a
 * lookup gets a {@code ConcurrentModificationException}, and so does a {@code removeIf} predicate
 * that frees the next resident of its walk, moves it to the other segment, or frees the resident it
 * then matches; the node version failed there or went on over detached nodes. An
 * {@code ArithmeticException} from a weight total near {@code Long.MAX_VALUE} is thrown by the same
 * call as before; the lanes are left consistent (the node version could keep a resident on no
 * list).</p>
 *
 * @param <K> key type
 * @param <V> value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/WeightedSegmentedLRUCache.java}
 * blob {@code 0c827a068c78bcd61ff234f04f12dd1d67438a78} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class WeightedSegmentedLRUCache<K, V> {

    /** Caller-owned retained-weight function. */
    @FunctionalInterface
    public interface Weigher<K, V> {
        long weightOf(K key, V value);
    }

    /** Result of a pressure/admission trim. */
    public record TrimResult(
            int evictedEntries,
            long evictedWeight,
            int demotedEntries) {

        public TrimResult {
            if (evictedEntries < 0 || evictedWeight < 0L || demotedEntries < 0) {
                throw new IllegalArgumentException("negative segmented LRU trim result");
            }
        }

        public static TrimResult none() {
            return new TrimResult(0, 0L, 0);
        }

        public TrimResult plus(TrimResult other) {
            Objects.requireNonNull(other, "other");
            return new TrimResult(
                    Math.addExact(evictedEntries, other.evictedEntries),
                    Math.addExact(evictedWeight, other.evictedWeight),
                    Math.addExact(demotedEntries, other.demotedEntries));
        }
    }

    /** Result of an admission attempt. */
    public record PutResult(
            boolean admitted,
            boolean replaced,
            long admittedWeight,
            TrimResult trim) {

        public PutResult {
            if (admittedWeight < 0L) {
                throw new IllegalArgumentException("admittedWeight must be >= 0");
            }
            trim = Objects.requireNonNull(trim, "trim");
            if (!admitted && admittedWeight != 0L) {
                throw new IllegalArgumentException(
                        "rejected admission cannot report retained weight");
            }
        }
    }

    /** One current hard-budget victim in exact eviction priority order. */
    public record AdmissionVictim<K>(K key, long weight, boolean protectedResident) {
        public AdmissionVictim {
            Objects.requireNonNull(key, "key");
            if (weight <= 0L) {
                throw new IllegalArgumentException("victim weight must be > 0");
            }
        }
    }

    /** Result of explicit removal. */
    public record RemoveResult(int removedEntries, long removedWeight) {
        public RemoveResult {
            if (removedEntries < 0 || removedWeight < 0L) {
                throw new IllegalArgumentException("negative remove result");
            }
        }
    }

    /** Immutable diagnostics. */
    public record Snapshot(
            int entries,
            int probationEntries,
            int protectedEntries,
            long weight,
            long probationWeight,
            long protectedWeight,
            int maxEntries,
            long maxWeight,
            int protectedEntryTarget,
            long protectedWeightTarget,
            long hits,
            long probationHits,
            long protectedHits,
            long misses,
            long puts,
            long replacements,
            long promotions,
            long demotions,
            long evictions,
            long probationEvictions,
            long protectedEvictions,
            long oversizeRejections) {

        public Snapshot {
            if (entries < 0
                    || probationEntries < 0
                    || protectedEntries < 0
                    || weight < 0L
                    || probationWeight < 0L
                    || protectedWeight < 0L
                    || maxEntries <= 0
                    || maxWeight <= 0L
                    || protectedEntryTarget < 0
                    || protectedWeightTarget < 0L
                    || hits < 0L
                    || probationHits < 0L
                    || protectedHits < 0L
                    || misses < 0L
                    || puts < 0L
                    || replacements < 0L
                    || promotions < 0L
                    || demotions < 0L
                    || evictions < 0L
                    || probationEvictions < 0L
                    || protectedEvictions < 0L
                    || oversizeRejections < 0L) {
                throw new IllegalArgumentException("invalid segmented LRU snapshot");
            }
            if (probationEntries + protectedEntries != entries
                    || probationWeight + protectedWeight != weight
                    || probationHits + protectedHits != hits) {
                throw new IllegalArgumentException("segmented LRU accounting mismatch");
            }
        }
    }

    /** Slot index meaning "none" in the recency lanes, the free list and lookups. */
    private static final int NONE = -1;
    /** Segment of a resident; also the position of that segment's list ends in mru and lru. */
    private static final byte PROBATION = 0;
    private static final byte PROTECTED = 1;
    /** Slots of the first allocation; the lanes double up to {@code maxEntries + 1}. */
    private static final int MIN_SLOTS = 8;
    /** Largest number of slots: the index (2^30 entries at most) stays at most half full. */
    private static final int MAX_SLOTS = 1 << 29;
    /** The lanes of an empty cache. */
    private static final Object[] NO_OBJECTS = {};
    private static final long[] NO_LONGS = {};
    private static final int[] NO_INTS = {};
    private static final byte[] NO_BYTES = {};
    /** Golden-ratio multiplier (2^32 / phi): Fibonacci hashing of a key's hash into the index. */
    private static final int GOLDEN_RATIO = 0x9E3779B9;

    private final int maxEntries;
    private final long maxWeight;
    private final int protectedEntryTarget;
    private final long protectedWeightTarget;
    private final Weigher<K, V> weigher;

    /** Storage lanes of the residents, one slot per resident; a free slot holds nulls. */
    private Object[] keyLane = NO_OBJECTS;
    private Object[] valueLane = NO_OBJECTS;
    private long[] weightLane = NO_LONGS;
    /** {@code key.hashCode()} of each resident, kept so the index never calls it again. */
    private int[] hashLane = NO_INTS;
    /** PROBATION or PROTECTED, the list a resident is on. */
    private byte[] segmentLane = NO_BYTES;
    /** Recency lanes: the neighbour toward the most- and toward the least-recently-used end. */
    private int[] newerLane = NO_INTS;
    private int[] olderLane = NO_INTS;
    /** Open-addressing index: a key's hash to its slot plus one (0 is empty), linear probing. */
    private int[] index = NO_INTS;
    /** log2 of the index length. */
    private int indexBits;
    /** A key was admitted once: lookups call hashCode from then on, as HashMap after a put. */
    private boolean hashing;
    /** The value of {@code structure} when the slot's resident was admitted; 0 for a free slot. */
    private int[] stampLane = NO_INTS;
    /** Incremented by every admission, release and drop of the lanes; never reset. */
    private int structure;

    /** Most- and least-recently-used slot of each segment's list, by segment, or NONE. */
    private final int[] mru = {NONE, NONE};
    private final int[] lru = {NONE, NONE};
    /** Top of the stack of free slots, linked through {@code olderLane}, or NONE. */
    private int freeTop = NONE;
    /** Slots ever handed out; slots at or above it are unused. */
    private int used;
    private int size;

    /** Residents and their weight, by segment. */
    private final int[] segmentEntries = new int[2];
    private final long[] segmentWeight = new long[2];

    private long hits;
    private long probationHits;
    private long protectedHits;
    private long misses;
    private long puts;
    private long replacements;
    private long promotions;
    private long demotions;
    private long evictions;
    private long probationEvictions;
    private long protectedEvictions;
    private long oversizeRejections;

    /**
     * Build a segmented LRU.
     *
     * @param protectedFraction fraction of the configured count/weight budget reserved as the
     *     protected target; must be strictly between 0 and 1
     */
    public WeightedSegmentedLRUCache(
            int maxEntries,
            long maxWeight,
            double protectedFraction,
            Weigher<K, V> weigher) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be > 0");
        }
        if (maxWeight <= 0L) {
            throw new IllegalArgumentException("maxWeight must be > 0");
        }
        if (!Double.isFinite(protectedFraction)
                || protectedFraction <= 0.0d
                || protectedFraction >= 1.0d) {
            throw new IllegalArgumentException("protectedFraction must be between 0 and 1");
        }

        this.maxEntries = maxEntries;
        this.maxWeight = maxWeight;
        this.protectedEntryTarget =
                maxEntries == 1
                        ? 1
                        : Math.max(1, Math.min(maxEntries - 1,
                                (int) Math.floor(maxEntries * protectedFraction)));
        this.protectedWeightTarget =
                Math.max(1L, Math.min(maxWeight,
                        (long) Math.floor(maxWeight * protectedFraction)));
        this.weigher = Objects.requireNonNull(weigher, "weigher");
    }

    /** Lookup, updating segment-local recency and promoting a probation hit. */
    @SuppressWarnings("unchecked")
    public synchronized Optional<V> get(K key) {
        int slot = slotOf(key);
        if (slot == NONE) {
            misses++;
            return Optional.empty();
        }

        hits++;
        if (segmentLane[slot] == PROBATION) {
            probationHits++;
            promote(slot);
            rebalanceProtected();
        } else {
            protectedHits++;
            moveToFront(slot);
        }
        return Optional.of((V) valueLane[slot]);
    }

    /**
     * Admit or replace one entry.
     *
     * <p>New entries enter probation as MRU. Replacing an existing entry preserves its segment and
     * promotes its recency. An entry heavier than the absolute cache budget is rejected without
     * disturbing the resident working set.</p>
     *
     * @throws IllegalStateException if 2<sup>29</sup> residents already fill the lanes
     */
    public synchronized PutResult put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        long candidateWeight = requireWeight(weigher.weightOf(key, value));
        if (candidateWeight > maxWeight) {
            oversizeRejections++;
            return new PutResult(false, false, 0L, TrimResult.none());
        }

        int slot = slotOf(key);
        boolean replaced = slot != NONE;
        if (slot == NONE) {
            // hashed first (a hashCode that throws or calls back finds nothing half done), then an
            // overflowing probation weight throws before the entry is admitted
            int hash = key.hashCode();
            Math.addExact(segmentWeight[PROBATION], candidateWeight);
            slot = admit(key, value, candidateWeight, hash);
            addToFront(slot);
            segmentEntries[PROBATION]++;
            segmentWeight[PROBATION] =
                    Math.addExact(segmentWeight[PROBATION], candidateWeight);
        } else {
            byte segment = segmentLane[slot];
            // an overflowing segment weight throws before the resident changes
            long segmentTotal =
                    Math.addExact(segmentWeight[segment] - weightLane[slot], candidateWeight);
            valueLane[slot] = value;
            weightLane[slot] = candidateWeight;
            segmentWeight[segment] = segmentTotal;
            moveToFront(slot);
            replacements++;
        }
        puts++;

        TrimResult trim = rebalanceProtected().plus(trimToInternal(maxEntries, maxWeight));
        return new PutResult(true, replaced, candidateWeight, trim);
    }

    /**
     * Return the exact entries a new candidate of the supplied weight would evict at the configured
     * hard limits, without changing recency, segment membership or statistics.
     *
     * <p>The order is the same order used by hard trimming: probation LRU first, then protected
     * LRU. This enables a frequency-aware admission layer to compare one candidate with every
     * resident it would displace before committing the mutation.</p>
     */
    public synchronized List<AdmissionVictim<K>> admissionVictims(long candidateWeight) {
        return admissionVictims(candidateWeight, maxEntries, maxWeight);
    }

    /**
     * Return the exact victims under caller-selected temporary limits.
     *
     * <p>This is used by composite cache policies whose other resident region consumes part of the
     * same global count/weight budget. The supplied limits may tighten, but never exceed, this
     * cache's configured hard limits.</p>
     */
    @SuppressWarnings("unchecked")
    synchronized List<AdmissionVictim<K>> admissionVictims(
            long candidateWeight,
            int entryLimit,
            long weightLimit) {
        long checkedWeight = requireWeight(candidateWeight);
        if (entryLimit < 0 || entryLimit > maxEntries) {
            throw new IllegalArgumentException("entryLimit out of configured range");
        }
        if (weightLimit < 0L || weightLimit > maxWeight) {
            throw new IllegalArgumentException("weightLimit out of configured range");
        }
        if (entryLimit == 0 || checkedWeight > weightLimit) {
            return List.of();
        }

        int excessEntries = Math.max(0, size + 1 - entryLimit);
        long freeWeight = Math.max(0L, weightLimit - weight());
        long excessWeight = checkedWeight > freeWeight ? checkedWeight - freeWeight : 0L;
        if (excessEntries == 0 && excessWeight == 0L) {
            return List.of();
        }

        ArrayList<AdmissionVictim<K>> victims = new ArrayList<>();
        for (int segment = PROBATION; segment <= PROTECTED; segment++) {
            int slot = lru[segment];
            while ((excessEntries > 0 || excessWeight > 0L) && slot != NONE) {
                long slotWeight = weightLane[slot];
                victims.add(new AdmissionVictim<>(
                        (K) keyLane[slot], slotWeight, segment == PROTECTED));
                excessEntries = Math.max(0, excessEntries - 1);
                excessWeight = Math.max(0L, excessWeight - slotWeight);
                slot = newerLane[slot];
            }
        }
        return List.copyOf(victims);
    }

    /** Explicitly remove one key without affecting hit/miss statistics. */
    @SuppressWarnings("unchecked")
    public synchronized Optional<V> remove(K key) {
        // as HashMap.remove: a key is hashed first, even before the first admission
        int slot = key == null ? NONE : find(key, key.hashCode());
        if (slot == NONE) {
            return Optional.empty();
        }
        V value = (V) valueLane[slot];
        release(slot);
        if (size == 0) {
            dropLanes();
        }
        return Optional.of(value);
    }

    /**
     * Remove all matching keys in segment-LRU order.
     *
     * @throws ConcurrentModificationException if the predicate frees the next resident of the walk
     *     or moves it to the other segment, or frees the resident it then matches, itself or by
     *     {@code clear}, {@code trimTo} or an eviction; a resident it removes and does not match is
     *     skipped
     */
    public synchronized RemoveResult removeIf(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int removedEntries = 0;
        long removedWeight = 0L;

        RemoveAccumulator probation = removeIfFromList(PROBATION, predicate);
        removedEntries = Math.addExact(removedEntries, probation.entries());
        removedWeight = Math.addExact(removedWeight, probation.weight());

        RemoveAccumulator protectedRemoved = removeIfFromList(PROTECTED, predicate);
        removedEntries = Math.addExact(removedEntries, protectedRemoved.entries());
        removedWeight = Math.addExact(removedWeight, protectedRemoved.weight());

        if (size == 0) {
            dropLanes();
        }
        return new RemoveResult(removedEntries, removedWeight);
    }

    /**
     * Trim to temporary total limits.
     *
     * <p>Probation LRU entries are evicted first. This is intentional: one-hit scan entries should
     * leave before entries whose reuse earned protected residency.</p>
     */
    public synchronized TrimResult trimTo(int entryLimit, long weightLimit) {
        if (entryLimit < 0 || entryLimit > maxEntries) {
            throw new IllegalArgumentException(
                    "entryLimit must be between 0 and configured maxEntries");
        }
        if (weightLimit < 0L || weightLimit > maxWeight) {
            throw new IllegalArgumentException(
                    "weightLimit must be between 0 and configured maxWeight");
        }
        TrimResult trim = trimToInternal(entryLimit, weightLimit);
        if (size == 0) {
            dropLanes();
        }
        return trim;
    }

    /** Remove every cache-owned value; the lanes are dropped with every reference they held. */
    public synchronized RemoveResult clear() {
        int entries = size;
        long weight = weight();
        size = 0;
        dropLanes();
        Arrays.fill(segmentEntries, 0);
        Arrays.fill(segmentWeight, 0L);
        return new RemoveResult(entries, weight);
    }

    /** Membership check that does not alter recency or promotion state. */
    public synchronized boolean containsKey(K key) {
        return slotOf(key) != NONE;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized long weight() {
        return Math.addExact(segmentWeight[PROBATION], segmentWeight[PROTECTED]);
    }

    public int maxEntries() {
        return maxEntries;
    }

    public long maxWeight() {
        return maxWeight;
    }

    public int protectedEntryTarget() {
        return protectedEntryTarget;
    }

    public long protectedWeightTarget() {
        return protectedWeightTarget;
    }

    /**
     * Deterministic eviction order: probation LRU→MRU followed by protected LRU→MRU.
     *
     * <p>This is an eviction-priority order, not a claim that the two segments form one global
     * recency clock.</p>
     */
    @SuppressWarnings("unchecked")
    public synchronized List<K> keysInLruOrder() {
        ArrayList<K> keys = new ArrayList<>(size);
        for (int segment = PROBATION; segment <= PROTECTED; segment++) {
            for (int slot = lru[segment]; slot != NONE; slot = newerLane[slot]) {
                keys.add((K) keyLane[slot]);
            }
        }
        return List.copyOf(keys);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(
                size,
                segmentEntries[PROBATION],
                segmentEntries[PROTECTED],
                weight(),
                segmentWeight[PROBATION],
                segmentWeight[PROTECTED],
                maxEntries,
                maxWeight,
                protectedEntryTarget,
                protectedWeightTarget,
                hits,
                probationHits,
                protectedHits,
                misses,
                puts,
                replacements,
                promotions,
                demotions,
                evictions,
                probationEvictions,
                protectedEvictions,
                oversizeRejections);
    }

    private TrimResult rebalanceProtected() {
        int demoted = 0;
        while (segmentEntries[PROTECTED] > protectedEntryTarget
                || segmentWeight[PROTECTED] > protectedWeightTarget) {
            int candidate = lru[PROTECTED];
            if (candidate == NONE) break;
            demote(candidate);
            demoted++;
        }
        return demoted == 0
                ? TrimResult.none()
                : new TrimResult(0, 0L, demoted);
    }

    private TrimResult trimToInternal(int entryLimit, long weightLimit) {
        int evictedEntries = 0;
        long evictedWeight = 0L;

        while (size > 0 && (size > entryLimit || weight() > weightLimit)) {
            int victim = lru[PROBATION];
            if (victim == NONE) {
                victim = lru[PROTECTED];
                if (victim == NONE) break;
            }
            long victimWeight = weightLane[victim];
            byte segment = segmentLane[victim];
            release(victim);
            evictedEntries++;
            evictedWeight = Math.addExact(evictedWeight, victimWeight);
            evictions++;
            if (segment == PROBATION) probationEvictions++;
            else protectedEvictions++;
        }

        return evictedEntries == 0
                ? TrimResult.none()
                : new TrimResult(evictedEntries, evictedWeight, 0);
    }

    /** Tests the keys of one segment from its least recently used on, removing the matches. */
    @SuppressWarnings("unchecked")
    private RemoveAccumulator removeIfFromList(byte segment, Predicate<? super K> predicate) {
        int count = 0;
        long removedWeight = 0L;
        int current = lru[segment];
        while (current != NONE) {
            int newer = newerLane[current];
            int stamp = stampLane[current];
            int newerStamp = newer == NONE ? 0 : stampLane[newer];
            boolean matches = predicate.test((K) keyLane[current]);
            requireWalkIntact(segment, current, stamp, matches, newer, newerStamp);
            if (matches) {
                removedWeight = Math.addExact(removedWeight, weightLane[current]);
                count++;
                release(current);
            }
            current = newer;
        }
        return new RemoveAccumulator(count, removedWeight);
    }

    private void promote(int slot) {
        if (segmentLane[slot] != PROBATION) {
            throw new IllegalStateException("only probation entries can be promoted");
        }
        changeSegment(slot, PROBATION, PROTECTED);
        promotions++;
    }

    private void demote(int slot) {
        if (segmentLane[slot] != PROTECTED) {
            throw new IllegalStateException("only protected entries can be demoted");
        }
        changeSegment(slot, PROTECTED, PROBATION);
        demotions++;
    }

    /** Moves a resident to the front of the other segment's list, with its count and weight. */
    private void changeSegment(int slot, byte from, byte to) {
        // an overflowing segment weight throws before the resident moves
        long toWeight = Math.addExact(segmentWeight[to], weightLane[slot]);
        unlink(slot);
        segmentEntries[from]--;
        segmentWeight[from] -= weightLane[slot];
        segmentLane[slot] = to;
        segmentEntries[to]++;
        segmentWeight[to] = toWeight;
        addToFront(slot);
    }

    /** The slot of a resident equal to {@code key}, or NONE; hashes as {@code HashMap.get}. */
    private int slotOf(Object key) {
        if (key == null || !hashing) {
            // as HashMap: no hashCode call before the first admission, one per lookup after it
            return NONE;
        }
        return find(key, key.hashCode());
    }

    /**
     * The slot of a resident with the given hash and equal to {@code key} ({@code
     * key.equals(resident)}), or NONE.
     *
     * @throws ConcurrentModificationException if a key's {@code equals} changes the cache
     */
    private int find(Object key, int hash) {
        if (size == 0) {
            return NONE;
        }
        int seen = structure;
        int mask = index.length - 1;
        for (int i = home(hash); index[i] != 0; i = (i + 1) & mask) {
            int slot = index[i] - 1;
            if (hashLane[slot] == hash) {
                Object resident = keyLane[slot];
                if (resident == key) {
                    return slot;
                }
                boolean equal = key.equals(resident);
                if (structure != seen) {
                    throw new ConcurrentModificationException("a key's equals changed the cache");
                }
                if (equal) {
                    return slot;
                }
            }
        }
        return NONE;
    }

    /** Stores a new probation resident (hashed already) in a free slot and indexes it. */
    private int admit(K key, V value, long entryWeight, int hash) {
        int slot = takeSlot();
        keyLane[slot] = key;
        valueLane[slot] = value;
        weightLane[slot] = entryWeight;
        hashLane[slot] = hash;
        segmentLane[slot] = PROBATION;
        indexSlot(slot, hash);
        if (++structure == 0) {
            structure = 1;
        }
        stampLane[slot] = structure;
        hashing = true;
        size++;
        return slot;
    }

    /** Unlinks, unindexes, uncounts and frees a resident's slot. */
    private void release(int slot) {
        byte segment = segmentLane[slot];
        unlink(slot);
        unindexSlot(slot);
        segmentEntries[segment]--;
        segmentWeight[segment] -= weightLane[slot];
        keyLane[slot] = null;
        valueLane[slot] = null;
        stampLane[slot] = 0;
        structure++;
        olderLane[slot] = freeTop;
        freeTop = slot;
        size--;
    }

    private int takeSlot() {
        if (freeTop != NONE) {
            int slot = freeTop;
            freeTop = olderLane[slot];
            return slot;
        }
        if (used == keyLane.length) {
            grow();
        }
        return used++;
    }

    /**
     * Doubles the lanes, at most to maxEntries + 1 slots (a put adds before it trims), or one slot
     * more when an overflow stopped a trim. Every array is allocated before a field changes: an
     * OutOfMemoryError leaves the cache as it was.
     */
    private void grow() {
        long limit = Math.min(Math.max((long) maxEntries + 1, keyLane.length + 1L), MAX_SLOTS);
        int capacity = (int) Math.min(Math.max(MIN_SLOTS, 2L * keyLane.length), limit);
        if (capacity <= keyLane.length) {
            throw new IllegalStateException("WeightedSegmentedLRUCache lanes are full");
        }
        // the index is sized for at most maxEntries residents (load at most 1/2) and keeps one cell
        // empty when the lanes hold the extra slot a put takes before it trims
        long cells = Math.max(2 * Math.min(capacity, (long) maxEntries), capacity + 1L);
        int bits = 64 - Long.numberOfLeadingZeros(cells - 1);
        Object[] keys = Arrays.copyOf(keyLane, capacity);
        Object[] values = Arrays.copyOf(valueLane, capacity);
        long[] weights = Arrays.copyOf(weightLane, capacity);
        int[] hashes = Arrays.copyOf(hashLane, capacity);
        byte[] segments = Arrays.copyOf(segmentLane, capacity);
        int[] newer = Arrays.copyOf(newerLane, capacity);
        int[] older = Arrays.copyOf(olderLane, capacity);
        int[] stamps = Arrays.copyOf(stampLane, capacity);
        int[] fresh = bits > indexBits ? new int[1 << bits] : index;
        keyLane = keys;
        valueLane = values;
        weightLane = weights;
        hashLane = hashes;
        segmentLane = segments;
        newerLane = newer;
        olderLane = older;
        stampLane = stamps;
        if (fresh != index) {
            reindex(fresh, bits);
        }
    }

    /** Takes a fresh index of 2^bits entries, filled from the kept hashes. */
    private void reindex(int[] fresh, int bits) {
        index = fresh;
        indexBits = bits;
        for (int slot = 0; slot < used; slot++) {
            if (keyLane[slot] != null) {
                indexSlot(slot, hashLane[slot]);
            }
        }
    }

    private int home(int hash) {
        return (hash * GOLDEN_RATIO) >>> (32 - indexBits);
    }

    private void indexSlot(int slot, int hash) {
        int mask = index.length - 1;
        int i = home(hash);
        while (index[i] != 0) {
            i = (i + 1) & mask;
        }
        index[i] = slot + 1;
    }

    /** Removes a slot from the index and shifts later entries of its run back (no tombstones). */
    private void unindexSlot(int slot) {
        int mask = index.length - 1;
        int hole = home(hashLane[slot]);
        while (index[hole] != slot + 1) {
            hole = (hole + 1) & mask;
        }
        for (int i = (hole + 1) & mask; index[i] != 0; i = (i + 1) & mask) {
            int wanted = home(hashLane[index[i] - 1]);
            // the entry at i may move to the hole when its home is not within (hole, i]
            if (((i - wanted) & mask) >= ((i - hole) & mask)) {
                index[hole] = index[i];
                hole = i;
            }
        }
        index[hole] = 0;
    }

    /** Moves a resident to the front of its own segment's list. */
    private void moveToFront(int slot) {
        if (slot != mru[segmentLane[slot]]) {
            unlink(slot);
            addToFront(slot);
        }
    }

    private void addToFront(int slot) {
        byte segment = segmentLane[slot];
        int front = mru[segment];
        newerLane[slot] = NONE;
        olderLane[slot] = front;
        if (front != NONE) {
            newerLane[front] = slot;
        } else {
            lru[segment] = slot;
        }
        mru[segment] = slot;
    }

    private void unlink(int slot) {
        byte segment = segmentLane[slot];
        int newer = newerLane[slot];
        int older = olderLane[slot];
        if (newer != NONE) {
            olderLane[newer] = older;
        } else {
            mru[segment] = older;
        }
        if (older != NONE) {
            newerLane[older] = newer;
        } else {
            lru[segment] = newer;
        }
    }

    /** Whether {@code slot} is a slot of the lanes still holding the resident of {@code stamp}. */
    private boolean holds(int slot, int stamp) {
        return slot < stampLane.length && stampLane[slot] == stamp;
    }

    /**
     * Throws when a callback freed the next resident of a segment walk or moved it to the other
     * segment, or freed the resident under test that it then matched, even if a new resident took
     * the slot. A resident removed but not matched is skipped, as the node-linked version did.
     */
    private void requireWalkIntact(
            byte segment, int current, int stamp, boolean matches, int newer, int newerStamp) {
        if ((matches && !holds(current, stamp))
                || (newer != NONE
                        && (!holds(newer, newerStamp) || segmentLane[newer] != segment))) {
            throw new ConcurrentModificationException(
                    "the removeIf predicate removed a resident under the walk");
        }
    }

    /** Lets an empty cache drop its lanes and index; the next admission allocates them again. */
    private void dropLanes() {
        keyLane = NO_OBJECTS;
        valueLane = NO_OBJECTS;
        weightLane = NO_LONGS;
        hashLane = NO_INTS;
        segmentLane = NO_BYTES;
        newerLane = NO_INTS;
        olderLane = NO_INTS;
        stampLane = NO_INTS;
        structure++;
        index = NO_INTS;
        indexBits = 0;
        Arrays.fill(mru, NONE);
        Arrays.fill(lru, NONE);
        freeTop = NONE;
        used = 0;
    }

    private static long requireWeight(long weight) {
        if (weight <= 0L) {
            throw new IllegalArgumentException("entry weight must be > 0");
        }
        return weight;
    }

    private record RemoveAccumulator(int entries, long weight) {}
}
