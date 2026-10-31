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
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableIntBooleanMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableIntBooleanMap;
import org.eclipse.collections.api.map.primitive.IntBooleanMap;
import org.eclipse.collections.impl.factory.primitive.IntBooleanMaps;

/**
 * ImmutableIntBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableIntBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableIntBooleanMapFactoryImpl implements ImmutableIntBooleanMapFactory
{
    public static final ImmutableIntBooleanMapFactory INSTANCE = new ImmutableIntBooleanMapFactoryImpl();

    @Override
    public ImmutableIntBooleanMap empty()
    {
        return ImmutableIntBooleanEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableIntBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntBooleanMap of(int key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableIntBooleanMap with(int key, boolean value)
    {
        return new ImmutableIntBooleanSingletonMap(key, value);
    }

    @Override
    public ImmutableIntBooleanMap ofAll(IntBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableIntBooleanMap withAll(IntBooleanMap map)
    {
        if (map instanceof ImmutableIntBooleanMap)
        {
            return (ImmutableIntBooleanMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            int key = map.keysView().intIterator().next();
            return new ImmutableIntBooleanSingletonMap(key, map.get(key));
        }
        return new ImmutableIntBooleanHashMap(map);
    }

    @Override
    public <T> ImmutableIntBooleanMap from(Iterable<T> iterable, IntFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        return IntBooleanMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
