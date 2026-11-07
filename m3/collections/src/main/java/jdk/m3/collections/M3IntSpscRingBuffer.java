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

/**
 * Lock-free single-producer/single-consumer primitive ring buffer.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntSpscRingBuffer.java}
 * blob {@code c630e63d17765e5adc4e3d4213970c93829a8583} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3IntSpscRingBuffer {
    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    static {
        try {
            var lookup = MethodHandles.lookup();
            HEAD = lookup.findVarHandle(M3IntSpscRingBuffer.class, "head", long.class);
            TAIL = lookup.findVarHandle(M3IntSpscRingBuffer.class, "tail", long.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
    private final int[] buffer;
    private final int mask;
    private volatile long head;
    private volatile long tail;

    public M3IntSpscRingBuffer(int capacity) {
        if (capacity < 2 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("capacity must be power of two >= 2");
        }
        buffer = new int[capacity];
        mask = capacity - 1;
    }
    public int capacity() { return buffer.length; }
    public long payloadBytes() { return (long) buffer.length * 4; }
    public boolean offer(int value) {
        long t = (long) TAIL.getOpaque(this);
        long h = (long) HEAD.getAcquire(this);
        if (t - h >= buffer.length) return false;
        buffer[(int) t & mask] = value;
        TAIL.setRelease(this, t + 1);
        return true;
    }
    public boolean isEmpty() {
        return (long) HEAD.getAcquire(this) == (long) TAIL.getAcquire(this);
    }
    public int size() {
        long h = (long) HEAD.getAcquire(this);
        long t = (long) TAIL.getAcquire(this);
        return (int) (t - h);
    }
    public int poll(int missing) {
        long h = (long) HEAD.getOpaque(this);
        long t = (long) TAIL.getAcquire(this);
        if (h == t) return missing;
        int i = (int) h & mask;
        int value = buffer[i];
        buffer[i] = 0;
        HEAD.setRelease(this, h + 1);
        return value;
    }
}
