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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p>Expected complexity is O(1) for get/put/remove and O(evicted) for trims. Predicate removal and
 * ordered snapshots are O(n).</p>
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

    private enum Segment {
        PROBATION,
        PROTECTED
    }

    private final int maxEntries;
    private final long maxWeight;
    private final int protectedEntryTarget;
    private final long protectedWeightTarget;
    private final Weigher<K, V> weigher;
    private final Map<K, Node<K, V>> nodes;

    private final Node<K, V> probationHead = sentinel();
    private final Node<K, V> probationTail = sentinel();
    private final Node<K, V> protectedHead = sentinel();
    private final Node<K, V> protectedTail = sentinel();

    private int probationEntries;
    private int protectedEntries;
    private long probationWeight;
    private long protectedWeight;

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
        this.nodes = new HashMap<>();

        linkSentinels(probationHead, probationTail);
        linkSentinels(protectedHead, protectedTail);
    }

    /** Lookup, updating segment-local recency and promoting a probation hit. */
    public synchronized Optional<V> get(K key) {
        Node<K, V> node = nodes.get(key);
        if (node == null) {
            misses++;
            return Optional.empty();
        }

        hits++;
        if (node.segment == Segment.PROBATION) {
            probationHits++;
            promote(node);
            rebalanceProtected();
        } else {
            protectedHits++;
            moveToFront(node, protectedHead);
        }
        return Optional.of(node.value);
    }

    /**
     * Admit or replace one entry.
     *
     * <p>New entries enter probation as MRU. Replacing an existing entry preserves its segment and
     * promotes its recency. An entry heavier than the absolute cache budget is rejected without
     * disturbing the resident working set.</p>
     */
    public synchronized PutResult put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        long candidateWeight = requireWeight(weigher.weightOf(key, value));
        if (candidateWeight > maxWeight) {
            oversizeRejections++;
            return new PutResult(false, false, 0L, TrimResult.none());
        }

        Node<K, V> node = nodes.get(key);
        boolean replaced = node != null;
        if (node == null) {
            node = new Node<>(key, value, candidateWeight, Segment.PROBATION);
            nodes.put(key, node);
            addToFront(node, probationHead);
            probationEntries++;
            probationWeight = Math.addExact(probationWeight, candidateWeight);
        } else {
            subtractWeight(node);
            node.value = value;
            node.weight = candidateWeight;
            addWeight(node);
            moveToFront(
                    node,
                    node.segment == Segment.PROBATION ? probationHead : protectedHead);
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

        int excessEntries = Math.max(0, nodes.size() + 1 - entryLimit);
        long freeWeight = Math.max(0L, weightLimit - weight());
        long excessWeight = checkedWeight > freeWeight ? checkedWeight - freeWeight : 0L;
        if (excessEntries == 0 && excessWeight == 0L) {
            return List.of();
        }

        ArrayList<AdmissionVictim<K>> victims = new ArrayList<>();
        Node<K, V> current = probationTail.previous;
        while ((excessEntries > 0 || excessWeight > 0L) && current != probationHead) {
            victims.add(new AdmissionVictim<>(current.key, current.weight, false));
            excessEntries = Math.max(0, excessEntries - 1);
            excessWeight = Math.max(0L, excessWeight - current.weight);
            current = current.previous;
        }

        current = protectedTail.previous;
        while ((excessEntries > 0 || excessWeight > 0L) && current != protectedHead) {
            victims.add(new AdmissionVictim<>(current.key, current.weight, true));
            excessEntries = Math.max(0, excessEntries - 1);
            excessWeight = Math.max(0L, excessWeight - current.weight);
            current = current.previous;
        }

        return List.copyOf(victims);
    }

    /** Explicitly remove one key without affecting hit/miss statistics. */
    public synchronized Optional<V> remove(K key) {
        Node<K, V> node = nodes.remove(key);
        if (node == null) return Optional.empty();
        detachAndAccount(node);
        return Optional.of(node.value);
    }

    /** Remove all matching keys in segment-LRU order. */
    public synchronized RemoveResult removeIf(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int removedEntries = 0;
        long removedWeight = 0L;

        RemoveAccumulator probation =
                removeIfFromList(probationTail.previous, probationHead, predicate);
        removedEntries = Math.addExact(removedEntries, probation.entries());
        removedWeight = Math.addExact(removedWeight, probation.weight());

        RemoveAccumulator protectedRemoved =
                removeIfFromList(protectedTail.previous, protectedHead, predicate);
        removedEntries = Math.addExact(removedEntries, protectedRemoved.entries());
        removedWeight = Math.addExact(removedWeight, protectedRemoved.weight());

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
        return trimToInternal(entryLimit, weightLimit);
    }

    /** Remove every cache-owned value. */
    public synchronized RemoveResult clear() {
        int entries = nodes.size();
        long weight = weight();
        nodes.clear();
        resetList(probationHead, probationTail);
        resetList(protectedHead, protectedTail);
        probationEntries = 0;
        protectedEntries = 0;
        probationWeight = 0L;
        protectedWeight = 0L;
        return new RemoveResult(entries, weight);
    }

    /** Membership check that does not alter recency or promotion state. */
    public synchronized boolean containsKey(K key) {
        return nodes.containsKey(key);
    }

    public synchronized int size() {
        return nodes.size();
    }

    public synchronized long weight() {
        return Math.addExact(probationWeight, protectedWeight);
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
    public synchronized List<K> keysInLruOrder() {
        ArrayList<K> keys = new ArrayList<>(nodes.size());
        appendKeys(probationTail.previous, probationHead, keys);
        appendKeys(protectedTail.previous, protectedHead, keys);
        return List.copyOf(keys);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(
                nodes.size(),
                probationEntries,
                protectedEntries,
                weight(),
                probationWeight,
                protectedWeight,
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
        while (protectedEntries > protectedEntryTarget
                || protectedWeight > protectedWeightTarget) {
            Node<K, V> candidate = protectedTail.previous;
            if (candidate == protectedHead) break;
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

        while (!nodes.isEmpty()
                && (nodes.size() > entryLimit || weight() > weightLimit)) {
            Node<K, V> victim = probationTail.previous;
            if (victim == probationHead) {
                victim = protectedTail.previous;
                if (victim == protectedHead) break;
            }
            long victimWeight = victim.weight;
            Segment segment = victim.segment;
            nodes.remove(victim.key);
            detachAndAccount(victim);
            evictedEntries++;
            evictedWeight = Math.addExact(evictedWeight, victimWeight);
            evictions++;
            if (segment == Segment.PROBATION) probationEvictions++;
            else protectedEvictions++;
        }

        return evictedEntries == 0
                ? TrimResult.none()
                : new TrimResult(evictedEntries, evictedWeight, 0);
    }

    private RemoveAccumulator removeIfFromList(
            Node<K, V> start,
            Node<K, V> sentinel,
            Predicate<? super K> predicate) {
        int count = 0;
        long removedWeight = 0L;
        Node<K, V> current = start;
        while (current != sentinel) {
            Node<K, V> previous = current.previous;
            if (predicate.test(current.key)) {
                nodes.remove(current.key);
                removedWeight = Math.addExact(removedWeight, current.weight);
                count++;
                detachAndAccount(current);
            }
            current = previous;
        }
        return new RemoveAccumulator(count, removedWeight);
    }

    private void promote(Node<K, V> node) {
        if (node.segment != Segment.PROBATION) {
            throw new IllegalStateException("only probation entries can be promoted");
        }
        unlink(node);
        probationEntries--;
        probationWeight -= node.weight;
        node.segment = Segment.PROTECTED;
        protectedEntries++;
        protectedWeight = Math.addExact(protectedWeight, node.weight);
        addToFront(node, protectedHead);
        promotions++;
    }

    private void demote(Node<K, V> node) {
        if (node.segment != Segment.PROTECTED) {
            throw new IllegalStateException("only protected entries can be demoted");
        }
        unlink(node);
        protectedEntries--;
        protectedWeight -= node.weight;
        node.segment = Segment.PROBATION;
        probationEntries++;
        probationWeight = Math.addExact(probationWeight, node.weight);
        addToFront(node, probationHead);
        demotions++;
    }

    private void detachAndAccount(Node<K, V> node) {
        unlink(node);
        if (node.segment == Segment.PROBATION) {
            probationEntries--;
            probationWeight -= node.weight;
        } else {
            protectedEntries--;
            protectedWeight -= node.weight;
        }
    }

    private void subtractWeight(Node<K, V> node) {
        if (node.segment == Segment.PROBATION) probationWeight -= node.weight;
        else protectedWeight -= node.weight;
    }

    private void addWeight(Node<K, V> node) {
        if (node.segment == Segment.PROBATION) {
            probationWeight = Math.addExact(probationWeight, node.weight);
        } else {
            protectedWeight = Math.addExact(protectedWeight, node.weight);
        }
    }

    private static <K, V> void moveToFront(
            Node<K, V> node,
            Node<K, V> head) {
        unlink(node);
        addToFront(node, head);
    }

    private static <K, V> void addToFront(
            Node<K, V> node,
            Node<K, V> head) {
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

    private static <K, V> void appendKeys(
            Node<K, V> start,
            Node<K, V> sentinel,
            List<K> target) {
        Node<K, V> current = start;
        while (current != sentinel) {
            target.add(current.key);
            current = current.previous;
        }
    }

    private static <K, V> Node<K, V> sentinel() {
        return new Node<>(null, null, 0L, null);
    }

    private static <K, V> void linkSentinels(
            Node<K, V> head,
            Node<K, V> tail) {
        head.next = tail;
        tail.previous = head;
    }

    private static <K, V> void resetList(
            Node<K, V> head,
            Node<K, V> tail) {
        head.previous = null;
        head.next = tail;
        tail.previous = head;
        tail.next = null;
    }

    private static long requireWeight(long weight) {
        if (weight <= 0L) {
            throw new IllegalArgumentException("entry weight must be > 0");
        }
        return weight;
    }

    private record RemoveAccumulator(int entries, long weight) {}

    private static final class Node<K, V> {
        private final K key;
        private V value;
        private long weight;
        private Segment segment;
        private Node<K, V> previous;
        private Node<K, V> next;

        private Node(
                K key,
                V value,
                long weight,
                Segment segment) {
            this.key = key;
            this.value = value;
            this.weight = weight;
            this.segment = segment;
        }
    }
}
