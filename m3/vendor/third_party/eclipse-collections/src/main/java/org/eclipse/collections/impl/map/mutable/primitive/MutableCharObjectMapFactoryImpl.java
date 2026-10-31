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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableCharObjectMapFactory;
import org.eclipse.collections.api.map.primitive.MutableCharObjectMap;
import org.eclipse.collections.api.map.primitive.CharObjectMap;
import org.eclipse.collections.impl.factory.primitive.CharObjectMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableCharObjectMapFactoryImpl is a factory implementation which creates instances of type {@link MutableCharObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableCharObjectMapFactoryImpl implements MutableCharObjectMapFactory
{
    public static final MutableCharObjectMapFactory INSTANCE = new MutableCharObjectMapFactoryImpl();

    @Override
    public <V> MutableCharObjectMap<V> empty()
    {
        return new CharObjectHashMap(0);
    }

    @Override
    public <V> MutableCharObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> MutableCharObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> MutableCharObjectMap<V> of(char key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> MutableCharObjectMap<V> with(char key, V value)
    {
        return CharObjectHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <V> MutableCharObjectMap<V> of(char key1, V value1, char key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableCharObjectMap<V> with(char key1, V value1, char key2, V value2)
    {
        return CharObjectHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableCharObjectMap<V> of(char key1, V value1, char key2, V value2, char key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableCharObjectMap<V> with(char key1, V value1, char key2, V value2, char key3, V value3)
    {
        return CharObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableCharObjectMap<V> of(char key1, V value1, char key2, V value2, char key3, V value3, char key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableCharObjectMap<V> with(char key1, V value1, char key2, V value2, char key3, V value3, char key4, V value4)
    {
        return CharObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableCharObjectMap<V> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <V> MutableCharObjectMap<V> withInitialCapacity(int capacity)
    {
        return new CharObjectHashMap<>(capacity);
    }

    @Override
    public <V> MutableCharObjectMap<V> ofAll(CharObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> MutableCharObjectMap<V> withAll(CharObjectMap<? extends V> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new CharObjectHashMap<>(map);
    }

    @Override
    public <T, V> MutableCharObjectMap<V> from(Iterable<T> iterable, CharFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        MutableCharObjectMap<V> map = CharObjectMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.charValueOf(each), valueFunction.valueOf(each)));
        return map;
    }
}
