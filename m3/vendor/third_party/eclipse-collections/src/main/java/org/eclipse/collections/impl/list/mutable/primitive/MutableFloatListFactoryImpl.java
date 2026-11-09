/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.mutable.primitive;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.factory.list.primitive.MutableFloatListFactory;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.impl.factory.primitive.FloatLists;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableFloatListFactoryImpl is a factory implementation which creates instances of type {@link MutableFloatList}.
 * This file was automatically generated from template file mutablePrimitiveListFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableFloatListFactoryImpl implements MutableFloatListFactory
{
    public static final MutableFloatListFactory INSTANCE = new MutableFloatListFactoryImpl();

    @Override
    public MutableFloatList empty()
    {
        return new FloatArrayList();
    }

    @Override
    public MutableFloatList of()
    {
        return this.empty();
    }

    @Override
    public MutableFloatList with()
    {
        return this.empty();
    }

    @Override
    public MutableFloatList withInitialCapacity(int capacity)
    {
        return new FloatArrayList(capacity);
    }

    @Override
    public MutableFloatList of(float... items)
    {
        return this.with(items);
    }

    /**
     * Creates a new list using the passed {@code items} argument as the backing store.
     * <p>
     * !!! WARNING: This method uses the passed in array, so can be very unsafe if the original
     * array is held onto anywhere else. !!!
     */
    @Override
    public MutableFloatList with(float... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return FloatArrayList.newListWith(items);
    }

    @Override
    public MutableFloatList ofAll(FloatIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableFloatList withAll(FloatIterable items)
    {
        return FloatArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableFloatList ofAll(Iterable<Float> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableFloatList withAll(Iterable<Float> iterable)
    {
        return Iterate.collectFloat(iterable, Float::floatValue, FloatLists.mutable.empty());
    }
}
