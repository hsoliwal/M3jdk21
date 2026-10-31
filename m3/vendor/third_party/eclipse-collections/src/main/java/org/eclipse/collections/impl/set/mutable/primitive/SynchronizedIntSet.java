/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.mutable.primitive;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedIntCollection;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.impl.factory.primitive.IntSets;

/**
 * A synchronized view of a {@link MutableIntSet}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link IntIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveSet.stg.
 *
 * @see MutableIntSet#asSynchronized()
 * @see MutableSet#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedIntSet
        extends AbstractSynchronizedIntCollection
        implements MutableIntSet
{
    private static final long serialVersionUID = 1L;

    public SynchronizedIntSet(MutableIntSet set)
    {
        super(set);
    }

    public SynchronizedIntSet(MutableIntSet set, Object newLock)
    {
        super(set, newLock);
    }

    /**
     * This method will take a MutableIntSet and wrap it directly in a SynchronizedIntSet.
     */
    public static SynchronizedIntSet of(MutableIntSet set)
    {
        return new SynchronizedIntSet(set);
    }

    /**
     * This method will take a MutableIntSet and wrap it directly in a SynchronizedIntSet.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedIntSet of(MutableIntSet set, Object lock)
    {
        return new SynchronizedIntSet(set, lock);
    }

    private MutableIntSet getMutableIntSet()
    {
        return (MutableIntSet) this.getIntCollection();
    }

    @Override
    public SynchronizedIntSet without(int element)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntSet().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedIntSet with(int element)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntSet().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedIntSet withAll(IntIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntSet().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedIntSet withoutAll(IntIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntSet().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableIntSet select(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntSet().select(predicate);
        }
    }

    @Override
    public MutableIntSet reject(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntSet().reject(predicate);
        }
    }

    @Override
    public <V> MutableSet<V> collect(IntToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntSet().collect(function);
        }
    }

    @Override
    public LazyIterable<IntIntPair> cartesianProduct(
            IntSet set)
    {
        synchronized (this.getLock())
        {
            return IntSets.cartesianProduct(this, set);
        }
    }

    @Override
    public boolean equals(Object otherSet)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntSet().equals(otherSet);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntSet().hashCode();
        }
    }

    @Override
    public LazyIntIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyIntIterableAdapter(this);
        }
    }

    @Override
    public MutableIntSet asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableIntSet(this);
        }
    }

    @Override
    public MutableIntSet asSynchronized()
    {
        return this;
    }

    public IntSet freeze()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntSet().freeze();
        }
    }

    @Override
    public ImmutableIntSet toImmutable()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntSet().toImmutable();
        }
    }

    /**
     * @since 9.2.
     */
    public MutableIntSet newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntSet().newEmpty();
        }
    }
}
