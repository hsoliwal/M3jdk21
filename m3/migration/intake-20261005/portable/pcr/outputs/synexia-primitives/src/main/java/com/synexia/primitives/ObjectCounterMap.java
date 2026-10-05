package com.synexia.primitives;

import java.util.Objects;

/**
 * Object-key long counter map with Agrona-style missing-value semantics.
 *
 * <p>Object keys occupy one flat reference lane; counts occupy one long[] lane.
 * There is no entry object per key.
 */
public final class ObjectCounterMap<K> {
    @FunctionalInterface
    public interface CounterConsumer<K> {
        void accept(K key, long count);
    }

    private final long missingValue;
    private final ObjectPrimitiveHashMap<K> counts;

    public ObjectCounterMap() {
        this(0L);
    }

    public ObjectCounterMap(long missingValue) {
        this.missingValue = missingValue;
        counts = new ObjectPrimitiveHashMap<>(PrimitiveKind.LONG);
    }

    public long missingValue() { return missingValue; }
    public int size() { return counts.size(); }
    public boolean isEmpty() { return counts.isEmpty(); }
    public long primitivePayloadBytes() { return counts.primitivePayloadBytes(); }
    public int referenceSlots() { return counts.referenceSlots(); }

    public boolean containsKey(Object key) { return counts.containsKey(key); }

    public long get(Object key) {
        return counts.getOrDefaultBits(key, missingValue);
    }

    public long putBits(K key, long value) {
        if (value == missingValue) {
            throw new IllegalArgumentException("cannot store missingValue");
        }
        return counts.putBits(key, value, missingValue);
    }

    public long getAndAdd(K key, long delta) {
        long previous = get(key);
        if (delta == 0L) return previous;
        long next = Math.addExact(previous, delta);
        if (next == missingValue) {
            counts.removeOrDefaultBits(key, missingValue);
        } else {
            counts.putBits(key, next, missingValue);
        }
        return previous;
    }

    public long addAndGet(K key, long delta) {
        long previous = getAndAdd(key, delta);
        return Math.addExact(previous, delta);
    }

    public long increment(K key) { return addAndGet(key, 1L); }
    public long decrement(K key) { return addAndGet(key, -1L); }
    public long getAndIncrement(K key) { return getAndAdd(key, 1L); }
    public long getAndDecrement(K key) { return getAndAdd(key, -1L); }

    public long remove(Object key) {
        return counts.removeOrDefaultBits(key, missingValue);
    }

    public long minValue() {
        if (counts.isEmpty()) return missingValue;
        long[] values = (long[]) counts.valueArray();
        Object[] keys = counts.keyArray();
        long best = counts.hasNullKey() ? counts.nullValueBits() : Long.MAX_VALUE;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && values[i] < best) best = values[i];
        }
        return best;
    }

    public long maxValue() {
        if (counts.isEmpty()) return missingValue;
        long[] values = (long[]) counts.valueArray();
        Object[] keys = counts.keyArray();
        long best = counts.hasNullKey() ? counts.nullValueBits() : Long.MIN_VALUE;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && values[i] > best) best = values[i];
        }
        return best;
    }

    public void clear() { counts.clear(); }

    public void forEach(CounterConsumer<? super K> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        counts.forEach(consumer::accept);
    }
}
