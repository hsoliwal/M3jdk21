/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.mutable.primitive;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.FloatIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedFloatCollection;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;

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
