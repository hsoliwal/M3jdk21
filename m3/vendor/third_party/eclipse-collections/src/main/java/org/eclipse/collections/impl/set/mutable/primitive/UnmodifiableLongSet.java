/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.mutable.primitive;

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableLongCollection;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.impl.factory.primitive.LongSets;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveSet.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableLongSet
        extends AbstractUnmodifiableLongCollection
        implements MutableLongSet
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableLongSet(MutableLongSet set)
    {
        super(set);
    }

    /**
     * This method will take a MutableLongSet and wrap it directly in a UnmodifiableLongSet.
     */
    public static UnmodifiableLongSet of(MutableLongSet set)
    {
        if (set == null)
        {
            throw new IllegalArgumentException("cannot create an UnmodifiableLongSet for null");
        }
        return new UnmodifiableLongSet(set);
    }

    private MutableLongSet getMutableLongSet()
    {
        return (MutableLongSet) this.getLongCollection();
    }

    @Override
    public UnmodifiableLongSet with(long element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongSet without(long element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongSet withAll(LongIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableLongSet withoutAll(LongIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableLongSet select(LongPredicate predicate)
    {
        return this.getMutableLongSet().select(predicate);
    }

    @Override
    public MutableLongSet reject(LongPredicate predicate)
    {
        return this.getMutableLongSet().reject(predicate);
    }

    @Override
    public <V> MutableSet<V> collect(LongToObjectFunction<? extends V> function)
    {
        return this.getMutableLongSet().collect(function);
    }

    @Override
    public LazyIterable<LongLongPair> cartesianProduct(LongSet set)
    {
        return LongSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object otherSet)
    {
        return this.getMutableLongSet().equals(otherSet);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableLongSet().hashCode();
    }

    @Override
    public MutableLongSet asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableLongSet asSynchronized()
    {
        return new SynchronizedLongSet(this);
    }

    public LongSet freeze()
    {
        return this.getMutableLongSet().freeze();
    }

    @Override
    public ImmutableLongSet toImmutable()
    {
        return this.getMutableLongSet().toImmutable();
    }

    /**
     * @since 9.2.
     */
    public MutableLongSet newEmpty()
    {
        return this.getMutableLongSet().newEmpty();
    }
}
