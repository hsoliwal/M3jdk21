/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableIntObjectMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;

/**
 * ImmutableIntObjectMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableIntObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableIntObjectMapFactoryImpl implements ImmutableIntObjectMapFactory
{
    public static final ImmutableIntObjectMapFactory INSTANCE = new ImmutableIntObjectMapFactoryImpl();

    @Override
    public <V> ImmutableIntObjectMap<V> empty()
    {
        return (ImmutableIntObjectMap<V>) ImmutableIntObjectEmptyMap.INSTANCE;
    }

    @Override
    public <V> ImmutableIntObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableIntObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableIntObjectMap<V> of(int key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> ImmutableIntObjectMap<V> with(int key, V value)
    {
        return new ImmutableIntObjectSingletonMap<>(key, value);
    }

    @Override
    public <V> ImmutableIntObjectMap<V> ofAll(IntObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> ImmutableIntObjectMap<V> withAll(IntObjectMap<? extends V> map)
    {
        if (map instanceof ImmutableIntObjectMap)
        {
            return (ImmutableIntObjectMap<V>) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            //TODO use keysView() when available.
            final int[] array = new int[1];
            map.forEachKey((int each) -> array[0] = each);
            return new ImmutableIntObjectSingletonMap<>(array[0], map.get(array[0]));
        }
        return new ImmutableIntObjectHashMap<>(map);
    }

    @Override
    public <T, V> ImmutableIntObjectMap<V> from(Iterable<T> iterable, IntFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        IntObjectMap<V> map = IntObjectMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
