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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableLongObjectMapFactory;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.map.primitive.LongObjectMap;
import org.eclipse.collections.impl.factory.primitive.LongObjectMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableLongObjectMapFactoryImpl is a factory implementation which creates instances of type {@link MutableLongObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableLongObjectMapFactoryImpl implements MutableLongObjectMapFactory
{
    public static final MutableLongObjectMapFactory INSTANCE = new MutableLongObjectMapFactoryImpl();

    @Override
    public <V> MutableLongObjectMap<V> empty()
    {
        return new LongObjectHashMap(0);
    }

    @Override
    public <V> MutableLongObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> MutableLongObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> MutableLongObjectMap<V> of(long key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> MutableLongObjectMap<V> with(long key, V value)
    {
        return LongObjectHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <V> MutableLongObjectMap<V> of(long key1, V value1, long key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableLongObjectMap<V> with(long key1, V value1, long key2, V value2)
    {
        return LongObjectHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableLongObjectMap<V> of(long key1, V value1, long key2, V value2, long key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableLongObjectMap<V> with(long key1, V value1, long key2, V value2, long key3, V value3)
    {
        return LongObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableLongObjectMap<V> of(long key1, V value1, long key2, V value2, long key3, V value3, long key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableLongObjectMap<V> with(long key1, V value1, long key2, V value2, long key3, V value3, long key4, V value4)
    {
        return LongObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableLongObjectMap<V> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <V> MutableLongObjectMap<V> withInitialCapacity(int capacity)
    {
        return new LongObjectHashMap<>(capacity);
    }

    @Override
    public <V> MutableLongObjectMap<V> ofAll(LongObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> MutableLongObjectMap<V> withAll(LongObjectMap<? extends V> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new LongObjectHashMap<>(map);
    }

    @Override
    public <T, V> MutableLongObjectMap<V> from(Iterable<T> iterable, LongFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        MutableLongObjectMap<V> map = LongObjectMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.longValueOf(each), valueFunction.valueOf(each)));
        return map;
    }
}
