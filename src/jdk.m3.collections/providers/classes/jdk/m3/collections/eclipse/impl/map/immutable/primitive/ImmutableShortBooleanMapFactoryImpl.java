/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.immutable.primitive;

import jdk.m3.collections.eclipse.api.block.function.primitive.ShortFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortBooleanMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.ShortBooleanMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortBooleanMaps;

/**
 * ImmutableShortBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortBooleanMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableShortBooleanMapFactoryImpl implements ImmutableShortBooleanMapFactory
{
    public static final ImmutableShortBooleanMapFactory INSTANCE = new ImmutableShortBooleanMapFactoryImpl();

    @Override
    public ImmutableShortBooleanMap empty()
    {
        return ImmutableShortBooleanEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableShortBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortBooleanMap of(short key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableShortBooleanMap with(short key, boolean value)
    {
        return new ImmutableShortBooleanSingletonMap(key, value);
    }

    @Override
    public ImmutableShortBooleanMap ofAll(ShortBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableShortBooleanMap withAll(ShortBooleanMap map)
    {
        if (map instanceof ImmutableShortBooleanMap)
        {
            return (ImmutableShortBooleanMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            short key = map.keysView().shortIterator().next();
            return new ImmutableShortBooleanSingletonMap(key, map.get(key));
        }
        return new ImmutableShortBooleanHashMap(map);
    }

    @Override
    public <T> ImmutableShortBooleanMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        return ShortBooleanMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
