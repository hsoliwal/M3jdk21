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
import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectIntMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectIntMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectIntMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectIntMaps;

/**
 * ImmutableObjectIntMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableObjectIntMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "unchecked"})
public class ImmutableObjectIntMapFactoryImpl implements ImmutableObjectIntMapFactory
{
    public static final ImmutableObjectIntMapFactory INSTANCE = new ImmutableObjectIntMapFactoryImpl();

    @Override
    public <K> ImmutableObjectIntMap<K> empty()
    {
        return (ImmutableObjectIntMap<K>) ImmutableObjectIntEmptyMap.INSTANCE;
    }

    @Override
    public <K> ImmutableObjectIntMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectIntMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectIntMap<K> of(K key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> ImmutableObjectIntMap<K> with(K key, int value)
    {
        return new ImmutableObjectIntSingletonMap<>(key, value);
    }

    @Override
    public <K> ImmutableObjectIntMap<K> ofAll(ObjectIntMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> ImmutableObjectIntMap<K> withAll(ObjectIntMap<? extends K> map)
    {
        if (map instanceof ImmutableObjectIntMap)
        {
            return (ImmutableObjectIntMap<K>) map;
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
            return new ImmutableObjectIntSingletonMap<>((K) array[0], map.get(array[0]));
        }
        return new ImmutableObjectIntHashMap<>(map);
    }

    @Override
    public <T, K> ImmutableObjectIntMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, IntFunction<? super T> valueFunction)
    {
        MutableObjectIntMap<K> map = ObjectIntMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
