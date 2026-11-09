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
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableShortFloatMapFactory;
import org.eclipse.collections.impl.factory.primitive.ShortFloatMaps;
import org.eclipse.collections.api.map.primitive.MutableShortFloatMap;
import org.eclipse.collections.api.map.primitive.ShortFloatMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableShortFloatMapFactoryImpl is a factory implementation which creates instances of type {@link MutableShortFloatMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableShortFloatMapFactoryImpl implements MutableShortFloatMapFactory
{
    public static final MutableShortFloatMapFactory INSTANCE = new MutableShortFloatMapFactoryImpl();

    @Override
    public MutableShortFloatMap empty()
    {
        return new ShortFloatHashMap(0);
    }

    @Override
    public MutableShortFloatMap of()
    {
        return this.empty();
    }

    @Override
    public MutableShortFloatMap with()
    {
        return this.empty();
    }

    @Override
    public MutableShortFloatMap with(short key, float value)
    {
        return ShortFloatHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableShortFloatMap of(short key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableShortFloatMap of(short key1, float value1, short key2, float value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableShortFloatMap with(short key1, float value1, short key2, float value2)
    {
        return ShortFloatHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableShortFloatMap of(short key1, float value1, short key2, float value2, short key3, float value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortFloatMap with(short key1, float value1, short key2, float value2, short key3, float value3)
    {
        return ShortFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableShortFloatMap of(
            short key1, float value1, short key2, float value2, short key3, float value3, short key4, float value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortFloatMap with(
            short key1, float value1, short key2, float value2, short key3, float value3, short key4, float value4)
    {
        return ShortFloatHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableShortFloatMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableShortFloatMap withInitialCapacity(int capacity)
    {
        return new ShortFloatHashMap(capacity);
    }

    @Override
    public MutableShortFloatMap ofAll(ShortFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableShortFloatMap withAll(ShortFloatMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ShortFloatHashMap(map);
    }

    @Override
    public <T> MutableShortFloatMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        MutableShortFloatMap map = ShortFloatMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.shortValueOf(each), valueFunction.floatValueOf(each)));
        return map;
    }
}
