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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 * Bounded weak canonicalization for immutable values whose global retention would be undesirable.
 *
 * <p>Reference equality is guaranteed only while the canonical instance remains strongly reachable.
 * Once it is collected, an equal future value may become a new canonical instance. Keys are held
 * strongly until their weak value is collected and the reference queue is drained, so callers should
 * use compact immutable keys that do not strongly retain the candidate itself.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/canonical/WeakCanonicalPool.java}
 * blob {@code e977419986a717802046601caf82d38f194cf63c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class WeakCanonicalPool<T, K> implements CanonicalPool<T> {
    private final long poolId = PoolIds.next();
    private final int maxEntries;
    private final long maxWeight;
    private final CanonicalPoolSupport<T, K> support;
    private final Map<K, WeakEntry<K, T>> entries = new HashMap<>();
    private final ReferenceQueue<T> queue = new ReferenceQueue<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong admissions = new AtomicLong();
    private final AtomicLong collisions = new AtomicLong();
    private final AtomicLong capacityRejects = new AtomicLong();
    private final AtomicLong collected = new AtomicLong();
    private long generation = 1L;
    private long retainedWeight;

    public WeakCanonicalPool(
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
    public synchronized T intern(T candidate) {
        drainCollected();
        K key = support.key(candidate);
        WeakEntry<K, T> current = entries.get(key);
        T existing = liveValue(key, current);
        if (existing != null) {
            requireEquivalent(existing, candidate);
            hits.incrementAndGet();
            return existing;
        }
        return admit(key, candidate);
    }

    @Override
    public synchronized Optional<T> find(T candidate) {
        drainCollected();
        K key = support.key(candidate);
        T existing = liveValue(key, entries.get(key));
        if (existing == null) {
            return Optional.empty();
        }
        requireEquivalent(existing, candidate);
        return Optional.of(existing);
    }

    /** O(1)-expected lookup by an already constructed compact immutable key. */
    public synchronized Optional<T> findByKey(K key) {
        drainCollected();
        K checked = Objects.requireNonNull(key, "key");
        return Optional.ofNullable(liveValue(checked, entries.get(checked)));
    }

    /**
     * Lazy weak canonical construction. The factory is invoked only when no live value exists.
     * The produced value must reproduce the supplied exact key.
     */
    public synchronized T getOrCreateExact(K suppliedKey, Supplier<? extends T> factory) {
        drainCollected();
        K key = Objects.requireNonNull(suppliedKey, "key");
        Objects.requireNonNull(factory, "factory");
        T existing = liveValue(key, entries.get(key));
        if (existing != null) {
            hits.incrementAndGet();
            return existing;
        }
        T candidate = Objects.requireNonNull(factory.get(), "factory result");
        K producedKey = support.key(candidate);
        if (!key.equals(producedKey)) {
            throw new IllegalArgumentException("factory value does not reproduce supplied canonical key");
        }
        return admit(key, candidate);
    }

    @Override
    public long poolId() {
        return poolId;
    }

    @Override
    public synchronized long generation() {
        return generation;
    }

    @Override
    public synchronized int size() {
        drainCollected();
        return entries.size();
    }

    @Override
    public synchronized long retainedWeight() {
        drainCollected();
        return retainedWeight;
    }

    @Override
    public synchronized CanonicalPoolStats stats() {
        drainCollected();
        return new CanonicalPoolStats(
                poolId,
                generation,
                entries.size(),
                retainedWeight,
                hits.get(),
                admissions.get(),
                collisions.get(),
                capacityRejects.get(),
                collected.get());
    }

    @Override
    public synchronized void clear() {
        // Exhaustion must not clear values while leaving their identity generation unchanged.
        long nextGeneration = Math.addExact(generation, 1L);
        entries.clear();
        retainedWeight = 0L;
        generation = nextGeneration;
        while (queue.poll() != null) {
            // Drop stale queue notifications from the previous generation.
        }
    }

    private T admit(K key, T candidate) {
        // Hold the live canonical value strongly across reentrant admission checks.
        T existing = reusableValue(key, candidate);
        if (existing != null) {
            return existing;
        }
        long valueWeight = support.weight(candidate);
        // Caller-supplied weight calculation may have admitted this key recursively.
        existing = reusableValue(key, candidate);
        if (existing != null) {
            return existing;
        }
        if (entries.size() >= maxEntries || valueWeight > maxWeight - retainedWeight) {
            capacityRejects.incrementAndGet();
            throw new CanonicalPoolCapacityException();
        }
        WeakEntry<K, T> admitted = new WeakEntry<>(key, candidate, valueWeight, queue);
        entries.put(key, admitted);
        retainedWeight += valueWeight;
        admissions.incrementAndGet();
        return candidate;
    }

    private T liveValue(K key, WeakEntry<K, T> entry) {
        if (entry == null) {
            return null;
        }
        T value = entry.get();
        if (value != null) {
            return value;
        }
        if (entries.remove(key, entry)) {
            retainedWeight -= entry.weight;
            collected.incrementAndGet();
        }
        return null;
    }

    /** Reuse, rather than replace, the canonical instance published by a nested call. */
    private T reusableValue(K key, T candidate) {
        T existing = liveValue(key, entries.get(key));
        if (existing != null) {
            requireEquivalent(existing, candidate);
            hits.incrementAndGet();
        }
        return existing;
    }

    private void requireEquivalent(T canonical, T candidate) {
        if (!support.equivalent(canonical, candidate)) {
            collisions.incrementAndGet();
            throw new CanonicalKeyCollisionException();
        }
    }

    @SuppressWarnings("unchecked")
    private void drainCollected() {
        WeakEntry<K, T> entry;
        while ((entry = (WeakEntry<K, T>) queue.poll()) != null) {
            if (entries.remove(entry.key, entry)) {
                retainedWeight -= entry.weight;
                collected.incrementAndGet();
            }
        }
    }

    private static final class WeakEntry<K, T> extends WeakReference<T> {
        private final K key;
        private final long weight;

        private WeakEntry(K key, T value, long weight, ReferenceQueue<T> queue) {
            super(Objects.requireNonNull(value, "value"), queue);
            this.key = Objects.requireNonNull(key, "key");
            this.weight = weight;
        }
    }
}
