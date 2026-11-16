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
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongLongMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.LongLongMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongLongMaps;

/**
 * ImmutableLongLongMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableLongLongMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableLongLongMapFactoryImpl implements ImmutableLongLongMapFactory
{
    public static final ImmutableLongLongMapFactory INSTANCE = new ImmutableLongLongMapFactoryImpl();

    @Override
    public ImmutableLongLongMap empty()
    {
        return ImmutableLongLongEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableLongLongMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongLongMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongLongMap of(long key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableLongLongMap with(long key, long value)
    {
        return new ImmutableLongLongSingletonMap(key, value);
    }

    @Override
    public ImmutableLongLongMap ofAll(LongLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableLongLongMap withAll(LongLongMap map)
    {
        if (map instanceof ImmutableLongLongMap)
        {
            return (ImmutableLongLongMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            long key = map.keysView().longIterator().next();
            return new ImmutableLongLongSingletonMap(key, map.get(key));
        }
        return new ImmutableLongLongHashMap(map);
    }

    @Override
    public <T> ImmutableLongLongMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        return LongLongMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
