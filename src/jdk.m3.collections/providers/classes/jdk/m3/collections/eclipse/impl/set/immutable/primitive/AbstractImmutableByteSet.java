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

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.factory.primitive.ByteSets;
import jdk.m3.collections.eclipse.api.iterator.ByteIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableByteSet;
import jdk.m3.collections.eclipse.api.set.primitive.ByteSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableByteSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ByteHashSet;
import jdk.m3.collections.eclipse.impl.set.primitive.AbstractByteSet;

/**
 * This file was automatically generated from template file abstractImmutablePrimitiveSet.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
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
