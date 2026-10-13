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

import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableShortIntMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableShortIntMap;
import org.eclipse.collections.api.map.primitive.ShortIntMap;
import org.eclipse.collections.impl.factory.primitive.ShortIntMaps;

/**
 * ImmutableShortIntMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableShortIntMapFactoryImpl implements ImmutableShortIntMapFactory
{
    public static final ImmutableShortIntMapFactory INSTANCE = new ImmutableShortIntMapFactoryImpl();

    @Override
    public ImmutableShortIntMap empty()
    {
        return ImmutableShortIntEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableShortIntMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortIntMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortIntMap of(short key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableShortIntMap with(short key, int value)
    {
        return new ImmutableShortIntSingletonMap(key, value);
    }

    @Override
    public ImmutableShortIntMap ofAll(ShortIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableShortIntMap withAll(ShortIntMap map)
    {
        if (map instanceof ImmutableShortIntMap)
        {
            return (ImmutableShortIntMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            short key = map.keysView().shortIterator().next();
            return new ImmutableShortIntSingletonMap(key, map.get(key));
        }
        return new ImmutableShortIntHashMap(map);
    }

    @Override
    public <T> ImmutableShortIntMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        return ShortIntMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
