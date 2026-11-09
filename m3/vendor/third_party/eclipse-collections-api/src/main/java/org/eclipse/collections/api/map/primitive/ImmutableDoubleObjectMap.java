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
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.predicate.primitive.DoubleObjectPredicate;

/**
 * This file was automatically generated from template file immutablePrimitiveObjectMap.stg.
 *
 * @since 3.0.
 */
public interface ImmutableDoubleObjectMap<V> extends DoubleObjectMap<V>, ImmutablePrimitiveObjectMap<V>
{
    @Override
    ImmutableDoubleObjectMap<V> tap(Procedure<? super V> procedure);

    @Override
    ImmutableDoubleObjectMap<V> select(DoubleObjectPredicate<? super V> predicate);

    @Override
    ImmutableDoubleObjectMap<V> reject(DoubleObjectPredicate<? super V> predicate);

    /**
     * Copy this map, associate the value with the key (replacing the value if one
     * already exists for {@code key}), and return the copy as new immutable map.
     * A copy is always made even if {@code key} is already associated with {@code value}.
     * @param key the key to add
     * @param value the value to associate with the key in the copy
     * @return an immutable copy of this map with {@code value} associated with {@code key}
     */
    ImmutableDoubleObjectMap<V> newWithKeyValue(double key, V value);

    /**
     * Copy this map, remove any associated value with the key (if one exists), and
     * return the copy as a new immutable map.
     * @param key the key to remove
     * @return an immutable copy of this map with {@code key} removed
     */
    ImmutableDoubleObjectMap<V> newWithoutKey(double key);

    /**
     * Copy this map, remove any associated values with the specified keys (if any exist),
     * and return the copy as a new immutable map.
     * @param keys the keys to remove
     * @return an immutable copy of this map with all keys in {@code keys} removed
     */
    ImmutableDoubleObjectMap<V> newWithoutAllKeys(DoubleIterable keys);

    @Override
    ImmutableObjectDoubleMap<V> flipUniqueValues();
}
