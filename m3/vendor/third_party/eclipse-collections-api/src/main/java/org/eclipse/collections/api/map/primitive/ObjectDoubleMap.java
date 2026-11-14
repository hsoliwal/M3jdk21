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

import java.util.Set;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.primitive.ObjectDoublePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectDoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.function.primitive.ObjectObjectDoubleToObjectFunction;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.tuple.primitive.ObjectDoublePair;

/**
 * This file was automatically generated from template file objectPrimitiveMap.stg.
 *
 * @since 3.0.
 */
public interface ObjectDoubleMap<K> extends DoubleIterable
{
    /**
     * Retrieves the value associated with the key. If no mapping exists for the key,
     * the default value (usually {@code 0}) is returned.
     * @param key the key
     * @return the value associated with the key, or the default value if no such
     * mapping exists
     */
    double get(Object key);

    /**
     * Retrieves the value associated with the key, throwing an {@link IllegalStateException}
     * if no such mapping exists.
     * @param key the key
     * @return the value associated with the key
     * @throws IllegalStateException if no mapping exists for the key
     */
    double getOrThrow(Object key);

    /**
     * Retrieves the value associated with the key, returning the specified default
     * value if no such mapping exists.
     * @param key the key
     * @param ifAbsent the default value to return if no mapping exists for {@code key}
     * @return the value associated with the key, or {@code ifAbsent} if no such
     * mapping exists.
     */
    double getIfAbsent(Object key, double ifAbsent);

    /**
     * Returns whether or not the key is present in the map.
     * @param key the key
     * @return if a mapping exists in this map for the key
     */
    boolean containsKey(Object key);

    /**
     * Returns whether or not this map contains the value.
     * @param value the value to test
     * @return if this collection contains the value
     */
    boolean containsValue(double value);

    /**
     * Iterates through each value in this map.
     * @param procedure the procedure to invoke for each value in this map.
     */
    void forEachValue(DoubleProcedure procedure);

    /**
     * Iterates through each key in the map, invoking the procedure for each.
     * @param procedure the procedure to invoke for each key
     */
    void forEachKey(Procedure<? super K> procedure);

    /**
     * Iterates through each key/value pair in the map, invoking the procedure for each.
     * @param procedure the procedure to invoke for each key/value pair
     */
    void forEachKeyValue(ObjectDoubleProcedure<? super K> procedure);

    /**
     * Return the DoubleObjectMap that is obtained by flipping the direction of this map and making the associations
     * from value to key.
     *
     * @throws IllegalStateException if the DoubleObjectMap contains duplicate values.
     * @since 9.0
     */
    DoubleObjectMap<K> flipUniqueValues();

    /**
     * Implements the injectInto pattern with each key/value pair of the map.
     * @param value to be injected into the map
     * @param function to apply to the injected value and key/value pairs
     * @return result of injecting the injectedValue into the map
     * @since 11.1
     */
    default <IV> IV injectIntoKeyValue(IV injectedValue, ObjectObjectDoubleToObjectFunction<? super IV, ? super K, ? extends IV> function)
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
    ObjectDoubleMap<K> select(ObjectDoublePredicate<? super K> predicate);

    /**
     * Return a copy of this map containing only the key/value pairs that do not match the
     * predicate.
     * @param predicate the predicate to determine which key/value pairs in this map should be
     * excluded from the returned map
     * @return a copy of this map without the matching key/value pairs
     */
    ObjectDoubleMap<K> reject(ObjectDoublePredicate<? super K> predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ObjectDoubleMap<K> tap(DoubleProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Follows the same general contract as {@link java.util.AbstractMap#toString()}
     *
     * @return a string representation of this ObjectDoubleMap
     */
    @Override
    String toString();

    /**
     * Returns a copy of this map that is immutable (if this map is mutable) or
     * itself if it is already immutable.
     * @return an immutable map that is equivalent to this one
     */
    ImmutableObjectDoubleMap<K> toImmutable();

    /**
     * Returns a set containing all the keys in this map. The set is backed by the
     * map, so any modifications to the returned set will affect this map.
     * @return a mutable set containing the keys in this map
     */
    Set<K> keySet();

    /**
     * Returns the values in this map as a separate collection. The returned collection is backed by the map, so any
     * changes made to the returned collection will affect the state of this map.
     * @return the values as a collection backed by this map
     */
    MutableDoubleCollection values();

    /**
     * Returns a view of the keys in this map. This iterable is backed by the map, so
     * any modifications to the underlying map will be reflected in the keys returned
     * by the iterable.
     * @return a view of the keys in this map
     * @since 5.0
     */
    LazyIterable<K> keysView();

    /**
     * Returns a view of the key/value pairs in this map. This iterable is backed by
     * the map, so any modifications to the underlying map will be reflected in the
     * pairs returned by the iterable.
     * @return a view of the keys in this map
     * @since 5.0
     */
    RichIterable<ObjectDoublePair<K>> keyValuesView();
}
