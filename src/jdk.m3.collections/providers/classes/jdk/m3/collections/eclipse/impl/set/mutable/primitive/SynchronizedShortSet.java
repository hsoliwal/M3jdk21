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

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableShortSet;
import jdk.m3.collections.eclipse.api.set.primitive.ShortSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortShortPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedShortCollection;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyShortIterableAdapter;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortSets;
import jdk.m3.collections.eclipse.impl.set.mutable.SynchronizedMutableSet;

/**
 * A synchronized view of a {@link MutableShortSet}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link ShortIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveSet.stg.
 *
 * @see MutableShortSet#asSynchronized()
 * @see MutableSet#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedShortSet
        extends AbstractSynchronizedShortCollection
        implements MutableShortSet
{
    private static final long serialVersionUID = 1L;

    public SynchronizedShortSet(MutableShortSet set)
    {
        super(set);
    }

    public SynchronizedShortSet(MutableShortSet set, Object newLock)
    {
        super(set, newLock);
    }

    /**
     * This method will take a MutableShortSet and wrap it directly in a SynchronizedShortSet.
     */
    public static SynchronizedShortSet of(MutableShortSet set)
    {
        return new SynchronizedShortSet(set);
    }

    /**
     * This method will take a MutableShortSet and wrap it directly in a SynchronizedShortSet.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedShortSet of(MutableShortSet set, Object lock)
    {
        return new SynchronizedShortSet(set, lock);
    }

    private MutableShortSet getMutableShortSet()
    {
        return (MutableShortSet) this.getShortCollection();
    }

    @Override
    public SynchronizedShortSet without(short element)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortSet().remove(element);
        }
        return this;
    }

    @Override
    public MutableSet<Short> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableSet.of(new BoxedMutableShortSet(this), this.getLock());
        }
    }

    @Override
    public SynchronizedShortSet with(short element)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortSet().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedShortSet withAll(ShortIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortSet().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedShortSet withoutAll(ShortIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortSet().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableShortSet select(ShortPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortSet().select(predicate);
        }
    }

    @Override
    public MutableShortSet reject(ShortPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortSet().reject(predicate);
        }
    }

    @Override
    public <V> MutableSet<V> collect(ShortToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortSet().collect(function);
        }
    }

    @Override
    public LazyIterable<ShortShortPair> cartesianProduct(
            ShortSet set)
    {
        synchronized (this.getLock())
        {
            return ShortSets.cartesianProduct(this, set);
        }
    }

    @Override
    public boolean equals(Object otherSet)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortSet().equals(otherSet);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortSet().hashCode();
        }
    }

    @Override
    public LazyShortIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyShortIterableAdapter(this);
        }
    }

    @Override
    public MutableShortSet asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableShortSet(this);
        }
    }

    @Override
    public MutableShortSet asSynchronized()
    {
        return this;
    }

    public ShortSet freeze()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortSet().freeze();
        }
    }

    @Override
    public ImmutableShortSet toImmutable()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortSet().toImmutable();
        }
    }

    /**
     * @since 9.2.
     */
    public MutableShortSet newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortSet().newEmpty();
        }
    }
}
