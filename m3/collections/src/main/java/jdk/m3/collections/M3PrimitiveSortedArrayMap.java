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

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Read-dense sorted primitive map: binary search + parallel-array copy on mutation.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/PrimitiveSortedArrayMap.java}
 * blob {@code b9cfb4b013c01db233f6ea5e6a1fa53de06951dd} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveSortedArrayMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final M3PrimitiveKind keyKind;
    private final M3PrimitiveKind valueKind;
    private Object keys;
    private Object values;
    private int size;

    public M3PrimitiveSortedArrayMap(M3PrimitiveKind keyKind, M3PrimitiveKind valueKind) {
        this(keyKind, valueKind, 8);
    }

    public M3PrimitiveSortedArrayMap(M3PrimitiveKind keyKind, M3PrimitiveKind valueKind, int initialCapacity) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        keys = keyKind.newArray(capacity);
        values = valueKind.newArray(capacity);
    }

    public M3PrimitiveKind keyKind() { return keyKind; }
    public M3PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(keys); }
    public long payloadBytes() {
        return keyKind.payloadBytes(capacity()) + valueKind.payloadBytes(capacity());
    }

    public boolean containsKeyBits(long keyBits) { return search(keyBits) >= 0; }

    public long getOrDefaultBits(long keyBits, long defaultValueBits) {
        int index = search(keyBits);
        return index < 0 ? defaultValueBits : valueKind.readBits(values, index);
    }

    public long putBits(long keyBits, long valueBits, long absentValueBits) {
        int result = search(keyBits);
        if (result >= 0) {
            long old = valueKind.readBits(values, result);
            valueKind.writeBits(values, result, valueBits);
            return old;
        }
        int insertion = -result - 1;
        ensure(size + 1);
        System.arraycopy(keys, insertion, keys, insertion + 1, size - insertion);
        System.arraycopy(values, insertion, values, insertion + 1, size - insertion);
        keyKind.writeBits(keys, insertion, keyBits);
        valueKind.writeBits(values, insertion, valueBits);
        size++;
        return absentValueBits;
    }

    public long removeOrDefaultBits(long keyBits, long defaultValueBits) {
        int index = search(keyBits);
        if (index < 0) return defaultValueBits;
        long old = valueKind.readBits(values, index);
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(keys, index + 1, keys, index, moved);
            System.arraycopy(values, index + 1, values, index, moved);
        }
        size--;
        keyKind.clear(keys, size, size + 1);
        valueKind.clear(values, size, size + 1);
        return old;
    }

    public long firstKeyBits() {
        if (size == 0) throw new NoSuchElementException();
        return keyKind.readBits(keys, 0);
    }

    public long lastKeyBits() {
        if (size == 0) throw new NoSuchElementException();
        return keyKind.readBits(keys, size - 1);
    }

    public int binarySearchKeyBits(long keyBits) { return search(keyBits); }

    public void forEach(EntryConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) {
            action.accept(keyKind.readBits(keys, i), valueKind.readBits(values, i));
        }
    }

    public void clear() {
        keyKind.clear(keys, 0, size);
        valueKind.clear(values, 0, size);
        size = 0;
    }

    Object keyArray() { return keys; }
    Object valueArray() { return values; }

    /** Delegate to the already typed JDK primitive-array binary-search owner. */
    private int search(long keyBits) {
        return keyKind.binarySearch(keys, 0, size, keyBits);
    }

    /** Test-only copy injection; preserves M3PrimitiveKind exact-width storage. */
    @FunctionalInterface
    interface BackingCopier {
        Object copy(M3PrimitiveKind kind, Object source, int capacity);
    }

    private void ensure(int required) {
        ensure(required, M3PrimitiveKind::copyOf);
    }

    /** Stage both typed arrays before either becomes visible to the map. */
    void ensure(int required, BackingCopier copier) {
        if (required <= capacity()) return;
        Objects.requireNonNull(copier, "copier");
        int next = capacity() + (capacity() >>> 1) + 1;
        if (next < required) next = required;
        Object nextKeys = Objects.requireNonNull(copier.copy(keyKind, keys, next), "keys copy");
        Object nextValues = Objects.requireNonNull(copier.copy(valueKind, values, next), "values copy");
        if (nextKeys.getClass() != keys.getClass()
                || nextValues.getClass() != values.getClass()
                || java.lang.reflect.Array.getLength(nextKeys) != next
                || java.lang.reflect.Array.getLength(nextValues) != next) {
            throw new IllegalStateException("incomplete M3PrimitiveSortedArrayMap backing growth");
        }
        keys = nextKeys;
        values = nextValues;
    }
}
