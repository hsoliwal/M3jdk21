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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.ShortSets;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.ImmutableShortSet;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.set.primitive.AbstractShortSet;

/**
 * This file was automatically generated from template file abstractImmutablePrimitiveSet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractImmutableShortSet extends AbstractShortSet implements ImmutableShortSet
{
    @Override
    public ImmutableShortSet newWith(short element)
    {
        return ShortHashSet.newSet(this).with(element).toImmutable();
    }

    @Override
    public ImmutableShortSet newWithout(short element)
    {
        return ShortHashSet.newSet(this).without(element).toImmutable();
    }

    @Override
    public ImmutableShortSet newWithAll(ShortIterable elements)
    {
        return ShortHashSet.newSet(this).withAll(elements).toImmutable();
    }

    @Override
    public ImmutableShortSet newWithoutAll(ShortIterable elements)
    {
        return ShortHashSet.newSet(this).withoutAll(elements).toImmutable();
    }

    @Override
    public ShortSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableShortSet toImmutable()
    {
        return this;
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ShortIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                ShortIterator iterator = this.shortIterator();
                while (iterator.hasNext())
                {
                    MutableShortSet batch = ShortSets.mutable.empty();
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
