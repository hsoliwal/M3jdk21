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
 * Sorted primitive list-multimap backed by two aligned primitive lanes.
 *
 * <p>Keys are sorted; values for the same key retain insertion order. Duplicate
 * key/value pairs can optionally be suppressed without allocating entry objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveSortedMultiMap.java}
 * blob {@code 230dcbbc58c609fad7b47a87f446678b48e12b20} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveSortedMultiMap {
    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private final PrimitiveSoaStore rows;
    private final boolean uniquePairs;

    public PrimitiveSortedMultiMap(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        this(keyKind, valueKind, 8, false);
    }

    public PrimitiveSortedMultiMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind, int initialCapacity, boolean uniquePairs) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        rows = new PrimitiveSoaStore(initialCapacity, 0, keyKind, valueKind);
        this.uniquePairs = uniquePairs;
    }

    public int size() { return rows.size(); }
    public boolean isEmpty() { return rows.isEmpty(); }
    public long payloadBytes() { return rows.payloadBytes(); }
    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }
    public boolean uniquePairs() { return uniquePairs; }

    /**
     * Returns a caller-owned live values view for one key.
     *
     * <p>The view retains only this multimap and one primitive key carrier. Missing
     * keys therefore return an empty live view whose add operation creates the
     * mapping. No collection object is retained per key. Values preserve this
     * multimap's existing insertion order for the key. The owner and view are not
     * thread-safe.</p>
     */
    public ValuesView valuesViewBits(long keyBits) {
        return new ValuesView(keyKind.storageBits(keyBits));
    }

    public final class ValuesView {
        private final long keyBits;

        private ValuesView(long keyBits) {
            this.keyBits = keyBits;
        }

        public int size() {
            return countKeyBits(keyBits);
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public boolean containsBits(long valueBits) {
            return containsEntryBits(keyBits, valueBits);
        }

        public boolean addBits(long valueBits) {
            return PrimitiveSortedMultiMap.this.addBits(keyBits, valueBits);
        }

        public boolean removeBits(long valueBits) {
            return PrimitiveSortedMultiMap.this.removeEntryBits(keyBits, valueBits);
        }

        public void clear() {
            PrimitiveSortedMultiMap.this.removeAllKeyBits(keyBits);
        }

        public long getBits(int index) {
            int from = lowerBound(keyBits);
            int length = upperBound(keyBits) - from;
            Objects.checkIndex(index, length);
            return rows.getBits(1, from + index);
        }

        public Object toArrayBits() {
            int from = lowerBound(keyBits);
            int length = upperBound(keyBits) - from;
            Object result = valueKind.newArray(length);
            for (int offset = 0; offset < length; offset++) {
                valueKind.writeBits(result, offset, rows.getBits(1, from + offset));
            }
            return result;
        }

        public void forEach(LongConsumer consumer) {
            PrimitiveSortedMultiMap.this.forEachValueBits(
                    keyBits, Objects.requireNonNull(consumer, "consumer"));
        }
    }

    public boolean containsKeyBits(long keyBits) {
        return lowerBound(keyBits) < upperBound(keyBits);
    }

    public boolean containsEntryBits(long keyBits, long valueBits) {
        int from = lowerBound(keyBits);
        int to = upperBound(keyBits);
        for (int row = from; row < to; row++) {
            if (valueKind.equalBits(rows.getBits(1, row), valueBits)) return true;
        }
        return false;
    }

    public int countKeyBits(long keyBits) {
        return upperBound(keyBits) - lowerBound(keyBits);
    }

    public boolean addBits(long keyBits, long valueBits) {
        int from = lowerBound(keyBits);
        int to = upperBound(keyBits);
        if (uniquePairs) {
            for (int row = from; row < to; row++) {
                if (valueKind.equalBits(rows.getBits(1, row), valueBits)) return false;
            }
        }
        rows.insert2(to, keyBits, valueBits, (byte) 0, 0L);
        return true;
    }

    public boolean removeEntryBits(long keyBits, long valueBits) {
        int from = lowerBound(keyBits);
        int to = upperBound(keyBits);
        for (int row = from; row < to; row++) {
            if (valueKind.equalBits(rows.getBits(1, row), valueBits)) {
                rows.removeAt(row);
                return true;
            }
        }
        return false;
    }

    public int removeAllKeyBits(long keyBits) {
        int from = lowerBound(keyBits);
        int to = upperBound(keyBits);
        int removed = to - from;
        rows.removeRange(from, to);
        return removed;
    }

    /**
     * Removes every value for one key and returns a caller-owned exact-width
     * primitive array containing the removed values in their prior insertion order.
     * Missing keys return an empty array of the declared value kind.
     */
    public Object removeAllValuesBits(long keyBits) {
        Object previous = valuesViewBits(keyBits).toArrayBits();
        removeAllKeyBits(keyBits);
        return previous;
    }

    /**
     * Replaces one key's values from an exact-width primitive array slice and
     * returns the prior values as a caller-owned exact-width primitive array.
     *
     * <p>The source slice is validated and maximum required capacity is prepared
     * before any mapping is removed. List mode preserves duplicates and input
     * order; unique-pair mode retains the first occurrence of each value according
     * to this multimap's existing equality rules.</p>
     */
    public Object replaceValuesBits(long keyBits, Object source, int from, int length) {
        valueKind.checkRange(source, from, length);
        Object previous = valuesViewBits(keyBits).toArrayBits();
        int current = java.lang.reflect.Array.getLength(previous);
        rows.ensureCapacity(rows.size() - current + length);
        removeAllKeyBits(keyBits);
        for (int index = 0; index < length; index++) {
            addBits(keyBits, valueKind.readBits(source, from + index));
        }
        return previous;
    }

    public void forEachValueBits(long keyBits, LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int from = lowerBound(keyBits);
        int to = upperBound(keyBits);
        for (int row = from; row < to; row++) consumer.accept(rows.getBits(1, row));
    }

    public long keyBitsAt(int row) { return rows.getBits(0, row); }
    public long valueBitsAt(int row) { return rows.getBits(1, row); }

    public PrimitiveSoaStore snapshotRows() {
        int size = rows.size();
        int[] indexes = new int[size];
        for (int i = 0; i < size; i++) indexes[i] = i;
        return rows.copyRows(indexes);
    }

    public void clear() { rows.clear(); }

    private int lowerBound(long keyBits) {
        int low = 0;
        int high = rows.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (keyKind.compareBits(rows.getBits(0, mid), keyBits) < 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    private int upperBound(long keyBits) {
        int low = 0;
        int high = rows.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (keyKind.compareBits(rows.getBits(0, mid), keyBits) <= 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }
}
