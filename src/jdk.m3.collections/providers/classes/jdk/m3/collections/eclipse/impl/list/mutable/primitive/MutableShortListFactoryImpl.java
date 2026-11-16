/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.mutable.primitive;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.factory.list.primitive.MutableShortListFactory;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortLists;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableShortListFactoryImpl is a factory implementation which creates instances of type {@link MutableShortList}.
 * This file was automatically generated from template file mutablePrimitiveListFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableShortListFactoryImpl implements MutableShortListFactory
{
    public static final MutableShortListFactory INSTANCE = new MutableShortListFactoryImpl();

    @Override
    public MutableShortList empty()
    {
        return new ShortArrayList();
    }

    @Override
    public MutableShortList of()
    {
        return this.empty();
    }

    @Override
    public MutableShortList with()
    {
        return this.empty();
    }

    @Override
    public MutableShortList withInitialCapacity(int capacity)
    {
        return new ShortArrayList(capacity);
    }

    @Override
    public MutableShortList of(short... items)
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
    public MutableShortList with(short... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return ShortArrayList.newListWith(items);
    }

    @Override
    public MutableShortList ofAll(ShortIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableShortList withAll(ShortIterable items)
    {
        return ShortArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableShortList ofAll(Iterable<Short> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableShortList withAll(Iterable<Short> iterable)
    {
        return Iterate.collectShort(iterable, Short::shortValue, ShortLists.mutable.empty());
    }
}
