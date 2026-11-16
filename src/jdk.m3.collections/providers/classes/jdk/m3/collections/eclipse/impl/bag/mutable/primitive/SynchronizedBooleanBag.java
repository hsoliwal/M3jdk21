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

import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableBooleanBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableBooleanBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.BooleanIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.BooleanIntPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedBooleanCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanBags;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyBooleanIterableAdapter;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;

/**
 * A synchronized view of a {@link MutableBooleanBag}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link BooleanIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveBag.stg.
 *
 * @see MutableBooleanBag#asSynchronized()
 * @see MutableBag#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedBooleanBag
        extends AbstractSynchronizedBooleanCollection
        implements MutableBooleanBag
{
    private static final long serialVersionUID = 1L;

    public SynchronizedBooleanBag(MutableBooleanBag bag)
    {
        super(bag);
    }

    public SynchronizedBooleanBag(MutableBooleanBag bag, Object newLock)
    {
        super(bag, newLock);
    }

    private MutableBooleanBag getMutableBooleanBag()
    {
        return (MutableBooleanBag) this.getBooleanCollection();
    }

    @Override
    public SynchronizedBooleanBag with(boolean element)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanBag().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedBooleanBag without(boolean element)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanBag().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedBooleanBag withAll(BooleanIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanBag().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedBooleanBag withoutAll(BooleanIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanBag().removeAll(elements);
        }
        return this;
    }

    @Override
    public void addOccurrences(boolean item, int occurrences)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanBag().addOccurrences(item, occurrences);
        }
    }

    @Override
    public boolean removeOccurrences(boolean item, int occurrences)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().removeOccurrences(item, occurrences);
        }
    }

    @Override
    public int sizeDistinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().sizeDistinct();
        }
    }

    @Override
    public int occurrencesOf(boolean item)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().occurrencesOf(item);
        }
    }

    @Override
    public void forEachWithOccurrences(BooleanIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableBooleanBag().forEachWithOccurrences(procedure);
        }
    }

    @Override
    public MutableBooleanBag select(BooleanPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().select(predicate);
        }
    }

    @Override
    public MutableBooleanBag selectByOccurrences(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().selectByOccurrences(predicate);
        }
    }

    @Override
    public MutableBooleanSet selectUnique()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().selectUnique();
        }
    }

    @Override
    public MutableList<BooleanIntPair> topOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().topOccurrences(count);
        }
    }

    @Override
    public MutableList<BooleanIntPair> bottomOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().bottomOccurrences(count);
        }
    }

    @Override
    public MutableBooleanBag reject(BooleanPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().reject(predicate);
        }
    }

    @Override
    public <V> MutableBag<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().collect(function);
        }
    }

    @Override
    public boolean equals(Object otherBag)
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().equals(otherBag);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().hashCode();
        }
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyBooleanIterableAdapter(this);
        }
    }

    @Override
    public MutableBooleanBag asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableBooleanBag(this);
        }
    }

    @Override
    public MutableBooleanBag asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableBooleanBag toImmutable()
    {
        synchronized (this.getLock())
        {
            return BooleanBags.immutable.withAll(this);
        }
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableBooleanBag newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableBooleanBag().newEmpty();
        }
    }
}
