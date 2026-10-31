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

import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectLongPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableLongCollection;

/**
 * This file was automatically generated from template file immutableObjectPrimitiveMap.stg.
 *
 * @since 3.0.
 */
public interface ImmutableObjectLongMap<K> extends ObjectLongMap<K>
{
    @Override
    ImmutableObjectLongMap<K> select(ObjectLongPredicate<? super K> predicate);

    @Override
    ImmutableObjectLongMap<K> reject(ObjectLongPredicate<? super K> predicate);

    @Override
    ImmutableLongCollection select(LongPredicate predicate);

    @Override
    ImmutableLongCollection reject(LongPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableObjectLongMap<K> tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> ImmutableCollection<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * Copy this map, associate the value with the key (replacing the value if one
     * already exists for {@code key}), and return the copy as new immutable map.
     * A copy is always made even if {@code key} is already associated with {@code value}.
     * @param key the key to add
     * @param value the value to associate with the key in the copy
     * @return an immutable copy of this map with {@code value} associated with {@code key}
     */
    ImmutableObjectLongMap<K> newWithKeyValue(K key, long value);

    /**
     * Copy this map, remove any associated value with the key (if one exists), and
     * return the copy as a new immutable map.
     * @param key the key to remove
     * @return an immutable copy of this map with {@code key} removed
     */
    ImmutableObjectLongMap<K> newWithoutKey(K key);

    /**
     * Copy this map, remove any associated values with the specified keys (if any exist),
     * and return the copy as a new immutable map.
     * @param keys the keys to remove
     * @return an immutable copy of this map with all keys in {@code keys} removed
     */
    ImmutableObjectLongMap<K> newWithoutAllKeys(Iterable<? extends K> keys);

    @Override
    ImmutableLongObjectMap<K> flipUniqueValues();
}
