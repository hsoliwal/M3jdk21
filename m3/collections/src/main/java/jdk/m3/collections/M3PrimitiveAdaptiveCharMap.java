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
 * Char-key primitive map that switches representation when density justifies it.
 *
 * <p>Sparse mode uses the existing exact-width CHAR hash map. When the next
 * sparse table capacity would consume at least as much primitive payload as a
 * full 65,536-slot direct table plus one presence bit per char, entries migrate
 * once to direct indexing. {@link #compact()} can deliberately move a shrunken
 * direct map back to sparse storage.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveAdaptiveCharMap.java}
 * blob {@code da906534a26f16e144ebc9a4a423985eedc98cd2} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveAdaptiveCharMap {
    private static final int DOMAIN_SIZE = 1 << Character.SIZE;
    private static final float LOAD_FACTOR = M3DenseHash.DEFAULT_LOAD_FACTOR;

    @FunctionalInterface
    public interface EntryConsumer {
        void accept(char key, long valueBits);
    }

    private final M3PrimitiveKind valueKind;
    private M3PrimitivePrimitiveHashMap sparse;
    private M3PrimitiveDirectCharMap direct;

    public M3PrimitiveAdaptiveCharMap(M3PrimitiveKind valueKind) {
        this(valueKind, 16);
    }

    public M3PrimitiveAdaptiveCharMap(M3PrimitiveKind valueKind, int expectedSize) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (expectedSize < 0 || expectedSize > DOMAIN_SIZE) {
            throw new IllegalArgumentException("expectedSize must be 0..65536");
        }
        if (directPayloadBytes() <= sparsePayloadBytes(Math.max(1, expectedSize))) {
            direct = new M3PrimitiveDirectCharMap(valueKind);
        } else {
            sparse = new M3PrimitivePrimitiveHashMap(
                    M3PrimitiveKind.CHAR, valueKind, Math.max(1, expectedSize), LOAD_FACTOR);
        }
    }

    public M3PrimitiveKind valueKind() { return valueKind; }
    public int size() { return direct != null ? direct.size() : sparse.size(); }
    public boolean isEmpty() { return size() == 0; }
    public boolean isDirect() { return direct != null; }
    public long payloadBytes() { return direct != null ? direct.payloadBytes() : sparse.payloadBytes(); }
    public long directPayloadBytes() {
        return valueKind.payloadBytes(DOMAIN_SIZE) + (DOMAIN_SIZE / 8L);
    }

    public boolean containsKey(char key) {
        return direct != null ? direct.containsKey(key) : sparse.containsKeyBits(key);
    }

    public long getOrDefaultBits(char key, long defaultBits) {
        return direct != null
                ? direct.getOrDefaultBits(key, defaultBits)
                : sparse.getOrDefaultBits(key, defaultBits);
    }

    public long putBits(char key, long valueBits, long absentBits) {
        if (direct != null) return direct.putBits(key, valueBits, absentBits);

        boolean contained = sparse.containsKeyBits(key);
        if (!contained && shouldUseDirect(sparse.size() + 1)) {
            migrateToDirect();
            return direct.putBits(key, valueBits, absentBits);
        }
        return sparse.putBits(key, valueBits, absentBits);
    }

    public long removeOrDefaultBits(char key, long defaultBits) {
        return direct != null
                ? direct.removeOrDefaultBits(key, defaultBits)
                : sparse.removeOrDefaultBits(key, defaultBits);
    }

    public void clear() {
        if (direct != null) direct.clear();
        else sparse.clear();
    }

    /**
     * Re-evaluates representation after removals. This is explicit to avoid
     * representation thrash on ordinary mutations.
     */
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
                    consumer.accept((char) keyBits, valueBits));
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
        int capacity = M3DenseHash.capacityFor(expectedSize, LOAD_FACTOR);
        long bytesPerSlot = M3PrimitiveKind.CHAR.widthBytes()
                + (long) valueKind.widthBytes()
                + Byte.BYTES;
        return bytesPerSlot * capacity;
    }

    private void migrateToDirect() {
        M3PrimitiveDirectCharMap next = new M3PrimitiveDirectCharMap(valueKind);
        sparse.forEach((keyBits, valueBits) ->
                next.putBits((char) keyBits, valueBits, 0L));
        direct = next;
        sparse = null;
    }

    private void migrateToSparse() {
        M3PrimitivePrimitiveHashMap next = new M3PrimitivePrimitiveHashMap(
                M3PrimitiveKind.CHAR, valueKind, Math.max(1, direct.size()), LOAD_FACTOR);
        direct.forEach((key, valueBits) -> next.putBits(key, valueBits, 0L));
        sparse = next;
        direct = null;
        sparse.compact();
    }
}
