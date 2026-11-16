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

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableIntSet;
import jdk.m3.collections.eclipse.api.set.primitive.IntSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.IntIntPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedIntCollection;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyIntIterableAdapter;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntSets;
import jdk.m3.collections.eclipse.impl.set.mutable.SynchronizedMutableSet;

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
    public MutableSet<Integer> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableSet.of(new BoxedMutableIntSet(this), this.getLock());
        }
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
