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

import jdk.m3.collections.LongSequence;

import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * Live read-only primitive facade. Backing aliases remain live.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/ReadOnlyLongSequence.java}
 * blob {@code 69a5c740c552396fe442797cf5a6919c385075cb} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections bag + read-only sequence batch (v6.2, receipt contract): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ReadOnlyLongSequence extends ForwardingLongSequence {
    private final LongSequence owner;
    public ReadOnlyLongSequence(LongSequence owner) {
        this.owner = java.util.Objects.requireNonNull(owner, "owner");
    }
    @Override protected LongSequence delegate() { return owner; }

    @Override public boolean add(long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public boolean addIf(long value, LongPredicate predicate) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public boolean addAll(long[] values) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void insert(int index, long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public long set(int index, long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public long removeAt(int index) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public boolean removeValue(long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public int filterInPlace(LongPredicate predicate) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void trimToSize() { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void clear() { throw new UnsupportedOperationException("read-only collection"); }
}
