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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.factory.set.primitive.ImmutableCharSetFactory;
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.factory.primitive.CharSets;

/**
 * ImmutableCharSetFactoryImpl is a factory implementation which creates instances of type {@link ImmutableCharSet}.
 * This file was automatically generated from template file immutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 4.0.
 */
public class ImmutableCharSetFactoryImpl implements ImmutableCharSetFactory
{
    public static final ImmutableCharSetFactory INSTANCE = new ImmutableCharSetFactoryImpl();

    @Override
    public ImmutableCharSet empty()
    {
        return ImmutableCharEmptySet.INSTANCE;
    }

    @Override
    public ImmutableCharSet of()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharSet with()
    {
        return this.empty();
    }

    @Override
    public ImmutableCharSet of(char one)
    {
        return this.with(one);
    }

    @Override
    public ImmutableCharSet with(char one)
    {
        return new ImmutableCharSingletonSet(one);
    }

    @Override
    public ImmutableCharSet of(char... items)
    {
        return this.with(items);
    }

    @Override
    public ImmutableCharSet with(char... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        if (items.length == 1)
        {
            return this.with(items[0]);
        }
        return CharHashSet.newSetWith(items).toImmutable();
    }

    @Override
    public ImmutableCharSet ofAll(CharIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public ImmutableCharSet withAll(CharIterable items)
    {
        if (items instanceof ImmutableCharSet)
        {
            return (ImmutableCharSet) items;
        }
        return this.with(items.toArray());
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableCharSet ofAll(Iterable<Character> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public ImmutableCharSet withAll(Iterable<Character> iterable)
    {
        return CharSets.mutable.withAll(iterable).toImmutable();
    }
}
