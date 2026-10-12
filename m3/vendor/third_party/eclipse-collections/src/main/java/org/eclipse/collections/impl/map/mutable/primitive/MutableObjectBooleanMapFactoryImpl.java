/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableObjectBooleanMapFactory;
import org.eclipse.collections.api.map.primitive.MutableObjectBooleanMap;
import org.eclipse.collections.api.map.primitive.ObjectBooleanMap;
import org.eclipse.collections.impl.factory.primitive.ObjectBooleanMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableObjectBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectBooleanMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableObjectBooleanMapFactoryImpl implements MutableObjectBooleanMapFactory
{
    public static final MutableObjectBooleanMapFactory INSTANCE = new MutableObjectBooleanMapFactoryImpl();

    @Override
    public <K> MutableObjectBooleanMap<K> empty()
    {
        return new ObjectBooleanHashMap(0);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectBooleanMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectBooleanMap<K> of(K key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> with(K key, boolean value)
    {
        return ObjectBooleanHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> of(K key1, boolean value1, K key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> with(K key1, boolean value1, K key2, boolean value2)
    {
        return ObjectBooleanHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> of(K key1, boolean value1, K key2, boolean value2, K key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> with(K key1, boolean value1, K key2, boolean value2, K key3, boolean value3)
    {
        return ObjectBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> of(K key1, boolean value1, K key2, boolean value2, K key3, boolean value3, K key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> with(K key1, boolean value1, K key2, boolean value2, K key3, boolean value3, K key4, boolean value4)
    {
        return ObjectBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> withInitialCapacity(int capacity)
    {
        return new ObjectBooleanHashMap<>(capacity);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> ofAll(ObjectBooleanMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> MutableObjectBooleanMap<K> withAll(ObjectBooleanMap<? extends K> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ObjectBooleanHashMap<>(map);
    }

    @Override
    public <T, K> MutableObjectBooleanMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        MutableObjectBooleanMap<K> map = ObjectBooleanMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.valueOf(each), valueFunction.booleanValueOf(each)));
        return map;
    }
}
