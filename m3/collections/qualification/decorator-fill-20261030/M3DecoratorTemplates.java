/*
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
 */

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * DECORATOR_FILL helper templates: the nested helper classes the generator emits where no JDK
 * owner fits (H2 MAP_ITERATOR_ADAPTER, H4 UNMODIFIABLE_MAP_ITERATOR, H3 CHECKED_ENTRY_SET). Data
 * only: each atom is a text block that {@code M3DecoratorFill} selects and, for H4, instantiates
 * with {@code formatted}.
 */
final class M3DecoratorTemplates {
    private M3DecoratorTemplates() {
    }

    /**
     * H2 template (MAP_ITERATOR_ADAPTER): the EntrySetToMapIteratorAdapter contract over this map's
     * entrySet().
     */
    static final String MAP_ITERATOR_HELPER = """

                /** MAP_ITERATOR_ADAPTER helper (no JDK owner): a MapIterator over an entry set, \
            the EntrySetToMapIteratorAdapter contract. */
                private static final class EntrySetMapIterator<K, V>
                        implements org.apache.commons.collections4.MapIterator<K, V>, \
            org.apache.commons.collections4.ResettableIterator<K> {
                    private final java.util.Set<java.util.Map.Entry<K, V>> entrySet;
                    private java.util.Iterator<java.util.Map.Entry<K, V>> iterator;
                    private java.util.Map.Entry<K, V> entry;

                    EntrySetMapIterator(final java.util.Set<java.util.Map.Entry<K, V>> entrySet) {
                        this.entrySet = entrySet;
                        reset();
                    }

                    private synchronized java.util.Map.Entry<K, V> current() {
                        if (entry == null) {
                            throw new IllegalStateException();
                        }
                        return entry;
                    }

                    @Override
                    public K getKey() {
                        return current().getKey();
                    }

                    @Override
                    public V getValue() {
                        return current().getValue();
                    }

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public K next() {
                        entry = iterator.next();
                        return getKey();
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                        entry = null;
                    }

                    @Override
                    public synchronized void reset() {
                        iterator = entrySet.iterator();
                    }

                    @Override
                    public V setValue(final V value) {
                        return current().setValue(value);
                    }
                }
            """;

    /**
     * v1.6 H4 template (UNMODIFIABLE_MAP_ITERATOR): the UnmodifiableMapIterator contract, or with
     * {@code %1$s = OrderedMapIterator} and the previous-direction members in {@code %2$s}, the
     * UnmodifiableOrderedMapIterator contract. Reads delegate; remove and setValue are rejected.
     */
    static final String UNMODIFIABLE_MAP_ITERATOR_HELPER = """

                /**
                 * UNMODIFIABLE_MAP_ITERATOR helper (no JDK owner): a %1$s that delegates reads \
            and rejects
                 * remove and setValue, the Unmodifiable%1$s contract.
                 */
                private static final class Unmodifiable%1$sView<K, V>
                        implements org.apache.commons.collections4.%1$s<K, V> {
                    private final org.apache.commons.collections4.%1$s<? extends K, ? extends V> \
            iterator;

                    Unmodifiable%1$sView(
                            final org.apache.commons.collections4.%1$s<? extends K, ? extends V> \
            iterator) {
                        this.iterator = java.util.Objects.requireNonNull(iterator, "iterator");
                    }

                    @Override
                    public K getKey() {
                        return iterator.getKey();
                    }

                    @Override
                    public V getValue() {
                        return iterator.getValue();
                    }

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }
            %2$s
                    @Override
                    public K next() {
                        return iterator.next();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("remove() is not supported");
                    }

                    @Override
                    public V setValue(final V value) {
                        throw new UnsupportedOperationException("setValue() is not supported");
                    }
                }
            """;

    /** The previous-direction members of the ordered H4 helper. */
    static final String ORDERED_MAP_ITERATOR_MEMBERS = """

                    @Override
                    public boolean hasPrevious() {
                        return iterator.hasPrevious();
                    }

                    @Override
                    public K previous() {
                        return iterator.previous();
                    }
            """;

    /**
     * H3 template (CHECKED_ENTRY_SET): entry set view whose entries pass setValue through
     * checkSetValue.
     */
    static final String CHECKED_ENTRY_SET_HELPER = """

                /**
                 * CHECKED_ENTRY_SET helper (no JDK owner): entry set view whose entries check \
            setValue, the
                 * AbstractInputCheckedMapDecorator.EntrySet contract. The element type is an \
            unbounded type variable so
                 * add(Object) erases without a checkcast bridge, as in Commons.
                 */
                private final class CheckedEntrySet<X> implements java.util.Set<X>, \
            java.io.Serializable {
                    @java.io.Serial
                    private static final long serialVersionUID = 1L;
                    @SuppressWarnings("serial")
                    private final java.util.Set<X> entries;

                    CheckedEntrySet(final java.util.Set<X> entries) {
                        this.entries = java.util.Objects.requireNonNull(entries, "collection");
                    }

                    @Override
                    public boolean add(final X object) {
                        return entries.add(object);
                    }

                    @Override
                    public boolean addAll(final java.util.Collection<? extends X> coll) {
                        return entries.addAll(coll);
                    }

                    @Override
                    public void clear() {
                        entries.clear();
                    }

                    @Override
                    public boolean contains(final Object object) {
                        return entries.contains(object);
                    }

                    @Override
                    public boolean containsAll(final java.util.Collection<?> coll) {
                        return entries.containsAll(coll);
                    }

                    @Override
                    public boolean equals(final Object object) {
                        return object == this || entries.equals(object);
                    }

                    @Override
                    public int hashCode() {
                        return entries.hashCode();
                    }

                    @Override
                    public boolean isEmpty() {
                        return entries.isEmpty();
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public java.util.Iterator<X> iterator() {
                        return (java.util.Iterator<X>) new \
            CheckedEntryIterator((java.util.Iterator<java.util.Map.Entry<K, V>>) \
            entries.iterator());
                    }

                    @Override
                    public boolean remove(final Object object) {
                        return entries.remove(object);
                    }

                    @Override
                    public boolean removeAll(final java.util.Collection<?> coll) {
                        return entries.removeAll(coll);
                    }

                    @Override
                    public boolean removeIf(final java.util.function.Predicate<? super X> filter) {
                        return entries.removeIf(filter);
                    }

                    @Override
                    public boolean retainAll(final java.util.Collection<?> coll) {
                        return entries.retainAll(coll);
                    }

                    @Override
                    public int size() {
                        return entries.size();
                    }

                    @Override
                    public Object[] toArray() {
                        final Object[] array = entries.toArray();
                        java.util.Arrays.setAll(array, i -> wrap(array[i]));
                        return array;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T[] toArray(final T[] array) {
                        final Object[] result = entries.toArray(array.length > 0
                                ? (Object[]) \
            java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), 0) : array);
                        java.util.Arrays.setAll(result, i -> wrap(result[i]));
                        if (result.length > array.length) {
                            return (T[]) result;
                        }
                        System.arraycopy(result, 0, array, 0, result.length);
                        if (array.length > result.length) {
                            array[result.length] = null;
                        }
                        return array;
                    }

                    @Override
                    public String toString() {
                        return entries.toString();
                    }

                    @SuppressWarnings("unchecked")
                    private Object wrap(final Object entry) {
                        return new CheckedEntry((java.util.Map.Entry<K, V>) entry);
                    }
                }

                /** CHECKED_ENTRY_SET helper: iterator of {@link CheckedEntrySet}; every returned \
            entry checks setValue. */
                private final class CheckedEntryIterator implements \
            java.util.Iterator<java.util.Map.Entry<K, V>> {
                    private final java.util.Iterator<java.util.Map.Entry<K, V>> iterator;

                    CheckedEntryIterator(final java.util.Iterator<java.util.Map.Entry<K, V>> \
            iterator) {
                        this.iterator = java.util.Objects.requireNonNull(iterator, "iterator");
                    }

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public java.util.Map.Entry<K, V> next() {
                        return new CheckedEntry(iterator.next());
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                }

                /** CHECKED_ENTRY_SET helper: boundary flyweight entry; setValue passes the value \
            lane first. */
                private final class CheckedEntry implements java.util.Map.Entry<K, V>, \
            org.apache.commons.collections4.KeyValue<K, V> {
                    private final java.util.Map.Entry<K, V> entry;

                    CheckedEntry(final java.util.Map.Entry<K, V> entry) {
                        this.entry = java.util.Objects.requireNonNull(entry, "entry");
                    }

                    @Override
                    public boolean equals(final Object object) {
                        return object == this || entry.equals(object);
                    }

                    @Override
                    public K getKey() {
                        return entry.getKey();
                    }

                    @Override
                    public V getValue() {
                        return entry.getValue();
                    }

                    @Override
                    public int hashCode() {
                        return entry.hashCode();
                    }

                    @Override
                    public V setValue(final V value) {
                        return entry.setValue(checkSetValue(value));
                    }

                    @Override
                    public String toString() {
                        return entry.toString();
                    }
                }
            """;
}
