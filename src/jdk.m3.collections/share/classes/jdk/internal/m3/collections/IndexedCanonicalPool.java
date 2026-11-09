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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 * Strong, bounded canonical pool with dense O(1) local ids and O(1) reverse lookup.
 *
 * <p>No entry is evicted automatically. That invariant is intentional: after admission, equal
 * values interned in the same generation return the exact same Java reference, so reference
 * equality ({@code ==}) is valid for those canonical values. {@link #clear()} is the explicit
 * generation boundary that permits old canonical instances to diverge from newly admitted ones.</p>
 *
 * <p>Expected lookup is O(1) when the supplied key has a well-distributed hash. Key equality is
 * never accepted as sufficient proof when the supplied exact-equivalence predicate disagrees;
 * such a collision fails closed.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/canonical/IndexedCanonicalPool.java}
 * blob {@code cf5a21b9f0e4d8a4c8939d59fd270d23b2d3ff2e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IndexedCanonicalPool<T, K> implements CanonicalPool<T> {
    private final long poolId = PoolIds.next();
    private final int maxEntries;
    private final long maxWeight;
    private final CanonicalPoolSupport<T, K> support;
    private final ConcurrentMap<K, Entry<T>> entries = new ConcurrentHashMap<>();
    private final ArrayList<T> byId = new ArrayList<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong admissions = new AtomicLong();
    private final AtomicLong collisions = new AtomicLong();
    private final AtomicLong capacityRejects = new AtomicLong();
    private volatile long generation = 1L;
    private long retainedWeight;

    public IndexedCanonicalPool(
            int maxEntries,
            long maxWeight,
            Function<? super T, ? extends K> keyExtractor,
            BiPredicate<? super T, ? super T> equivalence,
            ToLongFunction<? super T> weight) {
        if (maxEntries < 0 || maxWeight < 0) {
            throw new IllegalArgumentException("canonical pool bounds must be non-negative");
        }
        this.maxEntries = maxEntries;
        this.maxWeight = maxWeight;
        this.support = new CanonicalPoolSupport<>(keyExtractor, equivalence, weight);
    }

    @Override
    public T intern(T candidate) {
        return internEntry(candidate).value();
    }

    /** Admit or resolve a value and return its generation-scoped dense id. */
    public CanonicalId internId(T candidate) {
        Entry<T> entry = internEntry(candidate);
        return new CanonicalId(poolId, entry.generation(), entry.localId());
    }

    @Override
    public Optional<T> find(T candidate) {
        K key = support.key(candidate);
        Entry<T> entry = liveEntry(key);
        if (entry == null) {
            return Optional.empty();
        }
        requireEquivalent(entry.value(), candidate);
        return Optional.of(entry.value());
    }

    /** O(1)-expected lookup by an already constructed exact immutable key. */
    public Optional<T> findByKey(K key) {
        Entry<T> entry = liveEntry(Objects.requireNonNull(key, "key"));
        return entry == null ? Optional.empty() : Optional.of(entry.value());
    }

    /**
     * Lazy canonical construction. The supplier is invoked only when the exact key is absent.
     * The produced value is verified to reproduce the supplied key before admission.
     */
    public T getOrCreateExact(K key, Supplier<? extends T> factory) {
        return getOrCreateEntry(key, factory).value();
    }

    /** Lazy exact-key construction returning the dense generation-scoped id. */
    public CanonicalId getOrCreateIdExact(K key, Supplier<? extends T> factory) {
        Entry<T> entry = getOrCreateEntry(key, factory);
        return new CanonicalId(poolId, entry.generation(), entry.localId());
    }

    /** Resolve one exact id in O(1), rejecting foreign or stale generations. */
    public synchronized T resolve(CanonicalId id) {
        Objects.requireNonNull(id, "id");
        if (id.poolId() != poolId) {
            throw new IllegalArgumentException("canonical id belongs to a different pool");
        }
        if (id.generation() != generation) {
            throw new IllegalStateException("canonical id belongs to a stale pool generation");
        }
        return byId.get(id.localId());
    }

    /** Returns the existing scoped id without admitting a new value. */
    public Optional<CanonicalId> findId(T candidate) {
        K key = support.key(candidate);
        Entry<T> entry = liveEntry(key);
        if (entry == null) {
            return Optional.empty();
        }
        requireEquivalent(entry.value(), candidate);
        return Optional.of(new CanonicalId(poolId, entry.generation(), entry.localId()));
    }

    @Override
    public long poolId() {
        return poolId;
    }

    @Override
    public long generation() {
        return generation;
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public synchronized long retainedWeight() {
        return retainedWeight;
    }

    @Override
    public synchronized CanonicalPoolStats stats() {
        return new CanonicalPoolStats(
                poolId,
                generation,
                entries.size(),
                retainedWeight,
                hits.get(),
                admissions.get(),
                collisions.get(),
                capacityRejects.get(),
                0L);
    }

    @Override
    public synchronized void clear() {
        // Exhaustion must not clear values while leaving their identity generation unchanged.
        long nextGeneration = Math.addExact(generation, 1L);
        entries.clear();
        byId.clear();
        retainedWeight = 0L;
        generation = nextGeneration;
    }

    private Entry<T> getOrCreateEntry(K suppliedKey, Supplier<? extends T> factory) {
        K key = Objects.requireNonNull(suppliedKey, "key");
        Objects.requireNonNull(factory, "factory");
        Entry<T> current = liveEntry(key);
        if (current != null) {
            hits.incrementAndGet();
            return current;
        }
        synchronized (this) {
            current = liveEntry(key);
            if (current != null) {
                hits.incrementAndGet();
                return current;
            }
            T candidate = Objects.requireNonNull(factory.get(), "factory result");
            K producedKey = support.key(candidate);
            if (!key.equals(producedKey)) {
                throw new IllegalArgumentException("factory value does not reproduce supplied canonical key");
            }
            return admit(key, candidate);
        }
    }

    private Entry<T> liveEntry(K key) {
        Entry<T> entry = entries.get(key);
        return entry != null && entry.generation() == generation ? entry : null;
    }

    private Entry<T> internEntry(T candidate) {
        K key = support.key(candidate);
        Entry<T> current = entries.get(key);
        long observedGeneration = generation;
        if (current != null && current.generation() == observedGeneration) {
            requireEquivalent(current.value(), candidate);
            hits.incrementAndGet();
            return current;
        }
        synchronized (this) {
            current = entries.get(key);
            if (current != null && current.generation() == generation) {
                requireEquivalent(current.value(), candidate);
                hits.incrementAndGet();
                return current;
            }
            return admit(key, candidate);
        }
    }

    private Entry<T> admit(K key, T candidate) {
        // A factory/key callback can reenter this monitor and publish the same key.
        Entry<T> current = reusableEntry(key, candidate);
        if (current != null) {
            return current;
        }
        long valueWeight = support.weight(candidate);
        // Weight is also caller code. Never overwrite an admission it completed.
        current = reusableEntry(key, candidate);
        if (current != null) {
            return current;
        }
        if (entries.size() >= maxEntries || valueWeight > maxWeight - retainedWeight) {
            capacityRejects.incrementAndGet();
            throw new CanonicalPoolCapacityException();
        }
        int localId = byId.size();
        Entry<T> admitted = new Entry<>(candidate, localId, generation, valueWeight);
        byId.add(candidate);
        entries.put(key, admitted);
        retainedWeight += valueWeight;
        admissions.incrementAndGet();
        return admitted;
    }

    /** Reuse a live owner only after the existing exact-equivalence guard accepts it. */
    private Entry<T> reusableEntry(K key, T candidate) {
        Entry<T> current = liveEntry(key);
        if (current != null) {
            requireEquivalent(current.value(), candidate);
            hits.incrementAndGet();
        }
        return current;
    }

    private void requireEquivalent(T canonical, T candidate) {
        if (!support.equivalent(canonical, candidate)) {
            collisions.incrementAndGet();
            throw new CanonicalKeyCollisionException();
        }
    }

    private record Entry<T>(T value, int localId, long generation, long weight) {
        private Entry {
            Objects.requireNonNull(value, "value");
            if (localId < 0 || generation <= 0 || weight < 0) {
                throw new IllegalArgumentException("invalid canonical entry");
            }
        }
    }
}
