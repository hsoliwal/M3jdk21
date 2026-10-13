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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableFloatBooleanMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableFloatBooleanMap;
import org.eclipse.collections.api.map.primitive.FloatBooleanMap;
import org.eclipse.collections.impl.factory.primitive.FloatBooleanMaps;

/**
 * ImmutableFloatBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableFloatBooleanMapFactoryImpl implements ImmutableFloatBooleanMapFactory
{
    public static final ImmutableFloatBooleanMapFactory INSTANCE = new ImmutableFloatBooleanMapFactoryImpl();

    @Override
    public ImmutableFloatBooleanMap empty()
    {
        return ImmutableFloatBooleanEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableFloatBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatBooleanMap of(float key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableFloatBooleanMap with(float key, boolean value)
    {
        return new ImmutableFloatBooleanSingletonMap(key, value);
    }

    @Override
    public ImmutableFloatBooleanMap ofAll(FloatBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableFloatBooleanMap withAll(FloatBooleanMap map)
    {
        if (map instanceof ImmutableFloatBooleanMap)
        {
            return (ImmutableFloatBooleanMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            float key = map.keysView().floatIterator().next();
            return new ImmutableFloatBooleanSingletonMap(key, map.get(key));
        }
        return new ImmutableFloatBooleanHashMap(map);
    }

    @Override
    public <T> ImmutableFloatBooleanMap from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        return FloatBooleanMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
