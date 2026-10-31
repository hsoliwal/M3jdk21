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

import java.util.Map;

import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.primitive.IntFloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.function.primitive.ObjectIntFloatToObjectFunction;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.IntFloatPair;

/**
 * This file was automatically generated from template file primitivePrimitiveMap.stg.
 *
 * @since 3.0.
 */
public interface IntFloatMap extends FloatValuesMap
{
    /**
     * Retrieves the value associated with the key. If no mapping exists for the key,
     * the default value (usually {@code 0}) is returned.
     * @param key the key
     * @return the value associated with the key, or the default value if no such
     * mapping exists
     */
    float get(int key);

    /**
     * Retrieves the value associated with the key, returning the specified default
     * value if no such mapping exists.
     * @param key the key
     * @param ifAbsent the default value to return if no mapping exists for {@code key}
     * @return the value associated with the key, or {@code ifAbsent} if no such
     * mapping exists.
     */
    float getIfAbsent(int key, float ifAbsent);

    /**
     * Retrieves the value associated with the key, throwing an {@link IllegalStateException}
     * if no such mapping exists.
     * @param key the key
     * @return the value associated with the key
     * @throws IllegalStateException if no mapping exists for the key
     */
    float getOrThrow(int key);

    /**
     * Returns whether or not the key is present in the map.
     * @param key the key
     * @return if a mapping exists in this map for the key
     */
    boolean containsKey(int key);

    /**
     * Iterates through each key in the map, invoking the procedure for each.
     * @param procedure the procedure to invoke for each key
     */
    void forEachKey(IntProcedure procedure);

    /**
     * Iterates through each key/value pair in the map, invoking the procedure for each.
     * @param procedure the procedure to invoke for each key/value pair
     */
    void forEachKeyValue(IntFloatProcedure procedure);

    /**
     * Implements the injectInto pattern with each key/value pair of the map.
     * @param value to be injected into the map
     * @param function to apply to the injected value and key/value pairs
     * @return result of injecting the injectedValue into the map
     * @since 11.1
     */
    default <IV> IV injectIntoKeyValue(IV injectedValue, ObjectIntFloatToObjectFunction<? super IV, ? extends IV> function)
    {
        IV[] result = (IV[]) new Object[]{injectedValue};
        this.forEachKeyValue((key, value) -> result[0] = function.valueOf(result[0], key, value));
        return result[0];
    }

    /**
     * Returns a view of the keys in this map. This iterable is backed by the map, so
     * any modifications to the underlying map will be reflected in the keys returned
     * by the iterable.
     * @return a view of the keys in this map
     * @since 5.0
     */
    LazyIntIterable keysView();

    /**
     * Returns a view of the key/value pairs in this map. This iterable is backed by
     * the map, so any modifications to the underlying map will be reflected in the
     * pairs returned by the iterable.
     * @return a view of the keys in this map
     * @since 5.0
     */
    RichIterable<IntFloatPair> keyValuesView();

    /**
         * Return the FloatIntMap that is obtained by flipping the direction of this map and making the associations
         * from value to key.
         *
         * @throws IllegalStateException if the FloatIntMap contains duplicate values.
         * @since 9.0
         */
    FloatIntMap flipUniqueValues();

    /**
     * Return a copy of this map containing only the key/value pairs that match the predicate.
     * @param predicate the predicate to determine which key/value pairs in this map should be
     * included in the returned map
     * @return a copy of this map with the matching key/value pairs
     */
    IntFloatMap select(IntFloatPredicate predicate);

    /**
     * Return a copy of this map containing only the key/value pairs that do not match the
     * predicate.
     * @param predicate the predicate to determine which key/value pairs in this map should be
     * excluded from the returned map
     * @return a copy of this map without the matching key/value pairs
     */
    IntFloatMap reject(IntFloatPredicate predicate);

    /**
     * Follows the same general contract as {@link Map#equals(Object)}.
     */
    @Override
    boolean equals(Object o);

    /**
     * Follows the same general contract as {@link Map#hashCode()}.
     */
    @Override
    int hashCode();

    /**
     * Follows the same general contract as {@link java.util.AbstractMap#toString()}
     *
     * @return a string representation of this IntFloatMap
     */
    @Override
    String toString();

    /**
     * Returns a copy of this map that is immutable (if this map is mutable) or
     * itself if it is already immutable.
     * @return an immutable map that is equivalent to this one
     */
    ImmutableIntFloatMap toImmutable();

    /**
     * Returns a set containing all the keys in this map. The set is backed by the
     * map, so any modifications to the returned set will affect this map.
     * @return a mutable set containing the keys in this map
     */
    MutableIntSet keySet();
}
