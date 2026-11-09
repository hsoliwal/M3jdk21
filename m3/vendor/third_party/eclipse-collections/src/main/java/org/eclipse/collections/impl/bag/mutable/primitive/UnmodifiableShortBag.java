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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableShortBag;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortIntProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ShortIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableShortCollection;
import org.eclipse.collections.impl.factory.primitive.ShortBags;
import org.eclipse.collections.api.set.primitive.MutableShortSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveBag.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableShortBag
        extends AbstractUnmodifiableShortCollection
        implements MutableShortBag
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableShortBag(MutableShortBag bag)
    {
        super(bag);
    }

    private MutableShortBag getMutableShortBag()
    {
        return (MutableShortBag) this.getShortCollection();
    }

    @Override
    public UnmodifiableShortBag with(short element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortBag without(short element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortBag withAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortBag withoutAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void addOccurrences(short item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call addOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeOccurrences(short item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call removeOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public int sizeDistinct()
    {
        return this.getMutableShortBag().sizeDistinct();
    }

    @Override
    public int occurrencesOf(short item)
    {
        return this.getMutableShortBag().occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(ShortIntProcedure procedure)
    {
        this.getMutableShortBag().forEachWithOccurrences(procedure);
    }

    @Override
    public MutableShortBag selectByOccurrences(IntPredicate predicate)
    {
        return this.getMutableShortBag().selectByOccurrences(predicate);
    }

    @Override
    public MutableShortSet selectUnique()
    {
        return this.getMutableShortBag().selectUnique();
    }

    @Override
    public MutableList<ShortIntPair> topOccurrences(int count)
    {
        return this.getMutableShortBag().topOccurrences(count);
    }

    @Override
    public MutableList<ShortIntPair> bottomOccurrences(int count)
    {
        return this.getMutableShortBag().bottomOccurrences(count);
    }

    @Override
    public MutableShortBag select(ShortPredicate predicate)
    {
        return this.getMutableShortBag().select(predicate);
    }

    @Override
    public MutableShortBag reject(ShortPredicate predicate)
    {
        return this.getMutableShortBag().reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return this.getMutableShortBag().collect(function);
    }

    @Override
    public boolean equals(Object otherBag)
    {
        return this.getMutableShortBag().equals(otherBag);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableShortBag().hashCode();
    }

    @Override
    public MutableShortBag asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableShortBag asSynchronized()
    {
        return new SynchronizedShortBag(this);
    }

    @Override
    public ImmutableShortBag toImmutable()
    {
        return ShortBags.immutable.withAll(this);
    }

    /**
     * @since 9.2.
     */
    public MutableShortBag newEmpty()
    {
        return this.getMutableShortBag().newEmpty();
    }
}
