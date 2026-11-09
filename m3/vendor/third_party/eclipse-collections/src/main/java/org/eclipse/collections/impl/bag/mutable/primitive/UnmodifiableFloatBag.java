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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.FloatIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableFloatCollection;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveBag.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableFloatBag
        extends AbstractUnmodifiableFloatCollection
        implements MutableFloatBag
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableFloatBag(MutableFloatBag bag)
    {
        super(bag);
    }

    private MutableFloatBag getMutableFloatBag()
    {
        return (MutableFloatBag) this.getFloatCollection();
    }

    @Override
    public UnmodifiableFloatBag with(float element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatBag without(float element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatBag withAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatBag withoutAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void addOccurrences(float item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call addOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeOccurrences(float item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call removeOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public int sizeDistinct()
    {
        return this.getMutableFloatBag().sizeDistinct();
    }

    @Override
    public int occurrencesOf(float item)
    {
        return this.getMutableFloatBag().occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(FloatIntProcedure procedure)
    {
        this.getMutableFloatBag().forEachWithOccurrences(procedure);
    }

    @Override
    public MutableFloatBag selectByOccurrences(IntPredicate predicate)
    {
        return this.getMutableFloatBag().selectByOccurrences(predicate);
    }

    @Override
    public MutableFloatSet selectUnique()
    {
        return this.getMutableFloatBag().selectUnique();
    }

    @Override
    public MutableList<FloatIntPair> topOccurrences(int count)
    {
        return this.getMutableFloatBag().topOccurrences(count);
    }

    @Override
    public MutableList<FloatIntPair> bottomOccurrences(int count)
    {
        return this.getMutableFloatBag().bottomOccurrences(count);
    }

    @Override
    public MutableFloatBag select(FloatPredicate predicate)
    {
        return this.getMutableFloatBag().select(predicate);
    }

    @Override
    public MutableFloatBag reject(FloatPredicate predicate)
    {
        return this.getMutableFloatBag().reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return this.getMutableFloatBag().collect(function);
    }

    @Override
    public boolean equals(Object otherBag)
    {
        return this.getMutableFloatBag().equals(otherBag);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableFloatBag().hashCode();
    }

    @Override
    public MutableFloatBag asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableFloatBag asSynchronized()
    {
        return new SynchronizedFloatBag(this);
    }

    @Override
    public ImmutableFloatBag toImmutable()
    {
        return FloatBags.immutable.withAll(this);
    }

    /**
     * @since 9.2.
     */
    public MutableFloatBag newEmpty()
    {
        return this.getMutableFloatBag().newEmpty();
    }
}
