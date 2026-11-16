/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.immutable.primitive;

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.factory.primitive.IntSets;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableIntSet;
import jdk.m3.collections.eclipse.api.set.primitive.IntSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;
import jdk.m3.collections.eclipse.impl.set.primitive.AbstractIntSet;

/**
 * This file was automatically generated from template file abstractImmutablePrimitiveSet.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public abstract class AbstractImmutableIntSet extends AbstractIntSet implements ImmutableIntSet
{
    @Override
    public ImmutableIntSet newWith(int element)
    {
        return IntHashSet.newSet(this).with(element).toImmutable();
    }

    @Override
    public ImmutableIntSet newWithout(int element)
    {
        return IntHashSet.newSet(this).without(element).toImmutable();
    }

    @Override
    public ImmutableIntSet newWithAll(IntIterable elements)
    {
        return IntHashSet.newSet(this).withAll(elements).toImmutable();
    }

    @Override
    public ImmutableIntSet newWithoutAll(IntIterable elements)
    {
        return IntHashSet.newSet(this).withoutAll(elements).toImmutable();
    }

    @Override
    public IntSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableIntSet toImmutable()
    {
        return this;
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<IntIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                IntIterator iterator = this.intIterator();
                while (iterator.hasNext())
                {
                    MutableIntSet batch = IntSets.mutable.empty();
                    for (int i = 0; i < size && iterator.hasNext(); i++)
                    {
                        batch.add(iterator.next());
                    }
                    result.add(batch.toImmutable());
                }
            }
        }
        return result.toImmutable();
    }
}
