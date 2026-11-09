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
 * Signed-short-key primitive map that switches representation by payload cost.
 *
 * <p>Sparse mode reuses the exact-width SHORT hash map. When the next sparse
 * table capacity would consume at least the primitive payload of a full 65,536
 * slot direct table plus presence bits, the map migrates once to direct
 * indexing. Shrink demotion is explicit through {@link #compact()}.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveAdaptiveShortMap.java}
 * blob {@code 7e71281084f4c3876ca3b816ffca3f972c04c6d0} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveAdaptiveShortMap {
    private static final int DOMAIN_SIZE = 1 << Short.SIZE;
    private static final float LOAD_FACTOR = DenseHash.DEFAULT_LOAD_FACTOR;

    @FunctionalInterface
    public interface EntryConsumer {
        void accept(short key, long valueBits);
    }

    private final PrimitiveKind valueKind;
    private PrimitivePrimitiveHashMap sparse;
    private PrimitiveDirectShortMap direct;

    public PrimitiveAdaptiveShortMap(PrimitiveKind valueKind) {
        this(valueKind, 16);
    }

    public PrimitiveAdaptiveShortMap(PrimitiveKind valueKind, int expectedSize) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (expectedSize < 0 || expectedSize > DOMAIN_SIZE) {
            throw new IllegalArgumentException("expectedSize must be 0..65536");
        }
        if (directPayloadBytes() <= sparsePayloadBytes(Math.max(1, expectedSize))) {
            direct = new PrimitiveDirectShortMap(valueKind);
        } else {
            sparse = new PrimitivePrimitiveHashMap(
                    PrimitiveKind.SHORT, valueKind, Math.max(1, expectedSize), LOAD_FACTOR);
        }
    }

    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return direct != null ? direct.size() : sparse.size(); }
    public boolean isEmpty() { return size() == 0; }
    public boolean isDirect() { return direct != null; }
    public long payloadBytes() { return direct != null ? direct.payloadBytes() : sparse.payloadBytes(); }

    public long directPayloadBytes() {
        return valueKind.payloadBytes(DOMAIN_SIZE) + (DOMAIN_SIZE / 8L);
    }

    public boolean containsKey(short key) {
        return direct != null ? direct.containsKey(key) : sparse.containsKeyBits(key);
    }

    public long getOrDefaultBits(short key, long defaultBits) {
        return direct != null
                ? direct.getOrDefaultBits(key, defaultBits)
                : sparse.getOrDefaultBits(key, defaultBits);
    }

    public long putBits(short key, long valueBits, long absentBits) {
        if (direct != null) return direct.putBits(key, valueBits, absentBits);
        boolean contained = sparse.containsKeyBits(key);
        if (!contained && shouldUseDirect(sparse.size() + 1)) {
            migrateToDirect();
            return direct.putBits(key, valueBits, absentBits);
        }
        return sparse.putBits(key, valueBits, absentBits);
    }

    public long removeOrDefaultBits(short key, long defaultBits) {
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
                    consumer.accept((short) keyBits, valueBits));
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
        long bytesPerSlot = PrimitiveKind.SHORT.widthBytes()
                + (long) valueKind.widthBytes()
                + Byte.BYTES;
        return bytesPerSlot * capacity;
    }

    private void migrateToDirect() {
        PrimitiveDirectShortMap next = new PrimitiveDirectShortMap(valueKind);
        sparse.forEach((keyBits, valueBits) ->
                next.putBits((short) keyBits, valueBits, 0L));
        direct = next;
        sparse = null;
    }

    private void migrateToSparse() {
        PrimitivePrimitiveHashMap next = new PrimitivePrimitiveHashMap(
                PrimitiveKind.SHORT, valueKind, Math.max(1, direct.size()), LOAD_FACTOR);
        direct.forEach((key, valueBits) -> next.putBits(key, valueBits, 0L));
        sparse = next;
        direct = null;
        sparse.compact();
    }
}
