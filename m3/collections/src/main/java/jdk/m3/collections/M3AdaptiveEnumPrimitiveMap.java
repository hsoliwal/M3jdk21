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

/**
 * Enum-key primitive map that uses primitive ordinal hashing while sparse and
 * direct ordinal indexing when the fixed domain becomes cheaper.
 *
 * <p>No enum key reference is retained per mapping in either mode.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/AdaptiveEnumPrimitiveMap.java}
 * blob {@code d5b5658471ec1d0d34891ebc2b02a41c9b950a8e} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3AdaptiveEnumPrimitiveMap<E extends Enum<E>> {
    private static final float LOAD_FACTOR = M3DenseHash.DEFAULT_LOAD_FACTOR;

    @FunctionalInterface
    public interface OrdinalConsumer {
        void accept(int ordinal, long valueBits);
    }

    private final Class<E> keyType;
    private final M3PrimitiveKind valueKind;
    private final int domainSize;
    private M3PrimitivePrimitiveHashMap sparse;
    private M3EnumPrimitiveMap<E> direct;

    public M3AdaptiveEnumPrimitiveMap(Class<E> keyType, M3PrimitiveKind valueKind) {
        this(keyType, valueKind, 0);
    }

    public M3AdaptiveEnumPrimitiveMap(Class<E> keyType, M3PrimitiveKind valueKind, int expectedSize) {
        this.keyType = Objects.requireNonNull(keyType, "keyType");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        E[] constants = keyType.getEnumConstants();
        if (constants == null) throw new IllegalArgumentException("keyType is not an enum");
        domainSize = constants.length;
        if (expectedSize < 0 || expectedSize > domainSize) {
            throw new IllegalArgumentException("expectedSize must be 0.." + domainSize);
        }
        if (directPayloadBytes() <= sparsePayloadBytes(Math.max(1, expectedSize))) {
            direct = new M3EnumPrimitiveMap<>(keyType, valueKind);
        } else {
            sparse = new M3PrimitivePrimitiveHashMap(
                    M3PrimitiveKind.INT, valueKind, Math.max(1, expectedSize), LOAD_FACTOR);
        }
    }

    public Class<E> keyType() { return keyType; }
    public M3PrimitiveKind valueKind() { return valueKind; }
    public int keyCapacity() { return domainSize; }
    public int size() { return direct != null ? direct.size() : sparse.size(); }
    public boolean isEmpty() { return size() == 0; }
    public boolean isDirect() { return direct != null; }
    public long payloadBytes() {
        return direct != null ? direct.primitivePayloadBytes() : sparse.payloadBytes();
    }

    public long directPayloadBytes() {
        int words = Math.max(1, (domainSize + 63) >>> 6);
        return valueKind.payloadBytes(domainSize) + (long) words * Long.BYTES;
    }

    public boolean containsKey(E key) {
        int ordinal = ordinal(key);
        return direct != null ? direct.containsKey(key) : sparse.containsKeyBits(ordinal);
    }

    public long getOrDefaultBits(E key, long defaultBits) {
        int ordinal = ordinal(key);
        return direct != null
                ? direct.getOrDefaultBits(key, defaultBits)
                : sparse.getOrDefaultBits(ordinal, defaultBits);
    }

    public long putBits(E key, long valueBits, long absentBits) {
        int ordinal = ordinal(key);
        if (direct != null) return direct.putBits(key, valueBits, absentBits);
        boolean contained = sparse.containsKeyBits(ordinal);
        if (!contained && shouldUseDirect(sparse.size() + 1)) {
            migrateToDirect();
            return direct.putBits(key, valueBits, absentBits);
        }
        return sparse.putBits(ordinal, valueBits, absentBits);
    }

    public long removeOrDefaultBits(E key, long defaultBits) {
        int ordinal = ordinal(key);
        return direct != null
                ? direct.removeOrDefaultBits(key, defaultBits)
                : sparse.removeOrDefaultBits(ordinal, defaultBits);
    }

    public void clear() {
        if (direct != null) direct.clear();
        else sparse.clear();
    }

    public void compact() {
        int currentSize = size();
        if (direct != null && sparsePayloadBytes(Math.max(1, currentSize)) < directPayloadBytes()) {
            migrateToSparse();
        } else if (sparse != null) {
            sparse.compact();
            if (shouldUseDirect(currentSize)) migrateToDirect();
        }
    }

    public void forEachOrdinal(OrdinalConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (direct != null) {
            direct.forEachOrdinal(consumer::accept);
        } else {
            sparse.forEach((keyBits, valueBits) ->
                    consumer.accept((int) keyBits, valueBits));
        }
    }

    public int directSwitchSize() {
        if (domainSize == 0) return 0;
        int low = 0;
        int high = domainSize;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (shouldUseDirect(mid)) high = mid;
            else low = mid + 1;
        }
        return low;
    }

    private boolean shouldUseDirect(int expectedSize) {
        return directPayloadBytes() <= sparsePayloadBytes(Math.max(1, expectedSize));
    }

    private long sparsePayloadBytes(int expectedSize) {
        int capacity = M3DenseHash.capacityFor(expectedSize, LOAD_FACTOR);
        long bytesPerSlot = Integer.BYTES + (long) valueKind.widthBytes() + Byte.BYTES;
        return bytesPerSlot * capacity;
    }

    private void migrateToDirect() {
        M3EnumPrimitiveMap<E> next = new M3EnumPrimitiveMap<>(keyType, valueKind);
        E[] constants = keyType.getEnumConstants();
        sparse.forEach((keyBits, valueBits) ->
                next.putBits(constants[(int) keyBits], valueBits, 0L));
        direct = next;
        sparse = null;
    }

    private void migrateToSparse() {
        M3PrimitivePrimitiveHashMap next = new M3PrimitivePrimitiveHashMap(
                M3PrimitiveKind.INT, valueKind, Math.max(1, direct.size()), LOAD_FACTOR);
        direct.forEachOrdinal((ordinal, valueBits) -> next.putBits(ordinal, valueBits, 0L));
        sparse = next;
        direct = null;
        sparse.compact();
    }

    private int ordinal(E key) {
        Objects.requireNonNull(key, "key");
        if (key.getDeclaringClass() != keyType) {
            throw new IllegalArgumentException("key belongs to " + key.getDeclaringClass().getName());
        }
        return key.ordinal();
    }
}
