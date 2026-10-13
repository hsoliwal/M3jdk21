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

import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableCharIntMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableCharIntMap;
import org.eclipse.collections.api.map.primitive.CharIntMap;
import org.eclipse.collections.impl.factory.primitive.CharIntMaps;

/**
 * ImmutableCharIntMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableCharIntMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableCharIntMapFactoryImpl implements ImmutableCharIntMapFactory
{
    public static final ImmutableCharIntMapFactory INSTANCE = new ImmutableCharIntMapFactoryImpl();

    @Override
    public ImmutableCharIntMap empty()
    {
        return ImmutableCharIntEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableCharIntMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharIntMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharIntMap of(char key, int value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableCharIntMap with(char key, int value)
    {
        return new ImmutableCharIntSingletonMap(key, value);
    }

    @Override
    public ImmutableCharIntMap ofAll(CharIntMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableCharIntMap withAll(CharIntMap map)
    {
        if (map instanceof ImmutableCharIntMap)
        {
            return (ImmutableCharIntMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            char key = map.keysView().charIterator().next();
            return new ImmutableCharIntSingletonMap(key, map.get(key));
        }
        return new ImmutableCharIntHashMap(map);
    }

    @Override
    public <T> ImmutableCharIntMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, IntFunction<? super T> valueFunction)
    {
        return CharIntMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
