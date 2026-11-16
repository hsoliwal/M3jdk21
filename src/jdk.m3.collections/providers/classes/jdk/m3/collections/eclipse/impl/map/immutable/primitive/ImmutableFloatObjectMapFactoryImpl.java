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
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatObjectMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableFloatObjectMap;
import jdk.m3.collections.eclipse.api.map.primitive.FloatObjectMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatObjectMaps;

/**
 * ImmutableFloatObjectMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "unchecked"})
public class ImmutableFloatObjectMapFactoryImpl implements ImmutableFloatObjectMapFactory
{
    public static final ImmutableFloatObjectMapFactory INSTANCE = new ImmutableFloatObjectMapFactoryImpl();

    @Override
    public <V> ImmutableFloatObjectMap<V> empty()
    {
        return (ImmutableFloatObjectMap<V>) ImmutableFloatObjectEmptyMap.INSTANCE;
    }

    @Override
    public <V> ImmutableFloatObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableFloatObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableFloatObjectMap<V> of(float key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> ImmutableFloatObjectMap<V> with(float key, V value)
    {
        return new ImmutableFloatObjectSingletonMap<>(key, value);
    }

    @Override
    public <V> ImmutableFloatObjectMap<V> ofAll(FloatObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> ImmutableFloatObjectMap<V> withAll(FloatObjectMap<? extends V> map)
    {
        if (map instanceof ImmutableFloatObjectMap)
        {
            return (ImmutableFloatObjectMap<V>) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            //TODO use keysView() when available.
            final float[] array = new float[1];
            map.forEachKey((float each) -> array[0] = each);
            return new ImmutableFloatObjectSingletonMap<>(array[0], map.get(array[0]));
        }
        return new ImmutableFloatObjectHashMap<>(map);
    }

    @Override
    public <T, V> ImmutableFloatObjectMap<V> from(Iterable<T> iterable, FloatFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        FloatObjectMap<V> map = FloatObjectMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
