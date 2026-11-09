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

import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableIntFloatMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableIntFloatMap;
import org.eclipse.collections.api.map.primitive.IntFloatMap;
import org.eclipse.collections.impl.factory.primitive.IntFloatMaps;

/**
 * ImmutableIntFloatMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableIntFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableIntFloatMapFactoryImpl implements ImmutableIntFloatMapFactory
{
    public static final ImmutableIntFloatMapFactory INSTANCE = new ImmutableIntFloatMapFactoryImpl();

    @Override
    public ImmutableIntFloatMap empty()
    {
        return ImmutableIntFloatEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableIntFloatMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntFloatMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntFloatMap of(int key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableIntFloatMap with(int key, float value)
    {
        return new ImmutableIntFloatSingletonMap(key, value);
    }

    @Override
    public ImmutableIntFloatMap ofAll(IntFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableIntFloatMap withAll(IntFloatMap map)
    {
        if (map instanceof ImmutableIntFloatMap)
        {
            return (ImmutableIntFloatMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            int key = map.keysView().intIterator().next();
            return new ImmutableIntFloatSingletonMap(key, map.get(key));
        }
        return new ImmutableIntFloatHashMap(map);
    }

    @Override
    public <T> ImmutableIntFloatMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        return IntFloatMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
