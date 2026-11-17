/*
 * Copyright 2013 by Thomas Mauch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */
package jdk.m3.collections.brownies;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import jdk.m3.collections.brownies.KeyCollectionImpl.Key1CollectionImplBuilder;

/**
 * Key1List implements a key list with 1 key.
 * This key can be accessed fast.
 * It can provide fast access to its elements like a Set.
 * The elements allowed in the list can be constraint (null/duplicate values).
 *
 * @author Thomas Mauch
 *
 * @param <E> type of elements stored in the list
 * @param <K> type of key
 */
@SuppressWarnings({"serial", "this-escape"})
public class Key1List<E, K> extends KeyListImpl<E> {

    /** Build {@link Key1List} with specified builder function */
    public static <E, K> Key1List<E, K> buildList(UnaryOperator<Key1List.Builder<E, K>> builder) {
        return builder.apply(new Key1List.Builder<>()).build();
    }

    /**
     * Builder to construct {@link Key1List} instances.
     */
    public static class Builder<E, K> extends Key1CollectionImplBuilder<E, K, Builder<E, K>> {
        /**
         * Default constructor.
         */
        public Builder() {
            this(null);
        }

        /**
         * Private constructor used if extending Key1List.
         *
         * @param keyList   key list
         */
        Builder(Key1List<E, K> keyList) {
            this.keyList = keyList;
            initKeyMapBuilder(1);
        }

        /**
         * @return created list
         */
        @SuppressWarnings("unchecked")
        public Key1List<E, K> build() {
            if (keyColl == null) {
                keyColl = new KeyCollectionImpl<E>();
            }
            build(keyColl, true);
            if (keyList == null) {
                keyList = new Key1List<E, K>();
            }
            init(keyColl, keyList);
            return (Key1List<E, K>) keyList;
        }

        /**
         * @return created immutable list
         */
        public Key1List<E, K> immutable() {
            return build().unmodifiableList();
        }

        // -- Builder

        @Override
        public Builder<E, K> withWindowSize(int maxSize) {
            return super.withWindowSize(maxSize);
        }

        @Override
        public Builder<E, K> withListBig(boolean bigList) {
            return super.withListBig(bigList);
        }

        // -- Elem Map

        @Override
        public Builder<E, K> withOrderByElem(Class<?> type) {
            return super.withOrderByElem(type);
        }

        // -- Key Map 1

        @Override
        public Builder<E, K> withOrderByKey1(Class<?> type) {
            return super.withOrderByKey1(type);
        }
    }

    /**
     * Protected constructor used by builder or derived collections.
     */
    protected Key1List() {
    }

    /**
     * @return builder to use in extending classes
     */
    protected Builder<E, K> getBuilder() {
        return new Builder<E, K>(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Key1List<E, K> copy() {
        if (isReadOnly()) {
            Key1List<E, K> list = new Key1List<>(null);
            list.initCopy(this);
            return list;
        } else {
            return (Key1List<E, K>) doCopy();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Key1List<E, K> clone() {
        if (isReadOnly()) {
            return this;
        } else {
            return (Key1List<E, K>) doCopy();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Key1List<E, K> crop() {
        if (isReadOnly()) {
            Key1List<E, K> list = new Key1List<>(null);
            list.initCrop(this);
            return list;
        } else {
            return (Key1List<E, K>) doCrop();
        }
    }

    //-- Element methods

    @Override
    public IList<E> getAll(E elem) {
        return super.getAll(elem);
    }

    @Override
    public int count(E elem) {
        return super.count(elem);
    }

    @Override
    public IList<E> removeAll(E elem) {
        return super.removeAll(elem);
    }

    @Override
    public Set<E> getDistinct() {
        return super.getDistinct();
    }

    @Override
    public E put(E elem) {
        return super.put(elem);
    }

    //-- Key methods

    /**
     * Returns mapper for key map.
     *
     * @return mapper for key map
     */
    @SuppressWarnings("unchecked")
    public Function<E, K> getKey1Mapper() {
        return (Function<E, K>) getKeyMapper(1);
    }

    /**
     * Returns a map view to the key map.
     * The collection can be modified through the map as long as the constraint are not violated.
     * Note however that operations put() and remove() will be slow if the list is not sorted by the key and the element must therefore be searched.
     * The collections returned by the methods entrySet(), keySet(), and values() are immutable however.
     *
     * @return map view to key map
     * @throws IllegalArgumentException if the key map cannot be viewed as Map
     */
    public Map<K, E> asMap1() {
        return new KeyCollectionAsMap<K, E>(this, 1, false);
    }

    /**
     * Returns index of first element in list with specified key.
     *
     * @param key   key
     * @return      index of first element, -1 if no such element exists
     */
    public int indexOfKey1(K key) {
        return indexOfKey(1, key);
    }

    /**
     * Checks whether an element with specified key exists.
     *
     * @param key   key
     * @return      true if element with specified key exists, otherwise false
     */
    public boolean containsKey1(K key) {
        return containsKey(1, key);
    }

    /**
     * Returns element with specified key.
     * If there are several elements with the same key, the one added first will be returned.
     *
     * @param key   key
     * @return      element with specified key or null
     */
    public E getByKey1(K key) {
        return getByKey(1, key);
    }

    /**
     * Returns all elements with specified key.
     *
     * @param key   key
     * @return      all elements with specified key (never null)
     */
    public IList<E> getAllByKey1(K key) {
        return getAllByKey(1, key);
    }

    /**
     * Returns the number of elements with specified key.
     *
     * @param key   key
     * @return      number of elements with specified key
     */
    public int getCountByKey1(K key) {
        return getCountByKey(1, key);
    }

    /**
     * Removes element with specified key.
     * If there are several elements with the same key, the one added first will be removed.
     *
     * @param key   key
     * @return      element with specified key or null
     */
    public E removeByKey1(K key) {
        return removeByKey(1, key);
    }

    /**
     * Removes all elements with specified key.
     *
     * @param key   key
     * @return      removed elements with specified key (never null)
     */
    public IList<E> removeAllByKey1(K key) {
        return removeAllByKey(1, key);
    }

    /**
     * Returns list with all keys in element order.
     *
     * @return          list with all keys
     */
    @SuppressWarnings("unchecked")
    public IList<K> getAllKeys1() {
        return (GapList<K>) getAllKeys(1);
    }

    /**
     * Returns set with all distinct keys in the same order as in the key map.
     *
     * @return      set with all distinct keys
     */
    @SuppressWarnings("unchecked")
    public Set<K> getDistinctKeys1() {
        return (Set<K>) getDistinctKeys(1);
    }

    /**
     * Returns iterator for all distinct keys in the same order as in the key map.
     *
     * @return      iterator for all distinct keys
     */
    @SuppressWarnings("unchecked")
    public Iterator<K> iteratorDistinctKeys1() {
        return (Iterator<K>) iteratorDistinctKeys(1);
    }

    /**
     * Returns count for all distinct keys.
     *
     * @return      count for all distinct keys
     */
    public int countDistinctKeys1() {
        return countDistinctKeys(1);
    }

    /**
     * Adds element by key.
     * If there is no such element, the element is added.
     * If there is such an element, the element is replaced.
     * So said simply, it is a shortcut for the following code:
     * <pre>
     * removeByKey1(elem.getKey1());
     * add(elem);
     * </pre>
     * However the method is atomic in the sense that all or none operations are executed.
     * So if there is already such an element, but adding the new one fails due to a constraint violation,
     * the old element remains in the list.
     *
     * @param elem  element
     * @return      element with the same key which has been replaced or null otherwise
     */
    public E putByKey1(E elem) {
        return putByKey(1, elem, true);
    }

    /**
     * Adds or replaces element by key.
     * If there is no such element, the element is added.
     * If there is such an element, the element is left unchanged.
     * So said simply, it is a shortcut for the following code:
     * <pre>
     * if (!containsKey1(elem.getKey1())) {
     *   add(elem);
     * }
     * </pre>
     * However the method is atomic in the sense that all or none operations are executed.
     * So if there is already such an element, but adding the new one fails due to a constraint violation,
     * the old element remains in the list.
     *
     * @param elem  element
     * @return      element with the same key which has been left unchanged or null otherwise
     */
    public E putIfAbsentByKey1(E elem) {
        return putByKey(1, elem, false);
    }

    /**
     * Invalidate key value of element.
     * You must call an invalidate method if an element's key value has changed after adding it to the collection.
     *
     * @param oldKey    old key value
     * @param newKey    new key value
     * @param elem      element to invalidate (can be null if there are no duplicates with this key)
     */
    public void invalidateKey1(K oldKey, K newKey, E elem) {
        invalidateKey(1, oldKey, newKey, elem);
    }

    // --- ReadOnlyKey1List ---

    @Override
    public boolean isReadOnly() {
        return this instanceof ReadOnlyKey1List;
    }

    @Override
    public Key1List<E, K> unmodifiableList() {
        if (isReadOnly()) {
            return this;
        } else {
            return new ReadOnlyKey1List<E, K>(this);
        }
    }

    @Override
    public Key1List<E, K> immutableList() {
        if (isReadOnly()) {
            return this;
        } else {
            return new ReadOnlyKey1List<E, K>(copy());
        }
    }

    protected Key1List(Key1List<E, K> that) {
        if (that != null) {
            doAssign(that);
        }
    }

    /**
     * A read-only version of {@link Key1List}.
     * It is used to implement both unmodifiable and immutable lists.
     * Note that the client cannot change the list, but the content may change if the underlying list is changed.
     */
    protected static class ReadOnlyKey1List<E, K> extends Key1List<E, K> {

        /** UID for serialization */
        private static final long serialVersionUID = -1352274047348922584L;

        /**
         * Private constructor used internally.
         *
         * @param that  list to create an immutable view of
         */
        protected ReadOnlyKey1List(Key1List<E, K> that) {
            super(that);
        }

        @Override
        protected void doEnsureCapacity(int capacity) {
            error();
        }

        @Override
        protected boolean doAdd(int index, E elem) {
            error();
            return false;
        }

        @Override
        protected E doSet(int index, E elem) {
            error();
            return null;
        }

        @Override
        protected E doReSet(int index, E elem) {
            error();
            return null;
        }

        @Override
        protected E doRemove(int index) {
            error();
            return null;
        }

        @Override
        protected void doRemoveAll(int index, int len) {
            error();
        }

        @Override
        protected void doClear() {
            error();
        }

        @Override
        protected void doModify() {
            error();
        }

        @Override
        protected E removeByKey(int keyIndex, Object key) {
            error();
            return null;
        }

        @Override
        protected IList<E> removeAllByKey(int keyIndex, Object key) {
            error();
            return null;
        }

        @Override
        protected E putByKey(int keyIndex, E elem, boolean replace) {
            error();
            return null;
        }

        @Override
        protected void invalidateKey(int keyIndex, Object oldKey, Object newKey, E elem) {
            error();
        }

        /**
         * Throw exception if an attempt is made to change an immutable list.
         */
        private void error() {
            throw new UnsupportedOperationException("list is immutable");
        }
    }

}
