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
import org.eclipse.collections.api.factory.map.primitive.ImmutableIntIntMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableIntIntMap;
import org.eclipse.collections.api.map.primitive.IntIntMap;
import org.eclipse.collections.impl.factory.primitive.IntIntMaps;

/**
 * ImmutableIntIntMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableIntIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableIntIntMapFactoryImpl implements ImmutableIntIntMapFactory
{
    public static final ImmutableIntIntMapFactory INSTANCE = new ImmutableIntIntMapFactoryImpl();

    @Override
    public ImmutableIntIntMap empty()
    {
        return ImmutableIntIntEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableIntIntMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntIntMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntIntMap of(int key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableIntIntMap with(int key, int value)
    {
        return new ImmutableIntIntSingletonMap(key, value);
    }

    @Override
    public ImmutableIntIntMap ofAll(IntIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableIntIntMap withAll(IntIntMap map)
    {
        if (map instanceof ImmutableIntIntMap)
        {
            return (ImmutableIntIntMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            int key = map.keysView().intIterator().next();
            return new ImmutableIntIntSingletonMap(key, map.get(key));
        }
        return new ImmutableIntIntHashMap(map);
    }

    @Override
    public <T> ImmutableIntIntMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        return IntIntMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
