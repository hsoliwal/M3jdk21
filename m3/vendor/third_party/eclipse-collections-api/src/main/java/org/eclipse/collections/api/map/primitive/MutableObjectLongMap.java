/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.map.primitive;

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction0;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToLongFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.predicate.primitive.ObjectLongPredicate;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;

/**
 * This file was automatically generated from template file mutableObjectPrimitiveMap.stg.
 *
 * @since 3.0.
 */
public interface MutableObjectLongMap<K> extends ObjectLongMap<K>
{
    @Override
    MutableLongIterator longIterator();

    /**
     * Removes all entries from this map.
     */
    void clear();

    /**
     * Associates a value with the specified key. If a value is already associated
     * with the key in this map, it will be replaced with {@code value}.
     * @param key the key
     * @param value the value to associate with {@code value}
     */
    void put(K key, long value);

    /**
     * This method allows MutableObjectLongMap the ability to add an element in the form of {@code ObjectLongPair<K>}.
     *
     * @see #put(Object, long)
     * @since 9.1.0
     */
    default void putPair(ObjectLongPair<K> keyValuePair)
    {
        this.put(keyValuePair.getOne(), keyValuePair.getTwo());
    }

    /**
     * Puts all of the key/value mappings from the specified map into this map. If this
     * map already has a value associated with one of the keys in the map, it will be
     * replaced with the value in {@code map}.
     * @param map the map to copy into this map
     */
    void putAll(ObjectLongMap<? extends K> map);

    /**
     * Updates the values in-place.
     *
     * @param function that takes a key and its value and that returns a new value for this key
     * @since 10.0
     */
    void updateValues(ObjectLongToLongFunction<? super K> function);

    /**
     * Removes the mapping associated with the key, if one exists, from the map.
     * @param key the key to remove
     * @see #remove(Object)
     */
    void removeKey(K key);

    /**
     * Removes the mapping associated with the key, if one exists, from the map.
     * @param key the key to remove
     * @see #removeKey(K)
     */
    void remove(Object key);

    /**
     * Removes the mapping associated with the key, if one exists, from the map,
     * returning the previously associated value with the key. If no mapping
     * existed for the key, the specified default value is returned.
     * @param key the key to remove
     * @param value the default value to return if no mapping for the key exists
     * @return the value previously associated with the key, if one existed,
     * or {@code value} if not
     */
    long removeKeyIfAbsent(K key, long value);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates a value with the key.
     * a new value with they key
     * @param key the key
     * @param value the value to associate with {@code key} if no such mapping exists
     * @return the value associated with key, if one exists, or {@code value} if not
     */
    long getIfAbsentPut(K key, long value);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates a putValue with the key.
     * @param key the key
     * @param putValue the value to associate with {@code key} if no such mapping exists
     * @param defaultValue the value to return if no mapping associated with {@code key} exists
     * @return the value associated with key, if one exists, or {@code defaultValue} if not
     * @since 11.1.
     */
    default long getAndPut(K key, long putValue, long defaultValue)
    {
        long returnValue = this.getIfAbsent(key, defaultValue);
        this.put(key, putValue);
        return returnValue;
    }

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * invokes the supplier and associates the result with the key.
     * @param key the key
     * @param function the supplier that provides the value if no mapping exists for {@code key}
     * @return the value associated with the key, if one exists, or the result of
     * invoking {@code function} if not
     */
    long getIfAbsentPut(K key, LongFunction0 function);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates the result of invoking the value function with the key.
     * @param key the key
     * @param function the function that provides the value if no mapping exists.
     * The {@code key} will be passed as the argument to the function.
     * @return the value associated with the key, if one exists, or the result of
     * invoking {@code function} with {@code key} if not
     */
    long getIfAbsentPutWithKey(K key, LongFunction<? super K> function);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * invokes the value function with the parameter and associates the result with the key.
     * @param key the key
     * @param function the function that provides the value if no mapping exists.
     * The specified {@code parameter} will be passed as the argument to the function.
     * @param parameter the parameter to provide to {@code function} if no value
     * exists for {@code key}
     * @param <P> the type of the value function's {@code parameter}
     * @return the value associated with the key, if one exists, or the result of
     * invoking {@code function} with {@code parameter} if not
     */
    <P> long getIfAbsentPutWith(K key, LongFunction<? super P> function, P parameter);

    /**
     * Updates or sets the value associated with the key by applying the function to the
     * existing value, if one exists, or to the specified initial value if one does not.
     * @param key the key
     * @param initialValueIfAbsent the initial value to supply to the function if no
     * mapping exists for the key
     * @param function the function that returns the updated value based on the current
     * value or the initial value, if no value exists
     * @return the new value associated with the key, either as a result of applying
     * {@code function} to the value already associated with the key or as a result of
     * applying it to {@code initialValueIfAbsent} and associating the result with {@code key}
     */
    long updateValue(K key, long initialValueIfAbsent, LongToLongFunction function);

    @Override
    MutableLongObjectMap<K> flipUniqueValues();

    @Override
    MutableObjectLongMap<K> select(ObjectLongPredicate<? super K> predicate);

    @Override
    MutableObjectLongMap<K> reject(ObjectLongPredicate<? super K> predicate);

    @Override
    MutableLongCollection select(LongPredicate predicate);

    @Override
    MutableLongCollection reject(LongPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default MutableObjectLongMap<K> tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> MutableCollection<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * Associates a value with the specified key. If a value is already associated
     * with the key in this map, it will be replaced with {@code value}.
     * @param key the key
     * @param value the value to associate with {@code value}
     * @return this map
     * @see #put(K, long)
     */
    MutableObjectLongMap<K> withKeyValue(K key, long value);

    /**
     * Removes the mapping associated with the key, if one exists, from this map.
     * @param key the key to remove
     * @return this map
     * @see #remove(Object)
     */
    MutableObjectLongMap<K> withoutKey(K key);

    /**
     * Removes the mappings associated with all the keys, if they exist, from this map.
     * @param keys the keys to remove
     * @return this map
     * @see #remove(Object)
     */
    MutableObjectLongMap<K> withoutAllKeys(Iterable<? extends K> keys);

    default MutableObjectLongMap<K> withAllKeyValues(Iterable<ObjectLongPair<K>> keyValuePairs)
    {
        for (ObjectLongPair<K> keyValuePair : keyValuePairs)
        {
            this.putPair(keyValuePair);
        }
        return this;
    }

    MutableObjectLongMap<K> asUnmodifiable();

    MutableObjectLongMap<K> asSynchronized();

    long addToValue(K key, long toBeAdded);
}
