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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableShortLongMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableShortLongMap;
import org.eclipse.collections.api.map.primitive.ShortLongMap;
import org.eclipse.collections.impl.factory.primitive.ShortLongMaps;

/**
 * ImmutableShortLongMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortLongMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableShortLongMapFactoryImpl implements ImmutableShortLongMapFactory
{
    public static final ImmutableShortLongMapFactory INSTANCE = new ImmutableShortLongMapFactoryImpl();

    @Override
    public ImmutableShortLongMap empty()
    {
        return ImmutableShortLongEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableShortLongMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortLongMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortLongMap of(short key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableShortLongMap with(short key, long value)
    {
        return new ImmutableShortLongSingletonMap(key, value);
    }

    @Override
    public ImmutableShortLongMap ofAll(ShortLongMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableShortLongMap withAll(ShortLongMap map)
    {
        if (map instanceof ImmutableShortLongMap)
        {
            return (ImmutableShortLongMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            short key = map.keysView().shortIterator().next();
            return new ImmutableShortLongSingletonMap(key, map.get(key));
        }
        return new ImmutableShortLongHashMap(map);
    }

    @Override
    public <T> ImmutableShortLongMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, LongFunction<? super T> valueFunction)
    {
        return ShortLongMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
