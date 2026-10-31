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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.FloatSets;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.set.primitive.AbstractFloatSet;

/**
 * This file was automatically generated from template file abstractImmutablePrimitiveSet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractImmutableFloatSet extends AbstractFloatSet implements ImmutableFloatSet
{
    @Override
    public ImmutableFloatSet newWith(float element)
    {
        return FloatHashSet.newSet(this).with(element).toImmutable();
    }

    @Override
    public ImmutableFloatSet newWithout(float element)
    {
        return FloatHashSet.newSet(this).without(element).toImmutable();
    }

    @Override
    public ImmutableFloatSet newWithAll(FloatIterable elements)
    {
        return FloatHashSet.newSet(this).withAll(elements).toImmutable();
    }

    @Override
    public ImmutableFloatSet newWithoutAll(FloatIterable elements)
    {
        return FloatHashSet.newSet(this).withoutAll(elements).toImmutable();
    }

    @Override
    public FloatSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableFloatSet toImmutable()
    {
        return this;
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<FloatIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                FloatIterator iterator = this.floatIterator();
                while (iterator.hasNext())
                {
                    MutableFloatSet batch = FloatSets.mutable.empty();
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
