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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * Bounded exact-width single-producer/multi-consumer lock-free ring.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveSpmcRingBuffer.java}
 * blob {@code 76bba7bcfa9564dc4d53c0ec47b50fb3592f056a} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveSpmcRingBuffer {
    private static final VarHandle PRODUCER_INDEX;
    private static final VarHandle CONSUMER_INDEX;
    private static final VarHandle SEQUENCE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            PRODUCER_INDEX = lookup.findVarHandle(
                    PrimitiveSpmcRingBuffer.class, "producerIndex", long.class);
            CONSUMER_INDEX = lookup.findVarHandle(
                    PrimitiveSpmcRingBuffer.class, "consumerIndex", long.class);
            SEQUENCE = MethodHandles.arrayElementVarHandle(long[].class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final PrimitiveKind kind;
    private final Object elements;
    private final long[] sequences;
    private final int capacity;
    private final int mask;
    private volatile long producerIndex;
    private volatile long consumerIndex;

    public PrimitiveSpmcRingBuffer(PrimitiveKind kind, int requestedCapacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        capacity = roundCapacity(requestedCapacity);
        mask = capacity - 1;
        elements = kind.newArray(capacity);
        sequences = new long[capacity];
        for (int i = 0; i < capacity; i++) sequences[i] = i;
    }

    public PrimitiveKind kind() { return kind; }
    public int capacity() { return capacity; }
    public long payloadBytes() {
        return kind.payloadBytes(capacity) + (long) capacity * Long.BYTES;
    }

    public boolean offerBits(long bits) {
        long producer = producerIndex;
        int slot = ((int) producer) & mask;
        for (;;) {
            long sequence = (long) SEQUENCE.getAcquire(sequences, slot);
            long delta = sequence - producer;
            if (delta == 0L) {
                kind.writeBits(elements, slot, bits);
                SEQUENCE.setRelease(sequences, slot, producer + 1L);
                PRODUCER_INDEX.setRelease(this, producer + 1L);
                return true;
            }
            if (delta < 0L) {
                long consumer = (long) CONSUMER_INDEX.getAcquire(this);
                if (producer - capacity >= consumer) return false;
            }
            Thread.onSpinWait();
        }
    }

    /** Single producer: relaxed failure means the target slot is not immediately free. */
    public boolean relaxedOfferBits(long bits) {
        long producer = producerIndex;
        int slot = ((int) producer) & mask;
        if ((long) SEQUENCE.getAcquire(sequences, slot) != producer) return false;
        kind.writeBits(elements, slot, bits);
        SEQUENCE.setRelease(sequences, slot, producer + 1L);
        PRODUCER_INDEX.setRelease(this, producer + 1L);
        return true;
    }

    public long pollOrDefaultBits(long defaultBits) {
        long cachedProducer = -1L;
        for (;;) {
            long consumer = (long) CONSUMER_INDEX.getAcquire(this);
            int slot = ((int) consumer) & mask;
            long expected = consumer + 1L;
            long sequence = (long) SEQUENCE.getAcquire(sequences, slot);
            long delta = sequence - expected;
            if (delta == 0L) {
                if (CONSUMER_INDEX.compareAndSet(this, consumer, consumer + 1L)) {
                    long value = kind.readBits(elements, slot);
                    kind.clear(elements, slot, slot + 1);
                    SEQUENCE.setRelease(sequences, slot, consumer + capacity);
                    return value;
                }
            } else if (delta < 0L) {
                if (consumer >= cachedProducer) {
                    cachedProducer = (long) PRODUCER_INDEX.getAcquire(this);
                    if (consumer >= cachedProducer) return defaultBits;
                }
            }
            Thread.onSpinWait();
        }
    }

    /** May fail spuriously under consumer contention. */
    public long relaxedPollOrDefaultBits(long defaultBits) {
        long consumer = (long) CONSUMER_INDEX.getAcquire(this);
        int slot = ((int) consumer) & mask;
        long expected = consumer + 1L;
        if ((long) SEQUENCE.getAcquire(sequences, slot) != expected) return defaultBits;
        if (!CONSUMER_INDEX.compareAndSet(this, consumer, consumer + 1L)) return defaultBits;
        long value = kind.readBits(elements, slot);
        kind.clear(elements, slot, slot + 1);
        SEQUENCE.setRelease(sequences, slot, consumer + capacity);
        return value;
    }

    public int offerAllBits(Object source, int from, int length) {
        Objects.requireNonNull(source, "source");
        if (from < 0 || length < 0
                || from > java.lang.reflect.Array.getLength(source) - length) {
            throw new IndexOutOfBoundsException();
        }
        int offered = 0;
        while (offered < length && offerBits(kind.readBits(source, from + offered))) offered++;
        return offered;
    }

    public int drainToBits(Object target, int from, int limit) {
        Objects.requireNonNull(target, "target");
        int targetLength = java.lang.reflect.Array.getLength(target);
        if (from < 0 || limit < 0 || from > targetLength - limit) {
            throw new IndexOutOfBoundsException();
        }
        int drained = 0;
        while (drained < limit) {
            long consumer = (long) CONSUMER_INDEX.getAcquire(this);
            int slot = ((int) consumer) & mask;
            long expected = consumer + 1L;
            if ((long) SEQUENCE.getAcquire(sequences, slot) != expected) {
                long producer = (long) PRODUCER_INDEX.getAcquire(this);
                if (consumer >= producer) break;
                Thread.onSpinWait();
                continue;
            }
            if (!CONSUMER_INDEX.compareAndSet(this, consumer, consumer + 1L)) {
                Thread.onSpinWait();
                continue;
            }
            kind.writeBits(target, from + drained, kind.readBits(elements, slot));
            kind.clear(elements, slot, slot + 1);
            SEQUENCE.setRelease(sequences, slot, consumer + capacity);
            drained++;
        }
        return drained;
    }

    public boolean isEmpty() {
        return (long) CONSUMER_INDEX.getAcquire(this)
                >= (long) PRODUCER_INDEX.getAcquire(this);
    }

    public boolean isFull() {
        long consumer = (long) CONSUMER_INDEX.getAcquire(this);
        long producer = (long) PRODUCER_INDEX.getAcquire(this);
        return producer - consumer >= capacity;
    }

    public int size() {
        long consumer = (long) CONSUMER_INDEX.getAcquire(this);
        long producer = (long) PRODUCER_INDEX.getAcquire(this);
        long difference = producer - consumer;
        if (difference <= 0L) return 0;
        return difference >= capacity ? capacity : (int) difference;
    }

    private static int roundCapacity(int requested) {
        if (requested < 2) throw new IllegalArgumentException("capacity must be >= 2");
        int rounded = 2;
        while (rounded < requested) {
            if (rounded >= (1 << 30)) throw new IllegalArgumentException("capacity too large");
            rounded <<= 1;
        }
        return rounded;
    }
}
