/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.immutable.primitive;

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableIntSetFactory;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableIntSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntSets;
import java.util.stream.IntStream;

/**
 * ImmutableIntSetFactoryImpl is a factory implementation which creates instances of type {@link ImmutableIntSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class ImmutableIntSetFactoryImpl implements ImmutableIntSetFactory
{
    public static final ImmutableIntSetFactory INSTANCE = new ImmutableIntSetFactoryImpl();

    @Override
    public ImmutableIntSet empty()
    {
        return ImmutableIntEmptySet.INSTANCE;
    }

    @Override
    public ImmutableIntSet of()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntSet with()
    {
        return this.empty();
    }

    @Override
    public ImmutableIntSet of(int one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableIntSet with(int one)
    {
        return new ImmutableIntSingletonSet(one);
    }

    @Override
    public ImmutableIntSet of(int... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableIntSet with(int... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return IntHashSet.newSetWith(items).toImmutable();
    }

    @Override
    public ImmutableIntSet ofAll(IntIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableIntSet withAll(IntIterable items)
    {
        if (items instanceof ImmutableIntSet)
        {
            return (ImmutableIntSet) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableIntSet ofAll(Iterable<Integer> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableIntSet withAll(Iterable<Integer> iterable)
    {
        return IntSets.mutable.withAll(iterable).toImmutable();
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableIntSet ofAll(IntStream items)
    {
        return this.withAll(items);
    }

    /**
     * @since 9.0
     */
    @Override
    public ImmutableIntSet withAll(IntStream items)
    {
        MutableIntSet newSet = IntSets.mutable.empty();
        items.forEach(newSet::add);
        return newSet.toImmutable();
    }
}
