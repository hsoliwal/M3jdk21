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
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableShortObjectMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableShortObjectMap;
import org.eclipse.collections.api.map.primitive.ShortObjectMap;
import org.eclipse.collections.impl.factory.primitive.ShortObjectMaps;

/**
 * ImmutableShortObjectMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableShortObjectMapFactoryImpl implements ImmutableShortObjectMapFactory
{
    public static final ImmutableShortObjectMapFactory INSTANCE = new ImmutableShortObjectMapFactoryImpl();

    @Override
    public <V> ImmutableShortObjectMap<V> empty()
    {
        return (ImmutableShortObjectMap<V>) ImmutableShortObjectEmptyMap.INSTANCE;
    }

    @Override
    public <V> ImmutableShortObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableShortObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableShortObjectMap<V> of(short key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> ImmutableShortObjectMap<V> with(short key, V value)
    {
        return new ImmutableShortObjectSingletonMap<>(key, value);
    }

    @Override
    public <V> ImmutableShortObjectMap<V> ofAll(ShortObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> ImmutableShortObjectMap<V> withAll(ShortObjectMap<? extends V> map)
    {
        if (map instanceof ImmutableShortObjectMap)
        {
            return (ImmutableShortObjectMap<V>) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            //TODO use keysView() when available.
            final short[] array = new short[1];
            map.forEachKey((short each) -> array[0] = each);
            return new ImmutableShortObjectSingletonMap<>(array[0], map.get(array[0]));
        }
        return new ImmutableShortObjectHashMap<>(map);
    }

    @Override
    public <T, V> ImmutableShortObjectMap<V> from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        ShortObjectMap<V> map = ShortObjectMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
