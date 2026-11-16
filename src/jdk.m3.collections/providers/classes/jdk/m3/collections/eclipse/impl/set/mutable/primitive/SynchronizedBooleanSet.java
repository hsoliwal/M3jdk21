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

import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.iterator.BooleanIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableBooleanSet;
import jdk.m3.collections.eclipse.api.set.primitive.BooleanSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.BooleanBooleanPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedBooleanCollection;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyBooleanIterableAdapter;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanSets;
import jdk.m3.collections.eclipse.impl.set.mutable.SynchronizedMutableSet;

/**
 * A synchronized view of a {@link MutableBooleanSet}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link BooleanIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveSet.stg.
 *
 * @see MutableBooleanSet#asSynchronized()
 * @see MutableSet#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedBooleanSet
        extends AbstractSynchronizedBooleanCollection
        implements MutableBooleanSet
{
    private static final long serialVersionUID = 1L;

    public SynchronizedBooleanSet(MutableBooleanSet set)
    {
        super(set);
    }

    public SynchronizedBooleanSet(MutableBooleanSet set, Object newLock)
    {
        super(set, newLock);
    }

    /**
     * This method will take a MutableBooleanSet and wrap it directly in a SynchronizedBooleanSet.
     */
    public static SynchronizedBooleanSet of(MutableBooleanSet set)
    {
        return new SynchronizedBooleanSet(set);
    }

    /**
     * This method will take a MutableBooleanSet and wrap it directly in a SynchronizedBooleanSet.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedBooleanSet of(MutableBooleanSet set, Object lock)
    {
        return new SynchronizedBooleanSet(set, lock);
    }

    private MutableBooleanSet getMutableBooleanSet()
    {
        return (MutableBooleanSet) this.getBooleanCollection();
    }

    @Override
    public SynchronizedBooleanSet without(boolean element)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanSet().remove(element);
        }
        return this;
    }

    @Override
    public MutableSet<Boolean> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableSet.of(new BoxedMutableBooleanSet(this), this.getLock());
        }
    }

    @Override
    public SynchronizedBooleanSet with(boolean element)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanSet().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedBooleanSet withAll(BooleanIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanSet().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedBooleanSet withoutAll(BooleanIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanSet().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableBooleanSet select(BooleanPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanSet().select(predicate);
        }
    }

    @Override
    public MutableBooleanSet reject(BooleanPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanSet().reject(predicate);
        }
    }

    @Override
    public <V> MutableSet<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanSet().collect(function);
        }
    }

    @Override
    public LazyIterable<BooleanBooleanPair> cartesianProduct(
            BooleanSet set)
    {
        synchronized (this.getLock())
        {
            return BooleanSets.cartesianProduct(this, set);
        }
    }

    @Override
    public boolean equals(Object otherSet)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanSet().equals(otherSet);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanSet().hashCode();
        }
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyBooleanIterableAdapter(this);
        }
    }

    @Override
    public MutableBooleanSet asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableBooleanSet(this);
        }
    }

    @Override
    public MutableBooleanSet asSynchronized()
    {
        return this;
    }

    public BooleanSet freeze()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanSet().freeze();
        }
    }

    @Override
    public ImmutableBooleanSet toImmutable()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanSet().toImmutable();
        }
    }

    /**
     * @since 9.2.
     */
    public MutableBooleanSet newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanSet().newEmpty();
        }
    }
}
