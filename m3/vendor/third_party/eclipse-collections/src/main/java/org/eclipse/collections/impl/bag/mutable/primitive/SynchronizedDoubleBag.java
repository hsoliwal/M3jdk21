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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableDoubleBag;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.DoubleIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedDoubleCollection;
import org.eclipse.collections.impl.factory.primitive.DoubleBags;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;

/**
 * A synchronized view of a {@link MutableDoubleBag}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link DoubleIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveBag.stg.
 *
 * @see MutableDoubleBag#asSynchronized()
 * @see MutableBag#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedDoubleBag
        extends AbstractSynchronizedDoubleCollection
        implements MutableDoubleBag
{
    private static final long serialVersionUID = 1L;

    public SynchronizedDoubleBag(MutableDoubleBag bag)
    {
        super(bag);
    }

    public SynchronizedDoubleBag(MutableDoubleBag bag, Object newLock)
    {
        super(bag, newLock);
    }

    private MutableDoubleBag getMutableDoubleBag()
    {
        return (MutableDoubleBag) this.getDoubleCollection();
    }

    @Override
    public SynchronizedDoubleBag with(double element)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleBag().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedDoubleBag without(double element)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleBag().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedDoubleBag withAll(DoubleIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleBag().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedDoubleBag withoutAll(DoubleIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleBag().removeAll(elements);
        }
        return this;
    }

    @Override
    public void addOccurrences(double item, int occurrences)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleBag().addOccurrences(item, occurrences);
        }
    }

    @Override
    public boolean removeOccurrences(double item, int occurrences)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().removeOccurrences(item, occurrences);
        }
    }

    @Override
    public int sizeDistinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().sizeDistinct();
        }
    }

    @Override
    public int occurrencesOf(double item)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().occurrencesOf(item);
        }
    }

    @Override
    public void forEachWithOccurrences(DoubleIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableDoubleBag().forEachWithOccurrences(procedure);
        }
    }

    @Override
    public MutableDoubleBag select(DoublePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().select(predicate);
        }
    }

    @Override
    public MutableDoubleBag selectByOccurrences(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().selectByOccurrences(predicate);
        }
    }

    @Override
    public MutableDoubleSet selectUnique()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().selectUnique();
        }
    }

    @Override
    public MutableList<DoubleIntPair> topOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().topOccurrences(count);
        }
    }

    @Override
    public MutableList<DoubleIntPair> bottomOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().bottomOccurrences(count);
        }
    }

    @Override
    public MutableDoubleBag reject(DoublePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().reject(predicate);
        }
    }

    @Override
    public <V> MutableBag<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().collect(function);
        }
    }

    @Override
    public boolean equals(Object otherBag)
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().equals(otherBag);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().hashCode();
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
    public MutableDoubleBag asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableDoubleBag(this);
        }
    }

    @Override
    public MutableDoubleBag asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableDoubleBag toImmutable()
    {
        synchronized (this.getLock())
        {
            return DoubleBags.immutable.withAll(this);
        }
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableDoubleBag newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableDoubleBag().newEmpty();
        }
    }
}
