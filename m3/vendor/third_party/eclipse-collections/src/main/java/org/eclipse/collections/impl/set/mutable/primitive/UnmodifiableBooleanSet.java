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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableBooleanSet;
import org.eclipse.collections.api.set.primitive.BooleanSet;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableBooleanCollection;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.BooleanBooleanPair;
import org.eclipse.collections.impl.factory.primitive.BooleanSets;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveSet.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableBooleanSet
        extends AbstractUnmodifiableBooleanCollection
        implements MutableBooleanSet
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableBooleanSet(MutableBooleanSet set)
    {
        super(set);
    }

    /**
     * This method will take a MutableBooleanSet and wrap it directly in a UnmodifiableBooleanSet.
     */
    public static UnmodifiableBooleanSet of(MutableBooleanSet set)
    {
        if (set == null)
        {
            throw new IllegalArgumentException("cannot create an UnmodifiableBooleanSet for null");
        }
        return new UnmodifiableBooleanSet(set);
    }

    private MutableBooleanSet getMutableBooleanSet()
    {
        return (MutableBooleanSet) this.getBooleanCollection();
    }

    @Override
    public UnmodifiableBooleanSet with(boolean element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanSet without(boolean element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanSet withAll(BooleanIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableBooleanSet withoutAll(BooleanIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableBooleanSet select(BooleanPredicate predicate)
    {
        return this.getMutableBooleanSet().select(predicate);
    }

    @Override
    public MutableBooleanSet reject(BooleanPredicate predicate)
    {
        return this.getMutableBooleanSet().reject(predicate);
    }

    @Override
    public <V> MutableSet<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return this.getMutableBooleanSet().collect(function);
    }

    @Override
    public LazyIterable<BooleanBooleanPair> cartesianProduct(BooleanSet set)
    {
        return BooleanSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object otherSet)
    {
        return this.getMutableBooleanSet().equals(otherSet);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableBooleanSet().hashCode();
    }

    @Override
    public MutableBooleanSet asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableBooleanSet asSynchronized()
    {
        return new SynchronizedBooleanSet(this);
    }

    public BooleanSet freeze()
    {
        return this.getMutableBooleanSet().freeze();
    }

    @Override
    public ImmutableBooleanSet toImmutable()
    {
        return this.getMutableBooleanSet().toImmutable();
    }

    /**
     * @since 9.2.
     */
    public MutableBooleanSet newEmpty()
    {
        return this.getMutableBooleanSet().newEmpty();
    }
}
