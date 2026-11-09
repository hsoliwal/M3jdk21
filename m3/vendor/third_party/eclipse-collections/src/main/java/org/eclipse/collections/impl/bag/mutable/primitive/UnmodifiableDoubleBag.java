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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableDoubleBag;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.DoubleIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableDoubleCollection;
import org.eclipse.collections.impl.factory.primitive.DoubleBags;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveBag.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableDoubleBag
        extends AbstractUnmodifiableDoubleCollection
        implements MutableDoubleBag
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableDoubleBag(MutableDoubleBag bag)
    {
        super(bag);
    }

    private MutableDoubleBag getMutableDoubleBag()
    {
        return (MutableDoubleBag) this.getDoubleCollection();
    }

    @Override
    public UnmodifiableDoubleBag with(double element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleBag without(double element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleBag withAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleBag withoutAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void addOccurrences(double item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call addOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeOccurrences(double item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call removeOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public int sizeDistinct()
    {
        return this.getMutableDoubleBag().sizeDistinct();
    }

    @Override
    public int occurrencesOf(double item)
    {
        return this.getMutableDoubleBag().occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(DoubleIntProcedure procedure)
    {
        this.getMutableDoubleBag().forEachWithOccurrences(procedure);
    }

    @Override
    public MutableDoubleBag selectByOccurrences(IntPredicate predicate)
    {
        return this.getMutableDoubleBag().selectByOccurrences(predicate);
    }

    @Override
    public MutableDoubleSet selectUnique()
    {
        return this.getMutableDoubleBag().selectUnique();
    }

    @Override
    public MutableList<DoubleIntPair> topOccurrences(int count)
    {
        return this.getMutableDoubleBag().topOccurrences(count);
    }

    @Override
    public MutableList<DoubleIntPair> bottomOccurrences(int count)
    {
        return this.getMutableDoubleBag().bottomOccurrences(count);
    }

    @Override
    public MutableDoubleBag select(DoublePredicate predicate)
    {
        return this.getMutableDoubleBag().select(predicate);
    }

    @Override
    public MutableDoubleBag reject(DoublePredicate predicate)
    {
        return this.getMutableDoubleBag().reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return this.getMutableDoubleBag().collect(function);
    }

    @Override
    public boolean equals(Object otherBag)
    {
        return this.getMutableDoubleBag().equals(otherBag);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableDoubleBag().hashCode();
    }

    @Override
    public MutableDoubleBag asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableDoubleBag asSynchronized()
    {
        return new SynchronizedDoubleBag(this);
    }

    @Override
    public ImmutableDoubleBag toImmutable()
    {
        return DoubleBags.immutable.withAll(this);
    }

    /**
     * @since 9.2.
     */
    public MutableDoubleBag newEmpty()
    {
        return this.getMutableDoubleBag().newEmpty();
    }
}
