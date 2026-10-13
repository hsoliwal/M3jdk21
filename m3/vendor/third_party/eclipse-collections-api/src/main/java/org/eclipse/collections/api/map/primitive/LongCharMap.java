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

import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.primitive.LongCharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongCharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.function.primitive.ObjectLongCharToObjectFunction;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.LongCharPair;

/**
 * This file was automatically generated from template file primitivePrimitiveMap.stg.
 *
 * @since 3.0.
 */
public interface LongCharMap extends CharValuesMap
{
    /**
     * Retrieves the value associated with the key. If no mapping exists for the key,
     * the default value (usually {@code 0}) is returned.
     * @param key the key
     * @return the value associated with the key, or the default value if no such
     * mapping exists
     */
    char get(long key);

    /**
     * Retrieves the value associated with the key, returning the specified default
     * value if no such mapping exists.
     * @param key the key
     * @param ifAbsent the default value to return if no mapping exists for {@code key}
     * @return the value associated with the key, or {@code ifAbsent} if no such
     * mapping exists.
     */
    char getIfAbsent(long key, char ifAbsent);

    /**
     * Retrieves the value associated with the key, throwing an {@link IllegalStateException}
     * if no such mapping exists.
     * @param key the key
     * @return the value associated with the key
     * @throws IllegalStateException if no mapping exists for the key
     */
    char getOrThrow(long key);

    /**
     * Returns whether or not the key is present in the map.
     * @param key the key
     * @return if a mapping exists in this map for the key
     */
    boolean containsKey(long key);

    /**
     * Iterates through each key in the map, invoking the procedure for each.
     * @param procedure the procedure to invoke for each key
     */
    void forEachKey(LongProcedure procedure);

    /**
     * Iterates through each key/value pair in the map, invoking the procedure for each.
     * @param procedure the procedure to invoke for each key/value pair
     */
    void forEachKeyValue(LongCharProcedure procedure);

    /**
     * Implements the injectInto pattern with each key/value pair of the map.
     * @param value to be injected into the map
     * @param function to apply to the injected value and key/value pairs
     * @return result of injecting the injectedValue into the map
     * @since 11.1
     */
    default <IV> IV injectIntoKeyValue(IV injectedValue, ObjectLongCharToObjectFunction<? super IV, ? extends IV> function)
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
    LazyLongIterable keysView();

    /**
     * Returns a view of the key/value pairs in this map. This iterable is backed by
     * the map, so any modifications to the underlying map will be reflected in the
     * pairs returned by the iterable.
     * @return a view of the keys in this map
     * @since 5.0
     */
    RichIterable<LongCharPair> keyValuesView();

    /**
         * Return the CharLongMap that is obtained by flipping the direction of this map and making the associations
         * from value to key.
         *
         * @throws IllegalStateException if the CharLongMap contains duplicate values.
         * @since 9.0
         */
    CharLongMap flipUniqueValues();

    /**
     * Return a copy of this map containing only the key/value pairs that match the predicate.
     * @param predicate the predicate to determine which key/value pairs in this map should be
     * included in the returned map
     * @return a copy of this map with the matching key/value pairs
     */
    LongCharMap select(LongCharPredicate predicate);

    /**
     * Return a copy of this map containing only the key/value pairs that do not match the
     * predicate.
     * @param predicate the predicate to determine which key/value pairs in this map should be
     * excluded from the returned map
     * @return a copy of this map without the matching key/value pairs
     */
    LongCharMap reject(LongCharPredicate predicate);

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
     * @return a string representation of this LongCharMap
     */
    @Override
    String toString();

    /**
     * Returns a copy of this map that is immutable (if this map is mutable) or
     * itself if it is already immutable.
     * @return an immutable map that is equivalent to this one
     */
    ImmutableLongCharMap toImmutable();

    /**
     * Returns a set containing all the keys in this map. The set is backed by the
     * map, so any modifications to the returned set will affect this map.
     * @return a mutable set containing the keys in this map
     */
    MutableLongSet keySet();
}
