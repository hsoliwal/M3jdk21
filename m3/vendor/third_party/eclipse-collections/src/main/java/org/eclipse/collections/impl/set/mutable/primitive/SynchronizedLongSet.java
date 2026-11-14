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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedLongCollection;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.factory.primitive.LongSets;

/**
 * A synchronized view of a {@link MutableLongSet}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link LongIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveSet.stg.
 *
 * @see MutableLongSet#asSynchronized()
 * @see MutableSet#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedLongSet
        extends AbstractSynchronizedLongCollection
        implements MutableLongSet
{
    private static final long serialVersionUID = 1L;

    public SynchronizedLongSet(MutableLongSet set)
    {
        super(set);
    }

    public SynchronizedLongSet(MutableLongSet set, Object newLock)
    {
        super(set, newLock);
    }

    /**
     * This method will take a MutableLongSet and wrap it directly in a SynchronizedLongSet.
     */
    public static SynchronizedLongSet of(MutableLongSet set)
    {
        return new SynchronizedLongSet(set);
    }

    /**
     * This method will take a MutableLongSet and wrap it directly in a SynchronizedLongSet.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedLongSet of(MutableLongSet set, Object lock)
    {
        return new SynchronizedLongSet(set, lock);
    }

    private MutableLongSet getMutableLongSet()
    {
        return (MutableLongSet) this.getLongCollection();
    }

    @Override
    public SynchronizedLongSet without(long element)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongSet().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedLongSet with(long element)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongSet().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedLongSet withAll(LongIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongSet().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedLongSet withoutAll(LongIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongSet().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableLongSet select(LongPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongSet().select(predicate);
        }
    }

    @Override
    public MutableLongSet reject(LongPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongSet().reject(predicate);
        }
    }

    @Override
    public <V> MutableSet<V> collect(LongToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongSet().collect(function);
        }
    }

    @Override
    public LazyIterable<LongLongPair> cartesianProduct(
            LongSet set)
    {
        synchronized (this.getLock())
        {
            return LongSets.cartesianProduct(this, set);
        }
    }

    @Override
    public boolean equals(Object otherSet)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongSet().equals(otherSet);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongSet().hashCode();
        }
    }

    @Override
    public LazyLongIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyLongIterableAdapter(this);
        }
    }

    @Override
    public MutableLongSet asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableLongSet(this);
        }
    }

    @Override
    public MutableLongSet asSynchronized()
    {
        return this;
    }

    public LongSet freeze()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongSet().freeze();
        }
    }

    @Override
    public ImmutableLongSet toImmutable()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongSet().toImmutable();
        }
    }

    /**
     * @since 9.2.
     */
    public MutableLongSet newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongSet().newEmpty();
        }
    }
}
