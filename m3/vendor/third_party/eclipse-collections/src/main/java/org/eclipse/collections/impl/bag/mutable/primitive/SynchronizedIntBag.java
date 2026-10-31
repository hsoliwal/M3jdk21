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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableIntBag;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedIntCollection;
import org.eclipse.collections.impl.factory.primitive.IntBags;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.api.set.primitive.MutableIntSet;

/**
 * A synchronized view of a {@link MutableIntBag}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link IntIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveBag.stg.
 *
 * @see MutableIntBag#asSynchronized()
 * @see MutableBag#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedIntBag
        extends AbstractSynchronizedIntCollection
        implements MutableIntBag
{
    private static final long serialVersionUID = 1L;

    public SynchronizedIntBag(MutableIntBag bag)
    {
        super(bag);
    }

    public SynchronizedIntBag(MutableIntBag bag, Object newLock)
    {
        super(bag, newLock);
    }

    private MutableIntBag getMutableIntBag()
    {
        return (MutableIntBag) this.getIntCollection();
    }

    @Override
    public SynchronizedIntBag with(int element)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntBag().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedIntBag without(int element)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntBag().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedIntBag withAll(IntIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntBag().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedIntBag withoutAll(IntIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntBag().removeAll(elements);
        }
        return this;
    }

    @Override
    public void addOccurrences(int item, int occurrences)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntBag().addOccurrences(item, occurrences);
        }
    }

    @Override
    public boolean removeOccurrences(int item, int occurrences)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().removeOccurrences(item, occurrences);
        }
    }

    @Override
    public int sizeDistinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().sizeDistinct();
        }
    }

    @Override
    public int occurrencesOf(int item)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().occurrencesOf(item);
        }
    }

    @Override
    public void forEachWithOccurrences(IntIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntBag().forEachWithOccurrences(procedure);
        }
    }

    @Override
    public MutableIntBag select(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().select(predicate);
        }
    }

    @Override
    public MutableIntBag selectByOccurrences(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().selectByOccurrences(predicate);
        }
    }

    @Override
    public MutableIntSet selectUnique()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().selectUnique();
        }
    }

    @Override
    public MutableList<IntIntPair> topOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().topOccurrences(count);
        }
    }

    @Override
    public MutableList<IntIntPair> bottomOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().bottomOccurrences(count);
        }
    }

    @Override
    public MutableIntBag reject(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().reject(predicate);
        }
    }

    @Override
    public <V> MutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().collect(function);
        }
    }

    @Override
    public boolean equals(Object otherBag)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().equals(otherBag);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().hashCode();
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
    public MutableIntBag asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableIntBag(this);
        }
    }

    @Override
    public MutableIntBag asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableIntBag toImmutable()
    {
        synchronized (this.getLock())
        {
            return IntBags.immutable.withAll(this);
        }
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableIntBag newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntBag().newEmpty();
        }
    }
}
