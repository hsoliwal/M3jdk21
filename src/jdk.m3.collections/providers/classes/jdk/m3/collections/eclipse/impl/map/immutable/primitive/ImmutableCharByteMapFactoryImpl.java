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

import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharByteMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableCharByteMap;
import jdk.m3.collections.eclipse.api.map.primitive.CharByteMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharByteMaps;

/**
 * ImmutableCharByteMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableCharByteMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableCharByteMapFactoryImpl implements ImmutableCharByteMapFactory
{
    public static final ImmutableCharByteMapFactory INSTANCE = new ImmutableCharByteMapFactoryImpl();

    @Override
    public ImmutableCharByteMap empty()
    {
        return ImmutableCharByteEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableCharByteMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharByteMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharByteMap of(char key, byte value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableCharByteMap with(char key, byte value)
    {
        return new ImmutableCharByteSingletonMap(key, value);
    }

    @Override
    public ImmutableCharByteMap ofAll(CharByteMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableCharByteMap withAll(CharByteMap map)
    {
        if (map instanceof ImmutableCharByteMap)
        {
            return (ImmutableCharByteMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            char key = map.keysView().charIterator().next();
            return new ImmutableCharByteSingletonMap(key, map.get(key));
        }
        return new ImmutableCharByteHashMap(map);
    }

    @Override
    public <T> ImmutableCharByteMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, ByteFunction<? super T> valueFunction)
    {
        return CharByteMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
