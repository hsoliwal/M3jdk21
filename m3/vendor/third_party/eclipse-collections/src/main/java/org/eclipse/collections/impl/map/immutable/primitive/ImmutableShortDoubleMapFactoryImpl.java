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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableShortDoubleMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableShortDoubleMap;
import org.eclipse.collections.api.map.primitive.ShortDoubleMap;
import org.eclipse.collections.impl.factory.primitive.ShortDoubleMaps;

/**
 * ImmutableShortDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableShortDoubleMapFactoryImpl implements ImmutableShortDoubleMapFactory
{
    public static final ImmutableShortDoubleMapFactory INSTANCE = new ImmutableShortDoubleMapFactoryImpl();

    @Override
    public ImmutableShortDoubleMap empty()
    {
        return ImmutableShortDoubleEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableShortDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortDoubleMap of(short key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableShortDoubleMap with(short key, double value)
    {
        return new ImmutableShortDoubleSingletonMap(key, value);
    }

    @Override
    public ImmutableShortDoubleMap ofAll(ShortDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableShortDoubleMap withAll(ShortDoubleMap map)
    {
        if (map instanceof ImmutableShortDoubleMap)
        {
            return (ImmutableShortDoubleMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            short key = map.keysView().shortIterator().next();
            return new ImmutableShortDoubleSingletonMap(key, map.get(key));
        }
        return new ImmutableShortDoubleHashMap(map);
    }

    @Override
    public <T> ImmutableShortDoubleMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        return ShortDoubleMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
