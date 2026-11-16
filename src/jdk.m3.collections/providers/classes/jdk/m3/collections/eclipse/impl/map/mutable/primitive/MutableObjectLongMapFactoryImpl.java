/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import jdk.m3.collections.eclipse.api.block.function.Function;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectLongMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectLongMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectLongMaps;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableObjectLongMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectLongMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "rawtypes", "unchecked"})
public class MutableObjectLongMapFactoryImpl implements MutableObjectLongMapFactory
{
    public static final MutableObjectLongMapFactory INSTANCE = new MutableObjectLongMapFactoryImpl();

    @Override
    public <K> MutableObjectLongMap<K> empty()
    {
        return new ObjectLongHashMap(0);
    }

    @Override
    public <K> MutableObjectLongMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectLongMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectLongMap<K> of(K key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> MutableObjectLongMap<K> with(K key, long value)
    {
        return ObjectLongHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <K> MutableObjectLongMap<K> of(K key1, long value1, K key2, long value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectLongMap<K> with(K key1, long value1, K key2, long value2)
    {
        return ObjectLongHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectLongMap<K> of(K key1, long value1, K key2, long value2, K key3, long value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectLongMap<K> with(K key1, long value1, K key2, long value2, K key3, long value3)
    {
        return ObjectLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectLongMap<K> of(K key1, long value1, K key2, long value2, K key3, long value3, K key4, long value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectLongMap<K> with(K key1, long value1, K key2, long value2, K key3, long value3, K key4, long value4)
    {
        return ObjectLongHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectLongMap<K> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <K> MutableObjectLongMap<K> withInitialCapacity(int capacity)
    {
        return new ObjectLongHashMap<>(capacity);
    }

    @Override
    public <K> MutableObjectLongMap<K> ofAll(ObjectLongMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> MutableObjectLongMap<K> withAll(ObjectLongMap<? extends K> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ObjectLongHashMap<>(map);
    }

    @Override
    public <T, K> MutableObjectLongMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, LongFunction<? super T> valueFunction)
    {
        MutableObjectLongMap<K> map = ObjectLongMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.valueOf(each), valueFunction.longValueOf(each)));
        return map;
    }
}
