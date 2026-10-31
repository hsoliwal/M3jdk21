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

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableLongFloatMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableLongFloatMap;
import org.eclipse.collections.api.map.primitive.LongFloatMap;
import org.eclipse.collections.impl.factory.primitive.LongFloatMaps;

/**
 * ImmutableLongFloatMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableLongFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableLongFloatMapFactoryImpl implements ImmutableLongFloatMapFactory
{
    public static final ImmutableLongFloatMapFactory INSTANCE = new ImmutableLongFloatMapFactoryImpl();

    @Override
    public ImmutableLongFloatMap empty()
    {
        return ImmutableLongFloatEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableLongFloatMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongFloatMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongFloatMap of(long key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableLongFloatMap with(long key, float value)
    {
        return new ImmutableLongFloatSingletonMap(key, value);
    }

    @Override
    public ImmutableLongFloatMap ofAll(LongFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableLongFloatMap withAll(LongFloatMap map)
    {
        if (map instanceof ImmutableLongFloatMap)
        {
            return (ImmutableLongFloatMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            long key = map.keysView().longIterator().next();
            return new ImmutableLongFloatSingletonMap(key, map.get(key));
        }
        return new ImmutableLongFloatHashMap(map);
    }

    @Override
    public <T> ImmutableLongFloatMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        return LongFloatMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
