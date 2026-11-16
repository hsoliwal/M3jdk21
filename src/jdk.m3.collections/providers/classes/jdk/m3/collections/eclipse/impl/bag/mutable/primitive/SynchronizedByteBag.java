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

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.LazyByteIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableByteBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableByteBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BytePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ByteIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.ByteIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.ByteIntPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedByteCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteBags;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyByteIterableAdapter;
import jdk.m3.collections.eclipse.api.set.primitive.MutableByteSet;

/**
 * A synchronized view of a {@link MutableByteBag}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link ByteIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveBag.stg.
 *
 * @see MutableByteBag#asSynchronized()
 * @see MutableBag#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedByteBag
        extends AbstractSynchronizedByteCollection
        implements MutableByteBag
{
    private static final long serialVersionUID = 1L;

    public SynchronizedByteBag(MutableByteBag bag)
    {
        super(bag);
    }

    public SynchronizedByteBag(MutableByteBag bag, Object newLock)
    {
        super(bag, newLock);
    }

    private MutableByteBag getMutableByteBag()
    {
        return (MutableByteBag) this.getByteCollection();
    }

    @Override
    public SynchronizedByteBag with(byte element)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteBag().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedByteBag without(byte element)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteBag().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedByteBag withAll(ByteIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteBag().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedByteBag withoutAll(ByteIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteBag().removeAll(elements);
        }
        return this;
    }

    @Override
    public void addOccurrences(byte item, int occurrences)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteBag().addOccurrences(item, occurrences);
        }
    }

    @Override
    public boolean removeOccurrences(byte item, int occurrences)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().removeOccurrences(item, occurrences);
        }
    }

    @Override
    public int sizeDistinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().sizeDistinct();
        }
    }

    @Override
    public int occurrencesOf(byte item)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().occurrencesOf(item);
        }
    }

    @Override
    public void forEachWithOccurrences(ByteIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableByteBag().forEachWithOccurrences(procedure);
        }
    }

    @Override
    public MutableByteBag select(BytePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().select(predicate);
        }
    }

    @Override
    public MutableByteBag selectByOccurrences(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().selectByOccurrences(predicate);
        }
    }

    @Override
    public MutableByteSet selectUnique()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().selectUnique();
        }
    }

    @Override
    public MutableList<ByteIntPair> topOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().topOccurrences(count);
        }
    }

    @Override
    public MutableList<ByteIntPair> bottomOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().bottomOccurrences(count);
        }
    }

    @Override
    public MutableByteBag reject(BytePredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().reject(predicate);
        }
    }

    @Override
    public <V> MutableBag<V> collect(ByteToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().collect(function);
        }
    }

    @Override
    public boolean equals(Object otherBag)
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().equals(otherBag);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().hashCode();
        }
    }

    @Override
    public LazyByteIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyByteIterableAdapter(this);
        }
    }

    @Override
    public MutableByteBag asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableByteBag(this);
        }
    }

    @Override
    public MutableByteBag asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableByteBag toImmutable()
    {
        synchronized (this.getLock())
        {
            return ByteBags.immutable.withAll(this);
        }
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableByteBag newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableByteBag().newEmpty();
        }
    }
}
