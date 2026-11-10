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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * Exact-width single-producer/single-consumer primitive ring.
 *
 * <p>Element writes/reads are plain primitive-array accesses. Release publication of tail
 * and acquire observation of tail provide the producer-to-consumer happens-before edge;
 * head uses the symmetric consumer-to-producer edge.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveSpscRingBuffer.java}
 * blob {@code 0d571193438f12ce15e540bbd8797b64101efec6} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveSpscRingBuffer {
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            HEAD = lookup.findVarHandle(PrimitiveSpscRingBuffer.class, "head", long.class);
            TAIL = lookup.findVarHandle(PrimitiveSpscRingBuffer.class, "tail", long.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final PrimitiveKind kind;
    private final Object elements;
    private final int capacity;
    private final int mask;
    private volatile long head;
    private volatile long tail;

    public PrimitiveSpscRingBuffer(PrimitiveKind kind, int requestedCapacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (requestedCapacity <= 0) throw new IllegalArgumentException("capacity <= 0");
        int rounded = 1;
        while (rounded < requestedCapacity) {
            if (rounded >= (1 << 30)) throw new IllegalArgumentException("capacity too large");
            rounded <<= 1;
        }
        capacity = rounded;
        mask = capacity - 1;
        elements = kind.newArray(capacity);
    }

    public PrimitiveKind kind() { return kind; }
    public int capacity() { return capacity; }
    public long payloadBytes() { return kind.payloadBytes(capacity); }

    public int size() {
        long h = (long) HEAD.getAcquire(this);
        long t = (long) TAIL.getAcquire(this);
        long difference = t - h;
        return difference > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) difference;
    }

    public boolean isEmpty() {
        return (long) HEAD.getAcquire(this) == (long) TAIL.getAcquire(this);
    }

    public boolean offerBits(long bits) {
        long currentTail = tail;
        long currentHead = (long) HEAD.getAcquire(this);
        if (currentTail - currentHead >= capacity) return false;
        kind.writeBits(elements, (int) currentTail & mask, bits);
        TAIL.setRelease(this, currentTail + 1);
        return true;
    }

    public long pollOrDefaultBits(long defaultValueBits) {
        long currentHead = head;
        long currentTail = (long) TAIL.getAcquire(this);
        if (currentHead == currentTail) return defaultValueBits;
        int index = (int) currentHead & mask;
        long value = kind.readBits(elements, index);
        kind.clear(elements, index, index + 1);
        HEAD.setRelease(this, currentHead + 1);
        return value;
    }

    Object backingArray() { return elements; }
}
