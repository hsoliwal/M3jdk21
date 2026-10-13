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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableByteCharMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableByteCharMap;
import org.eclipse.collections.api.map.primitive.ByteCharMap;
import org.eclipse.collections.impl.factory.primitive.ByteCharMaps;

/**
 * ImmutableByteCharMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteCharMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableByteCharMapFactoryImpl implements ImmutableByteCharMapFactory
{
    public static final ImmutableByteCharMapFactory INSTANCE = new ImmutableByteCharMapFactoryImpl();

    @Override
    public ImmutableByteCharMap empty()
    {
        return ImmutableByteCharEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableByteCharMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteCharMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableByteCharMap of(byte key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableByteCharMap with(byte key, char value)
    {
        return new ImmutableByteCharSingletonMap(key, value);
    }

    @Override
    public ImmutableByteCharMap ofAll(ByteCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableByteCharMap withAll(ByteCharMap map)
    {
        if (map instanceof ImmutableByteCharMap)
        {
            return (ImmutableByteCharMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            byte key = map.keysView().byteIterator().next();
            return new ImmutableByteCharSingletonMap(key, map.get(key));
        }
        return new ImmutableByteCharHashMap(map);
    }

    @Override
    public <T> ImmutableByteCharMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        return ByteCharMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
