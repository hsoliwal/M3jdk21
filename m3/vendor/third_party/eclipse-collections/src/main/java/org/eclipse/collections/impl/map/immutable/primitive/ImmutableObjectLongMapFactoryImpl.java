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
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableObjectLongMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.ObjectLongMap;
import org.eclipse.collections.impl.factory.primitive.ObjectLongMaps;

/**
 * ImmutableObjectLongMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableObjectLongMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableObjectLongMapFactoryImpl implements ImmutableObjectLongMapFactory
{
    public static final ImmutableObjectLongMapFactory INSTANCE = new ImmutableObjectLongMapFactoryImpl();

    @Override
    public <K> ImmutableObjectLongMap<K> empty()
    {
        return (ImmutableObjectLongMap<K>) ImmutableObjectLongEmptyMap.INSTANCE;
    }

    @Override
    public <K> ImmutableObjectLongMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectLongMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectLongMap<K> of(K key, long value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> ImmutableObjectLongMap<K> with(K key, long value)
    {
        return new ImmutableObjectLongSingletonMap<>(key, value);
    }

    @Override
    public <K> ImmutableObjectLongMap<K> ofAll(ObjectLongMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> ImmutableObjectLongMap<K> withAll(ObjectLongMap<? extends K> map)
    {
        if (map instanceof ImmutableObjectLongMap)
        {
            return (ImmutableObjectLongMap<K>) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            //TODO use keysView() when available.
            final Object[] array = new Object[1];
            map.forEachKey((K each) -> array[0] = each);
            return new ImmutableObjectLongSingletonMap<>((K) array[0], map.get(array[0]));
        }
        return new ImmutableObjectLongHashMap<>(map);
    }

    @Override
    public <T, K> ImmutableObjectLongMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, LongFunction<? super T> valueFunction)
    {
        MutableObjectLongMap<K> map = ObjectLongMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
