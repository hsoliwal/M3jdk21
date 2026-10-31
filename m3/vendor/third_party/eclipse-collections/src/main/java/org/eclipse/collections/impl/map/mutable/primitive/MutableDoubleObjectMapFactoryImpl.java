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
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleObjectMapFactory;
import org.eclipse.collections.api.map.primitive.MutableDoubleObjectMap;
import org.eclipse.collections.api.map.primitive.DoubleObjectMap;
import org.eclipse.collections.impl.factory.primitive.DoubleObjectMaps;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableDoubleObjectMapFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleObjectMap}.
 * This file was automatically generated from template file mutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleObjectMapFactoryImpl implements MutableDoubleObjectMapFactory
{
    public static final MutableDoubleObjectMapFactory INSTANCE = new MutableDoubleObjectMapFactoryImpl();

    @Override
    public <V> MutableDoubleObjectMap<V> empty()
    {
        return new DoubleObjectHashMap(0);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> MutableDoubleObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> MutableDoubleObjectMap<V> of(double key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> with(double key, V value)
    {
        return DoubleObjectHashMap.newWithKeysValues(key, value);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> of(double key1, V value1, double key2, V value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> with(double key1, V value1, double key2, V value2)
    {
        return DoubleObjectHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> of(double key1, V value1, double key2, V value2, double key3, V value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> with(double key1, V value1, double key2, V value2, double key3, V value3)
    {
        return DoubleObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> of(double key1, V value1, double key2, V value2, double key3, V value3, double key4, V value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> with(double key1, V value1, double key2, V value2, double key3, V value3, double key4, V value4)
    {
        return DoubleObjectHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> withInitialCapacity(int capacity)
    {
        return new DoubleObjectHashMap<>(capacity);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> ofAll(DoubleObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> MutableDoubleObjectMap<V> withAll(DoubleObjectMap<? extends V> map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new DoubleObjectHashMap<>(map);
    }

    @Override
    public <T, V> MutableDoubleObjectMap<V> from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        MutableDoubleObjectMap<V> map = DoubleObjectMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.doubleValueOf(each), valueFunction.valueOf(each)));
        return map;
    }
}
