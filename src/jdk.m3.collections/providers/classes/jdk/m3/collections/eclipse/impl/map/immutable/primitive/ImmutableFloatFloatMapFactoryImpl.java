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
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatFloatMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableFloatFloatMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatFloatMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatFloatMaps;

/**
 * ImmutableFloatFloatMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableFloatFloatMapFactoryImpl implements ImmutableFloatFloatMapFactory
{
    public static final ImmutableFloatFloatMapFactory INSTANCE = new ImmutableFloatFloatMapFactoryImpl();

    @Override
    public ImmutableFloatFloatMap empty()
    {
        return ImmutableFloatFloatEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableFloatFloatMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatFloatMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatFloatMap of(float key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableFloatFloatMap with(float key, float value)
    {
        return new ImmutableFloatFloatSingletonMap(key, value);
    }

    @Override
    public ImmutableFloatFloatMap ofAll(FloatFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableFloatFloatMap withAll(FloatFloatMap map)
    {
        if (map instanceof ImmutableFloatFloatMap)
        {
            return (ImmutableFloatFloatMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            float key = map.keysView().floatIterator().next();
            return new ImmutableFloatFloatSingletonMap(key, map.get(key));
        }
        return new ImmutableFloatFloatHashMap(map);
    }

    @Override
    public <T> ImmutableFloatFloatMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        return FloatFloatMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
