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

import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.predicate.primitive.ObjectBytePredicate;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;

/**
 * This file was automatically generated from template file mutableObjectPrimitiveMap.stg.
 *
 * @since 3.0.
 */
public interface MutableObjectByteMap<K> extends ObjectByteMap<K>
{
    @Override
    MutableByteIterator byteIterator();

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
    void put(K key, byte value);

    /**
     * This method allows MutableObjectByteMap the ability to add an element in the form of {@code ObjectBytePair<K>}.
     *
     * @see #put(Object, byte)
     * @since 9.1.0
     */
    default void putPair(ObjectBytePair<K> keyValuePair)
    {
        this.put(keyValuePair.getOne(), keyValuePair.getTwo());
    }

    /**
     * Puts all of the key/value mappings from the specified map into this map. If this
     * map already has a value associated with one of the keys in the map, it will be
     * replaced with the value in {@code map}.
     * @param map the map to copy into this map
     */
    void putAll(ObjectByteMap<? extends K> map);

    /**
     * Updates the values in-place.
     *
     * @param function that takes a key and its value and that returns a new value for this key
     * @since 10.0
     */
    void updateValues(ObjectByteToByteFunction<? super K> function);

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
    byte removeKeyIfAbsent(K key, byte value);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates a value with the key.
     * a new value with they key
     * @param key the key
     * @param value the value to associate with {@code key} if no such mapping exists
     * @return the value associated with key, if one exists, or {@code value} if not
     */
    byte getIfAbsentPut(K key, byte value);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates a putValue with the key.
     * @param key the key
     * @param putValue the value to associate with {@code key} if no such mapping exists
     * @param defaultValue the value to return if no mapping associated with {@code key} exists
     * @return the value associated with key, if one exists, or {@code defaultValue} if not
     * @since 11.1.
     */
    default byte getAndPut(K key, byte putValue, byte defaultValue)
    {
        byte returnValue = this.getIfAbsent(key, defaultValue);
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
    byte getIfAbsentPut(K key, ByteFunction0 function);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates the result of invoking the value function with the key.
     * @param key the key
     * @param function the function that provides the value if no mapping exists.
     * The {@code key} will be passed as the argument to the function.
     * @return the value associated with the key, if one exists, or the result of
     * invoking {@code function} with {@code key} if not
     */
    byte getIfAbsentPutWithKey(K key, ByteFunction<? super K> function);

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
    <P> byte getIfAbsentPutWith(K key, ByteFunction<? super P> function, P parameter);

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
    byte updateValue(K key, byte initialValueIfAbsent, ByteToByteFunction function);

    @Override
    MutableByteObjectMap<K> flipUniqueValues();

    @Override
    MutableObjectByteMap<K> select(ObjectBytePredicate<? super K> predicate);

    @Override
    MutableObjectByteMap<K> reject(ObjectBytePredicate<? super K> predicate);

    @Override
    MutableByteCollection select(BytePredicate predicate);

    @Override
    MutableByteCollection reject(BytePredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default MutableObjectByteMap<K> tap(ByteProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> MutableCollection<V> collect(ByteToObjectFunction<? extends V> function);

    /**
     * Associates a value with the specified key. If a value is already associated
     * with the key in this map, it will be replaced with {@code value}.
     * @param key the key
     * @param value the value to associate with {@code value}
     * @return this map
     * @see #put(K, byte)
     */
    MutableObjectByteMap<K> withKeyValue(K key, byte value);

    /**
     * Removes the mapping associated with the key, if one exists, from this map.
     * @param key the key to remove
     * @return this map
     * @see #remove(Object)
     */
    MutableObjectByteMap<K> withoutKey(K key);

    /**
     * Removes the mappings associated with all the keys, if they exist, from this map.
     * @param keys the keys to remove
     * @return this map
     * @see #remove(Object)
     */
    MutableObjectByteMap<K> withoutAllKeys(Iterable<? extends K> keys);

    default MutableObjectByteMap<K> withAllKeyValues(Iterable<ObjectBytePair<K>> keyValuePairs)
    {
        for (ObjectBytePair<K> keyValuePair : keyValuePairs)
        {
            this.putPair(keyValuePair);
        }
        return this;
    }

    MutableObjectByteMap<K> asUnmodifiable();

    MutableObjectByteMap<K> asSynchronized();

    byte addToValue(K key, byte toBeAdded);
}
