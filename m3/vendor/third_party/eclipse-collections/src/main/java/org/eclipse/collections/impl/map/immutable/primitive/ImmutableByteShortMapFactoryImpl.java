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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableByteShortMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableByteShortMap;
import org.eclipse.collections.api.map.primitive.ByteShortMap;
import org.eclipse.collections.impl.factory.primitive.ByteShortMaps;

/**
 * ImmutableByteShortMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteShortMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableByteShortMapFactoryImpl implements ImmutableByteShortMapFactory
{
    public static final ImmutableByteShortMapFactory INSTANCE = new ImmutableByteShortMapFactoryImpl();

    @Override
    public ImmutableByteShortMap empty()
    {
        return ImmutableByteShortEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableByteShortMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteShortMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteShortMap of(byte key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableByteShortMap with(byte key, short value)
    {
        return new ImmutableByteShortSingletonMap(key, value);
    }

    @Override
    public ImmutableByteShortMap ofAll(ByteShortMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableByteShortMap withAll(ByteShortMap map)
    {
        if (map instanceof ImmutableByteShortMap)
        {
            return (ImmutableByteShortMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            byte key = map.keysView().byteIterator().next();
            return new ImmutableByteShortSingletonMap(key, map.get(key));
        }
        return new ImmutableByteShortHashMap(map);
    }

    @Override
    public <T> ImmutableByteShortMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, ShortFunction<? super T> valueFunction)
    {
        return ByteShortMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
