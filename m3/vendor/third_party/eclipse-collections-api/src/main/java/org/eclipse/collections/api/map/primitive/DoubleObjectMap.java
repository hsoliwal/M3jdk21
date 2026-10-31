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

import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.primitive.DoubleObjectPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleObjectProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleObjectToObjectFunction;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;

/**
 * This file was automatically generated from template file primitiveObjectMap.stg.
 *
 * @since 3.0.
 */
public interface DoubleObjectMap<V> extends PrimitiveObjectMap<V>
{
    /**
     * Retrieves the value associated with the key. If no mapping exists for the key,
     * {@code null} is returned.
     * @param key the key
     * @return the value associated with the key, or the default value if no such
     * mapping exists
     */
    V get(double key);

    /**
     * Retrieves the value associated with the key if one exists; if it does not,
     * returns the result of invoking the value supplier.
     * @param key the key
     * @param function the supplier that provides the value if no mapping exists for {@code key}
     * @return the value associated with the key, if one exists, or the result of
     * invoking {@code ifAbsent} if not
     */
    V getIfAbsent(double key, Function0<? extends V> ifAbsent);

    /**
     * Returns whether or not the key is present in the map.
     * @param key the key
     * @return if a mapping exists in this map for the key
     */
    boolean containsKey(double key);

    @Override
    DoubleObjectMap<V> tap(Procedure<? super V> procedure);

    /**
     * Iterates through each key in the map, invoking the procedure for each.
     * @param procedure the procedure to invoke for each key
     */
    void forEachKey(DoubleProcedure procedure);

    /**
     * Iterates through each key/value pair in the map, invoking the procedure for each.
     * @param procedure the procedure to invoke for each key/value pair
     */
    void forEachKeyValue(DoubleObjectProcedure<? super V> procedure);

    /**
     * Implements the injectInto pattern with each key/value pair of the map.
     * @param value to be injected into the map
     * @param function to apply to the injected value and key/value pairs
     * @return result of injecting the injectedValue into the map
     * @since 11.1
     */
    default <IV> IV injectIntoKeyValue(IV injectedValue, ObjectDoubleObjectToObjectFunction<? super IV, ? super V, ? extends IV> function)
    {
        IV[] result = (IV[]) new Object[]{injectedValue};
        this.forEachKeyValue((key, value) -> result[0] = function.valueOf(result[0], key, value));
        return result[0];
    }

    /**
     * Return a copy of this map containing only the key/value pairs that match the predicate.
     * @param predicate the predicate to determine which key/value pairs in this map should be
     * included in the returned map
     * @return a copy of this map with the matching key/value pairs
     */
    DoubleObjectMap<V> select(DoubleObjectPredicate<? super V> predicate);

    /**
     * Return a copy of this map containing only the key/value pairs that do not match the
     * predicate.
     * @param predicate the predicate to determine which key/value pairs in this map should be
     * excluded from the returned map
     * @return a copy of this map without the matching key/value pairs
     */
    DoubleObjectMap<V> reject(DoubleObjectPredicate<? super V> predicate);

    /**
     * Returns a copy of this map that is immutable (if this map is mutable) or
     * itself if it is already immutable.
     * @return an immutable map that is equivalent to this one
     */
    ImmutableDoubleObjectMap<V> toImmutable();

    /**
     * Returns a set containing all the keys in this map. The set is backed by the
     * map, so any modifications to the returned set will affect this map.
     * @return a mutable set containing the keys in this map
     */
    MutableDoubleSet keySet();

    /**
     * Returns a view of the keys in this map. This iterable is backed by the map, so
     * any modifications to the underlying map will be reflected in the keys returned
     * by the iterable.
     * @return a view of the keys in this map
     * @since 5.0
     */
    LazyDoubleIterable keysView();

    /**
     * Returns a view of the key/value pairs in this map. This iterable is backed by
     * the map, so any modifications to the underlying map will be reflected in the
     * pairs returned by the iterable.
     * @return a view of the keys in this map
     * @since 5.0
     */
    RichIterable<DoubleObjectPair<V>> keyValuesView();

    /**
     * Return the ObjectDoubleMap that is obtained by flipping the direction of this map and making the associations
     * from value to key.
     *
     * @throws IllegalStateException if the ObjectDoubleMap contains duplicate values.
     * @since 9.0
     */
    ObjectDoubleMap<V> flipUniqueValues();
}
