// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/** One ordered primitive-ID topology, shared by sorted set/map and their views. */
final class M3IndexOrderedStore<K> {
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;
    private static final int[] EMPTY = new int[0];
    final MIndexSpace<K> space;
    final Comparator<? super K> comparator;
    int[] keys = EMPTY;
    int[] values;
    int size;
    long structure;
    long mutation;

    M3IndexOrderedStore(MIndexSpace<K> space, Comparator<? super K> comparator, boolean paired) {
        this.space = Objects.requireNonNull(space, "space");
        this.comparator = comparator;
        values = paired ? EMPTY : null;
    }

    void checkMutation(long expected) {
        if (mutation != expected) throw new ConcurrentModificationException("reentrant index mutation");
    }

    void checkStructure(long expected) {
        if (structure != expected) throw new ConcurrentModificationException("stale index coordinate");
    }

    @SuppressWarnings("unchecked")
    int compare(K left, K right) {
        long expected = mutation;
        int result = comparator == null
                ? ((Comparable<? super K>) Objects.requireNonNull(left, "key")).compareTo(right)
                : comparator.compare(left, right);
        checkMutation(expected);
        return result;
    }

    void validate(K key) { compare(key, key); }

    K key(int ordinal) {
        Objects.checkIndex(ordinal, size);
        return project(space, keys[ordinal]);
    }

    <T> T project(MIndexSpace<T> domain, int id) {
        if (id == -1) return null;
        long expected = mutation;
        T result = domain.value(id);
        checkMutation(expected);
        return result;
    }

    /** Null is an explicit sentinel, never an ID admitted to a domain. */
    <T> int admit(MIndexSpace<T> domain, T value) {
        if (value == null) return -1;
        long expected = mutation;
        int id = domain.id(value);
        checkMutation(expected);
        if (id < 0) throw new IllegalArgumentException("non-null value requires a non-negative domain ID");
        domain.requireId(id);
        checkMutation(expected);
        T projected = domain.value(id);
        checkMutation(expected);
        if (projected != value) {
            throw new IllegalArgumentException("MIndexSpace would substitute the caller's reference");
        }
        return id;
    }

    /** First comparator rank >= key, or > key when afterEqual is true. */
    int bound(K key, boolean afterEqual) {
        if (comparator == null) {
            Objects.requireNonNull(key, "key");
            if (!(key instanceof Comparable<?>)) throw new ClassCastException("key is not Comparable");
        }
        int low = 0, high = size;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            int comparison = compare(key(middle), key);
            if (comparison < 0 || comparison == 0 && afterEqual) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    int find(K key) {
        int ordinal = bound(key, false);
        return ordinal < size && compare(key(ordinal), key) == 0 ? ordinal : -1;
    }

    void insert(int ordinal, int keyId, int valueId) {
        if (ordinal < 0 || ordinal > size) throw new IndexOutOfBoundsException(ordinal);
        if (size == MAX_CAPACITY) throw new IllegalStateException("M3Index ordered capacity exhausted");
        ensure(size + 1);
        System.arraycopy(keys, ordinal, keys, ordinal + 1, size - ordinal);
        if (values != null) System.arraycopy(values, ordinal, values, ordinal + 1, size - ordinal);
        keys[ordinal] = keyId;
        if (values != null) values[ordinal] = valueId;
        size++;
        structure++;
        mutation++;
    }

    void replaceValue(int ordinal, int valueId) {
        Objects.checkIndex(ordinal, size);
        values[ordinal] = valueId;
        mutation++;
    }

    void remove(int ordinal) { removeRange(ordinal, ordinal + 1); }

    void removeRange(int from, int to) {
        Objects.checkFromToIndex(from, to, size);
        if (from == to) return;
        int count = to - from;
        System.arraycopy(keys, to, keys, from, size - to);
        Arrays.fill(keys, size - count, size, 0);
        if (values != null) {
            System.arraycopy(values, to, values, from, size - to);
            Arrays.fill(values, size - count, size, 0);
        }
        size -= count;
        if (size == 0) { keys = EMPTY; if (values != null) values = EMPTY; }
        structure++;
        mutation++;
    }

    /** Allocate every replacement before publishing either lane. No user capacity hints. */
    private void ensure(int minimum) {
        if (minimum <= keys.length) return;
        int geometric = (int) Math.min(MAX_CAPACITY,
                Math.max((long) minimum, Math.max(8L, keys.length + (keys.length >>> 1) + 1L)));
        int lanes = values == null ? 1 : 2;
        int capacity = M3IndexHeapAdmission.growthCapacity(keys.length, minimum, geometric,
                MAX_CAPACITY, Integer.BYTES * lanes, 24L * lanes);
        int[] nextKeys = Arrays.copyOf(keys, capacity);
        int[] nextValues = values == null ? null : Arrays.copyOf(values, capacity);
        keys = nextKeys;
        values = nextValues;
    }
}
