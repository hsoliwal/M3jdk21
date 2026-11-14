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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.DoubleSets;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.ImmutableDoubleSet;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.set.primitive.AbstractDoubleSet;

/**
 * This file was automatically generated from template file abstractImmutablePrimitiveSet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractImmutableDoubleSet extends AbstractDoubleSet implements ImmutableDoubleSet
{
    @Override
    public ImmutableDoubleSet newWith(double element)
    {
        return DoubleHashSet.newSet(this).with(element).toImmutable();
    }

    @Override
    public ImmutableDoubleSet newWithout(double element)
    {
        return DoubleHashSet.newSet(this).without(element).toImmutable();
    }

    @Override
    public ImmutableDoubleSet newWithAll(DoubleIterable elements)
    {
        return DoubleHashSet.newSet(this).withAll(elements).toImmutable();
    }

    @Override
    public ImmutableDoubleSet newWithoutAll(DoubleIterable elements)
    {
        return DoubleHashSet.newSet(this).withoutAll(elements).toImmutable();
    }

    @Override
    public DoubleSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableDoubleSet toImmutable()
    {
        return this;
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<DoubleIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                DoubleIterator iterator = this.doubleIterator();
                while (iterator.hasNext())
                {
                    MutableDoubleSet batch = DoubleSets.mutable.empty();
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
