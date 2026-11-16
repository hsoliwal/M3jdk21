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

import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongIntMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.LongIntMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongIntMaps;

/**
 * ImmutableLongIntMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableLongIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableLongIntMapFactoryImpl implements ImmutableLongIntMapFactory
{
    public static final ImmutableLongIntMapFactory INSTANCE = new ImmutableLongIntMapFactoryImpl();

    @Override
    public ImmutableLongIntMap empty()
    {
        return ImmutableLongIntEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableLongIntMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongIntMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongIntMap of(long key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableLongIntMap with(long key, int value)
    {
        return new ImmutableLongIntSingletonMap(key, value);
    }

    @Override
    public ImmutableLongIntMap ofAll(LongIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableLongIntMap withAll(LongIntMap map)
    {
        if (map instanceof ImmutableLongIntMap)
        {
            return (ImmutableLongIntMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            long key = map.keysView().longIterator().next();
            return new ImmutableLongIntSingletonMap(key, map.get(key));
        }
        return new ImmutableLongIntHashMap(map);
    }

    @Override
    public <T> ImmutableLongIntMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        return LongIntMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
