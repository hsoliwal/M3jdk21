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

import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableBooleanBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableBooleanBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanIntProcedure;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.BooleanIntPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractUnmodifiableBooleanCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanBags;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;

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
