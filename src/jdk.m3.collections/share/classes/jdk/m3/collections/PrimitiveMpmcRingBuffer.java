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
import java.util.function.LongConsumer;

/**
 * Bounded lock-free MPMC primitive ring.
 *
 * <p>Retained entry state is one exact-width primitive element lane plus one
 * {@code long[]} sequence lane. Producers CAS a global producer sequence,
 * consumers CAS a global consumer sequence, and each slot is published/freed
 * with release/acquire sequence operations. No cell, node, wrapper, lock or
 * monitor object is allocated per queue entry.
 *
 * <p>The algorithm shape follows the bounded sequence-slot MPMC family used by
 * Vyukov/JCTools, independently implemented here for exact-width primitive
 * storage.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveMpmcRingBuffer.java}
 * blob {@code d147d8ec65092b24eb6c7bf1787234927624a59c} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveMpmcRingBuffer {
    private static final VarHandle PRODUCER_INDEX;
    private static final VarHandle CONSUMER_INDEX;
    private static final VarHandle SEQUENCE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            PRODUCER_INDEX = lookup.findVarHandle(
                    PrimitiveMpmcRingBuffer.class, "producerIndex", long.class);
            CONSUMER_INDEX = lookup.findVarHandle(
                    PrimitiveMpmcRingBuffer.class, "consumerIndex", long.class);
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

    public PrimitiveMpmcRingBuffer(PrimitiveKind kind, int requestedCapacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (requestedCapacity < 2) {
            throw new IllegalArgumentException("capacity must be >= 2");
        }
        int rounded = 2;
        while (rounded < requestedCapacity) {
            if (rounded >= (1 << 30)) {
                throw new IllegalArgumentException("capacity too large");
            }
            rounded <<= 1;
        }
        capacity = rounded;
        mask = capacity - 1;
        elements = kind.newArray(capacity);
        sequences = new long[capacity];
        for (int i = 0; i < capacity; i++) {
            sequences[i] = i;
        }
    }

    public PrimitiveKind kind() { return kind; }
    public int capacity() { return capacity; }

    /**
     * Backing primitive-array payload only: exact-width elements plus one
     * sequence long per slot.
     */
    public long payloadBytes() {
        return kind.payloadBytes(capacity) + (long) capacity * Long.BYTES;
    }

    /**
     * Non-blocking offer. Returns false only after observing that the bounded
     * ring is full; CAS contention is retried.
     */
    public boolean offerBits(long bits) {
        long cachedConsumer = Long.MIN_VALUE;
        for (;;) {
            long producer = (long) PRODUCER_INDEX.getAcquire(this);
            int slot = ((int) producer) & mask;
            long sequence = (long) SEQUENCE.getAcquire(sequences, slot);
            long delta = sequence - producer;

            if (delta == 0L) {
                if (PRODUCER_INDEX.compareAndSet(this, producer, producer + 1L)) {
                    kind.writeBits(elements, slot, bits);
                    SEQUENCE.setRelease(sequences, slot, producer + 1L);
                    return true;
                }
            } else if (delta < 0L) {
                if (producer - capacity >= cachedConsumer) {
                    cachedConsumer = (long) CONSUMER_INDEX.getAcquire(this);
                    if (producer - capacity >= cachedConsumer) return false;
                }
            }
            Thread.onSpinWait();
        }
    }

    /**
     * Relaxed offer: may fail spuriously if the observed slot is unavailable or
     * the producer CAS loses a race. Useful in caller-controlled retry loops.
     */
    public boolean relaxedOfferBits(long bits) {
        long producer = (long) PRODUCER_INDEX.getAcquire(this);
        int slot = ((int) producer) & mask;
        if ((long) SEQUENCE.getAcquire(sequences, slot) != producer) return false;
        if (!PRODUCER_INDEX.compareAndSet(this, producer, producer + 1L)) return false;
        kind.writeBits(elements, slot, bits);
        SEQUENCE.setRelease(sequences, slot, producer + 1L);
        return true;
    }

    public int offerAllBits(Object source, int from, int length) {
        Objects.requireNonNull(source, "source");
        int sourceLength = java.lang.reflect.Array.getLength(source);
        if (from < 0 || length < 0 || from > sourceLength - length) {
            throw new IndexOutOfBoundsException();
        }
        int offered = 0;
        while (offered < length
                && offerBits(kind.readBits(source, from + offered))) {
            offered++;
        }
        return offered;
    }

    /**
     * Non-blocking poll. The supplied default is returned only after observing
     * the ring empty; CAS contention and reserved-but-not-yet-published slots
     * are retried.
     */
    public long pollOrDefaultBits(long defaultValueBits) {
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
                    if (consumer >= cachedProducer) return defaultValueBits;
                }
            }
            Thread.onSpinWait();
        }
    }

    /**
     * Relaxed poll: may report empty on contention or while a producer has
     * reserved but not yet published the next slot.
     */
    public long relaxedPollOrDefaultBits(long defaultValueBits) {
        long consumer = (long) CONSUMER_INDEX.getAcquire(this);
        int slot = ((int) consumer) & mask;
        long expected = consumer + 1L;
        if ((long) SEQUENCE.getAcquire(sequences, slot) != expected) return defaultValueBits;
        if (!CONSUMER_INDEX.compareAndSet(this, consumer, consumer + 1L)) {
            return defaultValueBits;
        }
        long value = kind.readBits(elements, slot);
        kind.clear(elements, slot, slot + 1);
        SEQUENCE.setRelease(sequences, slot, consumer + capacity);
        return value;
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
            long sequence = (long) SEQUENCE.getAcquire(sequences, slot);
            if (sequence != expected) {
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

    /**
     * Callback poll avoids reserving an out-of-band primitive sentinel.
     */
    public boolean tryPollBits(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        long cachedProducer = -1L;
        for (;;) {
            long index = (long) CONSUMER_INDEX.getAcquire(this);
            int slot = ((int) index) & mask;
            long expected = index + 1L;
            long sequence = (long) SEQUENCE.getAcquire(sequences, slot);
            long delta = sequence - expected;

            if (delta == 0L) {
                if (CONSUMER_INDEX.compareAndSet(this, index, index + 1L)) {
                    long value = kind.readBits(elements, slot);
                    kind.clear(elements, slot, slot + 1);
                    SEQUENCE.setRelease(sequences, slot, index + capacity);
                    consumer.accept(value);
                    return true;
                }
            } else if (delta < 0L) {
                if (index >= cachedProducer) {
                    cachedProducer = (long) PRODUCER_INDEX.getAcquire(this);
                    if (index >= cachedProducer) return false;
                }
            }
            Thread.onSpinWait();
        }
    }

    /**
     * Best-effort peek. The value is returned only if the consumer index is
     * unchanged after reading the published slot.
     */
    public long peekOrDefaultBits(long defaultValueBits) {
        for (;;) {
            long consumer = (long) CONSUMER_INDEX.getAcquire(this);
            int slot = ((int) consumer) & mask;
            long expected = consumer + 1L;
            long sequence = (long) SEQUENCE.getAcquire(sequences, slot);
            long delta = sequence - expected;
            if (delta < 0L) {
                long producer = (long) PRODUCER_INDEX.getAcquire(this);
                if (consumer >= producer) return defaultValueBits;
            } else if (delta == 0L) {
                long value = kind.readBits(elements, slot);
                if ((long) CONSUMER_INDEX.getAcquire(this) == consumer) return value;
            }
            Thread.onSpinWait();
        }
    }

    public int drainBits(LongConsumer consumer, int limit) {
        Objects.requireNonNull(consumer, "consumer");
        if (limit < 0) throw new IllegalArgumentException("limit < 0");
        int drained = 0;
        while (drained < limit && tryPollBits(consumer)) drained++;
        return drained;
    }

    /**
     * Approximate under concurrency, exact when quiescent.
     */
    public int size() {
        long consumer = (long) CONSUMER_INDEX.getAcquire(this);
        long producer = (long) PRODUCER_INDEX.getAcquire(this);
        long difference = producer - consumer;
        if (difference <= 0L) return 0;
        return difference >= capacity ? capacity : (int) difference;
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

    Object backingArray() { return elements; }
    long[] sequenceArray() { return sequences; }

    /**
     * Used only after an external capacity permit has been acquired.
     */
    void offerClaimedBits(long bits) {
        while (!offerBits(bits)) Thread.onSpinWait();
    }

    /**
     * Used only after an external item permit has been acquired.
     */
    long pollClaimedBits() {
        for (;;) {
            long consumer = (long) CONSUMER_INDEX.getAcquire(this);
            int slot = ((int) consumer) & mask;
            long expected = consumer + 1L;
            long sequence = (long) SEQUENCE.getAcquire(sequences, slot);
            if (sequence == expected
                    && CONSUMER_INDEX.compareAndSet(this, consumer, consumer + 1L)) {
                long value = kind.readBits(elements, slot);
                kind.clear(elements, slot, slot + 1);
                SEQUENCE.setRelease(sequences, slot, consumer + capacity);
                return value;
            }
            Thread.onSpinWait();
        }
    }
}
