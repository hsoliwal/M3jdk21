/* *****************************************************************************
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
 * Byte-key primitive map that chooses sparse hashing or direct indexing by
 * primitive payload cost.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveAdaptiveByteMap.java}
 * blob {@code caa2fca1e3d344e8dd4da79bedc73179871b89b3} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveAdaptiveByteMap {
    private static final int DOMAIN_SIZE = 1 << Byte.SIZE;
    private static final float LOAD_FACTOR = DenseHash.DEFAULT_LOAD_FACTOR;

    @FunctionalInterface
    public interface EntryConsumer {
        void accept(byte key, long valueBits);
    }

    private final PrimitiveKind valueKind;
    private PrimitivePrimitiveHashMap sparse;
    private PrimitiveDirectByteMap direct;

    public PrimitiveAdaptiveByteMap(PrimitiveKind valueKind) {
        this(valueKind, 8);
    }

    public PrimitiveAdaptiveByteMap(PrimitiveKind valueKind, int expectedSize) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (expectedSize < 0 || expectedSize > DOMAIN_SIZE) {
            throw new IllegalArgumentException("expectedSize must be 0..256");
        }
        if (directPayloadBytes() <= sparsePayloadBytes(Math.max(1, expectedSize))) {
            direct = new PrimitiveDirectByteMap(valueKind);
        } else {
            sparse = new PrimitivePrimitiveHashMap(
                    PrimitiveKind.BYTE, valueKind, Math.max(1, expectedSize), LOAD_FACTOR);
        }
    }

    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return direct != null ? direct.size() : sparse.size(); }
    public boolean isEmpty() { return size() == 0; }
    public boolean isDirect() { return direct != null; }
    public long payloadBytes() { return direct != null ? direct.payloadBytes() : sparse.payloadBytes(); }

    public long directPayloadBytes() {
        return valueKind.payloadBytes(DOMAIN_SIZE) + 4L * Long.BYTES;
    }

    public boolean containsKey(byte key) {
        return direct != null ? direct.containsKey(key) : sparse.containsKeyBits(key);
    }

    public long getOrDefaultBits(byte key, long defaultBits) {
        return direct != null
                ? direct.getOrDefaultBits(key, defaultBits)
                : sparse.getOrDefaultBits(key, defaultBits);
    }

    public long putBits(byte key, long valueBits, long absentBits) {
        if (direct != null) return direct.putBits(key, valueBits, absentBits);
        boolean contained = sparse.containsKeyBits(key);
        if (!contained && shouldUseDirect(sparse.size() + 1)) {
            migrateToDirect();
            return direct.putBits(key, valueBits, absentBits);
        }
        return sparse.putBits(key, valueBits, absentBits);
    }

    public long removeOrDefaultBits(byte key, long defaultBits) {
        return direct != null
                ? direct.removeOrDefaultBits(key, defaultBits)
                : sparse.removeOrDefaultBits(key, defaultBits);
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

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (direct != null) {
            direct.forEach(consumer::accept);
        } else {
            sparse.forEach((keyBits, valueBits) ->
                    consumer.accept((byte) keyBits, valueBits));
        }
    }

    public int directSwitchSize() {
        int low = 0;
        int high = DOMAIN_SIZE;
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
        int capacity = DenseHash.capacityFor(expectedSize, LOAD_FACTOR);
        long bytesPerSlot = PrimitiveKind.BYTE.widthBytes()
                + (long) valueKind.widthBytes()
                + Byte.BYTES;
        return bytesPerSlot * capacity;
    }

    private void migrateToDirect() {
        PrimitiveDirectByteMap next = new PrimitiveDirectByteMap(valueKind);
        sparse.forEach((keyBits, valueBits) ->
                next.putBits((byte) keyBits, valueBits, 0L));
        direct = next;
        sparse = null;
    }

    private void migrateToSparse() {
        PrimitivePrimitiveHashMap next = new PrimitivePrimitiveHashMap(
                PrimitiveKind.BYTE, valueKind, Math.max(1, direct.size()), LOAD_FACTOR);
        direct.forEach((key, valueBits) -> next.putBits(key, valueBits, 0L));
        sparse = next;
        direct = null;
        sparse.compact();
    }
}
