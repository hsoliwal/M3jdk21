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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableLongShortMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableLongShortMap;
import org.eclipse.collections.api.map.primitive.LongShortMap;
import org.eclipse.collections.impl.factory.primitive.LongShortMaps;

/**
 * ImmutableLongShortMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableLongShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableLongShortMapFactoryImpl implements ImmutableLongShortMapFactory
{
    public static final ImmutableLongShortMapFactory INSTANCE = new ImmutableLongShortMapFactoryImpl();

    @Override
    public ImmutableLongShortMap empty()
    {
        return ImmutableLongShortEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableLongShortMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongShortMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongShortMap of(long key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableLongShortMap with(long key, short value)
    {
        return new ImmutableLongShortSingletonMap(key, value);
    }

    @Override
    public ImmutableLongShortMap ofAll(LongShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableLongShortMap withAll(LongShortMap map)
    {
        if (map instanceof ImmutableLongShortMap)
        {
            return (ImmutableLongShortMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            long key = map.keysView().longIterator().next();
            return new ImmutableLongShortSingletonMap(key, map.get(key));
        }
        return new ImmutableLongShortHashMap(map);
    }

    @Override
    public <T> ImmutableLongShortMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        return LongShortMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
