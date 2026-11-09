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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableBooleanBag;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanIntProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.BooleanIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableBooleanCollection;
import org.eclipse.collections.impl.factory.primitive.BooleanBags;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveBag.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableBooleanBag
        extends AbstractUnmodifiableBooleanCollection
        implements MutableBooleanBag
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableBooleanBag(MutableBooleanBag bag)
    {
        super(bag);
    }

    private MutableBooleanBag getMutableBooleanBag()
    {
        return (MutableBooleanBag) this.getBooleanCollection();
    }

    @Override
    public UnmodifiableBooleanBag with(boolean element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanBag without(boolean element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanBag withAll(BooleanIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanBag withoutAll(BooleanIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void addOccurrences(boolean item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call addOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeOccurrences(boolean item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call removeOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public int sizeDistinct()
    {
        return this.getMutableBooleanBag().sizeDistinct();
    }

    @Override
    public int occurrencesOf(boolean item)
    {
        return this.getMutableBooleanBag().occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(BooleanIntProcedure procedure)
    {
        this.getMutableBooleanBag().forEachWithOccurrences(procedure);
    }

    @Override
    public MutableBooleanBag selectByOccurrences(IntPredicate predicate)
    {
        return this.getMutableBooleanBag().selectByOccurrences(predicate);
    }

    @Override
    public MutableBooleanSet selectUnique()
    {
        return this.getMutableBooleanBag().selectUnique();
    }

    @Override
    public MutableList<BooleanIntPair> topOccurrences(int count)
    {
        return this.getMutableBooleanBag().topOccurrences(count);
    }

    @Override
    public MutableList<BooleanIntPair> bottomOccurrences(int count)
    {
        return this.getMutableBooleanBag().bottomOccurrences(count);
    }

    @Override
    public MutableBooleanBag select(BooleanPredicate predicate)
    {
        return this.getMutableBooleanBag().select(predicate);
    }

    @Override
    public MutableBooleanBag reject(BooleanPredicate predicate)
    {
        return this.getMutableBooleanBag().reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return this.getMutableBooleanBag().collect(function);
    }

    @Override
    public boolean equals(Object otherBag)
    {
        return this.getMutableBooleanBag().equals(otherBag);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableBooleanBag().hashCode();
    }

    @Override
    public MutableBooleanBag asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableBooleanBag asSynchronized()
    {
        return new SynchronizedBooleanBag(this);
    }

    @Override
    public ImmutableBooleanBag toImmutable()
    {
        return BooleanBags.immutable.withAll(this);
    }

    /**
     * @since 9.2.
     */
    public MutableBooleanBag newEmpty()
    {
        return this.getMutableBooleanBag().newEmpty();
    }
}
