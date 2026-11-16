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
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortCharMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.ShortCharMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortCharMaps;

/**
 * ImmutableShortCharMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortCharMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableShortCharMapFactoryImpl implements ImmutableShortCharMapFactory
{
    public static final ImmutableShortCharMapFactory INSTANCE = new ImmutableShortCharMapFactoryImpl();

    @Override
    public ImmutableShortCharMap empty()
    {
        return ImmutableShortCharEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableShortCharMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortCharMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortCharMap of(short key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableShortCharMap with(short key, char value)
    {
        return new ImmutableShortCharSingletonMap(key, value);
    }

    @Override
    public ImmutableShortCharMap ofAll(ShortCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableShortCharMap withAll(ShortCharMap map)
    {
        if (map instanceof ImmutableShortCharMap)
        {
            return (ImmutableShortCharMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            short key = map.keysView().shortIterator().next();
            return new ImmutableShortCharSingletonMap(key, map.get(key));
        }
        return new ImmutableShortCharHashMap(map);
    }

    @Override
    public <T> ImmutableShortCharMap from(Iterable<T> iterable, ShortFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        return ShortCharMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
