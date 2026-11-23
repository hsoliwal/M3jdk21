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
package jdk.m3.collections.apache.map;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;

import jdk.m3.collections.apache.iterators.UnmodifiableIterator;
import jdk.m3.collections.apache.iterators.UnmodifiableListIterator;
import jdk.m3.collections.apache.list.UnmodifiableList;

/**
 * Lockstep reference: a verbatim copy of {@code LinkedMap.LinkedMapList} as it was before the
 * link-walk list iterator (Apache Commons Collections, Apache License 2.0), renamed. It extends the
 * running {@code java.util.AbstractList}, so its list iterator is the live {@code ListItr}.
 *
 * @param <K> The type of the keys
 */
final class ReferenceLinkedMapList<K> extends AbstractList<K> {

    private final LinkedMap<K, ?> parent;

    ReferenceLinkedMapList(final LinkedMap<K, ?> parent) {
        this.parent = parent;
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(final Object obj) {
        return parent.containsKey(obj);
    }

    @Override
    public boolean containsAll(final Collection<?> coll) {
        return parent.keySet().containsAll(coll);
    }

    @Override
    public K get(final int index) {
        return parent.get(index);
    }

    @Override
    public int indexOf(final Object obj) {
        return parent.indexOf(obj);
    }

    @Override
    public Iterator<K> iterator() {
        return UnmodifiableIterator.unmodifiableIterator(parent.keySet().iterator());
    }

    @Override
    public int lastIndexOf(final Object obj) {
        return parent.indexOf(obj);
    }

    @Override
    public ListIterator<K> listIterator() {
        return UnmodifiableListIterator.unmodifiableListIterator(super.listIterator());
    }

    @Override
    public ListIterator<K> listIterator(final int fromIndex) {
        return UnmodifiableListIterator.unmodifiableListIterator(super.listIterator(fromIndex));
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param index Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public K remove(final int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param obj Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public boolean remove(final Object obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param coll Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public boolean removeAll(final Collection<?> coll) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param filter Ignored.
     * @throws UnsupportedOperationException Always thrown.
     * @since 4.4
     */
    @Override
    public boolean removeIf(final Predicate<? super K> filter) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @param coll Ignored.
     * @throws UnsupportedOperationException Always thrown.
     */
    @Override
    public boolean retainAll(final Collection<?> coll) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return parent.size();
    }

    @Override
    public List<K> subList(final int fromIndexInclusive, final int toIndexExclusive) {
        return UnmodifiableList.unmodifiableList(
                super.subList(fromIndexInclusive, toIndexExclusive));
    }

    @Override
    public Object[] toArray() {
        return parent.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(final T[] array) {
        return parent.keySet().toArray(array);
    }
}
