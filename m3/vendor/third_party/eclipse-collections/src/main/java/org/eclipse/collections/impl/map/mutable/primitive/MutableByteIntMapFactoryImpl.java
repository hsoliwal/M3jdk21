/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableByteIntMapFactory;
import org.eclipse.collections.impl.factory.primitive.ByteIntMaps;
import org.eclipse.collections.api.map.primitive.MutableByteIntMap;
import org.eclipse.collections.api.map.primitive.ByteIntMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableByteIntMapFactoryImpl is a factory implementation which creates instances of type {@link MutableByteIntMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableByteIntMapFactoryImpl implements MutableByteIntMapFactory
{
    public static final MutableByteIntMapFactory INSTANCE = new MutableByteIntMapFactoryImpl();

    @Override
    public MutableByteIntMap empty()
    {
        return new ByteIntHashMap(0);
    }

    @Override
    public MutableByteIntMap of()
    {
        return this.empty();
    }

    @Override
    public MutableByteIntMap with()
    {
        return this.empty();
    }

    @Override
    public MutableByteIntMap with(byte key, int value)
    {
        return ByteIntHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableByteIntMap of(byte key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableByteIntMap of(byte key1, int value1, byte key2, int value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableByteIntMap with(byte key1, int value1, byte key2, int value2)
    {
        return ByteIntHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableByteIntMap of(byte key1, int value1, byte key2, int value2, byte key3, int value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteIntMap with(byte key1, int value1, byte key2, int value2, byte key3, int value3)
    {
        return ByteIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableByteIntMap of(
            byte key1, int value1, byte key2, int value2, byte key3, int value3, byte key4, int value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteIntMap with(
            byte key1, int value1, byte key2, int value2, byte key3, int value3, byte key4, int value4)
    {
        return ByteIntHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableByteIntMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableByteIntMap withInitialCapacity(int capacity)
    {
        return new ByteIntHashMap(capacity);
    }

    @Override
    public MutableByteIntMap ofAll(ByteIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableByteIntMap withAll(ByteIntMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new ByteIntHashMap(map);
    }

    @Override
    public <T> MutableByteIntMap from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        MutableByteIntMap map = ByteIntMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.byteValueOf(each), valueFunction.intValueOf(each)));
        return map;
    }
}
