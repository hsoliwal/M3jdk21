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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.function.primitive.ShortToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction0;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ShortDoubleToDoubleFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortDoublePredicate;
import org.eclipse.collections.api.tuple.primitive.ShortDoublePair;

/**
 * This file was automatically generated from template file mutablePrimitivePrimitiveMap.stg.
 *
 * @since 3.0.
 */
public interface MutableShortDoubleMap extends ShortDoubleMap, MutableDoubleValuesMap
{
    /**
     * Associates a value with the specified key. If a value is already associated
     * with the key in this map, it will be replaced with {@code value}.
     * @param key the key
     * @param value the value to associate with {@code value}
     */
    void put(short key, double value);

    /**
     * This method allows MutableShortDoubleMap the ability to add an element in the form of ShortDoublePair.
     *
     * @see #put(short, double)
     * @since 9.1.0
     */
    default void putPair(ShortDoublePair keyValuePair)
    {
        this.put(keyValuePair.getOne(), keyValuePair.getTwo());
    }

    /**
     * Puts all of the key/value mappings from the specified map into this map. If this
     * map already has a value associated with one of the keys in the map, it will be
     * replaced with the value in {@code map}.
     * @param map the map to copy into this map
     */
    void putAll(ShortDoubleMap map);

    /**
     * Updates the values in-place.
     *
     * @param function that takes a key and its value and that returns a new value for this key
     * @since 10.0
     */
    void updateValues(ShortDoubleToDoubleFunction function);

    /**
     * Removes the mapping associated with the key, if one exists, from the map.
     * @param key the key to remove
     * @see #remove(short)
     */
    void removeKey(short key);

    /**
     * Removes the mapping associated with the key, if one exists, from the map.
     * @param key the key to remove
     * @see #removeKey(short)
     */
    void remove(short key);

    /**
     * Removes the mapping associated with the key, if one exists, from the map,
     * returning the previously associated value with the key. If no mapping
     * existed for the key, the specified default value is returned.
     * @param key the key to remove
     * @param value the default value to return if no mapping for the key exists
     * @return the value previously associated with the key, if one existed,
     * or {@code value} if not
     */
    double removeKeyIfAbsent(short key, double value);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates a value with the key.
     * @param key the key
     * @param value the value to associate with {@code key} if no such mapping exists
     * @return the value associated with key, if one exists, or {@code value} if not
     */
    double getIfAbsentPut(short key, double value);

    /**
     * Retrieves the value associated with the key if one exists;
     * associates a putValue with the key.
     * @param key the key
     * @param putValue the value to associate with {@code key} if no such mapping exists
     * @param defaultValue the value to return if no mapping associated with {@code key} exists
     * @return the value associated with key, if one exists, or {@code defaultValue} if not
     * @since 11.1.
     */
    default double getAndPut(short key, double putValue, double defaultValue)
    {
        double returnValue = this.getIfAbsent(key, defaultValue);
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
    double getIfAbsentPut(short key, DoubleFunction0 function);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates the result of invoking the value function with the key.
     * @param key the key
     * @param function the function that provides the value if no mapping exists.
     * The {@code key} will be passed as the argument to the function.
     * @return the value associated with the key, if one exists, or the result of
     * invoking {@code function} with {@code key} if not
     */
    double getIfAbsentPutWithKey(short key, ShortToDoubleFunction function);

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
    <P> double getIfAbsentPutWith(short key, DoubleFunction<? super P> function, P parameter);

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
    double updateValue(short key, double initialValueIfAbsent, DoubleToDoubleFunction function);

    @Override
    MutableDoubleShortMap flipUniqueValues();

    @Override
    MutableShortDoubleMap select(ShortDoublePredicate predicate);

    @Override
    MutableShortDoubleMap reject(ShortDoublePredicate predicate);

    /**
     * Associates a value with the specified key. If a value is already associated
     * with the key in this map, it will be replaced with {@code value}.
     * @param key the key
     * @param value the value to associate with {@code value}
     * @return this map
     * @see #put(short, double)
     */
    MutableShortDoubleMap withKeyValue(short key, double value);

    /**
     * Removes the mapping associated with the key, if one exists, from this map.
     * @param key the key to remove
     * @return this map
     * @see #remove(short)
     */
    MutableShortDoubleMap withoutKey(short key);

    /**
     * Removes the mappings associated with all the keys, if they exist, from this map.
     * @param keys the keys to remove
     * @return this map
     * @see #remove(short)
     */
    MutableShortDoubleMap withoutAllKeys(ShortIterable keys);

    /**
     * Puts all of the key/value mappings from the specified pairs into this map. If this
     * map already has a value associated with one of the keys in the pairs, it will be
     * replaced with the value in the pair.
     * @param iterable the pairs to put into this map
     * @return this map
     * @see #putPair(ShortDoublePair)
     */
    default MutableShortDoubleMap withAllKeyValues(Iterable<ShortDoublePair> keyValuePairs)
    {
        for (ShortDoublePair keyValuePair : keyValuePairs)
        {
            this.putPair(keyValuePair);
        }
        return this;
    }

    /**
     * Returns an unmodifiable view of this map, delegating all read-only operations to this
     * map and throwing an {@link UnsupportedOperationException} for all mutating operations.
     * This avoids the overhead of copying the map when calling {@link #toImmutable()} while
     * still providing immutability.
     * @return an unmodifiable view of this map
     */
    MutableShortDoubleMap asUnmodifiable();

    /**
     * Returns a synchronized view of this map, delegating all operations to this map but
     * ensuring only one caller has access to the map at a time.
     * @return a synchronized view of this map
     */
    MutableShortDoubleMap asSynchronized();

    /**
     * Increments and updates the value associated with the key, if a value exists, or
     * sets the value to be the specified value if one does not.
     * @param key the key
     * @param toBeAdded the amount to increment the existing value, if one exists, or
     * to use as the initial value if one does not
     * @return the value after incrementing {@code toBeAdded} to the existing value
     * associated with {@code key} or {@code toBeAdded} if one does not
     */
    double addToValue(short key, double toBeAdded);
}
