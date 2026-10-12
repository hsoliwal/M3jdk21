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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableCharBag;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.CharIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedCharCollection;
import org.eclipse.collections.impl.factory.primitive.CharBags;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;
import org.eclipse.collections.api.set.primitive.MutableCharSet;

/**
 * A synchronized view of a {@link MutableCharBag}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link CharIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveBag.stg.
 *
 * @see MutableCharBag#asSynchronized()
 * @see MutableBag#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedCharBag
        extends AbstractSynchronizedCharCollection
        implements MutableCharBag
{
    private static final long serialVersionUID = 1L;

    public SynchronizedCharBag(MutableCharBag bag)
    {
        super(bag);
    }

    public SynchronizedCharBag(MutableCharBag bag, Object newLock)
    {
        super(bag, newLock);
    }

    private MutableCharBag getMutableCharBag()
    {
        return (MutableCharBag) this.getCharCollection();
    }

    @Override
    public SynchronizedCharBag with(char element)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharBag().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedCharBag without(char element)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharBag().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedCharBag withAll(CharIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharBag().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedCharBag withoutAll(CharIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharBag().removeAll(elements);
        }
        return this;
    }

    @Override
    public void addOccurrences(char item, int occurrences)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharBag().addOccurrences(item, occurrences);
        }
    }

    @Override
    public boolean removeOccurrences(char item, int occurrences)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().removeOccurrences(item, occurrences);
        }
    }

    @Override
    public int sizeDistinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().sizeDistinct();
        }
    }

    @Override
    public int occurrencesOf(char item)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().occurrencesOf(item);
        }
    }

    @Override
    public void forEachWithOccurrences(CharIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharBag().forEachWithOccurrences(procedure);
        }
    }

    @Override
    public MutableCharBag select(CharPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().select(predicate);
        }
    }

    @Override
    public MutableCharBag selectByOccurrences(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().selectByOccurrences(predicate);
        }
    }

    @Override
    public MutableCharSet selectUnique()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().selectUnique();
        }
    }

    @Override
    public MutableList<CharIntPair> topOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().topOccurrences(count);
        }
    }

    @Override
    public MutableList<CharIntPair> bottomOccurrences(int count)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().bottomOccurrences(count);
        }
    }

    @Override
    public MutableCharBag reject(CharPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().reject(predicate);
        }
    }

    @Override
    public <V> MutableBag<V> collect(CharToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().collect(function);
        }
    }

    @Override
    public boolean equals(Object otherBag)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().equals(otherBag);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().hashCode();
        }
    }

    @Override
    public LazyCharIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyCharIterableAdapter(this);
        }
    }

    @Override
    public MutableCharBag asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableCharBag(this);
        }
    }

    @Override
    public MutableCharBag asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableCharBag toImmutable()
    {
        synchronized (this.getLock())
        {
            return CharBags.immutable.withAll(this);
        }
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableCharBag newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharBag().newEmpty();
        }
    }
}
