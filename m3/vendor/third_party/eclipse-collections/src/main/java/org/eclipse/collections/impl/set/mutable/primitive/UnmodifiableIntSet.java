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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableIntCollection;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.primitive.IntSets;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveSet.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableIntSet
        extends AbstractUnmodifiableIntCollection
        implements MutableIntSet
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableIntSet(MutableIntSet set)
    {
        super(set);
    }

    /**
     * This method will take a MutableIntSet and wrap it directly in a UnmodifiableIntSet.
     */
    public static UnmodifiableIntSet of(MutableIntSet set)
    {
        if (set == null)
        {
            throw new IllegalArgumentException("cannot create an UnmodifiableIntSet for null");
        }
        return new UnmodifiableIntSet(set);
    }

    private MutableIntSet getMutableIntSet()
    {
        return (MutableIntSet) this.getIntCollection();
    }

    @Override
    public UnmodifiableIntSet with(int element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntSet without(int element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntSet withAll(IntIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableIntSet withoutAll(IntIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableIntSet select(IntPredicate predicate)
    {
        return this.getMutableIntSet().select(predicate);
    }

    @Override
    public MutableIntSet reject(IntPredicate predicate)
    {
        return this.getMutableIntSet().reject(predicate);
    }

    @Override
    public <V> MutableSet<V> collect(IntToObjectFunction<? extends V> function)
    {
        return this.getMutableIntSet().collect(function);
    }

    @Override
    public LazyIterable<IntIntPair> cartesianProduct(IntSet set)
    {
        return IntSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object otherSet)
    {
        return this.getMutableIntSet().equals(otherSet);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableIntSet().hashCode();
    }

    @Override
    public MutableIntSet asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableIntSet asSynchronized()
    {
        return new SynchronizedIntSet(this);
    }

    public IntSet freeze()
    {
        return this.getMutableIntSet().freeze();
    }

    @Override
    public ImmutableIntSet toImmutable()
    {
        return this.getMutableIntSet().toImmutable();
    }

    /**
     * @since 9.2.
     */
    public MutableIntSet newEmpty()
    {
        return this.getMutableIntSet().newEmpty();
    }
}
