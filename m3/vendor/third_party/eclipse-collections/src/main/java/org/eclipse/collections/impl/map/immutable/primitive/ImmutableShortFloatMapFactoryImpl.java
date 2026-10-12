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

import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableShortFloatMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableShortFloatMap;
import org.eclipse.collections.api.map.primitive.ShortFloatMap;
import org.eclipse.collections.impl.factory.primitive.ShortFloatMaps;

/**
 * ImmutableShortFloatMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortFloatMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableShortFloatMapFactoryImpl implements ImmutableShortFloatMapFactory
{
    public static final ImmutableShortFloatMapFactory INSTANCE = new ImmutableShortFloatMapFactoryImpl();

    @Override
    public ImmutableShortFloatMap empty()
    {
        return ImmutableShortFloatEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableShortFloatMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortFloatMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortFloatMap of(short key, float value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableShortFloatMap with(short key, float value)
    {
        return new ImmutableShortFloatSingletonMap(key, value);
    }

    @Override
    public ImmutableShortFloatMap ofAll(ShortFloatMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableShortFloatMap withAll(ShortFloatMap map)
    {
        if (map instanceof ImmutableShortFloatMap)
        {
            return (ImmutableShortFloatMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            short key = map.keysView().shortIterator().next();
            return new ImmutableShortFloatSingletonMap(key, map.get(key));
        }
        return new ImmutableShortFloatHashMap(map);
    }

    @Override
    public <T> ImmutableShortFloatMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, FloatFunction<? super T> valueFunction)
    {
        return ShortFloatMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
