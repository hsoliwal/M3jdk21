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
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectShortMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectShortMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectShortMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectShortMaps;

/**
 * ImmutableObjectShortMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableObjectShortMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "unchecked"})
public class ImmutableObjectShortMapFactoryImpl implements ImmutableObjectShortMapFactory
{
    public static final ImmutableObjectShortMapFactory INSTANCE = new ImmutableObjectShortMapFactoryImpl();

    @Override
    public <K> ImmutableObjectShortMap<K> empty()
    {
        return (ImmutableObjectShortMap<K>) ImmutableObjectShortEmptyMap.INSTANCE;
    }

    @Override
    public <K> ImmutableObjectShortMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectShortMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectShortMap<K> of(K key, short value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> ImmutableObjectShortMap<K> with(K key, short value)
    {
        return new ImmutableObjectShortSingletonMap<>(key, value);
    }

    @Override
    public <K> ImmutableObjectShortMap<K> ofAll(ObjectShortMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> ImmutableObjectShortMap<K> withAll(ObjectShortMap<? extends K> map)
    {
        if (map instanceof ImmutableObjectShortMap)
        {
            return (ImmutableObjectShortMap<K>) map;
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
            return new ImmutableObjectShortSingletonMap<>((K) array[0], map.get(array[0]));
        }
        return new ImmutableObjectShortHashMap<>(map);
    }

    @Override
    public <T, K> ImmutableObjectShortMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, ShortFunction<? super T> valueFunction)
    {
        MutableObjectShortMap<K> map = ObjectShortMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
