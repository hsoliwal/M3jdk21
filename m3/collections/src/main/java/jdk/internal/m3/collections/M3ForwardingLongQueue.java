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
package jdk.internal.m3.collections;

import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongQueue;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Direct primitive delegation. Adds no storage, policy or concurrency guarantee.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/ForwardingLongQueue.java}
 * blob {@code b637b12d4c603ef43e28f0fa5eaeba387efb0b81} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class M3ForwardingLongQueue implements LongQueue {
    protected M3ForwardingLongQueue() { }
    protected abstract LongQueue delegate();

    @Override public boolean offer(long value) { return delegate().offer(value); }
    @Override public long peek() { return delegate().peek(); }
    @Override public long poll() { return delegate().poll(); }
    @Override public int capacity() { return delegate().capacity(); }
    @Override public boolean tryPoll(long[] target, int targetIndex) { return delegate().tryPoll(target, targetIndex); }
    @Override public long pollReserved() { return delegate().pollReserved(); }
    @Override public int offerBatch(long[] source, int offset, int length) { return delegate().offerBatch(source, offset, length); }
    @Override public int drainTo(long[] target, int offset, int length) { return delegate().drainTo(target, offset, length); }
    @Override public int size() { return delegate().size(); }
    @Override public boolean contains(long value) { return delegate().contains(value); }
    @Override public void clear() { delegate().clear(); }
    @Override public LongIterator iterator() { return delegate().iterator(); }
    @Override public long[] toArray() { return delegate().toArray(); }
    @Override public boolean isEmpty() { return delegate().isEmpty(); }
    @Override public void forEachLong(LongConsumer consumer) { delegate().forEachLong(consumer); }
    @Override public java.util.Spliterator.OfLong longSpliterator() { return delegate().longSpliterator(); }
    @Override public java.util.stream.LongStream longStream() { return delegate().longStream(); }
    @Override public java.util.stream.LongStream parallelLongStream() { return delegate().parallelLongStream(); }
}
