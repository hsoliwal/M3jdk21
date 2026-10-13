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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedFloatCollection;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.impl.factory.primitive.FloatSets;

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
