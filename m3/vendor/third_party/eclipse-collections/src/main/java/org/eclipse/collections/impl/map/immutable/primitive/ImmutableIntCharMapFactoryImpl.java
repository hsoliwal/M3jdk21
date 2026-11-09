/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableIntCharMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableIntCharMap;
import org.eclipse.collections.api.map.primitive.IntCharMap;
import org.eclipse.collections.impl.factory.primitive.IntCharMaps;

/**
 * ImmutableIntCharMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableIntCharMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableIntCharMapFactoryImpl implements ImmutableIntCharMapFactory
{
    public static final ImmutableIntCharMapFactory INSTANCE = new ImmutableIntCharMapFactoryImpl();

    @Override
    public ImmutableIntCharMap empty()
    {
        return ImmutableIntCharEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableIntCharMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntCharMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntCharMap of(int key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableIntCharMap with(int key, char value)
    {
        return new ImmutableIntCharSingletonMap(key, value);
    }

    @Override
    public ImmutableIntCharMap ofAll(IntCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableIntCharMap withAll(IntCharMap map)
    {
        if (map instanceof ImmutableIntCharMap)
        {
            return (ImmutableIntCharMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            int key = map.keysView().intIterator().next();
            return new ImmutableIntCharSingletonMap(key, map.get(key));
        }
        return new ImmutableIntCharHashMap(map);
    }

    @Override
    public <T> ImmutableIntCharMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        return IntCharMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
