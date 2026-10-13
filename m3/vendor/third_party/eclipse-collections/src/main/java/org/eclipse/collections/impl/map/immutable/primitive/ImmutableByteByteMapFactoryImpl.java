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

import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableByteByteMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableByteByteMap;
import org.eclipse.collections.api.map.primitive.ByteByteMap;
import org.eclipse.collections.impl.factory.primitive.ByteByteMaps;

/**
 * ImmutableByteByteMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableByteByteMapFactoryImpl implements ImmutableByteByteMapFactory
{
    public static final ImmutableByteByteMapFactory INSTANCE = new ImmutableByteByteMapFactoryImpl();

    @Override
    public ImmutableByteByteMap empty()
    {
        return ImmutableByteByteEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableByteByteMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteByteMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteByteMap of(byte key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableByteByteMap with(byte key, byte value)
    {
        return new ImmutableByteByteSingletonMap(key, value);
    }

    @Override
    public ImmutableByteByteMap ofAll(ByteByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableByteByteMap withAll(ByteByteMap map)
    {
        if (map instanceof ImmutableByteByteMap)
        {
            return (ImmutableByteByteMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            byte key = map.keysView().byteIterator().next();
            return new ImmutableByteByteSingletonMap(key, map.get(key));
        }
        return new ImmutableByteByteHashMap(map);
    }

    @Override
    public <T> ImmutableByteByteMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        return ByteByteMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
