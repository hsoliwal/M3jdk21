/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded process-local canonical object pool for one immutable value type.
 *
 * <p>No eviction is performed. Once capacity is reached an unseen value passes
 * through unchanged, preserving identity already promised for admitted keys.
 *
 * @param <T> immutable value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextImmutablePool.java}
 * blob {@code 4c1a89fe0ce8156e6dcf61421679d89135d81e99} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ContextImmutablePool<T> implements AutoCloseable {

    private final ImmutableCodec<T> codec;
    private final int maxEntries;
    private final IdentityPool identityDomain;
    private boolean closed;
    private final ConcurrentHashMap<ImmutableKey, Entry<T>> entries =
            new ConcurrentHashMap<>();
    private final AtomicInteger admitted = new AtomicInteger();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong collisions = new AtomicLong();

    public ContextImmutablePool(
            ImmutableCodec<T> codec,
            int maxEntries) {
        this.codec =
                Objects.requireNonNull(
                        codec,
                        "codec");
        if (maxEntries < 1) {
            throw new IllegalArgumentException(
                    "maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
        this.identityDomain = null;
    }

    /** Compatibility constructor for an explicitly owned heterogeneous equality domain. */
    public ContextImmutablePool(int maxEntries) {
        this.codec = null;
        this.maxEntries = maxEntries;
        this.identityDomain = new IdentityPool(maxEntries);
    }

    public static IdentityPool global() { return Pools.global().identityDomain; }
    public static boolean isKnownImmutable(Object value) { return IdentityPool.isKnownImmutable(value); }
    public <E> java.util.List<E> internList(Iterable<? extends E> source) { return domain().internList(source); }
    public <E> java.util.Set<E> internSet(Iterable<? extends E> source) { return domain().internSet(source); }
    public <K,V> java.util.Map<K,V> internMap(java.util.Map<? extends K,? extends V> source) {
        return domain().internMap(source);
    }
    private IdentityPool domain() {
        if (identityDomain == null) throw new IllegalStateException("A codec pool admits only its registered type");
        return identityDomain;
    }

    public ImmutableCodec<T> codec() {
        if (codec == null) throw new IllegalStateException("Identity domains have no portable canonical codec");
        return codec;
    }

    public int maxEntries() {
        return maxEntries;
    }

    public int size() {
        if (identityDomain != null) return identityDomain.size();
        return admitted.get();
    }

    public synchronized ImmutableKey key(T value) {
        if (closed) throw new IllegalStateException("pool closed");
        codec();
        byte[] bytes =
                codec.canonicalBytes(value);
        return ImmutableKey.of(
                codec,
                bytes);
    }

    /**
     * Returns the admitted process-local canonical object for this exact
     * canonical byte sequence.
     */
    public synchronized T intern(T value) {
        if (closed) throw new IllegalStateException("pool closed");
        if (identityDomain != null) return identityDomain.intern(value);
        if (value == null) {
            return null;
        }

        byte[] bytes =
                codec.canonicalBytes(value);
        ImmutableKey key =
                ImmutableKey.of(
                        codec,
                        bytes);

        Entry<T> existing = entries.get(key);
        if (existing != null) {
            return existingValue(
                    existing,
                    bytes,
                    value);
        }

        if (!reserve()) {
            existing = entries.get(key);
            if (existing != null) {
                return existingValue(
                        existing,
                        bytes,
                        value);
            }
            rejected.incrementAndGet();
            return value;
        }

        Entry<T> created =
                new Entry<>(
                        bytes.clone(),
                        value);
        Entry<T> prior =
                entries.putIfAbsent(
                        key,
                        created);
        if (prior == null) {
            misses.incrementAndGet();
            return value;
        }

        admitted.decrementAndGet();
        return existingValue(
                prior,
                bytes,
                value);
    }

    public Stats stats() {
        if (identityDomain != null) {
            var state = identityDomain.stats();
            return new Stats(state.entries(), maxEntries, state.hits(), state.entries(), 0, 0);
        }
        return new Stats(
                admitted.get(),
                maxEntries,
                hits.get(),
                misses.get(),
                rejected.get(),
                collisions.get());
    }

    /**
     * Explicitly clears this pool. Existing caller references remain valid;
     * subsequent calls may establish new process-local canonical identities.
     */
    public synchronized void clear() {
        if (closed) throw new IllegalStateException("pool closed");
        if (identityDomain != null) { identityDomain.clear(); return; }
        entries.clear();
        admitted.set(0);
    }

    @Override public synchronized void close() {
        if (closed) return;
        if (identityDomain != null) identityDomain.close();
        entries.clear();
        admitted.set(0);
        closed = true;
    }

    private T existingValue(
            Entry<T> existing,
            byte[] bytes,
            T callerValue) {
        if (!Arrays.equals(
                existing.canonicalBytes(),
                bytes)) {
            collisions.incrementAndGet();
            return callerValue;
        }
        hits.incrementAndGet();
        return existing.value();
    }

    private boolean reserve() {
        for (;;) {
            int current = admitted.get();
            if (current >= maxEntries) {
                return false;
            }
            if (admitted.compareAndSet(
                    current,
                    current + 1)) {
                return true;
            }
        }
    }

    public record Stats(
            int entries,
            int capacity,
            long hits,
            long misses,
            long rejected,
            long digestCollisions) {
    }

    private record Entry<T>(
            byte[] canonicalBytes,
            T value) {
        private Entry {
            canonicalBytes =
                    canonicalBytes.clone();
            Objects.requireNonNull(
                    value,
                    "value");
        }

        @Override
        public byte[] canonicalBytes() {
            return canonicalBytes.clone();
        }
    }
}
