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

import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteDoubleMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableByteDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.ByteDoubleMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteDoubleMaps;

/**
 * ImmutableByteDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableByteDoubleMapFactoryImpl implements ImmutableByteDoubleMapFactory
{
    public static final ImmutableByteDoubleMapFactory INSTANCE = new ImmutableByteDoubleMapFactoryImpl();

    @Override
    public ImmutableByteDoubleMap empty()
    {
        return ImmutableByteDoubleEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableByteDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteDoubleMap of(byte key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableByteDoubleMap with(byte key, double value)
    {
        return new ImmutableByteDoubleSingletonMap(key, value);
    }

    @Override
    public ImmutableByteDoubleMap ofAll(ByteDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableByteDoubleMap withAll(ByteDoubleMap map)
    {
        if (map instanceof ImmutableByteDoubleMap)
        {
            return (ImmutableByteDoubleMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            byte key = map.keysView().byteIterator().next();
            return new ImmutableByteDoubleSingletonMap(key, map.get(key));
        }
        return new ImmutableByteDoubleHashMap(map);
    }

    @Override
    public <T> ImmutableByteDoubleMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        return ByteDoubleMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
