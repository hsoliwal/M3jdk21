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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.CharSets;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.set.primitive.AbstractCharSet;

/**
 * This file was automatically generated from template file abstractImmutablePrimitiveSet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractImmutableCharSet extends AbstractCharSet implements ImmutableCharSet
{
    @Override
    public ImmutableCharSet newWith(char element)
    {
        return CharHashSet.newSet(this).with(element).toImmutable();
    }

    @Override
    public ImmutableCharSet newWithout(char element)
    {
        return CharHashSet.newSet(this).without(element).toImmutable();
    }

    @Override
    public ImmutableCharSet newWithAll(CharIterable elements)
    {
        return CharHashSet.newSet(this).withAll(elements).toImmutable();
    }

    @Override
    public ImmutableCharSet newWithoutAll(CharIterable elements)
    {
        return CharHashSet.newSet(this).withoutAll(elements).toImmutable();
    }

    @Override
    public CharSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableCharSet toImmutable()
    {
        return this;
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<CharIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                CharIterator iterator = this.charIterator();
                while (iterator.hasNext())
                {
                    MutableCharSet batch = CharSets.mutable.empty();
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
