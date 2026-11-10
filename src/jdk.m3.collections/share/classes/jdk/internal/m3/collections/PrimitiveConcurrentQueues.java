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
package jdk.internal.m3.collections;

import jdk.m3.collections.LongQueue;

import java.util.Objects;
import jdk.internal.m3.collections.concurrent.PackedLongMpmcQueue;
import jdk.internal.m3.collections.concurrent.PackedLongSpscQueue;

/**
 * Type-safe primitive adapters over packed long concurrent queues.
 *
 * <p>Every lane is encoded into one primitive long word. Floating-point
 * adapters preserve raw IEEE-754 bits, including NaN payloads and signed zero.
 * MPMC is the compatibility default; use SPSC when ownership is structurally
 * one producer / one consumer to avoid the per-slot sequence lane.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PrimitiveConcurrentQueues.java}
 * blob {@code 29428131b1005a302f7b1858165683e3c23b3ddd} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrimitiveConcurrentQueues {
    private PrimitiveConcurrentQueues() { }

    public enum Topology {
        SPSC,
        MPMC
    }

    private static jdk.m3.collections.LongQueue queue(
            int capacity,
            Topology topology) {
        Objects.requireNonNull(topology, "topology");
        return switch (topology) {
            case SPSC -> new PackedLongSpscQueue(capacity);
            case MPMC -> new PackedLongMpmcQueue(capacity);
        };
    }

    public static final class BooleanQueue {
        private final jdk.m3.collections.LongQueue queue;
        public BooleanQueue(int capacity) { this(capacity, Topology.MPMC); }
        public BooleanQueue(int capacity, Topology topology) {
            queue = PrimitiveConcurrentQueues.queue(capacity, topology);
        }
        public boolean offer(boolean value) { return queue.offer(PrimitiveScalarCodec.encode(value)); }
        public boolean remove() { return PrimitiveScalarCodec.booleanValue(queue.poll()); }
        public int sizeApprox() { return queue.size(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class ByteQueue {
        private final jdk.m3.collections.LongQueue queue;
        public ByteQueue(int capacity) { this(capacity, Topology.MPMC); }
        public ByteQueue(int capacity, Topology topology) {
            queue = PrimitiveConcurrentQueues.queue(capacity, topology);
        }
        public boolean offer(byte value) { return queue.offer(value); }
        public byte remove() { return (byte) queue.poll(); }
        public int sizeApprox() { return queue.size(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class CharQueue {
        private final jdk.m3.collections.LongQueue queue;
        public CharQueue(int capacity) { this(capacity, Topology.MPMC); }
        public CharQueue(int capacity, Topology topology) {
            queue = PrimitiveConcurrentQueues.queue(capacity, topology);
        }
        public boolean offer(char value) { return queue.offer(value); }
        public char remove() { return (char) queue.poll(); }
        public int sizeApprox() { return queue.size(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class ShortQueue {
        private final jdk.m3.collections.LongQueue queue;
        public ShortQueue(int capacity) { this(capacity, Topology.MPMC); }
        public ShortQueue(int capacity, Topology topology) {
            queue = PrimitiveConcurrentQueues.queue(capacity, topology);
        }
        public boolean offer(short value) { return queue.offer(value); }
        public short remove() { return (short) queue.poll(); }
        public int sizeApprox() { return queue.size(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class IntQueue {
        private final jdk.m3.collections.LongQueue queue;
        public IntQueue(int capacity) { this(capacity, Topology.MPMC); }
        public IntQueue(int capacity, Topology topology) {
            queue = PrimitiveConcurrentQueues.queue(capacity, topology);
        }
        public boolean offer(int value) { return queue.offer(value); }
        public int remove() { return (int) queue.poll(); }
        public int sizeApprox() { return queue.size(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class LongQueue {
        private final jdk.m3.collections.LongQueue queue;
        public LongQueue(int capacity) { this(capacity, Topology.MPMC); }
        public LongQueue(int capacity, Topology topology) {
            queue = PrimitiveConcurrentQueues.queue(capacity, topology);
        }
        public boolean offer(long value) { return queue.offer(value); }
        public long remove() { return queue.poll(); }
        public int sizeApprox() { return queue.size(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class FloatQueue {
        private final jdk.m3.collections.LongQueue queue;
        public FloatQueue(int capacity) { this(capacity, Topology.MPMC); }
        public FloatQueue(int capacity, Topology topology) {
            queue = PrimitiveConcurrentQueues.queue(capacity, topology);
        }
        public boolean offer(float value) {
            return queue.offer(PrimitiveScalarCodec.encode(value));
        }
        public float remove() { return PrimitiveScalarCodec.floatValue(queue.poll()); }
        public int sizeApprox() { return queue.size(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class DoubleQueue {
        private final jdk.m3.collections.LongQueue queue;
        public DoubleQueue(int capacity) { this(capacity, Topology.MPMC); }
        public DoubleQueue(int capacity, Topology topology) {
            queue = PrimitiveConcurrentQueues.queue(capacity, topology);
        }
        public boolean offer(double value) {
            return queue.offer(PrimitiveScalarCodec.encode(value));
        }
        public double remove() { return PrimitiveScalarCodec.doubleValue(queue.poll()); }
        public int sizeApprox() { return queue.size(); }
        public int capacity() { return queue.capacity(); }
    }
}
