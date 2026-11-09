/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableFloatDoubleMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableFloatDoubleMap;
import org.eclipse.collections.api.map.primitive.FloatDoubleMap;
import org.eclipse.collections.impl.factory.primitive.FloatDoubleMaps;

/**
 * ImmutableFloatDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableFloatDoubleMapFactoryImpl implements ImmutableFloatDoubleMapFactory
{
    public static final ImmutableFloatDoubleMapFactory INSTANCE = new ImmutableFloatDoubleMapFactoryImpl();

    @Override
    public ImmutableFloatDoubleMap empty()
    {
        return ImmutableFloatDoubleEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableFloatDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatDoubleMap of(float key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableFloatDoubleMap with(float key, double value)
    {
        return new ImmutableFloatDoubleSingletonMap(key, value);
    }

    @Override
    public ImmutableFloatDoubleMap ofAll(FloatDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableFloatDoubleMap withAll(FloatDoubleMap map)
    {
        if (map instanceof ImmutableFloatDoubleMap)
        {
            return (ImmutableFloatDoubleMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            float key = map.keysView().floatIterator().next();
            return new ImmutableFloatDoubleSingletonMap(key, map.get(key));
        }
        return new ImmutableFloatDoubleHashMap(map);
    }

    @Override
    public <T> ImmutableFloatDoubleMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        return FloatDoubleMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
