/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.immutable.primitive;

import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatShortMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableFloatShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatShortMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatShortMaps;

/**
 * ImmutableFloatShortMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableFloatShortMapFactoryImpl implements ImmutableFloatShortMapFactory
{
    public static final ImmutableFloatShortMapFactory INSTANCE = new ImmutableFloatShortMapFactoryImpl();

    @Override
    public ImmutableFloatShortMap empty()
    {
        return ImmutableFloatShortEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableFloatShortMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatShortMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatShortMap of(float key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableFloatShortMap with(float key, short value)
    {
        return new ImmutableFloatShortSingletonMap(key, value);
    }

    @Override
    public ImmutableFloatShortMap ofAll(FloatShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableFloatShortMap withAll(FloatShortMap map)
    {
        if (map instanceof ImmutableFloatShortMap)
        {
            return (ImmutableFloatShortMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            float key = map.keysView().floatIterator().next();
            return new ImmutableFloatShortSingletonMap(key, map.get(key));
        }
        return new ImmutableFloatShortHashMap(map);
    }

    @Override
    public <T> ImmutableFloatShortMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        return FloatShortMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
