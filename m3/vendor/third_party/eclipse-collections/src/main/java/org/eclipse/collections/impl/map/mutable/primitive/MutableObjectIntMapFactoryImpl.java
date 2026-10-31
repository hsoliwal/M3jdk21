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
import org.eclipse.collections.api.factory.map.primitive.MutableObjectIntMapFactory;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableObjectIntMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectIntMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableObjectIntMapFactoryImpl implements MutableObjectIntMapFactory
{
    public static final MutableObjectIntMapFactory INSTANCE = new MutableObjectIntMapFactoryImpl();

    @Override
    public <K> MutableObjectIntMap<K> empty()
    {
        return new ObjectIntHashMap(0);
    }

    @Override
    public <K> MutableObjectIntMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectIntMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectIntMap<K> of(K key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> MutableObjectIntMap<K> with(K key, int value)
    {
        return ObjectIntHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <K> MutableObjectIntMap<K> of(K key1, int value1, K key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectIntMap<K> with(K key1, int value1, K key2, int value2)
    {
        return ObjectIntHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectIntMap<K> of(K key1, int value1, K key2, int value2, K key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectIntMap<K> with(K key1, int value1, K key2, int value2, K key3, int value3)
    {
        return ObjectIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectIntMap<K> of(K key1, int value1, K key2, int value2, K key3, int value3, K key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectIntMap<K> with(K key1, int value1, K key2, int value2, K key3, int value3, K key4, int value4)
    {
        return ObjectIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectIntMap<K> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <K> MutableObjectIntMap<K> withInitialCapacity(int capacity)
    {
        return new ObjectIntHashMap<>(capacity);
    }

    @Override
    public <K> MutableObjectIntMap<K> ofAll(ObjectIntMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> MutableObjectIntMap<K> withAll(ObjectIntMap<? extends K> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ObjectIntHashMap<>(map);
    }

    @Override
    public <T, K> MutableObjectIntMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, IntFunction<? super T> valueFunction)
    {
        MutableObjectIntMap<K> map = ObjectIntMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.valueOf(each), valueFunction.intValueOf(each)));
        return map;
    }
}
