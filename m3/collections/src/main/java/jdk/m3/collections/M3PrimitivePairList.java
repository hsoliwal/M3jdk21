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
 * Struct-of-arrays pair sequence: two primitive lanes, no pair object per row.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitivePairList.java}
 * blob {@code d0554b8a8da5d67a48b0dfd2a1068617ca469662} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitivePairList {
    @FunctionalInterface
    public interface PairConsumer {
        void accept(long leftBits, long rightBits);
    }

    private final M3PrimitiveKind leftKind;
    private final M3PrimitiveKind rightKind;
    private Object left;
    private Object right;
    private int size;

    public M3PrimitivePairList(M3PrimitiveKind leftKind, M3PrimitiveKind rightKind) {
        this(leftKind, rightKind, 8);
    }

    public M3PrimitivePairList(M3PrimitiveKind leftKind, M3PrimitiveKind rightKind, int initialCapacity) {
        this.leftKind = Objects.requireNonNull(leftKind, "leftKind");
        this.rightKind = Objects.requireNonNull(rightKind, "rightKind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        left = leftKind.newArray(capacity);
        right = rightKind.newArray(capacity);
    }

    public M3PrimitiveKind leftKind() { return leftKind; }
    public M3PrimitiveKind rightKind() { return rightKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(left); }
    public long payloadBytes() {
        return leftKind.payloadBytes(capacity()) + rightKind.payloadBytes(capacity());
    }

    public void addBits(long leftBits, long rightBits) {
        ensure(size + 1);
        leftKind.writeBits(left, size, leftBits);
        rightKind.writeBits(right, size, rightBits);
        size++;
    }

    public long leftBits(int index) {
        return leftKind.readBits(left, check(index));
    }

    public long rightBits(int index) {
        return rightKind.readBits(right, check(index));
    }

    public void setBits(int index, long leftBits, long rightBits) {
        int i = check(index);
        leftKind.writeBits(left, i, leftBits);
        rightKind.writeBits(right, i, rightBits);
    }

    public void removeAt(int index) {
        int i = check(index);
        int moved = size - i - 1;
        if (moved > 0) {
            System.arraycopy(left, i + 1, left, i, moved);
            System.arraycopy(right, i + 1, right, i, moved);
        }
        size--;
        leftKind.clear(left, size, size + 1);
        rightKind.clear(right, size, size + 1);
    }

    public void clear() {
        leftKind.clear(left, 0, size);
        rightKind.clear(right, 0, size);
        size = 0;
    }

    public Object leftArray() { return leftKind.copyOf(left, size); }
    public Object rightArray() { return rightKind.copyOf(right, size); }

    public void forEach(PairConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) {
            action.accept(leftKind.readBits(left, i), rightKind.readBits(right, i));
        }
    }

    Object leftBackingArray() { return left; }
    Object rightBackingArray() { return right; }

    private int check(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return index;
    }

    /** Test-only copy injection; the default remains the existing M3PrimitiveKind owner. */
    @FunctionalInterface
    interface BackingCopier {
        Object copy(M3PrimitiveKind kind, Object source, int capacity);
    }

    private void ensure(int required) {
        ensure(required, M3PrimitiveKind::copyOf);
    }

    /** Two-phase, type-checked backing growth; no partial lane publication. */
    void ensure(int required, BackingCopier copier) {
        if (required <= capacity()) return;
        Objects.requireNonNull(copier, "copier");
        int next = capacity() + (capacity() >>> 1) + 1;
        if (next < required) next = required;
        Object nextLeft = Objects.requireNonNull(copier.copy(leftKind, left, next), "left copy");
        Object nextRight = Objects.requireNonNull(copier.copy(rightKind, right, next), "right copy");
        if (nextLeft.getClass() != left.getClass()
                || nextRight.getClass() != right.getClass()
                || java.lang.reflect.Array.getLength(nextLeft) != next
                || java.lang.reflect.Array.getLength(nextRight) != next) {
            throw new IllegalStateException("incomplete M3PrimitivePairList backing growth");
        }
        left = nextLeft;
        right = nextRight;
    }
}
