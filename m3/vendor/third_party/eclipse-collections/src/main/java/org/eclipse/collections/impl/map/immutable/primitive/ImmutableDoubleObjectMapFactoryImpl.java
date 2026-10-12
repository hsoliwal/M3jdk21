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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleObjectMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleObjectMap;
import org.eclipse.collections.api.map.primitive.DoubleObjectMap;
import org.eclipse.collections.impl.factory.primitive.DoubleObjectMaps;

/**
 * ImmutableDoubleObjectMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleObjectMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableDoubleObjectMapFactoryImpl implements ImmutableDoubleObjectMapFactory
{
    public static final ImmutableDoubleObjectMapFactory INSTANCE = new ImmutableDoubleObjectMapFactoryImpl();

    @Override
    public <V> ImmutableDoubleObjectMap<V> empty()
    {
        return (ImmutableDoubleObjectMap<V>) ImmutableDoubleObjectEmptyMap.INSTANCE;
    }

    @Override
    public <V> ImmutableDoubleObjectMap<V> of()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableDoubleObjectMap<V> with()
    {
        return this.empty();
    }

    @Override
    public <V> ImmutableDoubleObjectMap<V> of(double key, V value)
    {
        return this.with(key, value);
    }

    @Override
    public <V> ImmutableDoubleObjectMap<V> with(double key, V value)
    {
        return new ImmutableDoubleObjectSingletonMap<>(key, value);
    }

    @Override
    public <V> ImmutableDoubleObjectMap<V> ofAll(DoubleObjectMap<? extends V> map)
    {
        return this.withAll(map);
    }

    @Override
    public <V> ImmutableDoubleObjectMap<V> withAll(DoubleObjectMap<? extends V> map)
    {
        if (map instanceof ImmutableDoubleObjectMap)
        {
            return (ImmutableDoubleObjectMap<V>) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            //TODO use keysView() when available.
            final double[] array = new double[1];
            map.forEachKey((double each) -> array[0] = each);
            return new ImmutableDoubleObjectSingletonMap<>(array[0], map.get(array[0]));
        }
        return new ImmutableDoubleObjectHashMap<>(map);
    }

    @Override
    public <T, V> ImmutableDoubleObjectMap<V> from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, Function<? super T, ? extends V> valueFunction)
    {
        DoubleObjectMap<V> map = DoubleObjectMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
