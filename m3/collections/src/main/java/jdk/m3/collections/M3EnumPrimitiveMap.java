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

import java.util.Arrays;
import java.util.Objects;

/**
 * Enum-key to exact-width primitive-value map using the enum ordinal directly.
 *
 * <p>No enum key is retained per mapping. One collection-level Class reference
 * defines the domain; values are a primitive array indexed by ordinal and
 * presence is one bit per enum constant.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/EnumPrimitiveMap.java}
 * blob {@code c2f8117635cd813e72de165af55c786f638df35c} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3EnumPrimitiveMap<E extends Enum<E>> {
    @FunctionalInterface
    public interface OrdinalConsumer {
        void accept(int ordinal, long valueBits);
    }

    private final Class<E> keyType;
    private final M3PrimitiveKind valueKind;
    private final int domainSize;
    private final Object values;
    private final long[] present;
    private int size;

    public M3EnumPrimitiveMap(Class<E> keyType, M3PrimitiveKind valueKind) {
        this.keyType = Objects.requireNonNull(keyType, "keyType");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        E[] constants = keyType.getEnumConstants();
        if (constants == null) throw new IllegalArgumentException("keyType is not an enum");
        domainSize = constants.length;
        values = valueKind.newArray(domainSize);
        present = new long[Math.max(1, (domainSize + 63) >>> 6)];
    }

    public Class<E> keyType() { return keyType; }
    public M3PrimitiveKind valueKind() { return valueKind; }
    public int keyCapacity() { return domainSize; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public long primitivePayloadBytes() {
        return valueKind.payloadBytes(domainSize) + (long) present.length * Long.BYTES;
    }

    public boolean containsKey(E key) {
        int ordinal = ordinal(key);
        return (present[ordinal >>> 6] & (1L << (ordinal & 63))) != 0L;
    }

    public long getOrDefaultBits(E key, long defaultBits) {
        int ordinal = ordinal(key);
        return containsOrdinal(ordinal) ? valueKind.readBits(values, ordinal) : defaultBits;
    }

    public long putBits(E key, long valueBits, long absentBits) {
        int ordinal = ordinal(key);
        long bit = 1L << (ordinal & 63);
        int word = ordinal >>> 6;
        boolean contained = (present[word] & bit) != 0L;
        long old = contained ? valueKind.readBits(values, ordinal) : absentBits;
        valueKind.writeBits(values, ordinal, valueBits);
        if (!contained) {
            present[word] |= bit;
            size++;
        }
        return old;
    }

    public long removeOrDefaultBits(E key, long defaultBits) {
        int ordinal = ordinal(key);
        long bit = 1L << (ordinal & 63);
        int word = ordinal >>> 6;
        if ((present[word] & bit) == 0L) return defaultBits;
        long old = valueKind.readBits(values, ordinal);
        valueKind.clear(values, ordinal, ordinal + 1);
        present[word] &= ~bit;
        size--;
        return old;
    }

    public void clear() {
        if (size == 0) return;
        valueKind.clear(values, 0, domainSize);
        Arrays.fill(present, 0L);
        size = 0;
    }

    /**
     * Iterates primitive ordinals, avoiding a retained enum-reference lane and
     * avoiding per-entry key materialization.
     */
    public void forEachOrdinal(OrdinalConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int wordIndex = 0; wordIndex < present.length; wordIndex++) {
            long word = present[wordIndex];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                int ordinal = (wordIndex << 6) + bit;
                if (ordinal < domainSize) {
                    consumer.accept(ordinal, valueKind.readBits(values, ordinal));
                }
                word &= word - 1L;
            }
        }
    }

    Object valueArray() { return values; }
    long[] presenceWords() { return present; }

    private boolean containsOrdinal(int ordinal) {
        return (present[ordinal >>> 6] & (1L << (ordinal & 63))) != 0L;
    }

    private int ordinal(E key) {
        Objects.requireNonNull(key, "key");
        if (key.getDeclaringClass() != keyType) {
            throw new IllegalArgumentException("key belongs to " + key.getDeclaringClass().getName());
        }
        return key.ordinal();
    }
}
