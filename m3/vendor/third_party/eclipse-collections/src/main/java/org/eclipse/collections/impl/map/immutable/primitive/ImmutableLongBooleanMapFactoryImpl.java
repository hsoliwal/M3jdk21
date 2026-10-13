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

import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableLongBooleanMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableLongBooleanMap;
import org.eclipse.collections.api.map.primitive.LongBooleanMap;
import org.eclipse.collections.impl.factory.primitive.LongBooleanMaps;

/**
 * ImmutableLongBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableLongBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableLongBooleanMapFactoryImpl implements ImmutableLongBooleanMapFactory
{
    public static final ImmutableLongBooleanMapFactory INSTANCE = new ImmutableLongBooleanMapFactoryImpl();

    @Override
    public ImmutableLongBooleanMap empty()
    {
        return ImmutableLongBooleanEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableLongBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongBooleanMap of(long key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableLongBooleanMap with(long key, boolean value)
    {
        return new ImmutableLongBooleanSingletonMap(key, value);
    }

    @Override
    public ImmutableLongBooleanMap ofAll(LongBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableLongBooleanMap withAll(LongBooleanMap map)
    {
        if (map instanceof ImmutableLongBooleanMap)
        {
            return (ImmutableLongBooleanMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            long key = map.keysView().longIterator().next();
            return new ImmutableLongBooleanSingletonMap(key, map.get(key));
        }
        return new ImmutableLongBooleanHashMap(map);
    }

    @Override
    public <T> ImmutableLongBooleanMap from(Iterable<T> iterable, LongFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        return LongBooleanMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
