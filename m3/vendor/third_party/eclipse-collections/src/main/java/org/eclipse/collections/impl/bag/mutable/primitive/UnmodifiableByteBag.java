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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableByteBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ByteIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableByteCollection;
import org.eclipse.collections.impl.factory.primitive.ByteBags;
import org.eclipse.collections.api.set.primitive.MutableByteSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveBag.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableByteBag
        extends AbstractUnmodifiableByteCollection
        implements MutableByteBag
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableByteBag(MutableByteBag bag)
    {
        super(bag);
    }

    private MutableByteBag getMutableByteBag()
    {
        return (MutableByteBag) this.getByteCollection();
    }

    @Override
    public UnmodifiableByteBag with(byte element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableByteBag without(byte element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableByteBag withAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableByteBag withoutAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void addOccurrences(byte item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call addOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeOccurrences(byte item, int occurrences)
    {
        throw new UnsupportedOperationException("Cannot call removeOccurrences() on " + this.getClass().getSimpleName());
    }

    @Override
    public int sizeDistinct()
    {
        return this.getMutableByteBag().sizeDistinct();
    }

    @Override
    public int occurrencesOf(byte item)
    {
        return this.getMutableByteBag().occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(ByteIntProcedure procedure)
    {
        this.getMutableByteBag().forEachWithOccurrences(procedure);
    }

    @Override
    public MutableByteBag selectByOccurrences(IntPredicate predicate)
    {
        return this.getMutableByteBag().selectByOccurrences(predicate);
    }

    @Override
    public MutableByteSet selectUnique()
    {
        return this.getMutableByteBag().selectUnique();
    }

    @Override
    public MutableList<ByteIntPair> topOccurrences(int count)
    {
        return this.getMutableByteBag().topOccurrences(count);
    }

    @Override
    public MutableList<ByteIntPair> bottomOccurrences(int count)
    {
        return this.getMutableByteBag().bottomOccurrences(count);
    }

    @Override
    public MutableByteBag select(BytePredicate predicate)
    {
        return this.getMutableByteBag().select(predicate);
    }

    @Override
    public MutableByteBag reject(BytePredicate predicate)
    {
        return this.getMutableByteBag().reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return this.getMutableByteBag().collect(function);
    }

    @Override
    public boolean equals(Object otherBag)
    {
        return this.getMutableByteBag().equals(otherBag);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableByteBag().hashCode();
    }

    @Override
    public MutableByteBag asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableByteBag asSynchronized()
    {
        return new SynchronizedByteBag(this);
    }

    @Override
    public ImmutableByteBag toImmutable()
    {
        return ByteBags.immutable.withAll(this);
    }

    /**
     * @since 9.2.
     */
    public MutableByteBag newEmpty()
    {
        return this.getMutableByteBag().newEmpty();
    }
}
