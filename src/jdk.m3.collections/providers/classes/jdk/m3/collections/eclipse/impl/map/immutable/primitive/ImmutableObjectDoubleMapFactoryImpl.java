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
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectDoubleMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableObjectDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.ObjectDoubleMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ObjectDoubleMaps;

/**
 * ImmutableObjectDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableObjectDoubleMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings({"missing-explicit-ctor", "unchecked"})
public class ImmutableObjectDoubleMapFactoryImpl implements ImmutableObjectDoubleMapFactory
{
    public static final ImmutableObjectDoubleMapFactory INSTANCE = new ImmutableObjectDoubleMapFactoryImpl();

    @Override
    public <K> ImmutableObjectDoubleMap<K> empty()
    {
        return (ImmutableObjectDoubleMap<K>) ImmutableObjectDoubleEmptyMap.INSTANCE;
    }

    @Override
    public <K> ImmutableObjectDoubleMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectDoubleMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectDoubleMap<K> of(K key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> ImmutableObjectDoubleMap<K> with(K key, double value)
    {
        return new ImmutableObjectDoubleSingletonMap<>(key, value);
    }

    @Override
    public <K> ImmutableObjectDoubleMap<K> ofAll(ObjectDoubleMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> ImmutableObjectDoubleMap<K> withAll(ObjectDoubleMap<? extends K> map)
    {
        if (map instanceof ImmutableObjectDoubleMap)
        {
            return (ImmutableObjectDoubleMap<K>) map;
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
            return new ImmutableObjectDoubleSingletonMap<>((K) array[0], map.get(array[0]));
        }
        return new ImmutableObjectDoubleHashMap<>(map);
    }

    @Override
    public <T, K> ImmutableObjectDoubleMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        MutableObjectDoubleMap<K> map = ObjectDoubleMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
