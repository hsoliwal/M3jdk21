/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.immutable.primitive;

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.factory.set.primitive.ImmutableLongSetFactory;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import java.util.stream.LongStream;

/**
 * ImmutableLongSetFactoryImpl is a factory implementation which creates instances of type {@link ImmutableLongSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableLongSetFactoryImpl implements ImmutableLongSetFactory
{
    public static final ImmutableLongSetFactory INSTANCE = new ImmutableLongSetFactoryImpl();

    @Override
    public ImmutableLongSet empty()
    {
        return ImmutableLongEmptySet.INSTANCE;
    }

    @Override
    public ImmutableLongSet of()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongSet with()
    {
        return this.empty();
    }

    @Override
    public ImmutableLongSet of(long one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableLongSet with(long one)
    {
        return new ImmutableLongSingletonSet(one);
    }

    @Override
    public ImmutableLongSet of(long... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableLongSet with(long... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return LongHashSet.newSetWith(items).toImmutable();
    }

    @Override
    public ImmutableLongSet ofAll(LongIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableLongSet withAll(LongIterable items)
    {
        if (items instanceof ImmutableLongSet)
        {
            return (ImmutableLongSet) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableLongSet ofAll(Iterable<Long> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableLongSet withAll(Iterable<Long> iterable)
    {
        return LongSets.mutable.withAll(iterable).toImmutable();
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableLongSet ofAll(LongStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableLongSet withAll(LongStream items)
    {
        return this.with(items.toArray());
    }
}
