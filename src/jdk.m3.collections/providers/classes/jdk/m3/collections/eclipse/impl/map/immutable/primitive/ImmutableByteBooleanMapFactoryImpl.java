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
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteBooleanMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableByteBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.ByteBooleanMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteBooleanMaps;

/**
 * ImmutableByteBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableByteBooleanMapFactoryImpl implements ImmutableByteBooleanMapFactory
{
    public static final ImmutableByteBooleanMapFactory INSTANCE = new ImmutableByteBooleanMapFactoryImpl();

    @Override
    public ImmutableByteBooleanMap empty()
    {
        return ImmutableByteBooleanEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableByteBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteBooleanMap of(byte key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableByteBooleanMap with(byte key, boolean value)
    {
        return new ImmutableByteBooleanSingletonMap(key, value);
    }

    @Override
    public ImmutableByteBooleanMap ofAll(ByteBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableByteBooleanMap withAll(ByteBooleanMap map)
    {
        if (map instanceof ImmutableByteBooleanMap)
        {
            return (ImmutableByteBooleanMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            byte key = map.keysView().byteIterator().next();
            return new ImmutableByteBooleanSingletonMap(key, map.get(key));
        }
        return new ImmutableByteBooleanHashMap(map);
    }

    @Override
    public <T> ImmutableByteBooleanMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        return ByteBooleanMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
