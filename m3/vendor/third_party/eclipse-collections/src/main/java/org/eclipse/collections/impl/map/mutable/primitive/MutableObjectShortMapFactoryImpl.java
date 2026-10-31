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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableObjectShortMapFactory;
import org.eclipse.collections.api.map.primitive.MutableObjectShortMap;
import org.eclipse.collections.api.map.primitive.ObjectShortMap;
import org.eclipse.collections.impl.factory.primitive.ObjectShortMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableObjectShortMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectShortMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableObjectShortMapFactoryImpl implements MutableObjectShortMapFactory
{
    public static final MutableObjectShortMapFactory INSTANCE = new MutableObjectShortMapFactoryImpl();

    @Override
    public <K> MutableObjectShortMap<K> empty()
    {
        return new ObjectShortHashMap(0);
    }

    @Override
    public <K> MutableObjectShortMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectShortMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectShortMap<K> of(K key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> MutableObjectShortMap<K> with(K key, short value)
    {
        return ObjectShortHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <K> MutableObjectShortMap<K> of(K key1, short value1, K key2, short value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectShortMap<K> with(K key1, short value1, K key2, short value2)
    {
        return ObjectShortHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectShortMap<K> of(K key1, short value1, K key2, short value2, K key3, short value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectShortMap<K> with(K key1, short value1, K key2, short value2, K key3, short value3)
    {
        return ObjectShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectShortMap<K> of(K key1, short value1, K key2, short value2, K key3, short value3, K key4, short value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectShortMap<K> with(K key1, short value1, K key2, short value2, K key3, short value3, K key4, short value4)
    {
        return ObjectShortHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectShortMap<K> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <K> MutableObjectShortMap<K> withInitialCapacity(int capacity)
    {
        return new ObjectShortHashMap<>(capacity);
    }

    @Override
    public <K> MutableObjectShortMap<K> ofAll(ObjectShortMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> MutableObjectShortMap<K> withAll(ObjectShortMap<? extends K> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ObjectShortHashMap<>(map);
    }

    @Override
    public <T, K> MutableObjectShortMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, ShortFunction<? super T> valueFunction)
    {
        MutableObjectShortMap<K> map = ObjectShortMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.valueOf(each), valueFunction.shortValueOf(each)));
        return map;
    }
}
