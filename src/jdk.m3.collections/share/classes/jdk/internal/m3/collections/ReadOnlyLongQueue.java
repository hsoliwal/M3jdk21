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

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Live read-only primitive facade. Backing aliases remain live.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/ReadOnlyLongQueue.java}
 * blob {@code 4bd3dc7ad30481b431752b2693800721d164f9ee} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ReadOnlyLongQueue extends ForwardingLongQueue {
    private final LongQueue owner;
    public ReadOnlyLongQueue(LongQueue owner) {
        this.owner = java.util.Objects.requireNonNull(owner, "owner");
    }
    @Override protected LongQueue delegate() { return owner; }

    @Override public boolean offer(long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public long poll() { throw new UnsupportedOperationException("read-only collection"); }
    @Override public boolean tryPoll(long[] target, int targetIndex) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public long pollReserved() { throw new UnsupportedOperationException("read-only collection"); }
    @Override public int offerBatch(long[] source, int offset, int length) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public int drainTo(long[] target, int offset, int length) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void clear() { throw new UnsupportedOperationException("read-only collection"); }
}
