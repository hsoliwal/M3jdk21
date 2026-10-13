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

import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableShortDoubleMapFactory;
import org.eclipse.collections.impl.factory.primitive.ShortDoubleMaps;
import org.eclipse.collections.api.map.primitive.MutableShortDoubleMap;
import org.eclipse.collections.api.map.primitive.ShortDoubleMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableShortDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link MutableShortDoubleMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableShortDoubleMapFactoryImpl implements MutableShortDoubleMapFactory
{
    public static final MutableShortDoubleMapFactory INSTANCE = new MutableShortDoubleMapFactoryImpl();

    @Override
    public MutableShortDoubleMap empty()
    {
        return new ShortDoubleHashMap(0);
    }

    @Override
    public MutableShortDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public MutableShortDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public MutableShortDoubleMap with(short key, double value)
    {
        return ShortDoubleHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableShortDoubleMap of(short key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableShortDoubleMap of(short key1, double value1, short key2, double value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableShortDoubleMap with(short key1, double value1, short key2, double value2)
    {
        return ShortDoubleHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableShortDoubleMap of(short key1, double value1, short key2, double value2, short key3, double value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortDoubleMap with(short key1, double value1, short key2, double value2, short key3, double value3)
    {
        return ShortDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortDoubleMap of(
            short key1, double value1, short key2, double value2, short key3, double value3, short key4, double value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortDoubleMap with(
            short key1, double value1, short key2, double value2, short key3, double value3, short key4, double value4)
    {
        return ShortDoubleHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortDoubleMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableShortDoubleMap withInitialCapacity(int capacity)
    {
        return new ShortDoubleHashMap(capacity);
    }

    @Override
    public MutableShortDoubleMap ofAll(ShortDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableShortDoubleMap withAll(ShortDoubleMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ShortDoubleHashMap(map);
    }

    @Override
    public <T> MutableShortDoubleMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        MutableShortDoubleMap map = ShortDoubleMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.shortValueOf(each), valueFunction.doubleValueOf(each)));
        return map;
    }
}
