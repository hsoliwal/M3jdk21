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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.factory.set.primitive.ImmutableFloatSetFactory;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.factory.primitive.FloatSets;

/**
 * ImmutableFloatSetFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableFloatSetFactoryImpl implements ImmutableFloatSetFactory
{
    public static final ImmutableFloatSetFactory INSTANCE = new ImmutableFloatSetFactoryImpl();

    @Override
    public ImmutableFloatSet empty()
    {
        return ImmutableFloatEmptySet.INSTANCE;
    }

    @Override
    public ImmutableFloatSet of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatSet with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatSet of(float one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableFloatSet with(float one)
    {
        return new ImmutableFloatSingletonSet(one);
    }

    @Override
    public ImmutableFloatSet of(float... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableFloatSet with(float... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return FloatHashSet.newSetWith(items).toImmutable();
    }

    @Override
    public ImmutableFloatSet ofAll(FloatIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableFloatSet withAll(FloatIterable items)
    {
        if (items instanceof ImmutableFloatSet)
        {
            return (ImmutableFloatSet) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableFloatSet ofAll(Iterable<Float> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableFloatSet withAll(Iterable<Float> iterable)
    {
        return FloatSets.mutable.withAll(iterable).toImmutable();
    }
}
