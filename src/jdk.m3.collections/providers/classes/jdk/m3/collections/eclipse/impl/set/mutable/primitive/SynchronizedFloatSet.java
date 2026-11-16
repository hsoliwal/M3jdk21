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

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.iterator.FloatIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableFloatSet;
import jdk.m3.collections.eclipse.api.set.primitive.FloatSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatFloatPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedFloatCollection;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyFloatIterableAdapter;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatSets;
import jdk.m3.collections.eclipse.impl.set.mutable.SynchronizedMutableSet;

/**
 * A synchronized view of a {@link MutableFloatSet}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link FloatIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveSet.stg.
 *
 * @see MutableFloatSet#asSynchronized()
 * @see MutableSet#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedFloatSet
        extends AbstractSynchronizedFloatCollection
        implements MutableFloatSet
{
    private static final long serialVersionUID = 1L;

    public SynchronizedFloatSet(MutableFloatSet set)
    {
        super(set);
    }

    public SynchronizedFloatSet(MutableFloatSet set, Object newLock)
    {
        super(set, newLock);
    }

    /**
     * This method will take a MutableFloatSet and wrap it directly in a SynchronizedFloatSet.
     */
    public static SynchronizedFloatSet of(MutableFloatSet set)
    {
        return new SynchronizedFloatSet(set);
    }

    /**
     * This method will take a MutableFloatSet and wrap it directly in a SynchronizedFloatSet.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedFloatSet of(MutableFloatSet set, Object lock)
    {
        return new SynchronizedFloatSet(set, lock);
    }

    private MutableFloatSet getMutableFloatSet()
    {
        return (MutableFloatSet) this.getFloatCollection();
    }

    @Override
    public SynchronizedFloatSet without(float element)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatSet().remove(element);
        }
        return this;
    }

    @Override
    public MutableSet<Float> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableSet.of(new BoxedMutableFloatSet(this), this.getLock());
        }
    }

    @Override
    public SynchronizedFloatSet with(float element)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatSet().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedFloatSet withAll(FloatIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatSet().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedFloatSet withoutAll(FloatIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatSet().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableFloatSet select(FloatPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatSet().select(predicate);
        }
    }

    @Override
    public MutableFloatSet reject(FloatPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatSet().reject(predicate);
        }
    }

    @Override
    public <V> MutableSet<V> collect(FloatToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatSet().collect(function);
        }
    }

    @Override
    public LazyIterable<FloatFloatPair> cartesianProduct(
            FloatSet set)
    {
        synchronized (this.getLock())
        {
            return FloatSets.cartesianProduct(this, set);
        }
    }

    @Override
    public boolean equals(Object otherSet)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatSet().equals(otherSet);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatSet().hashCode();
        }
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyFloatIterableAdapter(this);
        }
    }

    @Override
    public MutableFloatSet asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableFloatSet(this);
        }
    }

    @Override
    public MutableFloatSet asSynchronized()
    {
        return this;
    }

    public FloatSet freeze()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatSet().freeze();
        }
    }

    @Override
    public ImmutableFloatSet toImmutable()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatSet().toImmutable();
        }
    }

    /**
     * @since 9.2.
     */
    public MutableFloatSet newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatSet().newEmpty();
        }
    }
}
