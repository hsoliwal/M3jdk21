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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.factory.list.primitive.MutableBooleanListFactory;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableBooleanListFactoryImpl is a factory implementation which creates instances of type {@link MutableBooleanList}.
 * This file was automatically generated from template file mutablePrimitiveListFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableBooleanListFactoryImpl implements MutableBooleanListFactory
{
    public static final MutableBooleanListFactory INSTANCE = new MutableBooleanListFactoryImpl();

    @Override
    public MutableBooleanList empty()
    {
        return new BooleanArrayList();
    }

    @Override
    public MutableBooleanList of()
    {
        return this.empty();
    }

    @Override
    public MutableBooleanList with()
    {
        return this.empty();
    }

    @Override
    public MutableBooleanList withInitialCapacity(int capacity)
    {
        return new BooleanArrayList(capacity);
    }

    @Override
    public MutableBooleanList of(boolean... items)
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
    public MutableBooleanList with(boolean... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return BooleanArrayList.newListWith(items);
    }

    @Override
    public MutableBooleanList ofAll(BooleanIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableBooleanList withAll(BooleanIterable items)
    {
        return BooleanArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableBooleanList ofAll(Iterable<Boolean> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableBooleanList withAll(Iterable<Boolean> iterable)
    {
        return Iterate.collectBoolean(iterable, Boolean::booleanValue, BooleanLists.mutable.empty());
    }
}
