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
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableCharDoubleMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableCharDoubleMap;
import org.eclipse.collections.api.map.primitive.CharDoubleMap;
import org.eclipse.collections.impl.factory.primitive.CharDoubleMaps;

/**
 * ImmutableCharDoubleMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableCharDoubleMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableCharDoubleMapFactoryImpl implements ImmutableCharDoubleMapFactory
{
    public static final ImmutableCharDoubleMapFactory INSTANCE = new ImmutableCharDoubleMapFactoryImpl();

    @Override
    public ImmutableCharDoubleMap empty()
    {
        return ImmutableCharDoubleEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableCharDoubleMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharDoubleMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharDoubleMap of(char key, double value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableCharDoubleMap with(char key, double value)
    {
        return new ImmutableCharDoubleSingletonMap(key, value);
    }

    @Override
    public ImmutableCharDoubleMap ofAll(CharDoubleMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableCharDoubleMap withAll(CharDoubleMap map)
    {
        if (map instanceof ImmutableCharDoubleMap)
        {
            return (ImmutableCharDoubleMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            char key = map.keysView().charIterator().next();
            return new ImmutableCharDoubleSingletonMap(key, map.get(key));
        }
        return new ImmutableCharDoubleHashMap(map);
    }

    @Override
    public <T> ImmutableCharDoubleMap from(Iterable<T> iterable, CharFunction<? super T> keyFunction, DoubleFunction<? super T> valueFunction)
    {
        return CharDoubleMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
