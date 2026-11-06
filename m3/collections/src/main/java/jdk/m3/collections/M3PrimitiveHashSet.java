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
package jdk.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Exact-width linear-probing primitive hash set.
 *
 * <p>The normalized zero key bit-pattern is the only empty-slot sentinel. A
 * real zero key is carried by one collection-level flag, so there is no
 * occupancy/tombstone lane and no per-entry object. Deletion uses backward
 * shifting.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/PrimitiveHashSet.java}
 * blob {@code 3c757420722cb2cd5aa9d65b25546014f6031b08} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveHashSet {
    private final M3PrimitiveKind kind;
    private final float loadFactor;
    private Object keys;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasZeroKey;

    public M3PrimitiveHashSet(M3PrimitiveKind kind) {
        this(kind, 16, M3DenseHash.DEFAULT_LOAD_FACTOR);
    }

    public M3PrimitiveHashSet(M3PrimitiveKind kind, int expected, float loadFactor) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.loadFactor = loadFactor;
        int capacity = M3DenseHash.capacityFor(expected, loadFactor);
        keys = kind.newArray(capacity);
        mask = capacity - 1;
        threshold = M3DenseHash.threshold(capacity, loadFactor);
    }

    public M3PrimitiveKind kind() { return kind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(keys); }
    public long payloadBytes() { return kind.payloadBytes(capacity()); }

    public boolean containsBits(long keyBits) {
        return isZeroKey(keyBits) ? hasZeroKey : findNonZero(keyBits) >= 0;
    }

    public boolean addBits(long keyBits) {
        if (isZeroKey(keyBits)) {
            if (hasZeroKey) return false;
            hasZeroKey = true;
            if (++size > threshold) rehash(capacity() << 1);
            return true;
        }
        int position = insertionPositionNonZero(keyBits);
        if (position < 0) return false;
        if (size + 1 > threshold) {
            rehash(capacity() << 1);
            position = insertionPositionNonZero(keyBits);
        }
        kind.writeBits(keys, position, keyBits);
        size++;
        return true;
    }

    public boolean removeBits(long keyBits) {
        if (isZeroKey(keyBits)) {
            if (!hasZeroKey) return false;
            hasZeroKey = false;
            size--;
            return true;
        }
        int position = findNonZero(keyBits);
        if (position < 0) return false;
        shiftKeys(position);
        size--;
        return true;
    }

    public void clear() {
        kind.clear(keys, 0, capacity());
        size = 0;
        hasZeroKey = false;
    }

    public void forEachBits(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        if (hasZeroKey) action.accept(0L);
        int remaining = size - (hasZeroKey ? 1 : 0);
        for (int i = 0; remaining > 0 && i < capacity(); i++) {
            long key = kind.readBits(keys, i);
            if (!isZeroStored(key)) {
                action.accept(key);
                remaining--;
            }
        }
    }

    public Object toPrimitiveArray() {
        Object result = kind.newArray(size);
        int out = 0;
        if (hasZeroKey) kind.writeBits(result, out++, 0L);
        for (int i = 0; i < capacity(); i++) {
            long key = kind.readBits(keys, i);
            if (!isZeroStored(key)) kind.writeBits(result, out++, key);
        }
        return result;
    }

    /** Returns true when every value in {@code other} is present in this set. */
    public boolean containsAll(M3PrimitiveHashSet other) {
        requireCompatible(other);
        if (other.size > size) return false;
        if (other.hasZeroKey && !hasZeroKey) return false;
        for (int i = 0; i < other.capacity(); i++) {
            long key = other.kind.readBits(other.keys, i);
            if (!other.isZeroStored(key) && !containsBits(key)) return false;
        }
        return true;
    }

    /** JDK-style mutating union. Returns true when this set changed. */
    public boolean addAll(M3PrimitiveHashSet other) {
        requireCompatible(other);
        if (other == this) return false;
        boolean changed = false;
        if (other.hasZeroKey) changed |= addBits(0L);
        for (int i = 0; i < other.capacity(); i++) {
            long key = other.kind.readBits(other.keys, i);
            if (!other.isZeroStored(key)) changed |= addBits(key);
        }
        return changed;
    }

    /** JDK-style mutating subtraction. Returns true when this set changed. */
    public boolean removeAll(M3PrimitiveHashSet other) {
        requireCompatible(other);
        if (other == this) {
            boolean changed = !isEmpty();
            clear();
            return changed;
        }
        boolean changed = false;
        if (other.hasZeroKey) changed |= removeBits(0L);
        for (int i = 0; i < other.capacity(); i++) {
            long key = other.kind.readBits(other.keys, i);
            if (!other.isZeroStored(key)) changed |= removeBits(key);
        }
        return changed;
    }

    /** JDK-style mutating intersection. Returns true when this set changed. */
    public boolean retainAll(M3PrimitiveHashSet other) {
        requireCompatible(other);
        if (other == this) return false;
        boolean changed = false;
        if (hasZeroKey && !other.hasZeroKey) {
            hasZeroKey = false;
            size--;
            changed = true;
        }
        int position = 0;
        while (position < capacity()) {
            long key = kind.readBits(keys, position);
            if (isZeroStored(key)) {
                position++;
            } else if (!other.containsBits(key)) {
                shiftKeys(position);
                size--;
                changed = true;
            } else {
                position++;
            }
        }
        return changed;
    }

    /** Returns a caller-owned copy with the same primitive kind and load factor. */
    public M3PrimitiveHashSet copy() {
        M3PrimitiveHashSet result =
                new M3PrimitiveHashSet(kind, Math.max(1, size), loadFactor);
        result.addAll(this);
        return result;
    }

    /** Returns a caller-owned union result. */
    public M3PrimitiveHashSet union(M3PrimitiveHashSet other) {
        requireCompatible(other);
        M3PrimitiveHashSet result = copy();
        result.addAll(other);
        return result;
    }

    /** Returns a caller-owned intersection result. */
    public M3PrimitiveHashSet intersection(M3PrimitiveHashSet other) {
        requireCompatible(other);
        M3PrimitiveHashSet smaller = size <= other.size ? this : other;
        M3PrimitiveHashSet larger = smaller == this ? other : this;
        M3PrimitiveHashSet result =
                new M3PrimitiveHashSet(kind, Math.max(1, smaller.size), loadFactor);
        if (smaller.hasZeroKey && larger.hasZeroKey) result.addBits(0L);
        for (int i = 0; i < smaller.capacity(); i++) {
            long key = smaller.kind.readBits(smaller.keys, i);
            if (!smaller.isZeroStored(key) && larger.containsBits(key)) {
                result.addBits(key);
            }
        }
        return result;
    }

    /** Returns a caller-owned difference result ({@code this - other}). */
    public M3PrimitiveHashSet difference(M3PrimitiveHashSet other) {
        requireCompatible(other);
        M3PrimitiveHashSet result = copy();
        result.removeAll(other);
        return result;
    }

    Object backingArray() { return keys; }
    boolean hasZeroKey() { return hasZeroKey; }

    private void requireCompatible(M3PrimitiveHashSet other) {
        Objects.requireNonNull(other, "other");
        if (kind != other.kind) {
            throw new IllegalArgumentException("primitive kind mismatch");
        }
    }

    private int findNonZero(long keyBits) {
        int position = kind.hashBits(keyBits) & mask;
        long current;
        while (!isZeroStored(current = kind.readBits(keys, position))) {
            if (kind.equalBits(current, keyBits)) return position;
            position = (position + 1) & mask;
        }
        return -1;
    }

    /** Returns insertion slot or -(existingSlot+1). */
    private int insertionPositionNonZero(long keyBits) {
        int position = kind.hashBits(keyBits) & mask;
        long current;
        while (!isZeroStored(current = kind.readBits(keys, position))) {
            if (kind.equalBits(current, keyBits)) return -position - 1;
            position = (position + 1) & mask;
        }
        return position;
    }

    private void shiftKeys(int position) {
        for (;;) {
            int last = position;
            position = (position + 1) & mask;
            long current;
            while (!isZeroStored(current = kind.readBits(keys, position))) {
                int slot = kind.hashBits(current) & mask;
                if (last <= position ? last >= slot || slot > position
                        : last >= slot && slot > position) {
                    break;
                }
                position = (position + 1) & mask;
            }
            if (isZeroStored(current)) {
                kind.clear(keys, last, last + 1);
                return;
            }
            kind.writeBits(keys, last, current);
        }
    }

    private void rehash(int newCapacity) {
        Object oldKeys = keys;
        int oldCapacity = capacity();
        keys = kind.newArray(newCapacity);
        mask = newCapacity - 1;
        threshold = M3DenseHash.threshold(newCapacity, loadFactor);
        for (int i = 0; i < oldCapacity; i++) {
            long key = kind.readBits(oldKeys, i);
            if (isZeroStored(key)) continue;
            int position = kind.hashBits(key) & mask;
            while (!isZeroStored(kind.readBits(keys, position))) {
                position = (position + 1) & mask;
            }
            kind.writeBits(keys, position, key);
        }
    }

    private boolean isZeroKey(long bits) {
        return kind.normalizeBits(bits) == 0L;
    }

    private boolean isZeroStored(long bits) {
        return bits == 0L;
    }
}
