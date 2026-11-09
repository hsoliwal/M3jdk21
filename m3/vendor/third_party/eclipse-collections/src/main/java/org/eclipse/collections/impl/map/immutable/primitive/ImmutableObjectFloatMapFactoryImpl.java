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
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableObjectFloatMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.MutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.eclipse.collections.impl.factory.primitive.ObjectFloatMaps;

/**
 * ImmutableObjectFloatMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableObjectFloatMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableObjectFloatMapFactoryImpl implements ImmutableObjectFloatMapFactory
{
    public static final ImmutableObjectFloatMapFactory INSTANCE = new ImmutableObjectFloatMapFactoryImpl();

    @Override
    public <K> ImmutableObjectFloatMap<K> empty()
    {
        return (ImmutableObjectFloatMap<K>) ImmutableObjectFloatEmptyMap.INSTANCE;
    }

    @Override
    public <K> ImmutableObjectFloatMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectFloatMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectFloatMap<K> of(K key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> ImmutableObjectFloatMap<K> with(K key, float value)
    {
        return new ImmutableObjectFloatSingletonMap<>(key, value);
    }

    @Override
    public <K> ImmutableObjectFloatMap<K> ofAll(ObjectFloatMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> ImmutableObjectFloatMap<K> withAll(ObjectFloatMap<? extends K> map)
    {
        if (map instanceof ImmutableObjectFloatMap)
        {
            return (ImmutableObjectFloatMap<K>) map;
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
            return new ImmutableObjectFloatSingletonMap<>((K) array[0], map.get(array[0]));
        }
        return new ImmutableObjectFloatHashMap<>(map);
    }

    @Override
    public <T, K> ImmutableObjectFloatMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, FloatFunction<? super T> valueFunction)
    {
        MutableObjectFloatMap<K> map = ObjectFloatMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
