/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.mutable.primitive;

import java.util.Collection;
import java.util.Collections;

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.LazyByteIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BytePredicate;
import jdk.m3.collections.eclipse.api.iterator.ByteIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableByteSet;
import jdk.m3.collections.eclipse.api.set.primitive.ByteSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableByteSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ByteBytePair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedByteCollection;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyByteIterableAdapter;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteSets;
import jdk.m3.collections.eclipse.impl.set.mutable.SynchronizedMutableSet;

/**
 * A synchronized view of a {@link MutableByteSet}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link ByteIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveSet.stg.
 *
 * @see MutableByteSet#asSynchronized()
 * @see MutableSet#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedByteSet
        extends AbstractSynchronizedByteCollection
        implements MutableByteSet
{
    private static final long serialVersionUID = 1L;

    public SynchronizedByteSet(MutableByteSet set)
    {
        super(set);
    }

    public SynchronizedByteSet(MutableByteSet set, Object newLock)
    {
        super(set, newLock);
    }

    /**
     * This method will take a MutableByteSet and wrap it directly in a SynchronizedByteSet.
     */
    public static SynchronizedByteSet of(MutableByteSet set)
    {
        return new SynchronizedByteSet(set);
    }

    /**
     * This method will take a MutableByteSet and wrap it directly in a SynchronizedByteSet.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedByteSet of(MutableByteSet set, Object lock)
    {
        return new SynchronizedByteSet(set, lock);
    }

    private MutableByteSet getMutableByteSet()
    {
        return (MutableByteSet) this.getByteCollection();
    }

    @Override
    public SynchronizedByteSet without(byte element)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteSet().remove(element);
        }
        return this;
    }

    @Override
    public MutableSet<Byte> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableSet.of(new BoxedMutableByteSet(this), this.getLock());
        }
    }

    @Override
    public SynchronizedByteSet with(byte element)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteSet().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedByteSet withAll(ByteIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteSet().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedByteSet withoutAll(ByteIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteSet().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableByteSet select(BytePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteSet().select(predicate);
        }
    }

    @Override
    public MutableByteSet reject(BytePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteSet().reject(predicate);
        }
    }

    @Override
    public <V> MutableSet<V> collect(ByteToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteSet().collect(function);
        }
    }

    @Override
    public LazyIterable<ByteBytePair> cartesianProduct(
            ByteSet set)
    {
        synchronized (this.getLock())
        {
            return ByteSets.cartesianProduct(this, set);
        }
    }

    @Override
    public boolean equals(Object otherSet)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteSet().equals(otherSet);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteSet().hashCode();
        }
    }

    @Override
    public LazyByteIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyByteIterableAdapter(this);
        }
    }

    @Override
    public MutableByteSet asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableByteSet(this);
        }
    }

    @Override
    public MutableByteSet asSynchronized()
    {
        return this;
    }

    public ByteSet freeze()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteSet().freeze();
        }
    }

    @Override
    public ImmutableByteSet toImmutable()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteSet().toImmutable();
        }
    }

    /**
     * @since 9.2.
     */
    public MutableByteSet newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteSet().newEmpty();
        }
    }
}
