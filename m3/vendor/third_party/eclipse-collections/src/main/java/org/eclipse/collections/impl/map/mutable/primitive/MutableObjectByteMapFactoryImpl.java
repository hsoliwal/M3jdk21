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
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableObjectByteMapFactory;
import org.eclipse.collections.api.map.primitive.MutableObjectByteMap;
import org.eclipse.collections.api.map.primitive.ObjectByteMap;
import org.eclipse.collections.impl.factory.primitive.ObjectByteMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableObjectByteMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectByteMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableObjectByteMapFactoryImpl implements MutableObjectByteMapFactory
{
    public static final MutableObjectByteMapFactory INSTANCE = new MutableObjectByteMapFactoryImpl();

    @Override
    public <K> MutableObjectByteMap<K> empty()
    {
        return new ObjectByteHashMap(0);
    }

    @Override
    public <K> MutableObjectByteMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectByteMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectByteMap<K> of(K key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> MutableObjectByteMap<K> with(K key, byte value)
    {
        return ObjectByteHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <K> MutableObjectByteMap<K> of(K key1, byte value1, K key2, byte value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectByteMap<K> with(K key1, byte value1, K key2, byte value2)
    {
        return ObjectByteHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectByteMap<K> of(K key1, byte value1, K key2, byte value2, K key3, byte value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectByteMap<K> with(K key1, byte value1, K key2, byte value2, K key3, byte value3)
    {
        return ObjectByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectByteMap<K> of(K key1, byte value1, K key2, byte value2, K key3, byte value3, K key4, byte value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectByteMap<K> with(K key1, byte value1, K key2, byte value2, K key3, byte value3, K key4, byte value4)
    {
        return ObjectByteHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectByteMap<K> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <K> MutableObjectByteMap<K> withInitialCapacity(int capacity)
    {
        return new ObjectByteHashMap<>(capacity);
    }

    @Override
    public <K> MutableObjectByteMap<K> ofAll(ObjectByteMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> MutableObjectByteMap<K> withAll(ObjectByteMap<? extends K> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ObjectByteHashMap<>(map);
    }

    @Override
    public <T, K> MutableObjectByteMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, ByteFunction<? super T> valueFunction)
    {
        MutableObjectByteMap<K> map = ObjectByteMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.valueOf(each), valueFunction.byteValueOf(each)));
        return map;
    }
}
