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
package jdk.internal.m3.collections.concurrent;

import jdk.m3.collections.LongCollection;
import jdk.m3.collections.LongIterator;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Read-optimized concurrent primitive long set.
 *
 * <p>Readers perform one acquire-load and binary search an immutable sorted
 * {@code long[]} snapshot. Writers copy the primitive array and CAS-publish the
 * replacement. There are no per-entry nodes or wrapper values.
 *
 * <p>Best for read-heavy, small/medium cardinality state. Write cost is O(n).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/ConcurrentPackedLongSet.java}
 * blob {@code b2158ba60131bda61284c580631e234f442bb1d2} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ConcurrentPackedLongSet implements LongCollection {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public ConcurrentPackedLongSet() {
    }

    private static final long[] EMPTY = new long[0];
    private static final VarHandle VALUES;

    static {
        try {
            VALUES = MethodHandles.lookup().findVarHandle(
                    ConcurrentPackedLongSet.class, "values", long[].class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @SuppressWarnings("unused")
    private volatile long[] values = EMPTY;

    private long[] current() {
        return (long[]) VALUES.getAcquire(this);
    }

    public boolean add(long value) {
        for (;;) {
            long[] before = current();
            int found = Arrays.binarySearch(before, value);
            if (found >= 0) return false;
            int insert = -found - 1;
            long[] after = new long[before.length + 1];
            System.arraycopy(before, 0, after, 0, insert);
            after[insert] = value;
            System.arraycopy(before, insert, after, insert + 1, before.length - insert);
            if (VALUES.compareAndSet(this, before, after)) return true;
        }
    }

    public boolean remove(long value) {
        for (;;) {
            long[] before = current();
            int found = Arrays.binarySearch(before, value);
            if (found < 0) return false;
            if (before.length == 1) {
                if (VALUES.compareAndSet(this, before, EMPTY)) return true;
                continue;
            }
            long[] after = new long[before.length - 1];
            System.arraycopy(before, 0, after, 0, found);
            System.arraycopy(before, found + 1, after, found, before.length - found - 1);
            if (VALUES.compareAndSet(this, before, after)) return true;
        }
    }

    @Override public boolean contains(long value) {
        return Arrays.binarySearch(current(), value) >= 0;
    }

    @Override public int size() {
        return current().length;
    }

    @Override public void clear() {
        for (;;) {
            long[] before = current();
            if (before.length == 0) return;
            if (VALUES.compareAndSet(this, before, EMPTY)) return;
        }
    }

    @Override public long[] toArray() {
        return current().clone();
    }

    /** Primitive splitting over one immutable captured array; no element copy is made. */
    @Override public java.util.Spliterator.OfLong longSpliterator() {
        long[] snapshot = current();
        return java.util.Spliterators.spliterator(snapshot, java.util.Spliterator.ORDERED
                | java.util.Spliterator.DISTINCT | java.util.Spliterator.IMMUTABLE);
    }

    @Override public LongIterator iterator() {
        long[] snapshot = current();
        return new LongIterator() {
            private int index;
            @Override public boolean hasNext() { return index < snapshot.length; }
            @Override public long nextLong() {
                if (!hasNext()) throw new NoSuchElementException();
                return snapshot[index++];
            }
        };
    }
}
