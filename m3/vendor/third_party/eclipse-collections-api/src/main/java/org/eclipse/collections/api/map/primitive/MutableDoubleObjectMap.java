/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.map.primitive;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleObjectPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;

/**
 * This file was automatically generated from template file mutablePrimitiveObjectMap.stg.
 *
 * @since 3.0.
 */
public interface MutableDoubleObjectMap<V> extends DoubleObjectMap<V>, MutablePrimitiveObjectMap<V>
{
    /**
     * Associates a value with the specified key. If a value is already associated
     * with the key in this map, it will be replaced with {@code value}.
     * @param key the key
     * @param value the value to associate with {@code value}
     * @return the value previously associated with {@code key} if one existed, or
     * {@code null} if not
     */
    V put(double key, V value);

    /**
     * This method allows MutableDoubleObjectMap the ability to add an element in the form of {@code DoubleObjectPair<V>}.
     *
     * @see #put(double, Object)
     * @since 9.1.0
     */
    default V putPair(DoubleObjectPair<V> keyValuePair)
    {
        return this.put(keyValuePair.getOne(), keyValuePair.getTwo());
    }

    /**
     * Puts all of the key/value mappings from the specified map into this map. If this
     * map already has a value associated with one of the keys in the map, it will be
     * replaced with the value in {@code map}.
     * @param map the map to copy into this map
     * @since 5.0.
     */
    void putAll(DoubleObjectMap<? extends V> map);

    /**
     * Removes the mapping associated with the key, if one exists, from the map.
     * @param key the key to remove
     * @see #remove(double)
     */
    V removeKey(double key);

    /**
     * Removes the mapping associated with the key, if one exists, from the map.
     * @param key the key to remove
     * @see #removeKey(double)
     */
    V remove(double key);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates a value with the key.
     * @param key the key
     * @param value the value to associate with {@code key} if no such mapping exists
     * @return the value associated with key, if one exists, or {@code value} if not
     */
    V getIfAbsentPut(double key, V value);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * invokes the supplier and associates the result with the key.
     * @param key the key
     * @param function the supplier that provides the value if no mapping exists for {@code key}
     * @return the value associated with the key, if one exists, or the result of
     * invoking {@code function} if not
     */
    V getIfAbsentPut(double key, Function0<? extends V> function);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * associates the result of invoking the value function with the key.
     * @param key the key
     * @param function the function that provides the value if no mapping exists.
     * The {@code key} will be passed as the argument to the function.
     * @return the value associated with the key, if one exists, or the result of
     * invoking {@code function} with {@code key} if not
     */
    V getIfAbsentPutWithKey(double key, DoubleToObjectFunction<? extends V> function);

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
    <P> V getIfAbsentPutWith(double key, Function<? super P, ? extends V> function, P parameter);

    /**
     * Look up the value associated with {@code key}, apply the {@code function} to it, and replace the value. If there
     * is no value associated with {@code key}, start it off with a value supplied by {@code factory}.
     */
    V updateValue(double key, Function0<? extends V> factory, Function<? super V, ? extends V> function);

    /**
     * Updates or sets the value associated with the key by applying the function to the
     * existing value, if one exists, or the initial value supplied by the factory if one does not.
     * @param key the key
     * @param factory the supplier providing the initial value to the function if no
     * mapping exists for the key
     * @param function the function that returns the updated value based on the current
     * value or the initial value, if no value exists. The specified {@code parameter}
     * will also be passed as the second argument to the function.
     * @param parameter the parameter to provide to {@code function} if no value
     * exists for {@code key}
     * @param <P> the type of the value function's {@code parameter}
     * @return the new value associated with the key, either as a result of applying
     * {@code function} to the value already associated with the key or as a result of
     * applying it to the value returned by {@code factory} and associating the result
     * with {@code key}
     */
    <P> V updateValueWith(double key, Function0<? extends V> factory, Function2<? super V, ? super P, ? extends V> function, P parameter);

    @Override
    MutableObjectDoubleMap<V> flipUniqueValues();

    @Override
    MutableDoubleObjectMap<V> tap(Procedure<? super V> procedure);

    @Override
    MutableDoubleObjectMap<V> select(DoubleObjectPredicate<? super V> predicate);

    @Override
    MutableDoubleObjectMap<V> reject(DoubleObjectPredicate<? super V> predicate);

    /**
     * Associates a value with the specified key. If a value is already associated
     * with the key in this map, it will be replaced with {@code value}.
     * @param key the key
     * @param value the value to associate with {@code value}
     * @return this map
     * @see #put(double, V)
     */
    MutableDoubleObjectMap<V> withKeyValue(double key, V value);

    /**
     * Removes the mapping associated with the key, if one exists, from this map.
     * @param key the key to remove
     * @return this map
     * @see #remove(double)
     */
    MutableDoubleObjectMap<V> withoutKey(double key);

    /**
     * Removes the mappings associated with all the keys, if they exist, from this map.
     * @param keys the keys to remove
     * @return this map
     * @see #remove(double)
     */
    MutableDoubleObjectMap<V> withoutAllKeys(DoubleIterable keys);

    /**
     * Puts all of the key/value mappings from the specified pairs into this map. If this
     * map already has a value associated with one of the keys in the pairs, it will be
     * replaced with the value in the pair.
     * @param iterable the pairs to put into this map
     * @return this map
     * @see #putPair(DoubleObjectPair)
     */
    default MutableDoubleObjectMap<V> withAllKeyValues(Iterable<DoubleObjectPair<V>> keyValuePairs)
    {
        for (DoubleObjectPair<V> keyValuePair : keyValuePairs)
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
    MutableDoubleObjectMap<V> asUnmodifiable();

    /**
     * Returns a synchronized view of this map, delegating all operations to this map but
     * ensuring only one caller has access to the map at a time.
     * @return a synchronized view of this map
     */
    MutableDoubleObjectMap<V> asSynchronized();
}
