/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.mutable.primitive;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.factory.set.primitive.MutableLongSetFactory;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongSets;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import java.util.stream.LongStream;

/**
 * MutableLongSetFactoryImpl is a factory implementation which creates instances of type {@link MutableLongSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableLongSetFactoryImpl implements MutableLongSetFactory
{
    public static final MutableLongSetFactory INSTANCE = new MutableLongSetFactoryImpl();

    @Override
    public MutableLongSet empty()
    {
        return new LongHashSet();
    }

    @Override
    public MutableLongSet of()
    {
        return this.empty();
    }

    @Override
    public MutableLongSet with()
    {
        return this.empty();
    }

    @Override
    public MutableLongSet withInitialCapacity(int capacity)
    {
        return new LongHashSet(capacity);
    }

    @Override
    public MutableLongSet of(long... items)
    {
        return this.with(items);
    }

    @Override
    public MutableLongSet with(long... items)
    {
        if (items == null || items.length == 0)
        {
            return this.empty();
        }
        return LongHashSet.newSetWith(items);
    }

    @Override
    public MutableLongSet ofAll(LongIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableLongSet withAll(LongIterable items)
    {
        return LongHashSet.newSet(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableLongSet ofAll(Iterable<Long> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableLongSet withAll(Iterable<Long> iterable)
    {
        return Iterate.collectLong(iterable, Long::longValue, LongSets.mutable.empty());
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableLongSet ofAll(LongStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public MutableLongSet withAll(LongStream items)
    {
        MutableLongSet newSet = this.empty();
        items.forEach(newSet::add);
        return newSet;
    }
}
