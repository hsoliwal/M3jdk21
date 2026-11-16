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
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectFloatMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectFloatMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectFloatMaps;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableObjectFloatMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectFloatMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "rawtypes", "unchecked"})
public class MutableObjectFloatMapFactoryImpl implements MutableObjectFloatMapFactory
{
    public static final MutableObjectFloatMapFactory INSTANCE = new MutableObjectFloatMapFactoryImpl();

    @Override
    public <K> MutableObjectFloatMap<K> empty()
    {
        return new ObjectFloatHashMap(0);
    }

    @Override
    public <K> MutableObjectFloatMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectFloatMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectFloatMap<K> of(K key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> MutableObjectFloatMap<K> with(K key, float value)
    {
        return ObjectFloatHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <K> MutableObjectFloatMap<K> of(K key1, float value1, K key2, float value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectFloatMap<K> with(K key1, float value1, K key2, float value2)
    {
        return ObjectFloatHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectFloatMap<K> of(K key1, float value1, K key2, float value2, K key3, float value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectFloatMap<K> with(K key1, float value1, K key2, float value2, K key3, float value3)
    {
        return ObjectFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectFloatMap<K> of(K key1, float value1, K key2, float value2, K key3, float value3, K key4, float value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectFloatMap<K> with(K key1, float value1, K key2, float value2, K key3, float value3, K key4, float value4)
    {
        return ObjectFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectFloatMap<K> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <K> MutableObjectFloatMap<K> withInitialCapacity(int capacity)
    {
        return new ObjectFloatHashMap<>(capacity);
    }

    @Override
    public <K> MutableObjectFloatMap<K> ofAll(ObjectFloatMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> MutableObjectFloatMap<K> withAll(ObjectFloatMap<? extends K> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ObjectFloatHashMap<>(map);
    }

    @Override
    public <T, K> MutableObjectFloatMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, FloatFunction<? super T> valueFunction)
    {
        MutableObjectFloatMap<K> map = ObjectFloatMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.valueOf(each), valueFunction.floatValueOf(each)));
        return map;
    }
}
