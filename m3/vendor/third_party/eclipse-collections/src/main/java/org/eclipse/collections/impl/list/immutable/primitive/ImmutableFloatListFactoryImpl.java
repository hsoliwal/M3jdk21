/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.immutable.primitive;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.factory.list.primitive.ImmutableFloatListFactory;
import org.eclipse.collections.api.list.primitive.ImmutableFloatList;
import org.eclipse.collections.impl.factory.primitive.FloatLists;

/**
 * ImmutableFloatListFactoryImpl is a factory implementation which creates instances of type {@link ImmutableFloatList}.
 * This file was automatically generated from template file immutablePrimitiveListFactoryImpl.stg.
 *
 * @since 3.2.
 */
public class ImmutableFloatListFactoryImpl implements ImmutableFloatListFactory
{
    public static final ImmutableFloatListFactory INSTANCE = new ImmutableFloatListFactoryImpl();

    @Override
    public ImmutableFloatList empty()
    {
        return ImmutableFloatEmptyList.INSTANCE;
    }

    @Override
    public ImmutableFloatList of()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatList with()
    {
        return this.empty();
    }

    @Override
    public ImmutableFloatList of(float one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableFloatList with(float one)
    {
        return new ImmutableFloatSingletonList(one);
    }

    @Override
    public ImmutableFloatList of(float... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableFloatList with(float... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return ImmutableFloatArrayList.newListWith(items);
    }

    @Override
    public ImmutableFloatList ofAll(FloatIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableFloatList withAll(FloatIterable items)
    {
        if (items instanceof ImmutableFloatList)
        {
            return (ImmutableFloatList) items;
        }
        if (items == null || items.size() == 0)
        {
            return this.with();
        }
        if (items.size() == 1)
        {
            return this.with(items.toArray()[0]);
        }
        return ImmutableFloatArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableFloatList ofAll(Iterable<Float> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableFloatList withAll(Iterable<Float> iterable)
    {
        return FloatLists.mutable.withAll(iterable).toImmutable();
    }
}
