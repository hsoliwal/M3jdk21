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

import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableIntCollection;

/**
 * This file was automatically generated from template file immutableObjectPrimitiveMap.stg.
 *
 * @since 3.0.
 */
public interface ImmutableObjectIntMap<K> extends ObjectIntMap<K>
{
    @Override
    ImmutableObjectIntMap<K> select(ObjectIntPredicate<? super K> predicate);

    @Override
    ImmutableObjectIntMap<K> reject(ObjectIntPredicate<? super K> predicate);

    @Override
    ImmutableIntCollection select(IntPredicate predicate);

    @Override
    ImmutableIntCollection reject(IntPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableObjectIntMap<K> tap(IntProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> ImmutableCollection<V> collect(IntToObjectFunction<? extends V> function);

    /**
     * Copy this map, associate the value with the key (replacing the value if one
     * already exists for {@code key}), and return the copy as new immutable map.
     * A copy is always made even if {@code key} is already associated with {@code value}.
     * @param key the key to add
     * @param value the value to associate with the key in the copy
     * @return an immutable copy of this map with {@code value} associated with {@code key}
     */
    ImmutableObjectIntMap<K> newWithKeyValue(K key, int value);

    /**
     * Copy this map, remove any associated value with the key (if one exists), and
     * return the copy as a new immutable map.
     * @param key the key to remove
     * @return an immutable copy of this map with {@code key} removed
     */
    ImmutableObjectIntMap<K> newWithoutKey(K key);

    /**
     * Copy this map, remove any associated values with the specified keys (if any exist),
     * and return the copy as a new immutable map.
     * @param keys the keys to remove
     * @return an immutable copy of this map with all keys in {@code keys} removed
     */
    ImmutableObjectIntMap<K> newWithoutAllKeys(Iterable<? extends K> keys);

    @Override
    ImmutableIntObjectMap<K> flipUniqueValues();
}
