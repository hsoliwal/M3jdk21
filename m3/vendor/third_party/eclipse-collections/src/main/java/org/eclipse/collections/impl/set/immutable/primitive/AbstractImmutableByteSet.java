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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.ByteSets;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.set.primitive.AbstractByteSet;

/**
 * This file was automatically generated from template file abstractImmutablePrimitiveSet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractImmutableByteSet extends AbstractByteSet implements ImmutableByteSet
{
    @Override
    public ImmutableByteSet newWith(byte element)
    {
        return ByteHashSet.newSet(this).with(element).toImmutable();
    }

    @Override
    public ImmutableByteSet newWithout(byte element)
    {
        return ByteHashSet.newSet(this).without(element).toImmutable();
    }

    @Override
    public ImmutableByteSet newWithAll(ByteIterable elements)
    {
        return ByteHashSet.newSet(this).withAll(elements).toImmutable();
    }

    @Override
    public ImmutableByteSet newWithoutAll(ByteIterable elements)
    {
        return ByteHashSet.newSet(this).withoutAll(elements).toImmutable();
    }

    @Override
    public ByteSet freeze()
    {
        return this;
    }

    @Override
    public ImmutableByteSet toImmutable()
    {
        return this;
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ByteIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                ByteIterator iterator = this.byteIterator();
                while (iterator.hasNext())
                {
                    MutableByteSet batch = ByteSets.mutable.empty();
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
