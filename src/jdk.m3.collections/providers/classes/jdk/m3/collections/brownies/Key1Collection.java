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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Key1Collection implements a collection with 1 key.
 * This key can be accessed fast.
 * It can provide fast access to its elements like a Set.
 * The elements allowed in the collection can be constraint (null/duplicate values).
 *
 * @author Thomas Mauch
 *
 * @see Key1List
 * @param <E> type of elements stored in the collection
 * @param <K> type of key
 */
@SuppressWarnings({"serial", "this-escape"})
public class Key1Collection<E, K> extends KeyCollectionImpl<E> {

    /** Build {@link Key1Collection} with specified builder function */
    public static <E, K> Key1Collection<E, K> buildCollection(UnaryOperator<Key1Collection.Builder<E, K>> builder) {
        return builder.apply(new Key1Collection.Builder<>()).build();
    }

    /**
     * Builder to construct {@link Key1Collection} instances.
     */
    public static class Builder<E, K> extends Key1CollectionImplBuilder<E, K, Builder<E, K>> {

        /**
         * Default constructor.
         */
        public Builder() {
            this(null);
        }

        /**
         * Protected constructor used if extending Key1Collection.
         *
         * @param keyColl   key collection
         */
        protected Builder(Key1Collection<E, K> keyColl) {
            this.keyColl = keyColl;
            initKeyMapBuilder(1);
        }

        /**
         * @return created collection
         */
        @SuppressWarnings("unchecked")
        public Key1Collection<E, K> build() {
            if (keyColl == null) {
                keyColl = new Key1Collection<>();
            }
            build(keyColl, false);
            init(keyColl);
            return (Key1Collection<E, K>) keyColl;
        }

    }

    /**
     * Protected constructor used by builder or derived collections.
     */
    protected Key1Collection() {
    }

    /**
     * @return builder to use in extending classes
     */
    protected Builder<E, K> getBuilder() {
        return new Builder<>(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Key1Collection<E, K> copy() {
        return (Key1Collection<E, K>) super.copy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Key1Collection<E, K> crop() {
        return (Key1Collection<E, K>) super.crop();
    }

    //-- Element methods

    @Override
    public E put(E elem) {
        return super.put(elem);
    }

    @Override
    public void invalidate(E elem) {
        super.invalidate(elem);
    }

    //-- Key methods

    /**
     * Returns mapper for key map.
     *
     * @return          mapper for key map
     */
    @SuppressWarnings("unchecked")
    public Function<E, K> getKey1Mapper() {
        return (Function<E, K>) super.getKeyMapper(1);
    }

    /**
     * Returns a map view to the key map.
     * The collection can be modified through the map as long as the constraint are not violated.
     * The collections returned by the methods entrySet(), keySet(), and values() are immutable however.
     *
     * @return map view to key map
     * @throws IllegalArgumentException if the key map cannot be viewed as Map
     */
    public Map<K, E> asMap1() {
        return new KeyCollectionAsMap<K, E>(this, 1, false);
    }

    /**
     * Checks whether an element with specified key exists.
     *
     * @param key   key
     * @return      true if element with specified key exists, otherwise false
     */
    public boolean containsKey1(K key) {
        return super.containsKey(1, key);
    }

    /**
     * Returns element with specified key.
     * If there are several elements with the same key, the one added first will be returned.
     *
     * @param key   key
     * @return      element with specified key or null
     */
    public E getByKey1(K key) {
        return super.getByKey(1, key);
    }

    /**
     * Returns all elements with specified key.
     *
     * @param key   key
     * @return      all elements with specified key (never null)
     */
    public Collection<E> getAllByKey1(K key) {
        return super.getAllByKey(1, key);
    }

    /**
     * Returns the number of elements with specified key.
     *
     * @param key   key
     * @return      number of elements with specified key
     */
    public int getCountByKey1(K key) {
        return super.getCountByKey(1, key);
    }

    /**
     * Removes element with specified key.
     * If there are several elements with the same key, the one added first will be removed.
     *
     * @param key   key
     * @return      element with specified key or null
     */
    public E removeByKey1(K key) {
        return super.removeByKey(1, key);
    }

    /**
     * Removes all elements with specified key.
     *
     * @param key   key
     * @return      removed elements with specified key (never null)
     */
    public Collection<E> removeAllByKey1(K key) {
        return super.removeAllByKey(1, key);
    }

    /**
     * Returns list with all keys in element order.
     *
     * @return          list with all keys
     */
    @SuppressWarnings("unchecked")
    public IList<K> getAllKeys1() {
        return (GapList<K>) super.getAllKeys(1);
    }

    /**
     * Returns set with all distinct keys in the same order as in the key map.
     *
     * @return      set with all distinct keys
     */
    @SuppressWarnings("unchecked")
    public Set<K> getDistinctKeys1() {
        return (Set<K>) super.getDistinctKeys(1);
    }

    /**
     * Returns iterator for all distinct keys in the same order as in the key map.
     *
     * @return      iterator for all distinct keys
     */
    @SuppressWarnings("unchecked")
    public Set<K> iteratorDistinctKeys1() {
        return (Set<K>) super.iteratorDistinctKeys(1);
    }

    /**
     * Returns count for all distinct keys.
     *
     * @return      count for all distinct keys
     */
    public int countDistinctKeys1() {
        return super.countDistinctKeys(1);
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
        return super.putByKey(1, elem, true);
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
        super.invalidateKey(1, oldKey, newKey, elem);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Key1Collection<E, K> filter(Predicate<? super E> filter) {
        return (Key1Collection<E, K>) super.filter(filter);
    }
}
