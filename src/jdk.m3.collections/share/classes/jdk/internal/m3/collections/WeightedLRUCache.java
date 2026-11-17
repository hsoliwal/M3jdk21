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
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Predicate;

/**
 * Thread-safe weighted least-recently-used cache.
 *
 * <p>Unlike {@code LRUCache}, this cache bounds both entry count and caller-defined retained
 * weight. The cache does not assume that weight is exact JVM heap usage; the supplied weigher owns
 * that contract. All get/put/remove/trim operations are O(1) amortized except predicate removal and
 * ordered-key snapshots, which are O(n).</p>
 *
 * <p>The residents live in array lanes, one slot each (key, value, weight, hash), with the recency
 * list as two {@code int} lanes of slot indices and an open-addressing {@code int} index from a
 * key's hash to its slot; there is no node or map entry object per resident. Freed slots are
 * reused, the lanes double up to {@code maxEntries + 1} slots (at most 2<sup>29</sup>) and are
 * dropped when an operation empties the cache (clear, trim, removeIf or the removal of the last
 * resident), and the index never calls a resident's {@code hashCode} again (the hashes are kept).
 * Access order does not depend on {@code LinkedHashMap} callbacks and trim operations return
 * precise eviction evidence.</p>
 *
 * <p>2026-11-17: storage rewritten onto array lanes (arrayify, leaf 1). Every method keeps its
 * results, recency order, counters and exceptions for keys that obey {@code equals} and
 * {@code hashCode}; {@code compareTo} is never consulted. Lookups call {@code hashCode} as
 * {@code HashMap} did; evictions and {@code removeIf} no longer call {@code hashCode} or
 * {@code equals} on the keys they remove. A key whose {@code equals} adds or removes a resident
 * during a lookup, or whose {@code hashCode} does so while the key is admitted, gets a
 * {@code ConcurrentModificationException}, and so does a {@code removeIf} predicate
 * that frees the next resident of its walk or the resident it then matches; the node version
 * failed there or went on over detached nodes.</p>
 *
 * @param <K> key type
 * @param <V> value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/WeightedLRUCache.java}
 * blob {@code 63bd9921ecc0e453299442ece6026cdc93b80b87} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class WeightedLRUCache<K, V> {

    /** Caller-owned retained-weight function. */
    @FunctionalInterface
    public interface Weigher<K, V> {
        long weightOf(K key, V value);
    }

    /** Result of an LRU trim or automatic admission trim. */
    public record TrimResult(int evictedEntries, long evictedWeight) {
        public TrimResult {
            if (evictedEntries < 0 || evictedWeight < 0L) {
                throw new IllegalArgumentException("negative trim result");
            }
        }

        public static TrimResult none() {
            return new TrimResult(0, 0L);
        }

        public TrimResult plus(TrimResult other) {
            Objects.requireNonNull(other, "other");
            return new TrimResult(
                    Math.addExact(evictedEntries, other.evictedEntries),
                    Math.addExact(evictedWeight, other.evictedWeight));
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

    /**
     * Result of a maintenance replacement that deliberately preserves the entry's current LRU
     * position.
     *
     * <p>This operation exists for pressure-driven representation demotion. It never admits an
     * absent key and never allows a heavier replacement.</p>
     */
    public record MaintenanceReplaceResult(
            boolean present,
            boolean replaced,
            long previousWeight,
            long replacementWeight,
            long releasedWeight) {
        public MaintenanceReplaceResult {
            if (previousWeight < 0L || replacementWeight < 0L || releasedWeight < 0L) {
                throw new IllegalArgumentException("negative maintenance replacement weight");
            }
            if (!present && (replaced || previousWeight != 0L || replacementWeight != 0L
                    || releasedWeight != 0L)) {
                throw new IllegalArgumentException("absent maintenance replacement has weight");
            }
            if (replaced && replacementWeight >= previousWeight) {
                throw new IllegalArgumentException("maintenance replacement must be strictly lighter");
            }
            if (replaced && releasedWeight != previousWeight - replacementWeight) {
                throw new IllegalArgumentException("maintenance replacement release mismatch");
            }
            if (!replaced && releasedWeight != 0L) {
                throw new IllegalArgumentException("rejected maintenance replacement released weight");
            }
        }

        public static MaintenanceReplaceResult absent() {
            return new MaintenanceReplaceResult(false, false, 0L, 0L, 0L);
        }
    }

    /** Result of explicit predicate removal. */
    public record RemoveResult(int removedEntries, long removedWeight) {
        public RemoveResult {
            if (removedEntries < 0 || removedWeight < 0L) {
                throw new IllegalArgumentException("negative remove result");
            }
        }
    }

    /** Immutable runtime statistics. */
    public record Snapshot(
            int entries,
            long weight,
            int maxEntries,
            long maxWeight,
            long hits,
            long misses,
            long puts,
            long replacements,
            long evictions,
            long oversizeRejections) {
        public Snapshot {
            if (entries < 0
                    || weight < 0L
                    || maxEntries <= 0
                    || maxWeight <= 0L
                    || hits < 0L
                    || misses < 0L
                    || puts < 0L
                    || replacements < 0L
                    || evictions < 0L
                    || oversizeRejections < 0L) {
                throw new IllegalArgumentException("invalid weighted LRU snapshot");
            }
        }
    }

    /** Slot index meaning "none" in the recency lanes, the free list and lookups. */
    private static final int NONE = -1;
    /** Slots of the first allocation; the lanes double up to {@code maxEntries + 1}. */
    private static final int MIN_SLOTS = 8;
    /** Largest number of slots: the index (2^30 entries at most) stays at most half full. */
    private static final int MAX_SLOTS = 1 << 29;
    /** The lanes of an empty cache. */
    private static final Object[] NO_OBJECTS = {};
    private static final long[] NO_LONGS = {};
    private static final int[] NO_INTS = {};
    /** Golden-ratio multiplier (2^32 / phi): Fibonacci hashing of a key's hash into the index. */
    private static final int GOLDEN_RATIO = 0x9E3779B9;

    private final int maxEntries;
    private final long maxWeight;
    private final Weigher<K, V> weigher;

    /** Storage lanes of the residents, one slot per resident; a free slot holds nulls. */
    private Object[] keyLane = NO_OBJECTS;
    private Object[] valueLane = NO_OBJECTS;
    private long[] weightLane = NO_LONGS;
    /** {@code key.hashCode()} of each resident, kept: the index never calls it again. */
    private int[] hashLane = NO_INTS;
    /** Recency lanes: the neighbour toward the most- and toward the least-recently-used end. */
    private int[] newerLane = NO_INTS;
    private int[] olderLane = NO_INTS;
    /** Open-addressing index, linear probing: from a key's hash to its slot plus one (0: empty). */
    private int[] index = NO_INTS;
    /** log2 of the index length. */
    private int indexBits;
    /** A key was admitted once: lookups call hashCode from then on, as HashMap after a put. */
    private boolean hashing;
    /** The value of {@code structure} when the slot's resident was admitted; 0 for a free slot. */
    private int[] stampLane = NO_INTS;
    /** Incremented by every admission, release and drop of the lanes; never reset. */
    private int structure;

    /** Most- and least-recently-used slot, or NONE. */
    private int mru = NONE;
    private int lru = NONE;
    /** Top of the stack of free slots, linked through {@code olderLane}, or NONE. */
    private int freeTop = NONE;
    /** Slots ever handed out; slots at or above it are unused. */
    private int used;
    private int size;

    private long weight;
    private long hits;
    private long misses;
    private long puts;
    private long replacements;
    private long evictions;
    private long oversizeRejections;

    public WeightedLRUCache(
            int maxEntries,
            long maxWeight,
            Weigher<K, V> weigher) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be > 0");
        }
        if (maxWeight <= 0L) {
            throw new IllegalArgumentException("maxWeight must be > 0");
        }
        this.maxEntries = maxEntries;
        this.maxWeight = maxWeight;
        this.weigher = Objects.requireNonNull(weigher, "weigher");
    }

    /** Lookup and promote the key to most-recently-used position. */
    @SuppressWarnings("unchecked")
    public synchronized Optional<V> get(K key) {
        int slot = slotOf(key);
        if (slot == NONE) {
            misses++;
            return Optional.empty();
        }
        hits++;
        moveToFront(slot);
        return Optional.of((V) valueLane[slot]);
    }

    /**
     * Admit or replace one entry.
     *
     * <p>An entry heavier than the configured absolute weight limit is rejected without evicting
     * existing entries. Accepted entries are promoted to MRU and the cache trims to both configured
     * limits.</p>
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
            slot = admit(key, value, candidateWeight);
            addToFront(slot);
        } else {
            weight -= weightLane[slot];
            valueLane[slot] = value;
            weightLane[slot] = candidateWeight;
            moveToFront(slot);
            replacements++;
        }
        // The candidate is already MRU, but its new charge is not in weight yet.
        // Trim the other charge first: maxWeight-candidateWeight cannot overflow.
        // This is equivalent to add-then-trim over mathematical integers, without
        // overflowing a transient long total and leaving partially changed state.
        TrimResult trim = trimToInternal(maxEntries, maxWeight - candidateWeight);
        weight = Math.addExact(weight, candidateWeight);
        puts++;
        return new PutResult(true, replaced, candidateWeight, trim);
    }

    /**
     * Observe one resident without changing recency or hit/miss statistics.
     *
     * <p>This is intended for maintenance code such as pressure-driven representation demotion.
     * Normal callers should prefer {@code get(...)} so useful work contributes recency.</p>
     */
    @SuppressWarnings("unchecked")
    public synchronized Optional<V> peek(K key) {
        Objects.requireNonNull(key, "key");
        int slot = slotOf(key);
        return slot == NONE ? Optional.empty() : Optional.of((V) valueLane[slot]);
    }

    /** Observe one resident's admitted weight without changing recency or statistics. */
    public synchronized OptionalLong peekWeight(K key) {
        Objects.requireNonNull(key, "key");
        int slot = slotOf(key);
        return slot == NONE ? OptionalLong.empty() : OptionalLong.of(weightLane[slot]);
    }

    /**
     * Replace an existing value only when the new representation is strictly lighter, without changing
     * its recency position.
     *
     * <p>The operation does not alter get/put hit, miss, put, replacement, or eviction counters.
     * It is a maintenance primitive for stripping rebuildable derived state while preserving the
     * semantic resident and its LRU ordering. An absent key is never admitted.</p>
     */
    public synchronized MaintenanceReplaceResult replaceIfLighterPreservingRecency(
            K key, V replacement) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(replacement, "replacement");
        int slot = slotOf(key);
        if (slot == NONE) return MaintenanceReplaceResult.absent();

        int stamp = stampLane[slot];
        long residentWeight = weightLane[slot];
        long replacementWeight = requireWeight(weigher.weightOf(key, replacement));
        // A weigher that removed the resident (freeing, perhaps reusing, its slot) gets the result
        // of the old resident and nothing is written, as the node version wrote a detached node.
        boolean stillResident = holds(slot, stamp);
        long previousWeight = stillResident ? weightLane[slot] : residentWeight;
        if (replacementWeight >= previousWeight) {
            return new MaintenanceReplaceResult(
                    true, false, previousWeight, replacementWeight, 0L);
        }

        long released = previousWeight - replacementWeight;
        if (stillResident) {
            valueLane[slot] = replacement;
            weightLane[slot] = replacementWeight;
            weight -= released;
        }
        return new MaintenanceReplaceResult(
                true, true, previousWeight, replacementWeight, released);
    }

    /** Explicitly remove one key. */
    @SuppressWarnings("unchecked")
    public synchronized Optional<V> remove(K key) {
        // as HashMap.remove: a key is hashed first, even before the first admission
        int slot = key == null ? NONE : find(key, key.hashCode());
        if (slot == NONE) {
            return Optional.empty();
        }
        V value = (V) valueLane[slot];
        weight -= weightLane[slot];
        release(slot);
        if (size == 0) {
            dropLanes();
        }
        return Optional.of(value);
    }

    /**
     * Remove all keys matching the supplied predicate, tested from the least recently used on.
     *
     * @throws ConcurrentModificationException if the predicate frees the next resident of the walk
     *     or the resident it then matches, itself or by {@code clear}, {@code trimTo} or an
     *     eviction; a resident it removes and does not match is skipped
     */
    @SuppressWarnings("unchecked")
    public synchronized RemoveResult removeIf(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int removedEntries = 0;
        long removedWeight = 0L;
        int current = lru;
        while (current != NONE) {
            int newer = newerLane[current];
            int stamp = stampLane[current];
            int newerStamp = newer == NONE ? 0 : stampLane[newer];
            boolean matches = predicate.test((K) keyLane[current]);
            requireWalkIntact(current, stamp, matches, newer, newerStamp);
            if (matches) {
                long slotWeight = weightLane[current];
                removedEntries++;
                removedWeight = Math.addExact(removedWeight, slotWeight);
                weight -= slotWeight;
                release(current);
            }
            current = newer;
        }
        if (size == 0) {
            dropLanes();
        }
        return new RemoveResult(removedEntries, removedWeight);
    }

    /**
     * Trim to caller-selected temporary limits.
     *
     * <p>Temporary limits must not exceed configured hard limits. This is intended for
     * memory-pressure policies that dynamically tighten the cache budget.</p>
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

    /** Remove all cache ownership; the lanes are dropped with every reference they held. */
    public synchronized RemoveResult clear() {
        int removedEntries = size;
        long removedWeight = weight;
        size = 0;
        weight = 0L;
        dropLanes();
        return new RemoveResult(removedEntries, removedWeight);
    }

    /** Membership check that does not alter recency. */
    public synchronized boolean containsKey(K key) {
        return slotOf(key) != NONE;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized long weight() {
        return weight;
    }

    public int maxEntries() {
        return maxEntries;
    }

    public long maxWeight() {
        return maxWeight;
    }

    /** LRU-to-MRU key order, primarily for deterministic diagnostics/tests. */
    @SuppressWarnings("unchecked")
    public synchronized List<K> keysInLruOrder() {
        ArrayList<K> keys = new ArrayList<>(size);
        for (int slot = lru; slot != NONE; slot = newerLane[slot]) {
            keys.add((K) keyLane[slot]);
        }
        return List.copyOf(keys);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(
                size,
                weight,
                maxEntries,
                maxWeight,
                hits,
                misses,
                puts,
                replacements,
                evictions,
                oversizeRejections);
    }

    private TrimResult trimToInternal(int entryLimit, long weightLimit) {
        int evictedEntries = 0;
        long evictedWeight = 0L;
        while (size > 0 && (size > entryLimit || weight > weightLimit)) {
            int slot = lru;
            long slotWeight = weightLane[slot];
            weight -= slotWeight;
            release(slot);
            evictedEntries++;
            evictedWeight = Math.addExact(evictedWeight, slotWeight);
            evictions++;
        }
        return new TrimResult(evictedEntries, evictedWeight);
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
     * @throws ConcurrentModificationException if a key's {@code equals} adds or removes a resident
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

    /** Stores a new resident in a free slot and indexes it; not yet in the recency list. */
    private int admit(K key, V value, long entryWeight) {
        // hashed first: a hashCode that throws finds no slot taken; one that adds or removes a
        // resident gets a CME (an equal key admitted meanwhile would be resident twice)
        int seen = structure;
        int hash = key.hashCode();
        if (structure != seen) {
            throw new ConcurrentModificationException("a key's hashCode changed the cache");
        }
        int slot = takeSlot();
        keyLane[slot] = key;
        valueLane[slot] = value;
        weightLane[slot] = entryWeight;
        hashLane[slot] = hash;
        indexSlot(slot, hash);
        if (++structure == 0) {
            structure = 1;
        }
        stampLane[slot] = structure;
        hashing = true;
        size++;
        return slot;
    }

    /** Unlinks, unindexes and frees a resident's slot. */
    private void release(int slot) {
        unlink(slot);
        unindexSlot(slot);
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
     * Doubles the lanes, at most to maxEntries + 1 slots (a put adds before it trims). Every array
     * is allocated before a field changes: an OutOfMemoryError leaves the cache as it was.
     */
    private void grow() {
        long limit = Math.min((long) maxEntries + 1, MAX_SLOTS);
        int capacity = (int) Math.min(Math.max(MIN_SLOTS, 2L * keyLane.length), limit);
        if (capacity <= keyLane.length) {
            throw new IllegalStateException("WeightedLRUCache lanes are full");
        }
        // the index is sized for at most maxEntries residents (load at most 1/2) and keeps one cell
        // empty when the lanes hold the extra slot a put takes before it trims
        long cells = Math.max(2 * Math.min(capacity, (long) maxEntries), capacity + 1L);
        int bits = 64 - Long.numberOfLeadingZeros(cells - 1);
        Object[] keys = Arrays.copyOf(keyLane, capacity);
        Object[] values = Arrays.copyOf(valueLane, capacity);
        long[] weights = Arrays.copyOf(weightLane, capacity);
        int[] hashes = Arrays.copyOf(hashLane, capacity);
        int[] newer = Arrays.copyOf(newerLane, capacity);
        int[] older = Arrays.copyOf(olderLane, capacity);
        int[] stamps = Arrays.copyOf(stampLane, capacity);
        int[] fresh = bits > indexBits ? new int[1 << bits] : index;
        keyLane = keys;
        valueLane = values;
        weightLane = weights;
        hashLane = hashes;
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
        for (int slot = lru; slot != NONE; slot = newerLane[slot]) {
            indexSlot(slot, hashLane[slot]);
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

    // The recency list is the module's doubly-linked int-slot atom: the least recently used slot
    // is its first, the most recently used its last, olderLane its previous and newerLane its next.

    private void moveToFront(int slot) {
        ends(PackedSlotLinkAtoms.moveLast(olderLane, newerLane, lru, mru, slot));
    }

    private void addToFront(int slot) {
        ends(PackedSlotLinkAtoms.linkLast(olderLane, newerLane, lru, mru, slot));
    }

    private void unlink(int slot) {
        ends(PackedSlotLinkAtoms.unlink(olderLane, newerLane, lru, mru, slot));
    }

    private void ends(long endpoints) {
        lru = PackedSlotLinkAtoms.first(endpoints);
        mru = PackedSlotLinkAtoms.last(endpoints);
    }

    /** Whether {@code slot} is a slot of the lanes still holding the resident of {@code stamp}. */
    private boolean holds(int slot, int stamp) {
        return slot < stampLane.length && stampLane[slot] == stamp;
    }

    /**
     * Throws when a callback freed the next resident of a walk, or the resident under test that it
     * then matched, even if a new resident took the slot. A resident removed but not matched is
     * skipped, as the node-linked version did.
     */
    private void requireWalkIntact(
            int current, int stamp, boolean matches, int newer, int newerStamp) {
        if ((matches && !holds(current, stamp)) || (newer != NONE && !holds(newer, newerStamp))) {
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
        newerLane = NO_INTS;
        olderLane = NO_INTS;
        stampLane = NO_INTS;
        structure++;
        index = NO_INTS;
        indexBits = 0;
        mru = NONE;
        lru = NONE;
        freeTop = NONE;
        used = 0;
    }

    private static long requireWeight(long value) {
        if (value <= 0L) {
            throw new IllegalArgumentException("entry weight must be > 0");
        }
        return value;
    }
}
