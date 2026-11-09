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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.factory.set.primitive.ImmutableShortSetFactory;
import org.eclipse.collections.api.set.primitive.ImmutableShortSet;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.factory.primitive.ShortSets;

/**
 * ImmutableShortSetFactoryImpl is a factory implementation which creates instances of type {@link ImmutableShortSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableShortSetFactoryImpl implements ImmutableShortSetFactory
{
    public static final ImmutableShortSetFactory INSTANCE = new ImmutableShortSetFactoryImpl();

    @Override
    public ImmutableShortSet empty()
    {
        return ImmutableShortEmptySet.INSTANCE;
    }

    @Override
    public ImmutableShortSet of()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortSet with()
    {
        return this.empty();
    }

    @Override
    public ImmutableShortSet of(short one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableShortSet with(short one)
    {
        return new ImmutableShortSingletonSet(one);
    }

    @Override
    public ImmutableShortSet of(short... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableShortSet with(short... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ShortHashSet.newSetWith(items).toImmutable();
    }

    @Override
    public ImmutableShortSet ofAll(ShortIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableShortSet withAll(ShortIterable items)
    {
        if (items instanceof ImmutableShortSet)
        {
            return (ImmutableShortSet) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableShortSet ofAll(Iterable<Short> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableShortSet withAll(Iterable<Short> iterable)
    {
        return ShortSets.mutable.withAll(iterable).toImmutable();
    }
}
