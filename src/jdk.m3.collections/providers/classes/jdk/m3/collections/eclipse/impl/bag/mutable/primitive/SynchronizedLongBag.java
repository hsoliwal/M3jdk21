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

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableLongBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableLongBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongIntPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedLongCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongBags;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyLongIterableAdapter;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;

/**
 * A synchronized view of a {@link MutableLongBag}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link LongIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveBag.stg.
 *
 * @see MutableLongBag#asSynchronized()
 * @see MutableBag#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedLongBag
        extends AbstractSynchronizedLongCollection
        implements MutableLongBag
{
    private static final long serialVersionUID = 1L;

    public SynchronizedLongBag(MutableLongBag bag)
    {
        super(bag);
    }

    public SynchronizedLongBag(MutableLongBag bag, Object newLock)
    {
        super(bag, newLock);
    }

    private MutableLongBag getMutableLongBag()
    {
        return (MutableLongBag) this.getLongCollection();
    }

    @Override
    public SynchronizedLongBag with(long element)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongBag().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedLongBag without(long element)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongBag().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedLongBag withAll(LongIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongBag().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedLongBag withoutAll(LongIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongBag().removeAll(elements);
        }
        return this;
    }

    @Override
    public void addOccurrences(long item, int occurrences)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongBag().addOccurrences(item, occurrences);
        }
    }

    @Override
    public boolean removeOccurrences(long item, int occurrences)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().removeOccurrences(item, occurrences);
        }
    }

    @Override
    public int sizeDistinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().sizeDistinct();
        }
    }

    @Override
    public int occurrencesOf(long item)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().occurrencesOf(item);
        }
    }

    @Override
    public void forEachWithOccurrences(LongIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableLongBag().forEachWithOccurrences(procedure);
        }
    }

    @Override
    public MutableLongBag select(LongPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().select(predicate);
        }
    }

    @Override
    public MutableLongBag selectByOccurrences(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().selectByOccurrences(predicate);
        }
    }

    @Override
    public MutableLongSet selectUnique()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().selectUnique();
        }
    }

    @Override
    public MutableList<LongIntPair> topOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().topOccurrences(count);
        }
    }

    @Override
    public MutableList<LongIntPair> bottomOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().bottomOccurrences(count);
        }
    }

    @Override
    public MutableLongBag reject(LongPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().reject(predicate);
        }
    }

    @Override
    public <V> MutableBag<V> collect(LongToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().collect(function);
        }
    }

    @Override
    public boolean equals(Object otherBag)
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().equals(otherBag);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().hashCode();
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
    public MutableLongBag asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableLongBag(this);
        }
    }

    @Override
    public MutableLongBag asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableLongBag toImmutable()
    {
        synchronized (this.getLock())
        {
            return LongBags.immutable.withAll(this);
        }
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableLongBag newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableLongBag().newEmpty();
        }
    }
}
