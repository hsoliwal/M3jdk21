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
 * Hot-path equality can therefore use the packed {@link ImmutableId} or its local primitive id.
 * The pool never evicts, because eviction would make identities unstable.</p>
 */
//FLLDCIM: RELOCATE - donor blob f216222edf15 at commit 3385d79a2968 (repository, path, URL and digests: make/m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe make/m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public final class StrongValuePool<T> {
    private static final VarHandle OBJECT_ARRAY =
        MethodHandles.arrayElementVarHandle(Object[].class);

    private final int typeId;
    private final Object[] values;
    private final ConcurrentHashMap<T, Integer> ids;
    private final StrongPoolOverflowPolicy overflowPolicy;
    private final Object insertLock = new Object();
    private final LongAdder hits = new LongAdder();
    private final LongAdder inserts = new LongAdder();
    private final LongAdder overflows = new LongAdder();
    private volatile int size;

    public StrongValuePool(int typeId, int capacity) {
        this(typeId, capacity, StrongPoolOverflowPolicy.REJECT);
    }

    public StrongValuePool(
            int typeId,
            int capacity,
            StrongPoolOverflowPolicy overflowPolicy) {
        if (typeId <= 0 || typeId > ImmutableId.MAX_TYPE_ID) {
            throw new IllegalArgumentException("type id must be in 1..65535");
        }
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.typeId = typeId;
        this.values = new Object[capacity];
        this.ids = new ConcurrentHashMap<>(Math.min(capacity, 1 << 20));
        this.overflowPolicy = Objects.requireNonNull(overflowPolicy, "overflowPolicy");
    }

    public ImmutableId internId(T value) {
        Objects.requireNonNull(value, "value");
        Integer existing = ids.get(value);
        if (existing != null) {
            hits.increment();
            return ImmutableId.of(typeId, existing);
        }

        synchronized (insertLock) {
            existing = ids.get(value);
            if (existing != null) {
                hits.increment();
                return ImmutableId.of(typeId, existing);
            }
            int next = size;
            if (next >= values.length) {
                overflows.increment();
                if (overflowPolicy == StrongPoolOverflowPolicy.REJECT) {
                    throw new IllegalStateException(
                        "strong immutable pool capacity exceeded: " + values.length);
                }
                return ImmutableId.NONE;
            }

            OBJECT_ARRAY.setRelease(values, next, value);
            /*
             * Publish the valid reverse-id range before publishing the value->id mapping. A reader
             * that observes the ConcurrentHashMap entry can therefore immediately resolve it.
             */
            size = next + 1;
            ids.put(value, next);
            inserts.increment();
            return ImmutableId.of(typeId, next);
        }
    }

    /** Return the retained representative, or the input itself on RETURN_INPUT overflow. */
    public T canonicalize(T value) {
        ImmutableId id = internId(value);
        return id.present() ? resolve(id) : value;
    }

    public ImmutableId findId(T value) {
        Integer local = ids.get(Objects.requireNonNull(value, "value"));
        return local == null ? ImmutableId.NONE : ImmutableId.of(typeId, local);
    }

    @SuppressWarnings("unchecked")
    public T resolve(ImmutableId id) {
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
    public StrongPoolOverflowPolicy overflowPolicy() { return overflowPolicy; }

    public StrongPoolStats stats() {
        return new StrongPoolStats(
            hits.sum(),
            inserts.sum(),
            overflows.sum(),
            size,
            values.length);
    }
}
