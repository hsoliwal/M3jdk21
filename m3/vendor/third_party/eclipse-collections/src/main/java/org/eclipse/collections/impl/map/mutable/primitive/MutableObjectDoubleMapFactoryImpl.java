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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableObjectDoubleMapFactory;
import org.eclipse.collections.api.map.primitive.MutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.ObjectDoubleMap;
import org.eclipse.collections.impl.factory.primitive.ObjectDoubleMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableObjectDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectDoubleMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableObjectDoubleMapFactoryImpl implements MutableObjectDoubleMapFactory
{
    public static final MutableObjectDoubleMapFactory INSTANCE = new MutableObjectDoubleMapFactoryImpl();

    @Override
    public <K> MutableObjectDoubleMap<K> empty()
    {
        return new ObjectDoubleHashMap(0);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectDoubleMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> MutableObjectDoubleMap<K> of(K key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> with(K key, double value)
    {
        return ObjectDoubleHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> of(K key1, double value1, K key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> with(K key1, double value1, K key2, double value2)
    {
        return ObjectDoubleHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> of(K key1, double value1, K key2, double value2, K key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> with(K key1, double value1, K key2, double value2, K key3, double value3)
    {
        return ObjectDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> of(K key1, double value1, K key2, double value2, K key3, double value3, K key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> with(K key1, double value1, K key2, double value2, K key3, double value3, K key4, double value4)
    {
        return ObjectDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> withInitialCapacity(int capacity)
    {
        return new ObjectDoubleHashMap<>(capacity);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> ofAll(ObjectDoubleMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> MutableObjectDoubleMap<K> withAll(ObjectDoubleMap<? extends K> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ObjectDoubleHashMap<>(map);
    }

    @Override
    public <T, K> MutableObjectDoubleMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        MutableObjectDoubleMap<K> map = ObjectDoubleMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.valueOf(each), valueFunction.doubleValueOf(each)));
        return map;
    }
}
