/*******************************************************************************
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p>The implementation uses a hash table plus an intrusive doubly-linked recency list so access
 * order does not depend on {@code LinkedHashMap} callbacks and trim operations can return precise
 * eviction evidence.</p>
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

    private final int maxEntries;
    private final long maxWeight;
    private final Weigher<K, V> weigher;
    private final Map<K, Node<K, V>> nodes;
    private final Node<K, V> head;
    private final Node<K, V> tail;

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
        this.nodes = new HashMap<>();
        this.head = new Node<>(null, null, 0L);
        this.tail = new Node<>(null, null, 0L);
        head.next = tail;
        tail.previous = head;
    }

    /** Lookup and promote the key to most-recently-used position. */
    public synchronized Optional<V> get(K key) {
        Node<K, V> node = nodes.get(key);
        if (node == null) {
            misses++;
            return Optional.empty();
        }
        hits++;
        moveToFront(node);
        return Optional.of(node.value);
    }

    /**
     * Admit or replace one entry.
     *
     * <p>An entry heavier than the configured absolute weight limit is rejected without evicting
     * existing entries. Accepted entries are promoted to MRU and the cache trims to both configured
     * limits.</p>
     */
    public synchronized PutResult put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        long candidateWeight = requireWeight(weigher.weightOf(key, value));
        if (candidateWeight > maxWeight) {
            oversizeRejections++;
            return new PutResult(false, false, 0L, TrimResult.none());
        }

        Node<K, V> existing = nodes.get(key);
        boolean replaced = existing != null;
        if (existing == null) {
            existing = new Node<>(key, value, candidateWeight);
            nodes.put(key, existing);
            addToFront(existing);
        } else {
            weight -= existing.weight;
            existing.value = value;
            existing.weight = candidateWeight;
            moveToFront(existing);
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
    public synchronized Optional<V> peek(K key) {
        Objects.requireNonNull(key, "key");
        Node<K, V> node = nodes.get(key);
        return node == null ? Optional.empty() : Optional.of(node.value);
    }

    /** Observe one resident's admitted weight without changing recency or statistics. */
    public synchronized OptionalLong peekWeight(K key) {
        Objects.requireNonNull(key, "key");
        Node<K, V> node = nodes.get(key);
        return node == null ? OptionalLong.empty() : OptionalLong.of(node.weight);
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
        Node<K, V> node = nodes.get(key);
        if (node == null) return MaintenanceReplaceResult.absent();

        long replacementWeight = requireWeight(weigher.weightOf(key, replacement));
        long previousWeight = node.weight;
        if (replacementWeight >= previousWeight) {
            return new MaintenanceReplaceResult(
                    true, false, previousWeight, replacementWeight, 0L);
        }

        node.value = replacement;
        node.weight = replacementWeight;
        long released = previousWeight - replacementWeight;
        weight -= released;
        return new MaintenanceReplaceResult(
                true, true, previousWeight, replacementWeight, released);
    }

    /** Explicitly remove one key. */
    public synchronized Optional<V> remove(K key) {
        Node<K, V> node = nodes.remove(key);
        if (node == null) return Optional.empty();
        unlink(node);
        weight -= node.weight;
        return Optional.of(node.value);
    }

    /** Remove all keys matching the supplied predicate. */
    public synchronized RemoveResult removeIf(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int removedEntries = 0;
        long removedWeight = 0L;
        Node<K, V> current = tail.previous;
        while (current != head) {
            Node<K, V> previous = current.previous;
            if (predicate.test(current.key)) {
                nodes.remove(current.key);
                unlink(current);
                removedEntries++;
                removedWeight = Math.addExact(removedWeight, current.weight);
                weight -= current.weight;
            }
            current = previous;
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
        return trimToInternal(entryLimit, weightLimit);
    }

    /** Remove all cache ownership. */
    public synchronized RemoveResult clear() {
        int removedEntries = nodes.size();
        long removedWeight = weight;
        nodes.clear();
        head.next = tail;
        tail.previous = head;
        weight = 0L;
        return new RemoveResult(removedEntries, removedWeight);
    }

    /** Membership check that does not alter recency. */
    public synchronized boolean containsKey(K key) {
        return nodes.containsKey(key);
    }

    public synchronized int size() {
        return nodes.size();
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
    public synchronized List<K> keysInLruOrder() {
        ArrayList<K> keys = new ArrayList<>(nodes.size());
        Node<K, V> current = tail.previous;
        while (current != head) {
            keys.add(current.key);
            current = current.previous;
        }
        return List.copyOf(keys);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(
                nodes.size(),
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
        while (!nodes.isEmpty()
                && (nodes.size() > entryLimit || weight > weightLimit)) {
            Node<K, V> lru = tail.previous;
            if (lru == head) break;
            nodes.remove(lru.key);
            unlink(lru);
            weight -= lru.weight;
            evictedEntries++;
            evictedWeight = Math.addExact(evictedWeight, lru.weight);
            evictions++;
        }
        return new TrimResult(evictedEntries, evictedWeight);
    }

    private void moveToFront(Node<K, V> node) {
        unlink(node);
        addToFront(node);
    }

    private void addToFront(Node<K, V> node) {
        node.previous = head;
        node.next = head.next;
        head.next.previous = node;
        head.next = node;
    }

    private static <K, V> void unlink(Node<K, V> node) {
        node.previous.next = node.next;
        node.next.previous = node.previous;
        node.previous = null;
        node.next = null;
    }

    private static long requireWeight(long value) {
        if (value <= 0L) {
            throw new IllegalArgumentException("entry weight must be > 0");
        }
        return value;
    }

    private static final class Node<K, V> {
        private final K key;
        private V value;
        private long weight;
        private Node<K, V> previous;
        private Node<K, V> next;

        private Node(K key, V value, long weight) {
            this.key = key;
            this.value = value;
            this.weight = weight;
        }
    }
}
