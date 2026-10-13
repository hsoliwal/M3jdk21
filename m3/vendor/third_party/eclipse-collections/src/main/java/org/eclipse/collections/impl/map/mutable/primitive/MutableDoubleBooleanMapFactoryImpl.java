/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleBooleanMapFactory;
import org.eclipse.collections.impl.factory.primitive.DoubleBooleanMaps;
import org.eclipse.collections.api.map.primitive.MutableDoubleBooleanMap;
import org.eclipse.collections.api.map.primitive.DoubleBooleanMap;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableDoubleBooleanMapFactoryImpl is a factory implementation which creates instances of type {@link MutableDoubleBooleanMap}.
 * This file was automatically generated from template file mutablePrimitivePrimitiveMapFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableDoubleBooleanMapFactoryImpl implements MutableDoubleBooleanMapFactory
{
    public static final MutableDoubleBooleanMapFactory INSTANCE = new MutableDoubleBooleanMapFactoryImpl();

    @Override
    public MutableDoubleBooleanMap empty()
    {
        return new DoubleBooleanHashMap(0);
    }

    @Override
    public MutableDoubleBooleanMap of()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleBooleanMap with()
    {
        return this.empty();
    }

    @Override
    public MutableDoubleBooleanMap with(double key, boolean value)
    {
        return DoubleBooleanHashMap.newWithKeysValues(key, value);
    }

    @Override
    public MutableDoubleBooleanMap of(double key, boolean value)
    {
        return this.with(key, value);
    }

    @Override
    public MutableDoubleBooleanMap of(double key1, boolean value1, double key2, boolean value2)
    {
        return this.with(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleBooleanMap with(double key1, boolean value1, double key2, boolean value2)
    {
        return DoubleBooleanHashMap.newWithKeysValues(key1, value1, key2, value2);
    }

    @Override
    public MutableDoubleBooleanMap of(double key1, boolean value1, double key2, boolean value2, double key3, boolean value3)
    {
        return this.with(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleBooleanMap with(double key1, boolean value1, double key2, boolean value2, double key3, boolean value3)
    {
        return DoubleBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3);
    }

    @Override
    public MutableDoubleBooleanMap of(
            double key1, boolean value1, double key2, boolean value2, double key3, boolean value3, double key4, boolean value4)
    {
        return this.with(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleBooleanMap with(
            double key1, boolean value1, double key2, boolean value2, double key3, boolean value3, double key4, boolean value4)
    {
        return DoubleBooleanHashMap.newWithKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public MutableDoubleBooleanMap ofInitialCapacity(int capacity)
    {
        return this.withInitialCapacity(capacity);
    }

    @Override
    public MutableDoubleBooleanMap withInitialCapacity(int capacity)
    {
        return new DoubleBooleanHashMap(capacity);
    }

    @Override
    public MutableDoubleBooleanMap ofAll(DoubleBooleanMap map)
    {
        return this.withAll(map);
    }

    @Override
    public MutableDoubleBooleanMap withAll(DoubleBooleanMap map)
    {
        if (map.isEmpty())
        {
            return this.empty();
        }
        return new DoubleBooleanHashMap(map);
    }

    @Override
    public <T> MutableDoubleBooleanMap from(Iterable<T> iterable, DoubleFunction<? super T> keyFunction, BooleanFunction<? super T> valueFunction)
    {
        MutableDoubleBooleanMap map = DoubleBooleanMaps.mutable.empty();
        Iterate.forEach(iterable, each -> map.put(keyFunction.doubleValueOf(each), valueFunction.booleanValueOf(each)));
        return map;
    }
}
