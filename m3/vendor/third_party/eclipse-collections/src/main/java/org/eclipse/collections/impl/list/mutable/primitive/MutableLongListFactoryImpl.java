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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.factory.list.primitive.MutableLongListFactory;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.utility.Iterate;
import java.util.stream.LongStream;

/**
 * MutableLongListFactoryImpl is a factory implementation which creates instances of type {@link MutableLongList}.
 * This file was automatically generated from template file mutablePrimitiveListFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableLongListFactoryImpl implements MutableLongListFactory
{
    public static final MutableLongListFactory INSTANCE = new MutableLongListFactoryImpl();

    @Override
    public MutableLongList empty()
    {
        return new LongArrayList();
    }

    @Override
    public MutableLongList of()
    {
        return this.empty();
    }

    @Override
    public MutableLongList with()
    {
        return this.empty();
    }

    @Override
    public MutableLongList withInitialCapacity(int capacity)
    {
        return new LongArrayList(capacity);
    }

    @Override
    public MutableLongList of(long... items)
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
    public MutableLongList with(long... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return LongArrayList.newListWith(items);
    }

    @Override
    public MutableLongList ofAll(LongIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableLongList withAll(LongIterable items)
    {
        return LongArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableLongList ofAll(Iterable<Long> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableLongList withAll(Iterable<Long> iterable)
    {
        return Iterate.collectLong(iterable, Long::longValue, LongLists.mutable.empty());
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableLongList ofAll(LongStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableLongList withAll(LongStream items)
    {
        return this.with(items.toArray());
    }
}
