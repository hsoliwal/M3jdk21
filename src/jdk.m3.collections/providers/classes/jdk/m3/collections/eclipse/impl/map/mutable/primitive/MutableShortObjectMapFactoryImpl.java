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
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortObjectMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.MutableShortObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.ShortObjectMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortObjectMaps;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableShortObjectMapFactoryImpl is a factory implementation which creates instances of type {@link MutableShortObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "rawtypes", "unchecked"})
public class MutableShortObjectMapFactoryImpl implements MutableShortObjectMapFactory
{
    public static final MutableShortObjectMapFactory INSTANCE = new MutableShortObjectMapFactoryImpl();

    @Override
    public <V> MutableShortObjectMap<V> empty()
    {
        return new ShortObjectHashMap(0);
    }

    @Override
    public <V> MutableShortObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> MutableShortObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> MutableShortObjectMap<V> of(short key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> MutableShortObjectMap<V> with(short key, V value)
    {
        return ShortObjectHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <V> MutableShortObjectMap<V> of(short key1, V value1, short key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableShortObjectMap<V> with(short key1, V value1, short key2, V value2)
    {
        return ShortObjectHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableShortObjectMap<V> of(short key1, V value1, short key2, V value2, short key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableShortObjectMap<V> with(short key1, V value1, short key2, V value2, short key3, V value3)
    {
        return ShortObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableShortObjectMap<V> of(short key1, V value1, short key2, V value2, short key3, V value3, short key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableShortObjectMap<V> with(short key1, V value1, short key2, V value2, short key3, V value3, short key4, V value4)
    {
        return ShortObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableShortObjectMap<V> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <V> MutableShortObjectMap<V> withInitialCapacity(int capacity)
    {
        return new ShortObjectHashMap<>(capacity);
    }

    @Override
    public <V> MutableShortObjectMap<V> ofAll(ShortObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> MutableShortObjectMap<V> withAll(ShortObjectMap<? extends V> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ShortObjectHashMap<>(map);
    }

    @Override
    public <T, V> MutableShortObjectMap<V> from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        MutableShortObjectMap<V> map = ShortObjectMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.shortValueOf(each), valueFunction.valueOf(each)));
        return map;
    }
}
