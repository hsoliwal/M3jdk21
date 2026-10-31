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
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableObjectCharMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.MutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.ObjectCharMap;
import org.eclipse.collections.impl.factory.primitive.ObjectCharMaps;

/**
 * ImmutableObjectCharMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableObjectCharMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableObjectCharMapFactoryImpl implements ImmutableObjectCharMapFactory
{
    public static final ImmutableObjectCharMapFactory INSTANCE = new ImmutableObjectCharMapFactoryImpl();

    @Override
    public <K> ImmutableObjectCharMap<K> empty()
    {
        return (ImmutableObjectCharMap<K>) ImmutableObjectCharEmptyMap.INSTANCE;
    }

    @Override
    public <K> ImmutableObjectCharMap<K> of()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectCharMap<K> with()
    {
        return this.empty();
    }

    @Override
    public <K> ImmutableObjectCharMap<K> of(K key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public <K> ImmutableObjectCharMap<K> with(K key, char value)
    {
        return new ImmutableObjectCharSingletonMap<>(key, value);
    }

    @Override
    public <K> ImmutableObjectCharMap<K> ofAll(ObjectCharMap<? extends K> map)
    {
        return this.withAll(map);
    }

    @Override
    public <K> ImmutableObjectCharMap<K> withAll(ObjectCharMap<? extends K> map)
    {
        if (map instanceof ImmutableObjectCharMap)
        {
            return (ImmutableObjectCharMap<K>) map;
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
            return new ImmutableObjectCharSingletonMap<>((K) array[0], map.get(array[0]));
        }
        return new ImmutableObjectCharHashMap<>(map);
    }

    @Override
    public <T, K> ImmutableObjectCharMap<K> from(Iterable<T> iterable, Function<? super T, ? extends K> keyFunction, CharFunction<? super T> valueFunction)
    {
        MutableObjectCharMap<K> map = ObjectCharMaps.mutable.from(iterable, keyFunction, valueFunction);
        return map.toImmutable();
    }
}
