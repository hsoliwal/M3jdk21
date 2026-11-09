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
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableByteObjectMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableByteObjectMap;
import org.eclipse.collections.api.map.primitive.ByteObjectMap;
import org.eclipse.collections.impl.factory.primitive.ByteObjectMaps;

/**
 * ImmutableByteObjectMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableByteObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableByteObjectMapFactoryImpl implements ImmutableByteObjectMapFactory
{
    public static final ImmutableByteObjectMapFactory INSTANCE = new ImmutableByteObjectMapFactoryImpl();

    @Override
    public <V> ImmutableByteObjectMap<V> empty()
    {
        return (ImmutableByteObjectMap<V>) ImmutableByteObjectEmptyMap.INSTANCE;
    }

    @Override
    public <V> ImmutableByteObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableByteObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableByteObjectMap<V> of(byte key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> ImmutableByteObjectMap<V> with(byte key, V value)
    {
        return new ImmutableByteObjectSingletonMap<>(key, value);
    }

    @Override
    public <V> ImmutableByteObjectMap<V> ofAll(ByteObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> ImmutableByteObjectMap<V> withAll(ByteObjectMap<? extends V> map)
    {
        if (map instanceof ImmutableByteObjectMap)
        {
            return (ImmutableByteObjectMap<V>) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            //TODO use keysView() when available.
            final byte[] array = new byte[1];
            map.forEachKey((byte each) -> array[0] = each);
            return new ImmutableByteObjectSingletonMap<>(array[0], map.get(array[0]));
        }
        return new ImmutableByteObjectHashMap<>(map);
    }

    @Override
    public <T, V> ImmutableByteObjectMap<V> from(Iterable<T> iterable, ByteFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        ByteObjectMap<V> map = ByteObjectMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
