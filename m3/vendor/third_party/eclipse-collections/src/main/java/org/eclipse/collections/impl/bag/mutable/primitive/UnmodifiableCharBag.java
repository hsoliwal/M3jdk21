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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableCharBag;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.CharIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableCharCollection;
import org.eclipse.collections.impl.factory.primitive.CharBags;
import org.eclipse.collections.api.set.primitive.MutableCharSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveBag.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableCharBag
        extends AbstractUnmodifiableCharCollection
        implements MutableCharBag
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableCharBag(MutableCharBag bag)
    {
        super(bag);
    }

    private MutableCharBag getMutableCharBag()
    {
        return (MutableCharBag) this.getCharCollection();
    }

    @Override
    public UnmodifiableCharBag with(char element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharBag without(char element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharBag withAll(CharIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharBag withoutAll(CharIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void addOccurrences(char item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call addOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeOccurrences(char item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call removeOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public int sizeDistinct()
    {
        return this.getMutableCharBag().sizeDistinct();
    }

    @Override
    public int occurrencesOf(char item)
    {
        return this.getMutableCharBag().occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(CharIntProcedure procedure)
    {
        this.getMutableCharBag().forEachWithOccurrences(procedure);
    }

    @Override
    public MutableCharBag selectByOccurrences(IntPredicate predicate)
    {
        return this.getMutableCharBag().selectByOccurrences(predicate);
    }

    @Override
    public MutableCharSet selectUnique()
    {
        return this.getMutableCharBag().selectUnique();
    }

    @Override
    public MutableList<CharIntPair> topOccurrences(int count)
    {
        return this.getMutableCharBag().topOccurrences(count);
    }

    @Override
    public MutableList<CharIntPair> bottomOccurrences(int count)
    {
        return this.getMutableCharBag().bottomOccurrences(count);
    }

    @Override
    public MutableCharBag select(CharPredicate predicate)
    {
        return this.getMutableCharBag().select(predicate);
    }

    @Override
    public MutableCharBag reject(CharPredicate predicate)
    {
        return this.getMutableCharBag().reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(CharToObjectFunction<? extends V> function)
    {
        return this.getMutableCharBag().collect(function);
    }

    @Override
    public boolean equals(Object otherBag)
    {
        return this.getMutableCharBag().equals(otherBag);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableCharBag().hashCode();
    }

    @Override
    public MutableCharBag asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableCharBag asSynchronized()
    {
        return new SynchronizedCharBag(this);
    }

    @Override
    public ImmutableCharBag toImmutable()
    {
        return CharBags.immutable.withAll(this);
    }

    /**
     * @since 9.2.
     */
    public MutableCharBag newEmpty()
    {
        return this.getMutableCharBag().newEmpty();
    }
}
