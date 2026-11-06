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
 * Full signed-short-domain direct map with exact-width primitive values.
 *
 * <p>{@link Short#toUnsignedInt(short)} maps every signed short to one of 65,536
 * slots. Presence costs one bit per possible key. No key array or hash metadata
 * is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/PrimitiveDirectShortMap.java}
 * blob {@code 86d5e373df4f361c2c4ed8474521c23c149df5ce} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveDirectShortMap {
    private static final int DOMAIN_SIZE = 1 << Short.SIZE;

    @FunctionalInterface
    public interface EntryConsumer {
        void accept(short key, long valueBits);
    }

    private final M3PrimitiveKind valueKind;
    private final Object values;
    private final long[] present = new long[DOMAIN_SIZE >>> 6];
    private int size;

    public M3PrimitiveDirectShortMap(M3PrimitiveKind valueKind) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        values = valueKind.newArray(DOMAIN_SIZE);
    }

    public M3PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int keyCapacity() { return DOMAIN_SIZE; }

    public long payloadBytes() {
        return valueKind.payloadBytes(DOMAIN_SIZE)
                + (long) present.length * Long.BYTES;
    }

    public boolean containsKey(short key) {
        int index = Short.toUnsignedInt(key);
        return (present[index >>> 6] & (1L << (index & 63))) != 0L;
    }

    public long getOrDefaultBits(short key, long defaultBits) {
        int index = Short.toUnsignedInt(key);
        return containsKey(key) ? valueKind.readBits(values, index) : defaultBits;
    }

    public long putBits(short key, long valueBits, long absentBits) {
        int index = Short.toUnsignedInt(key);
        long bit = 1L << (index & 63);
        int word = index >>> 6;
        boolean contained = (present[word] & bit) != 0L;
        long old = contained ? valueKind.readBits(values, index) : absentBits;
        valueKind.writeBits(values, index, valueBits);
        if (!contained) {
            present[word] |= bit;
            size++;
        }
        return old;
    }

    public long removeOrDefaultBits(short key, long defaultBits) {
        int index = Short.toUnsignedInt(key);
        long bit = 1L << (index & 63);
        int word = index >>> 6;
        if ((present[word] & bit) == 0L) return defaultBits;
        long old = valueKind.readBits(values, index);
        valueKind.clear(values, index, index + 1);
        present[word] &= ~bit;
        size--;
        return old;
    }

    public void clear() {
        if (size == 0) return;
        valueKind.clear(values, 0, DOMAIN_SIZE);
        Arrays.fill(present, 0L);
        size = 0;
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int wordIndex = 0; wordIndex < present.length; wordIndex++) {
            long word = present[wordIndex];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                int index = (wordIndex << 6) + bit;
                consumer.accept((short) index, valueKind.readBits(values, index));
                word &= word - 1L;
            }
        }
    }

    Object valueArray() { return values; }
    long[] presenceWords() { return present; }
}
