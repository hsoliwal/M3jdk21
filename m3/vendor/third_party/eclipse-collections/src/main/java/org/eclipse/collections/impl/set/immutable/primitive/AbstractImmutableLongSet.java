/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.immutable.primitive;

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.LongSets;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.set.primitive.AbstractLongSet;

/**
 * This file was automatically generated from template file abstractImmutablePrimitiveSet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractImmutableLongSet extends AbstractLongSet implements ImmutableLongSet
{
    @Override
    public ImmutableLongSet newWith(long element)
    {
        return LongHashSet.newSet(this).with(element).toImmutable();
    }

    @Override
    public ImmutableLongSet newWithout(long element)
    {
        return LongHashSet.newSet(this).without(element).toImmutable();
    }

    @Override
    public ImmutableLongSet newWithAll(LongIterable elements)
    {
        return LongHashSet.newSet(this).withAll(elements).toImmutable();
    }

    @Override
    public ImmutableLongSet newWithoutAll(LongIterable elements)
    {
        return LongHashSet.newSet(this).withoutAll(elements).toImmutable();
    }

    @Override
    public LongSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableLongSet toImmutable()
    {
        return this;
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<LongIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                LongIterator iterator = this.longIterator();
                while (iterator.hasNext())
                {
                    MutableLongSet batch = LongSets.mutable.empty();
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
