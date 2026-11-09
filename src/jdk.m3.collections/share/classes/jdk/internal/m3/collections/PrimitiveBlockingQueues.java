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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jdk.internal.m3.collections.packed.PackedLongBlockingQueue;

/**
 * Type-safe blocking primitive queues for all eight scalar families.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PrimitiveBlockingQueues.java}
 * blob {@code 9815e900365f19d80c2050a2a9881bb2a613ec73} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrimitiveBlockingQueues {
    private PrimitiveBlockingQueues() { }

    public static final class BooleanQueue {
        private final PackedLongBlockingQueue queue;
        public BooleanQueue(int capacity, boolean fair) {
            this(capacity, fair, PrimitiveConcurrentQueues.Topology.MPMC);
        }
        public BooleanQueue(int capacity, boolean fair, PrimitiveConcurrentQueues.Topology topology) {
            queue = new PackedLongBlockingQueue(capacity, fair, topology);
        }
        public boolean offer(boolean value) { return queue.offer(PrimitiveScalarCodec.encode(value)); }
        public boolean offer(boolean value, long timeout, TimeUnit unit) throws InterruptedException {
            return queue.offer(PrimitiveScalarCodec.encode(value), timeout, unit);
        }
        public void put(boolean value) throws InterruptedException { queue.put(PrimitiveScalarCodec.encode(value)); }
        public boolean poll() { return PrimitiveScalarCodec.booleanValue(queue.poll()); }
        public boolean poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            return PrimitiveScalarCodec.booleanValue(queue.poll(timeout, unit));
        }
        public boolean take() throws InterruptedException { return PrimitiveScalarCodec.booleanValue(queue.take()); }
        public int sizeApprox() { return queue.sizeApprox(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class ByteQueue {
        private final PackedLongBlockingQueue queue;
        public ByteQueue(int capacity, boolean fair) {
            this(capacity, fair, PrimitiveConcurrentQueues.Topology.MPMC);
        }
        public ByteQueue(int capacity, boolean fair, PrimitiveConcurrentQueues.Topology topology) {
            queue = new PackedLongBlockingQueue(capacity, fair, topology);
        }
        public boolean offer(byte value) { return queue.offer(value); }
        public boolean offer(byte value, long timeout, TimeUnit unit) throws InterruptedException {
            return queue.offer(value, timeout, unit);
        }
        public void put(byte value) throws InterruptedException { queue.put(value); }
        public byte poll() { return (byte) queue.poll(); }
        public byte poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            return (byte) queue.poll(timeout, unit);
        }
        public byte take() throws InterruptedException { return (byte) queue.take(); }
        public int sizeApprox() { return queue.sizeApprox(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class CharQueue {
        private final PackedLongBlockingQueue queue;
        public CharQueue(int capacity, boolean fair) {
            this(capacity, fair, PrimitiveConcurrentQueues.Topology.MPMC);
        }
        public CharQueue(int capacity, boolean fair, PrimitiveConcurrentQueues.Topology topology) {
            queue = new PackedLongBlockingQueue(capacity, fair, topology);
        }
        public boolean offer(char value) { return queue.offer(value); }
        public boolean offer(char value, long timeout, TimeUnit unit) throws InterruptedException {
            return queue.offer(value, timeout, unit);
        }
        public void put(char value) throws InterruptedException { queue.put(value); }
        public char poll() { return (char) queue.poll(); }
        public char poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            return (char) queue.poll(timeout, unit);
        }
        public char take() throws InterruptedException { return (char) queue.take(); }
        public int sizeApprox() { return queue.sizeApprox(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class ShortQueue {
        private final PackedLongBlockingQueue queue;
        public ShortQueue(int capacity, boolean fair) {
            this(capacity, fair, PrimitiveConcurrentQueues.Topology.MPMC);
        }
        public ShortQueue(int capacity, boolean fair, PrimitiveConcurrentQueues.Topology topology) {
            queue = new PackedLongBlockingQueue(capacity, fair, topology);
        }
        public boolean offer(short value) { return queue.offer(value); }
        public boolean offer(short value, long timeout, TimeUnit unit) throws InterruptedException {
            return queue.offer(value, timeout, unit);
        }
        public void put(short value) throws InterruptedException { queue.put(value); }
        public short poll() { return (short) queue.poll(); }
        public short poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            return (short) queue.poll(timeout, unit);
        }
        public short take() throws InterruptedException { return (short) queue.take(); }
        public int sizeApprox() { return queue.sizeApprox(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class IntQueue {
        private final PackedLongBlockingQueue queue;
        public IntQueue(int capacity, boolean fair) {
            this(capacity, fair, PrimitiveConcurrentQueues.Topology.MPMC);
        }
        public IntQueue(int capacity, boolean fair, PrimitiveConcurrentQueues.Topology topology) {
            queue = new PackedLongBlockingQueue(capacity, fair, topology);
        }
        public boolean offer(int value) { return queue.offer(value); }
        public boolean offer(int value, long timeout, TimeUnit unit) throws InterruptedException {
            return queue.offer(value, timeout, unit);
        }
        public void put(int value) throws InterruptedException { queue.put(value); }
        public int poll() { return (int) queue.poll(); }
        public int poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            return (int) queue.poll(timeout, unit);
        }
        public int take() throws InterruptedException { return (int) queue.take(); }
        public int sizeApprox() { return queue.sizeApprox(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class LongQueue {
        private final PackedLongBlockingQueue queue;
        public LongQueue(int capacity, boolean fair) {
            this(capacity, fair, PrimitiveConcurrentQueues.Topology.MPMC);
        }
        public LongQueue(int capacity, boolean fair, PrimitiveConcurrentQueues.Topology topology) {
            queue = new PackedLongBlockingQueue(capacity, fair, topology);
        }
        public boolean offer(long value) { return queue.offer(value); }
        public boolean offer(long value, long timeout, TimeUnit unit) throws InterruptedException {
            return queue.offer(value, timeout, unit);
        }
        public void put(long value) throws InterruptedException { queue.put(value); }
        public long poll() { return queue.poll(); }
        public long poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            return queue.poll(timeout, unit);
        }
        public long take() throws InterruptedException { return queue.take(); }
        public int sizeApprox() { return queue.sizeApprox(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class FloatQueue {
        private final PackedLongBlockingQueue queue;
        public FloatQueue(int capacity, boolean fair) {
            this(capacity, fair, PrimitiveConcurrentQueues.Topology.MPMC);
        }
        public FloatQueue(int capacity, boolean fair, PrimitiveConcurrentQueues.Topology topology) {
            queue = new PackedLongBlockingQueue(capacity, fair, topology);
        }
        public boolean offer(float value) { return queue.offer(PrimitiveScalarCodec.encode(value)); }
        public boolean offer(float value, long timeout, TimeUnit unit) throws InterruptedException {
            return queue.offer(PrimitiveScalarCodec.encode(value), timeout, unit);
        }
        public void put(float value) throws InterruptedException { queue.put(PrimitiveScalarCodec.encode(value)); }
        public float poll() { return PrimitiveScalarCodec.floatValue(queue.poll()); }
        public float poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            return PrimitiveScalarCodec.floatValue(queue.poll(timeout, unit));
        }
        public float take() throws InterruptedException { return PrimitiveScalarCodec.floatValue(queue.take()); }
        public int sizeApprox() { return queue.sizeApprox(); }
        public int capacity() { return queue.capacity(); }
    }

    public static final class DoubleQueue {
        private final PackedLongBlockingQueue queue;
        public DoubleQueue(int capacity, boolean fair) {
            this(capacity, fair, PrimitiveConcurrentQueues.Topology.MPMC);
        }
        public DoubleQueue(int capacity, boolean fair, PrimitiveConcurrentQueues.Topology topology) {
            queue = new PackedLongBlockingQueue(capacity, fair, topology);
        }
        public boolean offer(double value) { return queue.offer(PrimitiveScalarCodec.encode(value)); }
        public boolean offer(double value, long timeout, TimeUnit unit) throws InterruptedException {
            return queue.offer(PrimitiveScalarCodec.encode(value), timeout, unit);
        }
        public void put(double value) throws InterruptedException { queue.put(PrimitiveScalarCodec.encode(value)); }
        public double poll() { return PrimitiveScalarCodec.doubleValue(queue.poll()); }
        public double poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            return PrimitiveScalarCodec.doubleValue(queue.poll(timeout, unit));
        }
        public double take() throws InterruptedException { return PrimitiveScalarCodec.doubleValue(queue.take()); }
        public int sizeApprox() { return queue.sizeApprox(); }
        public int capacity() { return queue.capacity(); }
    }

}
