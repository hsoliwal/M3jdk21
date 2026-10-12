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
import org.eclipse.collections.api.factory.map.primitive.MutableObjectCharMapFactory;
import org.eclipse.collections.api.map.primitive.MutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.ObjectCharMap;
import org.eclipse.collections.impl.factory.primitive.ObjectCharMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableObjectCharMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectCharMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableObjectCharMapFactoryImpl implements MutableObjectCharMapFactory
{
    public static final MutableObjectCharMapFactory INSTANCE = new MutableObjectCharMapFactoryImpl();

    @Override
    public <K> MutableObjectCharMap<K> empty()
    {
        return new ObjectCharHashMap(0);
    }

    @Override
    public <K> MutableObjectCharMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectCharMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectCharMap<K> of(K key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> MutableObjectCharMap<K> with(K key, char value)
    {
        return ObjectCharHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <K> MutableObjectCharMap<K> of(K key1, char value1, K key2, char value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectCharMap<K> with(K key1, char value1, K key2, char value2)
    {
        return ObjectCharHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectCharMap<K> of(K key1, char value1, K key2, char value2, K key3, char value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectCharMap<K> with(K key1, char value1, K key2, char value2, K key3, char value3)
    {
        return ObjectCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectCharMap<K> of(K key1, char value1, K key2, char value2, K key3, char value3, K key4, char value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectCharMap<K> with(K key1, char value1, K key2, char value2, K key3, char value3, K key4, char value4)
    {
        return ObjectCharHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectCharMap<K> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <K> MutableObjectCharMap<K> withInitialCapacity(int capacity)
    {
        return new ObjectCharHashMap<>(capacity);
    }

    @Override
    public <K> MutableObjectCharMap<K> ofAll(ObjectCharMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> MutableObjectCharMap<K> withAll(ObjectCharMap<? extends K> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ObjectCharHashMap<>(map);
    }

    @Override
    public <T, K> MutableObjectCharMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, CharFunction<? super T> valueFunction)
    {
        MutableObjectCharMap<K> map = ObjectCharMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.valueOf(each), valueFunction.charValueOf(each)));
        return map;
    }
}
