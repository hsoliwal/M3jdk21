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

import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleLongMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableDoubleLongMap;
import jdk.m3.collections.eclipse.api.map.primitive.DoubleLongMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleLongMaps;

/**
 * ImmutableDoubleLongMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleLongMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableDoubleLongMapFactoryImpl implements ImmutableDoubleLongMapFactory
{
    public static final ImmutableDoubleLongMapFactory INSTANCE = new ImmutableDoubleLongMapFactoryImpl();

    @Override
    public ImmutableDoubleLongMap empty()
    {
        return ImmutableDoubleLongEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableDoubleLongMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleLongMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleLongMap of(double key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableDoubleLongMap with(double key, long value)
    {
        return new ImmutableDoubleLongSingletonMap(key, value);
    }

    @Override
    public ImmutableDoubleLongMap ofAll(DoubleLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableDoubleLongMap withAll(DoubleLongMap map)
    {
        if (map instanceof ImmutableDoubleLongMap)
        {
            return (ImmutableDoubleLongMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            double key = map.keysView().doubleIterator().next();
            return new ImmutableDoubleLongSingletonMap(key, map.get(key));
        }
        return new ImmutableDoubleLongHashMap(map);
    }

    @Override
    public <T> ImmutableDoubleLongMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        return DoubleLongMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
