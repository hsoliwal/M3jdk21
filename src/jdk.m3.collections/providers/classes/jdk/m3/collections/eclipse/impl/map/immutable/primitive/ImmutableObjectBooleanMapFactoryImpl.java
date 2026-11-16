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
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectBooleanMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectBooleanMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectBooleanMaps;

/**
 * ImmutableObjectBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableObjectBooleanMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "unchecked"})
public class ImmutableObjectBooleanMapFactoryImpl implements ImmutableObjectBooleanMapFactory
{
    public static final ImmutableObjectBooleanMapFactory INSTANCE = new ImmutableObjectBooleanMapFactoryImpl();

    @Override
    public <K> ImmutableObjectBooleanMap<K> empty()
    {
        return (ImmutableObjectBooleanMap<K>) ImmutableObjectBooleanEmptyMap.INSTANCE;
    }

    @Override
    public <K> ImmutableObjectBooleanMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectBooleanMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectBooleanMap<K> of(K key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> ImmutableObjectBooleanMap<K> with(K key, boolean value)
    {
        return new ImmutableObjectBooleanSingletonMap<>(key, value);
    }

    @Override
    public <K> ImmutableObjectBooleanMap<K> ofAll(ObjectBooleanMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> ImmutableObjectBooleanMap<K> withAll(ObjectBooleanMap<? extends K> map)
    {
        if (map instanceof ImmutableObjectBooleanMap)
        {
            return (ImmutableObjectBooleanMap<K>) map;
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
            return new ImmutableObjectBooleanSingletonMap<>((K) array[0], map.get(array[0]));
        }
        return new ImmutableObjectBooleanHashMap<>(map);
    }

    @Override
    public <T, K> ImmutableObjectBooleanMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        MutableObjectBooleanMap<K> map = ObjectBooleanMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
