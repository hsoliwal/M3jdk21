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
import org.eclipse.collections.api.factory.map.primitive.MutableByteObjectMapFactory;
import org.eclipse.collections.api.map.primitive.MutableByteObjectMap;
import org.eclipse.collections.api.map.primitive.ByteObjectMap;
import org.eclipse.collections.impl.factory.primitive.ByteObjectMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableByteObjectMapFactoryImpl is a factory implementation which creates instances of type {@link MutableByteObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableByteObjectMapFactoryImpl implements MutableByteObjectMapFactory
{
    public static final MutableByteObjectMapFactory INSTANCE = new MutableByteObjectMapFactoryImpl();

    @Override
    public <V> MutableByteObjectMap<V> empty()
    {
        return new ByteObjectHashMap(0);
    }

    @Override
    public <V> MutableByteObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> MutableByteObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> MutableByteObjectMap<V> of(byte key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> MutableByteObjectMap<V> with(byte key, V value)
    {
        return ByteObjectHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <V> MutableByteObjectMap<V> of(byte key1, V value1, byte key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableByteObjectMap<V> with(byte key1, V value1, byte key2, V value2)
    {
        return ByteObjectHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableByteObjectMap<V> of(byte key1, V value1, byte key2, V value2, byte key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableByteObjectMap<V> with(byte key1, V value1, byte key2, V value2, byte key3, V value3)
    {
        return ByteObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableByteObjectMap<V> of(byte key1, V value1, byte key2, V value2, byte key3, V value3, byte key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableByteObjectMap<V> with(byte key1, V value1, byte key2, V value2, byte key3, V value3, byte key4, V value4)
    {
        return ByteObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableByteObjectMap<V> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <V> MutableByteObjectMap<V> withInitialCapacity(int capacity)
    {
        return new ByteObjectHashMap<>(capacity);
    }

    @Override
    public <V> MutableByteObjectMap<V> ofAll(ByteObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> MutableByteObjectMap<V> withAll(ByteObjectMap<? extends V> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ByteObjectHashMap<>(map);
    }

    @Override
    public <T, V> MutableByteObjectMap<V> from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        MutableByteObjectMap<V> map = ByteObjectMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.byteValueOf(each), valueFunction.valueOf(each)));
        return map;
    }
}
