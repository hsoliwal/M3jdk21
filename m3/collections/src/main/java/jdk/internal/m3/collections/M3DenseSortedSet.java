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

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Immutable sorted set backed by one Object[] and no node object per element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseSortedSet.java}
 * blob {@code b08aca15dba0f16243bbff6c6ab5fd16102832f0} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3DenseSortedSet<E extends Comparable<? super E>>
        extends AbstractSet<E> {
    private static final M3DenseSortedSet<?> EMPTY = new M3DenseSortedSet<>(new Object[0]);
    private final Object[] values;

    private M3DenseSortedSet(Object[] values) { this.values = values; }

    @SuppressWarnings("unchecked")
    public static <E extends Comparable<? super E>> M3DenseSortedSet<E> empty() {
        return (M3DenseSortedSet<E>) EMPTY;
    }

    public static <E extends Comparable<? super E>> M3DenseSortedSet<E> copyOf(
            Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) return empty();
        Object[] sorted = source.toArray();
        Arrays.sort(sorted);
        int size = 0;
        Object previous = null;
        for (Object value : sorted) {
            Objects.requireNonNull(value, "null element");
            if (size == 0 || !value.equals(previous)) {
                sorted[size++] = value;
                previous = value;
            }
        }
        return new M3DenseSortedSet<>(size == sorted.length ? sorted : Arrays.copyOf(sorted, size));
    }

    @Override public int size() { return values.length; }
    @Override public boolean isEmpty() { return values.length == 0; }

    @Override public boolean contains(Object value) {
        if (value == null) return false;
        try {
            @SuppressWarnings("unchecked") E typed = (E) value;
            return Arrays.binarySearch(values, typed) >= 0;
        } catch (ClassCastException incompatible) {
            return false;
        }
    }

    @Override public Iterator<E> iterator() {
        return new Iterator<>() {
            private int index;
            @Override public boolean hasNext() { return index < values.length; }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                @SuppressWarnings("unchecked") E value = (E) values[index++];
                return value;
            }
        };
    }
}
