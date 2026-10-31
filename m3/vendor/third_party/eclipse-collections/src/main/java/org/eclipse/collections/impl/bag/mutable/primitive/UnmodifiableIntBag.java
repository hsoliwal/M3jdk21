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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableIntBag;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableIntCollection;
import org.eclipse.collections.impl.factory.primitive.IntBags;
import org.eclipse.collections.api.set.primitive.MutableIntSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveBag.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableIntBag
        extends AbstractUnmodifiableIntCollection
        implements MutableIntBag
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableIntBag(MutableIntBag bag)
    {
        super(bag);
    }

    private MutableIntBag getMutableIntBag()
    {
        return (MutableIntBag) this.getIntCollection();
    }

    @Override
    public UnmodifiableIntBag with(int element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntBag without(int element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntBag withAll(IntIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntBag withoutAll(IntIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void addOccurrences(int item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call addOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeOccurrences(int item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call removeOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public int sizeDistinct()
    {
        return this.getMutableIntBag().sizeDistinct();
    }

    @Override
    public int occurrencesOf(int item)
    {
        return this.getMutableIntBag().occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(IntIntProcedure procedure)
    {
        this.getMutableIntBag().forEachWithOccurrences(procedure);
    }

    @Override
    public MutableIntBag selectByOccurrences(IntPredicate predicate)
    {
        return this.getMutableIntBag().selectByOccurrences(predicate);
    }

    @Override
    public MutableIntSet selectUnique()
    {
        return this.getMutableIntBag().selectUnique();
    }

    @Override
    public MutableList<IntIntPair> topOccurrences(int count)
    {
        return this.getMutableIntBag().topOccurrences(count);
    }

    @Override
    public MutableList<IntIntPair> bottomOccurrences(int count)
    {
        return this.getMutableIntBag().bottomOccurrences(count);
    }

    @Override
    public MutableIntBag select(IntPredicate predicate)
    {
        return this.getMutableIntBag().select(predicate);
    }

    @Override
    public MutableIntBag reject(IntPredicate predicate)
    {
        return this.getMutableIntBag().reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        return this.getMutableIntBag().collect(function);
    }

    @Override
    public boolean equals(Object otherBag)
    {
        return this.getMutableIntBag().equals(otherBag);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableIntBag().hashCode();
    }

    @Override
    public MutableIntBag asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableIntBag asSynchronized()
    {
        return new SynchronizedIntBag(this);
    }

    @Override
    public ImmutableIntBag toImmutable()
    {
        return IntBags.immutable.withAll(this);
    }

    /**
     * @since 9.2.
     */
    public MutableIntBag newEmpty()
    {
        return this.getMutableIntBag().newEmpty();
    }
}
