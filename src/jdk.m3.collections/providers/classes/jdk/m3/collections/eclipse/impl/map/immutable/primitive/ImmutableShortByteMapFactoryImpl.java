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

import jdk.m3.collections.eclipse.api.block.function.primitive.ShortFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortByteMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.ShortByteMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortByteMaps;

/**
 * ImmutableShortByteMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableShortByteMapFactoryImpl implements ImmutableShortByteMapFactory
{
    public static final ImmutableShortByteMapFactory INSTANCE = new ImmutableShortByteMapFactoryImpl();

    @Override
    public ImmutableShortByteMap empty()
    {
        return ImmutableShortByteEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableShortByteMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortByteMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortByteMap of(short key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableShortByteMap with(short key, byte value)
    {
        return new ImmutableShortByteSingletonMap(key, value);
    }

    @Override
    public ImmutableShortByteMap ofAll(ShortByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableShortByteMap withAll(ShortByteMap map)
    {
        if (map instanceof ImmutableShortByteMap)
        {
            return (ImmutableShortByteMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            short key = map.keysView().shortIterator().next();
            return new ImmutableShortByteSingletonMap(key, map.get(key));
        }
        return new ImmutableShortByteHashMap(map);
    }

    @Override
    public <T> ImmutableShortByteMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        return ShortByteMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
