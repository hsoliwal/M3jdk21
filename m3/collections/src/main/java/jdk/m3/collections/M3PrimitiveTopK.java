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
 * Fixed-size primitive top/bottom-K filter backed by one heap array.
 * Non-competitive values are rejected before they enter retained state.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveTopK.java}
 * blob {@code 34eb50a1ab420b5f87b7a862b55dcf50a16f224a} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveTopK {
    public enum Mode { KEEP_SMALLEST, KEEP_LARGEST }

    private final PrimitiveKind kind;
    private final Mode mode;
    private final int limit;
    private final Object heap;
    private int size;

    public M3PrimitiveTopK(PrimitiveKind kind, int limit, Mode mode) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.mode = Objects.requireNonNull(mode, "mode");
        if (limit <= 0) throw new IllegalArgumentException("limit <= 0");
        this.limit = limit;
        heap = kind.newArray(limit);
    }

    public int size() { return size; }
    public int limit() { return limit; }
    public PrimitiveKind kind() { return kind; }
    public Mode mode() { return mode; }
    public long payloadBytes() { return kind.payloadBytes(limit); }

    /** Returns true when the candidate is retained after the operation. */
    public boolean offerBits(long bits) {
        if (size < limit) {
            siftUp(size++, bits);
            return true;
        }
        long threshold = kind.readBits(heap, 0);
        if (!betterThan(bits, threshold)) return false;
        siftDown(0, bits);
        return true;
    }

    /** The current rejection boundary (smallest of largest-K, or largest of smallest-K). */
    public long thresholdBits() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return kind.readBits(heap, 0);
    }

    public Object toSortedPrimitiveArray() {
        Object result = kind.newArray(size);
        System.arraycopy(heap, 0, result, 0, size);
        kind.sort(result, 0, size);
        return result;
    }

    Object backingArray() { return heap; }

    private boolean betterThan(long candidate, long threshold) {
        int comparison = kind.compareBits(candidate, threshold);
        return mode == Mode.KEEP_LARGEST ? comparison > 0 : comparison < 0;
    }

    private boolean rootBefore(long left, long right) {
        int comparison = kind.compareBits(left, right);
        return mode == Mode.KEEP_LARGEST ? comparison < 0 : comparison > 0;
    }

    private void siftUp(int index, long value) {
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            long parentValue = kind.readBits(heap, parent);
            if (!rootBefore(value, parentValue)) break;
            kind.writeBits(heap, index, parentValue);
            index = parent;
        }
        kind.writeBits(heap, index, value);
    }

    private void siftDown(int index, long value) {
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            long childValue = kind.readBits(heap, child);
            int right = child + 1;
            if (right < size) {
                long rightValue = kind.readBits(heap, right);
                if (rootBefore(rightValue, childValue)) {
                    child = right;
                    childValue = rightValue;
                }
            }
            if (!rootBefore(childValue, value)) break;
            kind.writeBits(heap, index, childValue);
            index = child;
        }
        kind.writeBits(heap, index, value);
    }
}
