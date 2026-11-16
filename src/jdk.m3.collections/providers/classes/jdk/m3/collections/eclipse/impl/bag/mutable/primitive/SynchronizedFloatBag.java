/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.bag.mutable.primitive;

import java.util.Collection;
import java.util.Collections;

import jdk.m3.collections.eclipse.api.FloatIterable;
import jdk.m3.collections.eclipse.api.LazyFloatIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableFloatBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableFloatBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.FloatPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.FloatIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.FloatIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatIntPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedFloatCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatBags;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyFloatIterableAdapter;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;

/**
 * A synchronized view of a {@link MutableFloatBag}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link FloatIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveBag.stg.
 *
 * @see MutableFloatBag#asSynchronized()
 * @see MutableBag#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedFloatBag
        extends AbstractSynchronizedFloatCollection
        implements MutableFloatBag
{
    private static final long serialVersionUID = 1L;

    public SynchronizedFloatBag(MutableFloatBag bag)
    {
        super(bag);
    }

    public SynchronizedFloatBag(MutableFloatBag bag, Object newLock)
    {
        super(bag, newLock);
    }

    private MutableFloatBag getMutableFloatBag()
    {
        return (MutableFloatBag) this.getFloatCollection();
    }

    @Override
    public SynchronizedFloatBag with(float element)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatBag().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedFloatBag without(float element)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatBag().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedFloatBag withAll(FloatIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatBag().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedFloatBag withoutAll(FloatIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatBag().removeAll(elements);
        }
        return this;
    }

    @Override
    public void addOccurrences(float item, int occurrences)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatBag().addOccurrences(item, occurrences);
        }
    }

    @Override
    public boolean removeOccurrences(float item, int occurrences)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().removeOccurrences(item, occurrences);
        }
    }

    @Override
    public int sizeDistinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().sizeDistinct();
        }
    }

    @Override
    public int occurrencesOf(float item)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().occurrencesOf(item);
        }
    }

    @Override
    public void forEachWithOccurrences(FloatIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableFloatBag().forEachWithOccurrences(procedure);
        }
    }

    @Override
    public MutableFloatBag select(FloatPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().select(predicate);
        }
    }

    @Override
    public MutableFloatBag selectByOccurrences(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().selectByOccurrences(predicate);
        }
    }

    @Override
    public MutableFloatSet selectUnique()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().selectUnique();
        }
    }

    @Override
    public MutableList<FloatIntPair> topOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().topOccurrences(count);
        }
    }

    @Override
    public MutableList<FloatIntPair> bottomOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().bottomOccurrences(count);
        }
    }

    @Override
    public MutableFloatBag reject(FloatPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().reject(predicate);
        }
    }

    @Override
    public <V> MutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().collect(function);
        }
    }

    @Override
    public boolean equals(Object otherBag)
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().equals(otherBag);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().hashCode();
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
    public MutableFloatBag asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableFloatBag(this);
        }
    }

    @Override
    public MutableFloatBag asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableFloatBag toImmutable()
    {
        synchronized (this.getLock())
        {
            return FloatBags.immutable.withAll(this);
        }
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableFloatBag newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableFloatBag().newEmpty();
        }
    }
}
