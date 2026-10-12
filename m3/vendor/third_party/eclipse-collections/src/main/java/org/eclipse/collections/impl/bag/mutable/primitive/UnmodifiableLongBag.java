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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.LongIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableLongCollection;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import org.eclipse.collections.api.set.primitive.MutableLongSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveBag.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableLongBag
        extends AbstractUnmodifiableLongCollection
        implements MutableLongBag
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableLongBag(MutableLongBag bag)
    {
        super(bag);
    }

    private MutableLongBag getMutableLongBag()
    {
        return (MutableLongBag) this.getLongCollection();
    }

    @Override
    public UnmodifiableLongBag with(long element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongBag without(long element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongBag withAll(LongIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongBag withoutAll(LongIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void addOccurrences(long item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call addOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeOccurrences(long item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call removeOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public int sizeDistinct()
    {
        return this.getMutableLongBag().sizeDistinct();
    }

    @Override
    public int occurrencesOf(long item)
    {
        return this.getMutableLongBag().occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(LongIntProcedure procedure)
    {
        this.getMutableLongBag().forEachWithOccurrences(procedure);
    }

    @Override
    public MutableLongBag selectByOccurrences(IntPredicate predicate)
    {
        return this.getMutableLongBag().selectByOccurrences(predicate);
    }

    @Override
    public MutableLongSet selectUnique()
    {
        return this.getMutableLongBag().selectUnique();
    }

    @Override
    public MutableList<LongIntPair> topOccurrences(int count)
    {
        return this.getMutableLongBag().topOccurrences(count);
    }

    @Override
    public MutableList<LongIntPair> bottomOccurrences(int count)
    {
        return this.getMutableLongBag().bottomOccurrences(count);
    }

    @Override
    public MutableLongBag select(LongPredicate predicate)
    {
        return this.getMutableLongBag().select(predicate);
    }

    @Override
    public MutableLongBag reject(LongPredicate predicate)
    {
        return this.getMutableLongBag().reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(LongToObjectFunction<? extends V> function)
    {
        return this.getMutableLongBag().collect(function);
    }

    @Override
    public boolean equals(Object otherBag)
    {
        return this.getMutableLongBag().equals(otherBag);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableLongBag().hashCode();
    }

    @Override
    public MutableLongBag asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableLongBag asSynchronized()
    {
        return new SynchronizedLongBag(this);
    }

    @Override
    public ImmutableLongBag toImmutable()
    {
        return LongBags.immutable.withAll(this);
    }

    /**
     * @since 9.2.
     */
    public MutableLongBag newEmpty()
    {
        return this.getMutableLongBag().newEmpty();
    }
}
