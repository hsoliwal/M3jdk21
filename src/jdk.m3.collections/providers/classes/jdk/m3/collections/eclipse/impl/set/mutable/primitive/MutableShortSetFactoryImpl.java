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

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.factory.set.primitive.MutableShortSetFactory;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortSets;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * MutableShortSetFactoryImpl is a factory implementation which creates instances of type {@link MutableShortSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableShortSetFactoryImpl implements MutableShortSetFactory
{
    public static final MutableShortSetFactory INSTANCE = new MutableShortSetFactoryImpl();

    @Override
    public MutableShortSet empty()
    {
        return new ShortHashSet();
    }

    @Override
    public MutableShortSet of()
    {
        return this.empty();
    }

    @Override
    public MutableShortSet with()
    {
        return this.empty();
    }

    @Override
    public MutableShortSet withInitialCapacity(int capacity)
    {
        return new ShortHashSet(capacity);
    }

    @Override
    public MutableShortSet of(short... items)
    {
        return this.with(items);
    }

    @Override
    public MutableShortSet with(short... items)
    {
        if (items == null || items.length == 0)
        {
            return this.empty();
        }
        return ShortHashSet.newSetWith(items);
    }

    @Override
    public MutableShortSet ofAll(ShortIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableShortSet withAll(ShortIterable items)
    {
        return ShortHashSet.newSet(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableShortSet ofAll(Iterable<Short> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableShortSet withAll(Iterable<Short> iterable)
    {
        return Iterate.collectShort(iterable, Short::shortValue, ShortSets.mutable.empty());
    }
}
