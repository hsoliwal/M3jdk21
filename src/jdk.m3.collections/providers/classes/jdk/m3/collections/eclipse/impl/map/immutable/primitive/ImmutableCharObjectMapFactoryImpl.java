/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.immutable.primitive;

import jdk.m3.collections.eclipse.api.block.function.Function;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharObjectMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableCharObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.CharObjectMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharObjectMaps;

/**
 * ImmutableCharObjectMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableCharObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "unchecked"})
public class ImmutableCharObjectMapFactoryImpl implements ImmutableCharObjectMapFactory
{
    public static final ImmutableCharObjectMapFactory INSTANCE = new ImmutableCharObjectMapFactoryImpl();

    @Override
    public <V> ImmutableCharObjectMap<V> empty()
    {
        return (ImmutableCharObjectMap<V>) ImmutableCharObjectEmptyMap.INSTANCE;
    }

    @Override
    public <V> ImmutableCharObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableCharObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableCharObjectMap<V> of(char key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> ImmutableCharObjectMap<V> with(char key, V value)
    {
        return new ImmutableCharObjectSingletonMap<>(key, value);
    }

    @Override
    public <V> ImmutableCharObjectMap<V> ofAll(CharObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> ImmutableCharObjectMap<V> withAll(CharObjectMap<? extends V> map)
    {
        if (map instanceof ImmutableCharObjectMap)
        {
            return (ImmutableCharObjectMap<V>) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            //TODO use keysView() when available.
            final char[] array = new char[1];
            map.forEachKey((char each) -> array[0] = each);
            return new ImmutableCharObjectSingletonMap<>(array[0], map.get(array[0]));
        }
        return new ImmutableCharObjectHashMap<>(map);
    }

    @Override
    public <T, V> ImmutableCharObjectMap<V> from(Iterable<T> iterable, CharFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        CharObjectMap<V> map = CharObjectMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
