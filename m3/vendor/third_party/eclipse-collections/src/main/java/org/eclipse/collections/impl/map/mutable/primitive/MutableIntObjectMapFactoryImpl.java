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
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableIntObjectMapFactory;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableIntObjectMapFactoryImpl is a factory implementation which creates instances of type {@link MutableIntObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableIntObjectMapFactoryImpl implements MutableIntObjectMapFactory
{
    public static final MutableIntObjectMapFactory INSTANCE = new MutableIntObjectMapFactoryImpl();

    @Override
    public <V> MutableIntObjectMap<V> empty()
    {
        return new IntObjectHashMap(0);
    }

    @Override
    public <V> MutableIntObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> MutableIntObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> MutableIntObjectMap<V> of(int key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> MutableIntObjectMap<V> with(int key, V value)
    {
        return IntObjectHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <V> MutableIntObjectMap<V> of(int key1, V value1, int key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableIntObjectMap<V> with(int key1, V value1, int key2, V value2)
    {
        return IntObjectHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableIntObjectMap<V> of(int key1, V value1, int key2, V value2, int key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableIntObjectMap<V> with(int key1, V value1, int key2, V value2, int key3, V value3)
    {
        return IntObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableIntObjectMap<V> of(int key1, V value1, int key2, V value2, int key3, V value3, int key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableIntObjectMap<V> with(int key1, V value1, int key2, V value2, int key3, V value3, int key4, V value4)
    {
        return IntObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableIntObjectMap<V> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <V> MutableIntObjectMap<V> withInitialCapacity(int capacity)
    {
        return new IntObjectHashMap<>(capacity);
    }

    @Override
    public <V> MutableIntObjectMap<V> ofAll(IntObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> MutableIntObjectMap<V> withAll(IntObjectMap<? extends V> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new IntObjectHashMap<>(map);
    }

    @Override
    public <T, V> MutableIntObjectMap<V> from(Iterable<T> iterable, IntFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        MutableIntObjectMap<V> map = IntObjectMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.intValueOf(each), valueFunction.valueOf(each)));
        return map;
    }
}
