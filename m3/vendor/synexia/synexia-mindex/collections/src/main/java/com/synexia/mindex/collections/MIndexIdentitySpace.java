// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;

/**
 * Local strong identity domain for mutable/effectful objects.
 *
 * <p>The domain retains one object reference per admitted identity. Its lookup
 * index is primitive; collections that use it still retain only integer IDs.
 */
public final class MIndexIdentitySpace<T> implements MIndexSpace<T> {
    private Object[] values;
    private int[] table;
    private int size;

    public MIndexIdentitySpace() {
        this(16);
    }

    public MIndexIdentitySpace(int expectedSize) {
        values = new Object[Math.max(8, expectedSize)];
        table = new int[IdSupport.tableCapacity(expectedSize)];
    }

    @Override
    public int id(T value) {
        int slot = findSlot(value);
        if (slot >= 0) {
            return table[slot] - 1;
        }
        if (size == IdSupport.MAX_ID) {
            throw new IllegalStateException("identity space exhausted");
        }
        ensureValueCapacity(size + 1);
        if ((size + 1L) * 3L >= table.length * 2L) {
            rehash(table.length << 1);
        }
        int id = size++;
        values[id] = value;
        int insert = vacant(value);
        table[insert] = id + 1;
        return id;
    }

    @Override
    public int findId(T value) {
        int slot = findSlot(value);
        return slot < 0 ? -1 : table[slot] - 1;
    }

    @Override
    public T value(int id) {
        requireId(id);
        @SuppressWarnings("unchecked")
        T value = (T) values[id];
        return value;
    }

    @Override
    public int size() {
        return size;
    }

    private int findSlot(T value) {
        int mask = table.length - 1;
        int slot = IdSupport.mix(System.identityHashCode(value)) & mask;
        while (table[slot] != 0) {
            int id = table[slot] - 1;
            if (values[id] == value) {
                return slot;
            }
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    private int vacant(T value) {
        int mask = table.length - 1;
        int slot = IdSupport.mix(System.identityHashCode(value)) & mask;
        while (table[slot] != 0) {
            slot = (slot + 1) & mask;
        }
        return slot;
    }

    private void ensureValueCapacity(int needed) {
        if (needed > values.length) {
            values = Arrays.copyOf(values, IdSupport.grown(values.length, needed));
        }
    }

    private void rehash(int requested) {
        int capacity = IdSupport.tableCapacity(Math.max(size + 1, requested >>> 1));
        int[] next = new int[capacity];
        int mask = capacity - 1;
        for (int id = 0; id < size; id++) {
            int slot = IdSupport.mix(System.identityHashCode(values[id])) & mask;
            while (next[slot] != 0) {
                slot = (slot + 1) & mask;
            }
            next[slot] = id + 1;
        }
        table = next;
    }
}
