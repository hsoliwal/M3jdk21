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

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.iterator.DoubleIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableDoubleSet;
import jdk.m3.collections.eclipse.api.set.primitive.DoubleSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableDoubleSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.DoubleDoublePair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedDoubleCollection;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyDoubleIterableAdapter;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleSets;
import jdk.m3.collections.eclipse.impl.set.mutable.SynchronizedMutableSet;

/**
 * A synchronized view of a {@link MutableDoubleSet}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link DoubleIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveSet.stg.
 *
 * @see MutableDoubleSet#asSynchronized()
 * @see MutableSet#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedDoubleSet
        extends AbstractSynchronizedDoubleCollection
        implements MutableDoubleSet
{
    private static final long serialVersionUID = 1L;

    public SynchronizedDoubleSet(MutableDoubleSet set)
    {
        super(set);
    }

    public SynchronizedDoubleSet(MutableDoubleSet set, Object newLock)
    {
        super(set, newLock);
    }

    /**
     * This method will take a MutableDoubleSet and wrap it directly in a SynchronizedDoubleSet.
     */
    public static SynchronizedDoubleSet of(MutableDoubleSet set)
    {
        return new SynchronizedDoubleSet(set);
    }

    /**
     * This method will take a MutableDoubleSet and wrap it directly in a SynchronizedDoubleSet.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedDoubleSet of(MutableDoubleSet set, Object lock)
    {
        return new SynchronizedDoubleSet(set, lock);
    }

    private MutableDoubleSet getMutableDoubleSet()
    {
        return (MutableDoubleSet) this.getDoubleCollection();
    }

    @Override
    public SynchronizedDoubleSet without(double element)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleSet().remove(element);
        }
        return this;
    }

    @Override
    public MutableSet<Double> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableSet.of(new BoxedMutableDoubleSet(this), this.getLock());
        }
    }

    @Override
    public SynchronizedDoubleSet with(double element)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleSet().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedDoubleSet withAll(DoubleIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleSet().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedDoubleSet withoutAll(DoubleIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleSet().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableDoubleSet select(DoublePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleSet().select(predicate);
        }
    }

    @Override
    public MutableDoubleSet reject(DoublePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleSet().reject(predicate);
        }
    }

    @Override
    public <V> MutableSet<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleSet().collect(function);
        }
    }

    @Override
    public LazyIterable<DoubleDoublePair> cartesianProduct(
            DoubleSet set)
    {
        synchronized (this.getLock())
        {
            return DoubleSets.cartesianProduct(this, set);
        }
    }

    @Override
    public boolean equals(Object otherSet)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleSet().equals(otherSet);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleSet().hashCode();
        }
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyDoubleIterableAdapter(this);
        }
    }

    @Override
    public MutableDoubleSet asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableDoubleSet(this);
        }
    }

    @Override
    public MutableDoubleSet asSynchronized()
    {
        return this;
    }

    public DoubleSet freeze()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleSet().freeze();
        }
    }

    @Override
    public ImmutableDoubleSet toImmutable()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleSet().toImmutable();
        }
    }

    /**
     * @since 9.2.
     */
    public MutableDoubleSet newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleSet().newEmpty();
        }
    }
}
