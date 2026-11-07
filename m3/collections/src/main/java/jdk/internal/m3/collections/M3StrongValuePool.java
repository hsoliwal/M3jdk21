// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Bounded strong canonical pool for deeply immutable values with stable equals/hashCode.
 *
 * <p>Equal accepted values receive one retained Java representative and one stable dense local id.
 * Hot-path equality can therefore use the packed {@link M3ImmutableId} or its local primitive id.
 * The pool never evicts, because eviction would make identities unstable.</p>
 */
//FLLDCIM: RELOCATE - donor blob f216222edf15 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public final class M3StrongValuePool<T> {
    private static final VarHandle OBJECT_ARRAY =
        MethodHandles.arrayElementVarHandle(Object[].class);

    private final int typeId;
    private final Object[] values;
    private final ConcurrentHashMap<T, Integer> ids;
    private final M3StrongPoolOverflowPolicy overflowPolicy;
    private final Object insertLock = new Object();
    private final LongAdder hits = new LongAdder();
    private final LongAdder inserts = new LongAdder();
    private final LongAdder overflows = new LongAdder();
    private volatile int size;

    public M3StrongValuePool(int typeId, int capacity) {
        this(typeId, capacity, M3StrongPoolOverflowPolicy.REJECT);
    }

    public M3StrongValuePool(
            int typeId,
            int capacity,
            M3StrongPoolOverflowPolicy overflowPolicy) {
        if (typeId <= 0 || typeId > M3ImmutableId.MAX_TYPE_ID) {
            throw new IllegalArgumentException("type id must be in 1..65535");
        }
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.typeId = typeId;
        this.values = new Object[capacity];
        this.ids = new ConcurrentHashMap<>(Math.min(capacity, 1 << 20));
        this.overflowPolicy = Objects.requireNonNull(overflowPolicy, "overflowPolicy");
    }

    public M3ImmutableId internId(T value) {
        Objects.requireNonNull(value, "value");
        Integer existing = ids.get(value);
        if (existing != null) {
            hits.increment();
            return M3ImmutableId.of(typeId, existing);
        }

        synchronized (insertLock) {
            existing = ids.get(value);
            if (existing != null) {
                hits.increment();
                return M3ImmutableId.of(typeId, existing);
            }
            int next = size;
            if (next >= values.length) {
                overflows.increment();
                if (overflowPolicy == M3StrongPoolOverflowPolicy.REJECT) {
                    throw new IllegalStateException(
                        "strong immutable pool capacity exceeded: " + values.length);
                }
                return M3ImmutableId.NONE;
            }

            OBJECT_ARRAY.setRelease(values, next, value);
            /*
             * Publish the valid reverse-id range before publishing the value->id mapping. A reader
             * that observes the ConcurrentHashMap entry can therefore immediately resolve it.
             */
            size = next + 1;
            ids.put(value, next);
            inserts.increment();
            return M3ImmutableId.of(typeId, next);
        }
    }

    /** Return the retained representative, or the input itself on RETURN_INPUT overflow. */
    public T canonicalize(T value) {
        M3ImmutableId id = internId(value);
        return id.present() ? resolve(id) : value;
    }

    public M3ImmutableId findId(T value) {
        Integer local = ids.get(Objects.requireNonNull(value, "value"));
        return local == null ? M3ImmutableId.NONE : M3ImmutableId.of(typeId, local);
    }

    @SuppressWarnings("unchecked")
    public T resolve(M3ImmutableId id) {
        Objects.requireNonNull(id, "id");
        if (!id.present() || id.typeId() != typeId) {
            throw new IllegalArgumentException("id belongs to another strong value pool");
        }
        long local = id.localId();
        int currentSize = size;
        if (local < 0L || local >= currentSize) throw new IndexOutOfBoundsException("local id " + local);
        return (T) OBJECT_ARRAY.getAcquire(values, (int) local);
    }

    public int typeId() { return typeId; }
    public int size() { return size; }
    public int capacity() { return values.length; }
    public boolean full() { return size >= values.length; }
    public M3StrongPoolOverflowPolicy overflowPolicy() { return overflowPolicy; }

    public M3StrongPoolStats stats() {
        return new M3StrongPoolStats(
            hits.sum(),
            inserts.sum(),
            overflows.sum(),
            size,
            values.length);
    }
}
