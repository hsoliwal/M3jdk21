// Modified 2026 by Hitesh Soliwal and Synexia contributors: owned M3 array storage, pinned bounded helper closure and documented query-contract repairs; incremental value and logical-key counters with an epoch-guarded recount.
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m3.collections.apache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import com.m3.collections.M3ArrayList;
import com.m3.collections.M3LinkedHashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.Transformer;

/**
 * Abstract implementation of the {@link MultiValuedMap} interface to simplify
 * the creation of subclass implementations.
 * <p>
 * Subclasses specify a Map implementation to use as the internal storage.
 * </p>
 *
 * @param <K> the type of the keys in this map
 * @param <V> the type of the values in this map
 * @since 4.1
 */
@SuppressWarnings("deprecation") // Pinned Apache Transformer API remains the donor protocol.
public abstract class M3AbstractMultiValuedMap<K, V> implements MultiValuedMap<K, V> {

    /**
     * Inner class that provides the AsMap view.
     */
    private final class AsMap extends AbstractMap<K, Collection<V>> {
        final class AsMapEntrySet extends AbstractSet<Map.Entry<K, Collection<V>>> {

            @Override
            public void clear() {
                AsMap.this.clear();
            }

            @Override
            public boolean contains(final Object o) {
                return map.entrySet().contains(o);
            }

            @Override
            public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                return new AsMapEntrySetIterator(map.entrySet().iterator());
            }

            @Override
            public boolean remove(final Object o) {
                if (!contains(o)) {
                    return false;
                }
                final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                M3AbstractMultiValuedMap.this.remove(entry.getKey());
                return true;
            }

            @Override
            public int size() {
                return AsMap.this.size();
            }
        }

        /**
         * EntrySet iterator for the asMap view.
         */
        final class AsMapEntrySetIterator extends M3ApacheAbstractIteratorDecorator<Map.Entry<K, Collection<V>>> {

            AsMapEntrySetIterator(final Iterator<Map.Entry<K, Collection<V>>> iterator) {
                super(iterator);
            }

            @Override
            public Map.Entry<K, Collection<V>> next() {
                final Map.Entry<K, Collection<V>> entry = super.next();
                final K key = entry.getKey();
                return new M3ApacheUnmodifiableMapEntry<>(key, wrappedCollection(key));
            }
        }

        final transient Map<K, Collection<V>> map;

        AsMap(final Map<K, Collection<V>> map) {
            this.map = map;
        }

        @Override
        public void clear() {
            M3AbstractMultiValuedMap.this.clear();
        }

        @Override
        public boolean containsKey(final Object key) {
            return map.containsKey(key);
        }

        @Override
        public Set<Map.Entry<K, Collection<V>>> entrySet() {
            return new AsMapEntrySet();
        }

        @Override
        public boolean equals(final Object object) {
            return this == object || map.equals(object);
        }

        @Override
        public Collection<V> get(final Object key) {
            final Collection<V> collection = map.get(key);
            if (collection == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            final K k = (K) key;
            return wrappedCollection(k);
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }

        @Override
        public Set<K> keySet() {
            return M3AbstractMultiValuedMap.this.keySet();
        }

        @Override
        public Collection<V> remove(final Object key) {
            final Collection<V> collection = map.remove(key);
            if (collection == null || collection.isEmpty()) {
                return null;
            }

            final Collection<V> output = createCollection();
            output.addAll(collection);
            collection.clear();
            return output;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    /**
     * Inner class that provides the Entry<K, V> view
     */
    private final class EntryValues extends AbstractCollection<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new M3ApacheLazyIteratorChain<Entry<K, V>>() {

                final Collection<K> keysCol = new M3ArrayList<>(keySet());
                final Iterator<K> keyIterator = keysCol.iterator();

                @Override
                protected Iterator<? extends Entry<K, V>> nextIterator(final int count) {
                    if (!keyIterator.hasNext()) {
                        return null;
                    }
                    final K key = keyIterator.next();
                    final Transformer<V, Entry<K, V>> entryTransformer = input -> new MultiValuedMapEntry(key, input);
                    return new M3ApacheTransformIterator<>(new ValuesIterator(key), entryTransformer);
                }
            };
        }

        @Override
        public int size() {
            return M3AbstractMultiValuedMap.this.size();
        }

    }

    /**
     * Inner class that provides a MultiSet<K> keys view.
     */
    private final class KeysMultiSet extends M3ApacheAbstractMultiSet<K> {

        private final class MapEntryTransformer implements Transformer<Map.Entry<K, Collection<V>>, MultiSet.Entry<K>> {

            @Override
            public MultiSet.Entry<K> transform(final Map.Entry<K, Collection<V>> mapEntry) {
                final K capturedKey = mapEntry.getKey();
                final Collection<V> capturedValues = mapEntry.getValue();
                return new M3ApacheAbstractMultiSet.AbstractEntry<K>() {

                    @Override
                    public int getCount() {
                        return capturedValues.size();
                    }

                    @Override
                    public K getElement() {
                        return capturedKey;
                    }
                };
            }
        }

        @Override
        public boolean contains(final Object o) {
            return hasMappings(o);
        }

        @Override
        protected Iterator<MultiSet.Entry<K>> createEntrySetIterator() {
            final MapEntryTransformer transformer = new MapEntryTransformer();
            return M3ApacheIteratorUtils.transformedIterator(logicalMap().entrySet().iterator(), transformer);
        }

        @Override
        public int getCount(final Object object) {
            int count = 0;
            final Collection<V> col = M3AbstractMultiValuedMap.this.map.get(object);
            if (col != null) {
                count = col.size();
            }
            return count;
        }

        //FLLDCIM: ALG - LC992 logical key count read from nonEmptyGroups, no view allocation
        @Override
        public boolean isEmpty() {
            return M3AbstractMultiValuedMap.this.isEmpty();
        }

        @Override
        public int size() {
            return M3AbstractMultiValuedMap.this.size();
        }

        //FLLDCIM: ALG - LC992 logical key count read from nonEmptyGroups
        @Override
        protected int uniqueElements() {
            return countedGroups();
        }
    }

    /**
     * Inner class for MultiValuedMap Entries.
     */
    private final class MultiValuedMapEntry extends M3ApacheAbstractMapEntry<K, V> {

        MultiValuedMapEntry(final K key, final V value) {
            super(key, value);
        }

        @Override
        public V setValue(final V value) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Inner class for MapIterator.
     */
    private final class MultiValuedMapIterator implements MapIterator<K, V> {

        private final Iterator<Entry<K, V>> it;

        private Entry<K, V> current;

        MultiValuedMapIterator() {
            this.it = M3AbstractMultiValuedMap.this.entries().iterator();
        }

        @Override
        public K getKey() {
            if (current == null) {
                throw new IllegalStateException();
            }
            return current.getKey();
        }

        @Override
        public V getValue() {
            if (current == null) {
                throw new IllegalStateException();
            }
            return current.getValue();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public K next() {
            current = it.next();
            return current.getKey();
        }

        @Override
        public void remove() {
            it.remove();
        }

        @Override
        public V setValue(final V value) {
            if (current == null) {
                throw new IllegalStateException();
            }
            return current.setValue(value);
        }

    }

    /**
     * Inner class that provides the values view.
     */
    private final class Values extends AbstractCollection<V> {
        @Override
        public void clear() {
            M3AbstractMultiValuedMap.this.clear();
        }

        @Override
        public Iterator<V> iterator() {
            final M3ApacheIteratorChain<V> chain = new M3ApacheIteratorChain<>();
            for (final K k : keySet()) {
                chain.addIterator(new ValuesIterator(k));
            }
            return chain;
        }

        @Override
        public int size() {
            return M3AbstractMultiValuedMap.this.size();
        }
    }

    /**
     * Inner class that provides the values iterator.
     */
    private final class ValuesIterator implements Iterator<V> {
        private final Object key;
        private final Collection<V> values;
        private final Iterator<V> iterator;

        ValuesIterator(final Object key) {
            this.key = key;
            this.values = M3AbstractMultiValuedMap.this.map.get(key);
            this.iterator = values.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public V next() {
            return iterator.next();
        }

        //FLLDCIM: ALG - LC904 distinct-count: held group counted only while still attached
        @Override
        public void remove() {
            final long ticket = open();
            final int before = values.size();
            try {
                iterator.remove();
            } finally {
                closeHeld(ticket, key, values, before);
            }
            if (values.isEmpty()) {
                M3AbstractMultiValuedMap.this.remove(key);
            }
        }
    }

    /**
     * Wrapped collection to handle add and remove on the collection returned
     * by get(object).
     * <p>
     * Currently, the wrapped collection is not cached and has to be retrieved
     * from the underlying map. This is safe, but not very efficient and
     * should be improved in subsequent releases. For this purpose, the
     * scope of this collection is set to package private to simplify later
     * refactoring.
     */
    class WrappedCollection implements Collection<V> {

        protected final K key;

        WrappedCollection(final K key) {
            this.key = key;
        }

        //FLLDCIM: ALG - LC904 distinct-count: 0<->1 group transition counted at the wrapper write
        @Override
        public boolean add(final V value) {
            final long ticket = open();
            Collection<V> coll = null;
            int before = 0;
            try {
                coll = getMapping();
                if (coll == null) {
                    coll = createCollection();
                    putGroup(key, coll);
                } else {
                    before = coll.size();
                }
                return coll.add(value);
            } finally {
                close(ticket, before, coll == null ? 0 : coll.size());
            }
        }

        //FLLDCIM: ALG - LC904 distinct-count: bulk delta measured, partial failure included
        @Override
        public boolean addAll(final Collection<? extends V> other) {
            final long ticket = open();
            Collection<V> coll = null;
            int before = 0;
            try {
                coll = getMapping();
                if (coll == null) {
                    coll = createCollection();
                    putGroup(key, coll);
                } else {
                    before = coll.size();
                }
                return coll.addAll(other);
            } finally {
                close(ticket, before, coll == null ? 0 : coll.size());
            }
        }

        //FLLDCIM: ALG - LC904 distinct-count: group emptied, then detached
        @Override
        public void clear() {
            final long ticket = open();
            Collection<V> coll = null;
            int before = 0;
            try {
                coll = getMapping();
                if (coll == null) {
                    return;
                }
                before = coll.size();
                coll.clear();
            } finally {
                close(ticket, before, coll == null ? 0 : coll.size());
            }
            M3AbstractMultiValuedMap.this.remove(key);
        }

        @Override
        public boolean contains(final Object obj) {
            final Collection<V> coll = getMapping();
            return coll != null && coll.contains(obj);
        }

        @Override
        public boolean containsAll(final Collection<?> other) {
            final Collection<V> coll = getMapping();
            Objects.requireNonNull(other, "other");
            return coll == null ? java.util.Collections.emptyList().containsAll(other) : coll.containsAll(other);
        }

        //FLLDCIM: ALG - owned read: does not open the getMap() epoch guard
        protected Collection<V> getMapping() {
            return M3AbstractMultiValuedMap.this.map.get(key);
        }

        @Override
        public boolean isEmpty() {
            final Collection<V> coll = getMapping();
            return coll == null || coll.isEmpty();
        }

        @Override
        public Iterator<V> iterator() {
            final Collection<V> coll = getMapping();
            if (coll == null) {
                return M3ApacheEmptyIterator.emptyIterator();
            }
            return new ValuesIterator(key);
        }

        //FLLDCIM: ALG - LC904 distinct-count: 1->0 transition counted before the detach
        @Override
        public boolean remove(final Object item) {
            final long ticket = open();
            Collection<V> coll = null;
            int before = 0;
            final boolean result;
            try {
                coll = getMapping();
                if (coll == null) {
                    return false;
                }
                before = coll.size();
                result = coll.remove(item);
            } finally {
                close(ticket, before, coll == null ? 0 : coll.size());
            }
            if (coll.isEmpty()) {
                M3AbstractMultiValuedMap.this.remove(key);
            }
            return result;
        }

        //FLLDCIM: ALG - LC904 distinct-count: bulk delta measured, partial failure included
        @Override
        public boolean removeAll(final Collection<?> c) {
            final long ticket = open();
            Collection<V> coll = null;
            int before = 0;
            final boolean result;
            try {
                coll = getMapping();
                before = coll == null ? 0 : coll.size();
                Objects.requireNonNull(c, "c");
                if (coll == null) {
                    return false;
                }
                result = coll.removeAll(c);
            } finally {
                close(ticket, before, coll == null ? 0 : coll.size());
            }
            if (coll.isEmpty()) {
                M3AbstractMultiValuedMap.this.remove(key);
            }
            return result;
        }

        //FLLDCIM: ALG - LC904 distinct-count: bulk delta measured, partial failure included
        @Override
        public boolean retainAll(final Collection<?> c) {
            final long ticket = open();
            Collection<V> coll = null;
            int before = 0;
            final boolean result;
            try {
                coll = getMapping();
                before = coll == null ? 0 : coll.size();
                Objects.requireNonNull(c, "c");
                if (coll == null) {
                    return false;
                }
                result = coll.retainAll(c);
            } finally {
                close(ticket, before, coll == null ? 0 : coll.size());
            }
            if (coll.isEmpty()) {
                M3AbstractMultiValuedMap.this.remove(key);
            }
            return result;
        }

        @Override
        public int size() {
            final Collection<V> coll = getMapping();
            return coll == null ? 0 : coll.size();
        }

        @Override
        public Object[] toArray() {
            final Collection<V> coll = getMapping();
            if (coll == null) {
                return M3ApacheCollectionUtils.EMPTY_COLLECTION.toArray();
            }
            return coll.toArray();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(final T[] a) {
            final Collection<V> coll = getMapping();
            if (coll == null) {
                return M3ApacheCollectionUtils.EMPTY_COLLECTION.toArray(a);
            }
            return coll.toArray(a);
        }

        @Override
        public String toString() {
            final Collection<V> coll = getMapping();
            if (coll == null) {
                return M3ApacheCollectionUtils.EMPTY_COLLECTION.toString();
            }
            return coll.toString();
        }

    }

    /** The values view */
    private transient Collection<V> valuesView;

    /** The EntryValues view */
    private transient EntryValues entryValuesView;

    /** The KeyMultiSet view */
    private transient MultiSet<K> keysMultiSetView;

    /** The AsMap view */
    private transient AsMap asMapView;

    /** The map used to store the data */
    private transient Map<K, Collection<V>> map;

    /** The cached logical (non-empty group) view; it holds no state of its own. */
    private transient LogicalGroups logicalView;

    //FLLDCIM: ALG - LC904 distinct-count: two ints replace the O(#keys) scans of size()/isEmpty()/keySet().size()
    /** Sum of the attached group sizes; valid only while {@link #exact} holds. */
    private transient int totalValues;

    /** Attached groups holding at least one value: the logical key count (private empty owners excluded). */
    private transient int nonEmptyGroups;

    //FLLDCIM: ALG - epoch guard: every counted write, recount and raw directory exposure advances the epoch
    /** Re-entrancy witness: a counted write applies its delta only if no other write, recount or exposure intervened. */
    private transient long epoch;

    /** Owned writes in progress; reads made meanwhile (from callbacks) recount instead of trusting the counters. */
    private transient int inFlight;

    /** True while the counters equal a recount of the directory. */
    private transient boolean exact;

    /**
     * Owned final leaves write the directory only through the counted mutation points of this
     * package. A foreign subclass may keep the map it passed to the wrapping constructor or got
     * from {@link #getMap()}/{@link #setMap(Map)}, so its reads always recount.
     */
    private final transient boolean ownedLeaf = ownedLeaf(getClass());

    /**
     * Constructor needed for subclass serialization.
     */
    protected M3AbstractMultiValuedMap() {
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param map  the map to wrap, must not be null
     * @throws NullPointerException if the map is null
     */
    @SuppressWarnings("unchecked")
    protected M3AbstractMultiValuedMap(final Map<K, ? extends Collection<V>> map) {
        this.map = (Map<K, Collection<V>>) Objects.requireNonNull(map, "map");
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return asMapView != null ? asMapView : (asMapView = new AsMap(logicalMap()));
    }

    //FLLDCIM: ALG - clear resets both counters to an exact zero
    @Override
    public void clear() {
        open();
        try {
            map.clear();
        } finally {
            closeCleared();
        }
    }

    @Override
    public boolean containsKey(final Object key) {
        return hasMappings(key);
    }

    @Override
    public boolean containsMapping(final Object key, final Object value) {
        final Collection<V> coll = map.get(key);
        return coll != null && coll.contains(value);
    }

    @Override
    public boolean containsValue(final Object value) {
        return values().contains(value);
    }

    /**
     * Creates a new Collection typed for a given subclass.
     *
     * @return a new Collection typed for a given subclass.
     */
    protected abstract Collection<V> createCollection();

    /**
     * Reads the map in using a custom routine.
     *
     * @param in the input stream
     * @throws IOException any of the usual I/O related exceptions
     * @throws ClassNotFoundException if the stream contains an object which class cannot be loaded
     * @throws ClassCastException if the stream does not contain the correct objects
     */
    protected void doReadObject(final ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        final int entrySize = in.readInt();
        for (int i = 0; i < entrySize; i++) {
            @SuppressWarnings("unchecked") // This will fail at runtime if the stream is incorrect
            final K key = (K) in.readObject();
            final Collection<V> values = get(key);
            final int valueSize = in.readInt();
            for (int j = 0; j < valueSize; j++) {
                @SuppressWarnings("unchecked") // see above
                final V value = (V) in.readObject();
                values.add(value);
            }
        }
        //FLLDCIM: ALG - transient counters rebuilt once after deserialization; the wire form is unchanged
        recount();
    }

    /**
     * Writes the map out using a custom routine.
     *
     * @param out the output stream
     * @throws IOException any of the usual I/O related exceptions
     */
    protected void doWriteObject(final ObjectOutputStream out) throws IOException {
        final Map<K, Collection<V>> visible = logicalMap();
        out.writeInt(visible.size());
        for (final Map.Entry<K, Collection<V>> entry : visible.entrySet()) {
            out.writeObject(entry.getKey());
            out.writeInt(entry.getValue().size());
            for (final V value : entry.getValue()) {
                out.writeObject(value);
            }
        }
    }

    @Override
    public Collection<Entry<K, V>> entries() {
        return entryValuesView != null ? entryValuesView : (entryValuesView = new EntryValues());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MultiValuedMap) {
            return asMap().equals(((MultiValuedMap<?, ?>) obj).asMap());
        }
        return false;
    }

    /**
     * Gets the collection of values associated with the specified key. This
     * would return an empty collection in case the mapping is not present
     *
     * @param key the key to retrieve
     * @return the {@code Collection} of values, will return an empty {@code Collection} for no mapping
     */
    @Override
    public Collection<V> get(final K key) {
        return wrappedCollection(key);
    }

    /**
     * Gets the map being wrapped.
     * <p>
     * The caller may write the returned map directly, so this call invalidates the value and
     * key counters; the next size query performs one validated O(#keys) recount.
     *
     * @return the wrapped map
     */
    //FLLDCIM: ALG - epoch guard: raw directory handed to a subclass invalidates the counters
    protected Map<K, ? extends Collection<V>> getMap() {
        expose();
        return map;
    }

    @Override
    public int hashCode() {
        return asMap().hashCode();
    }

    //FLLDCIM: ALG - LC992 logical emptiness from nonEmptyGroups in O(1), no view allocation
    @Override
    public boolean isEmpty() {
        return countedGroups() == 0;
    }

    /**
     * Returns a {@link MultiSet} view of the key mapping contained in this map.
     * <p>
     * Returns a MultiSet of keys with its values count as the count of the MultiSet.
     * This multiset is backed by the map, so any changes in the map is reflected here.
     * Any method which modifies this multiset like {@code add}, {@code remove},
     * {@link Iterator#remove()} etc throws {@code UnsupportedOperationException}.
     *
     * @return a bag view of the key mapping contained in this map
     */
    @Override
    public MultiSet<K> keys() {
        if (keysMultiSetView == null) {
            keysMultiSetView = M3ApacheUnmodifiableMultiSet.unmodifiableMultiSet(new KeysMultiSet());
        }
        return keysMultiSetView;
    }

    @Override
    public Set<K> keySet() {
        return logicalMap().keySet();
    }

    @Override
    public MapIterator<K, V> mapIterator() {
        if (isEmpty()) {
            return M3ApacheEmptyMapIterator.emptyMapIterator();
        }
        return new MultiValuedMapIterator();
    }

    /**
     * Adds the value to the collection associated with the specified key.
     * <p>
     * Unlike a normal {@code Map} the previous value is not replaced.
     * Instead the new value is added to the collection stored against the key.
     *
     * @param key the key to store against
     * @param value the value to add to the collection at the key
     * @return the value added if the map changed and null if the map did not change
     */
    //FLLDCIM: ALG - LC904 distinct-count: a new group is counted only once it is attached
    @Override
    public boolean put(final K key, final V value) {
        final long ticket = open();
        Collection<V> coll = null;
        int before = 0;
        try {
            final Collection<V> mapped = map.get(key);
            if (mapped == null) {
                final Collection<V> created = createCollection();
                if (created.add(value)) {
                    putGroup(key, created);
                    coll = created;
                    return true;
                }
                return false;
            }
            coll = mapped;
            before = coll.size();
            return coll.add(value);
        } finally {
            close(ticket, before, coll == null ? 0 : coll.size());
        }
    }

    /**
     * Adds Iterable values to the collection associated with the specified key.
     *
     * @param key the key to store against
     * @param values the values to add to the collection at the key, may not be null
     * @return true if this map changed
     * @throws NullPointerException if values is null
     */
    @Override
    public boolean putAll(final K key, final Iterable<? extends V> values) {
        Objects.requireNonNull(values, "values");

        if (values instanceof Collection<?>) {
            final Collection<? extends V> valueCollection = (Collection<? extends V>) values;
            return !valueCollection.isEmpty() && get(key).addAll(valueCollection);
        }
        final Iterator<? extends V> it = values.iterator();
        return it.hasNext() && M3ApacheCollectionUtils.addAll(get(key), it);
    }

    /**
     * Copies all of the mappings from the specified map to this map. The effect
     * of this call is equivalent to that of calling {@link #put(Object,Object)
     * put(k, v)} on this map once for each mapping from key {@code k} to value
     * {@code v} in the specified map. The behavior of this operation is
     * undefined if the specified map is modified while the operation is in
     * progress.
     *
     * @param map mappings to be stored in this map, may not be null
     * @return true if the map changed as a result of this operation
     * @throws NullPointerException if map is null
     */
    @Override
    public boolean putAll(final Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map");
        boolean changed = false;
        for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            changed |= put(entry.getKey(), entry.getValue());
        }
        return changed;
    }

    /**
     * Copies all of the mappings from the specified MultiValuedMap to this map.
     * The effect of this call is equivalent to that of calling
     * {@link #put(Object,Object) put(k, v)} on this map once for each mapping
     * from key {@code k} to value {@code v} in the specified map. The
     * behavior of this operation is undefined if the specified map is modified
     * while the operation is in progress.
     *
     * @param map mappings to be stored in this map, may not be null
     * @return true if the map changed as a result of this operation
     * @throws NullPointerException if map is null
     */
    @Override
    public boolean putAll(final MultiValuedMap<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map");
        boolean changed = false;
        for (final Map.Entry<? extends K, ? extends V> entry : map.entries()) {
            changed |= put(entry.getKey(), entry.getValue());
        }
        return changed;
    }

    /**
     * Removes all values associated with the specified key.
     * <p>
     * A subsequent {@code get(Object)} would return an empty collection.
     *
     * @param key  the key to remove values from
     * @return the {@code Collection} of values removed, will return an
     *   empty, unmodifiable collection for no mapping found
     */
    @Override
    public Collection<V> remove(final Object key) {
        return M3ApacheCollectionUtils.emptyIfNull(detach(key));
    }

    /**
     * Removes a specific key/value mapping from the multivalued map.
     * <p>
     * The value is removed from the collection mapped to the specified key.
     * Other values attached to that key are unaffected.
     * <p>
     * If the last value for a key is removed, an empty collection would be
     * returned from a subsequent {@link #get(Object)}.
     *
     * @param key the key to remove from
     * @param value the value to remove
     * @return true if the mapping was removed, false otherwise
     */
    //FLLDCIM: ALG - LC904 distinct-count: 1->0 transition counted, empty group then detached
    @Override
    public boolean removeMapping(final Object key, final Object value) {
        final long ticket = open();
        Collection<V> coll = null;
        int before = 0;
        final boolean changed;
        try {
            coll = map.get(key);
            if (coll == null) {
                return false;
            }
            before = coll.size();
            changed = coll.remove(value);
        } finally {
            close(ticket, before, coll == null ? 0 : coll.size());
        }
        if (coll.isEmpty()) {
            detach(key);
        }
        return changed;
    }

    /**
     * Sets the map being wrapped.
     * <p>
     * <strong>NOTE:</strong> this method should only be used during deserialization.
     * The counters are invalidated and rebuilt by the next size query or by
     * {@link #doReadObject(ObjectInputStream)}.
     *
     * @param map the map to wrap
     */
    //FLLDCIM: ALG - epoch guard: a replaced directory invalidates the counters
    @SuppressWarnings("unchecked")
    protected void setMap(final Map<K, ? extends Collection<V>> map) {
        this.map = (Map<K, Collection<V>>) map;
        expose();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation caches the total size. Every owned modification (the map,
     * its wrapped collections, sub-list views and the entry/value/key-set views)
     * updates the cached total and the logical key count by the size change it made.
     * After {@link #getMap()} or {@link #setMap(Map)}, or in a subclass not owned by
     * this package, the total is recomputed by iterating over the groups.
     */
    //FLLDCIM: ALG - LC904 distinct-count: O(1) read of totalValues
    @Override
    public int size() {
        return countedValues();
    }

    @Override
    public String toString() {
        return asMap().toString();
    }

    /**
     * Gets a collection containing all the values in the map.
     * <p>
     * Returns a collection containing all the values from all keys.
     *
     * @return a collection view of the values contained in this map
     */
    @Override
    public Collection<V> values() {
        final Collection<V> vs = valuesView;
        return vs != null ? vs : (valuesView = new Values());
    }

    Collection<V> wrappedCollection(final K key) {
        return new WrappedCollection(key);
    }


    /** Empty owners remain private so raw list aliases can clear and refill them. */
    private boolean hasMappings(final Object key) {
        final Collection<V> group = map.get(key);
        return group != null && !group.isEmpty();
    }

    //FLLDCIM: ALG - one cached logical view instead of one allocation per query
    private Map<K, Collection<V>> logicalMap() {
        final LogicalGroups view = logicalView;
        return view != null ? view : (logicalView = new LogicalGroups());
    }

    private final class LogicalGroups extends AbstractMap<K, Collection<V>> {
        @Override public Collection<V> get(final Object key) {
            final Collection<V> group = map.get(key);
            return group == null || group.isEmpty() ? null : group;
        }
        @Override public boolean containsKey(final Object key) { return hasMappings(key); }
        @Override public void clear() { M3AbstractMultiValuedMap.this.clear(); }
        //FLLDCIM: ALG - LC992 logical key count read from nonEmptyGroups
        @Override public int size() { return countedGroups(); }
        @Override public Set<K> keySet() {
            return new AbstractSet<>() {
                @Override public int size() { return LogicalGroups.this.size(); }
                @Override public boolean contains(final Object key) { return hasMappings(key); }
                @Override public void clear() { LogicalGroups.this.clear(); }
                @Override public boolean remove(final Object key) {
                    if (!hasMappings(key)) return false;
                    detach(key);
                    return true;
                }
                @Override public Iterator<K> iterator() {
                    final Iterator<Map.Entry<K, Collection<V>>> entries = groupIterator();
                    return new Iterator<>() {
                        @Override public boolean hasNext() { return entries.hasNext(); }
                        @Override public K next() { return entries.next().getKey(); }
                        @Override public void remove() { entries.remove(); }
                    };
                }
            };
        }
        @Override public Set<Map.Entry<K, Collection<V>>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return LogicalGroups.this.size(); }
                @Override public void clear() { LogicalGroups.this.clear(); }
                @Override public boolean contains(final Object object) {
                    if (!(object instanceof Map.Entry<?, ?> entry)) return false;
                    final Collection<V> group = LogicalGroups.this.get(entry.getKey());
                    return group != null && Objects.equals(group, entry.getValue());
                }
                @Override public boolean remove(final Object object) {
                    if (!contains(object)) return false;
                    detach(((Map.Entry<?, ?>) object).getKey());
                    return true;
                }
                @Override public Iterator<Map.Entry<K, Collection<V>>> iterator() { return groupIterator(); }
            };
        }
        private Iterator<Map.Entry<K, Collection<V>>> groupIterator() {
            final Iterator<Map.Entry<K, Collection<V>>> source = map.entrySet().iterator();
            return new Iterator<>() {
                // Cursor-local quota avoids prefetching beyond the last delivered entry.
                // As with the backing directory, modification outside this cursor is
                // unsupported while traversing it; group aliases are not a size cache.
                //FLLDCIM: ALG - LC992 quota now starts from nonEmptyGroups in O(1)
                private int remaining = LogicalGroups.this.size();
                /** Group of the entry the source cursor returned last (the one remove() detaches). */
                private Collection<V> last;
                @Override public boolean hasNext() {
                    final boolean underlying = source.hasNext();
                    return remaining > 0 && underlying;
                }
                @Override public Map.Entry<K, Collection<V>> next() {
                    if (remaining <= 0) throw new java.util.NoSuchElementException();
                    while (source.hasNext()) {
                        final Map.Entry<K, Collection<V>> entry = source.next();
                        last = entry.getValue();
                        if (!last.isEmpty()) { remaining--; return entry; }
                    }
                    throw new java.util.NoSuchElementException();
                }
                //FLLDCIM: ALG - LC904 distinct-count: cursor detach counts the whole group out
                @Override public void remove() {
                    final long ticket = open();
                    int removed = 0;
                    try {
                        source.remove();
                        removed = last.size();
                    } finally {
                        close(ticket, removed, 0);
                    }
                }
            };
        }
    }

    //FLLDCIM: ALG - epoch guard: only the owned final leaves keep exact counters between reads
    private static boolean ownedLeaf(final Class<?> type) {
        return type == M3ListValuedMap.class || type == M3SetValuedMap.class;
    }

    /** Owned read of one group; unlike {@link #getMap()} it does not invalidate the counters. */
    final Collection<V> group(final Object key) {
        return map.get(key);
    }

    /**
     * Attaches a group the caller found absent. Its values are counted by the enclosing
     * {@link #open()}/{@link #close(long, int, int)} pair; a replaced group (possible only
     * through re-entrant callbacks) invalidates the counters instead.
     */
    final void putGroup(final K key, final Collection<V> group) {
        if (map.put(key, group) != null) {
            expose();
        }
    }

    /** Removes the group mapped to {@code key} from the directory and counts its values out. */
    //FLLDCIM: ALG - LC904 distinct-count: detached group leaves both counters
    final Collection<V> detach(final Object key) {
        final long ticket = open();
        Collection<V> removed = null;
        try {
            removed = map.remove(key);
        } finally {
            close(ticket, removed == null ? 0 : removed.size(), 0);
        }
        return removed;
    }

    /**
     * Opens a counted owned write. Until the matching close, the counters are marked inexact so
     * a read from a callback recounts the directory. The returned ticket is the current epoch if
     * the counters were exact, otherwise -1.
     */
    final long open() {
        final long ticket = exact ? epoch : -1L;
        exact = false;
        inFlight++;
        return ticket;
    }

    /**
     * Closes a counted owned write on one attached group that held {@code before} values and now
     * holds {@code after}. If another write, a recount or a raw directory exposure happened since
     * {@link #open()}, the epoch differs and the counters stay inexact for one validated recount.
     */
    //FLLDCIM: ALG - LC904 distinct-count: 0<->1 group-size transitions move nonEmptyGroups
    final void close(final long ticket, final int before, final int after) {
        inFlight--;
        if (ticket == epoch) {
            totalValues += after - before;
            nonEmptyGroups += (after > 0 ? 1 : 0) - (before > 0 ? 1 : 0);
            exact = inFlight == 0;
        }
        epoch++;
    }

    /** Closes a counted write on a group obtained earlier; a group no longer attached counts nothing. */
    final void closeHeld(final long ticket, final Object key, final Collection<V> group, final int before) {
        final boolean attached = map.get(key) == group;
        close(ticket, before, attached ? group.size() : before);
    }

    /** Closes {@link #clear()}: an empty directory has exact zero counters whatever the history. */
    private void closeCleared() {
        inFlight--;
        if (map.isEmpty()) {
            totalValues = 0;
            nonEmptyGroups = 0;
            exact = ownedLeaf && inFlight == 0;
        }
        epoch++;
    }

    /** The raw directory left owned code (or was replaced): the next read recounts. */
    private void expose() {
        exact = false;
        epoch++;
    }

    //FLLDCIM: ALG - epoch guard: validated O(#keys) recount, the only non-O(1) read path
    private void recount() {
        int values = 0;
        int groups = 0;
        for (final Collection<V> group : map.values()) {
            final int count = group.size();
            values += count;
            if (count > 0) {
                groups++;
            }
        }
        totalValues = values;
        nonEmptyGroups = groups;
        exact = ownedLeaf && inFlight == 0;
        epoch++;
    }

    private int countedValues() {
        if (!exact) {
            recount();
        }
        return totalValues;
    }

    private int countedGroups() {
        if (!exact) {
            recount();
        }
        return nonEmptyGroups;
    }

    protected static <A, B> M3LinkedHashMap<A, B> newDirectory(final int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("negative initialMapCapacity");
        return new M3LinkedHashMap<>();
    }

}
