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

import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleCharMapFactory;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleCharMap;
import org.eclipse.collections.api.map.primitive.DoubleCharMap;
import org.eclipse.collections.impl.factory.primitive.DoubleCharMaps;

/**
 * ImmutableDoubleCharMapFactoryImpl is a factory implementation which creates instances of type {@link ImmutableDoubleCharMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableDoubleCharMapFactoryImpl implements ImmutableDoubleCharMapFactory
{
    public static final ImmutableDoubleCharMapFactory INSTANCE = new ImmutableDoubleCharMapFactoryImpl();

    @Override
    public ImmutableDoubleCharMap empty()
    {
        return ImmutableDoubleCharEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableDoubleCharMap of()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleCharMap with()
    {
        return this.empty();
    }

    @Override
    public ImmutableDoubleCharMap of(double key, char value)
    {
        return this.with(key, value);
    }

    @Override
    public ImmutableDoubleCharMap with(double key, char value)
    {
        return new ImmutableDoubleCharSingletonMap(key, value);
    }

    @Override
    public ImmutableDoubleCharMap ofAll(DoubleCharMap map)
    {
        return this.withAll(map);
    }

    @Override
    public ImmutableDoubleCharMap withAll(DoubleCharMap map)
    {
        if (map instanceof ImmutableDoubleCharMap)
        {
            return (ImmutableDoubleCharMap) map;
        }
        if (map.isEmpty())
        {
            return this.with();
        }
        if (map.size() == 1)
        {
            double key = map.keysView().doubleIterator().next();
            return new ImmutableDoubleCharSingletonMap(key, map.get(key));
        }
        return new ImmutableDoubleCharHashMap(map);
    }

    @Override
    public <T> ImmutableDoubleCharMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, CharFunction<? super T> valueFunction)
    {
        return DoubleCharMaps.mutable.from(iterable, keyFunction, valueFunction).toImmutable();
    }
}
