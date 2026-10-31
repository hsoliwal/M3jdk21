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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableShortBag;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortIntProcedure;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ShortIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedShortCollection;
import org.eclipse.collections.impl.factory.primitive.ShortBags;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;
import org.eclipse.collections.api.set.primitive.MutableShortSet;

/**
 * A synchronized view of a {@link MutableShortBag}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link ShortIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveBag.stg.
 *
 * @see MutableShortBag#asSynchronized()
 * @see MutableBag#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedShortBag
        extends AbstractSynchronizedShortCollection
        implements MutableShortBag
{
    private static final long serialVersionUID = 1L;

    public SynchronizedShortBag(MutableShortBag bag)
    {
        super(bag);
    }

    public SynchronizedShortBag(MutableShortBag bag, Object newLock)
    {
        super(bag, newLock);
    }

    private MutableShortBag getMutableShortBag()
    {
        return (MutableShortBag) this.getShortCollection();
    }

    @Override
    public SynchronizedShortBag with(short element)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortBag().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedShortBag without(short element)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortBag().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedShortBag withAll(ShortIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortBag().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedShortBag withoutAll(ShortIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortBag().removeAll(elements);
        }
        return this;
    }

    @Override
    public void addOccurrences(short item, int occurrences)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortBag().addOccurrences(item, occurrences);
        }
    }

    @Override
    public boolean removeOccurrences(short item, int occurrences)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().removeOccurrences(item, occurrences);
        }
    }

    @Override
    public int sizeDistinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().sizeDistinct();
        }
    }

    @Override
    public int occurrencesOf(short item)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().occurrencesOf(item);
        }
    }

    @Override
    public void forEachWithOccurrences(ShortIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableShortBag().forEachWithOccurrences(procedure);
        }
    }

    @Override
    public MutableShortBag select(ShortPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().select(predicate);
        }
    }

    @Override
    public MutableShortBag selectByOccurrences(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().selectByOccurrences(predicate);
        }
    }

    @Override
    public MutableShortSet selectUnique()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().selectUnique();
        }
    }

    @Override
    public MutableList<ShortIntPair> topOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().topOccurrences(count);
        }
    }

    @Override
    public MutableList<ShortIntPair> bottomOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().bottomOccurrences(count);
        }
    }

    @Override
    public MutableShortBag reject(ShortPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().reject(predicate);
        }
    }

    @Override
    public <V> MutableBag<V> collect(ShortToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().collect(function);
        }
    }

    @Override
    public boolean equals(Object otherBag)
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().equals(otherBag);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().hashCode();
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
    public MutableShortBag asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableShortBag(this);
        }
    }

    @Override
    public MutableShortBag asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableShortBag toImmutable()
    {
        synchronized (this.getLock())
        {
            return ShortBags.immutable.withAll(this);
        }
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableShortBag newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableShortBag().newEmpty();
        }
    }
}
