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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableDoubleSet;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableDoubleCollection;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.impl.factory.primitive.DoubleSets;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveSet.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableDoubleSet
        extends AbstractUnmodifiableDoubleCollection
        implements MutableDoubleSet
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableDoubleSet(MutableDoubleSet set)
    {
        super(set);
    }

    /**
     * This method will take a MutableDoubleSet and wrap it directly in a UnmodifiableDoubleSet.
     */
    public static UnmodifiableDoubleSet of(MutableDoubleSet set)
    {
        if (set == null)
        {
            throw new IllegalArgumentException("cannot create an UnmodifiableDoubleSet for null");
        }
        return new UnmodifiableDoubleSet(set);
    }

    private MutableDoubleSet getMutableDoubleSet()
    {
        return (MutableDoubleSet) this.getDoubleCollection();
    }

    @Override
    public UnmodifiableDoubleSet with(double element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleSet without(double element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleSet withAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableDoubleSet withoutAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleSet select(DoublePredicate predicate)
    {
        return this.getMutableDoubleSet().select(predicate);
    }

    @Override
    public MutableDoubleSet reject(DoublePredicate predicate)
    {
        return this.getMutableDoubleSet().reject(predicate);
    }

    @Override
    public <V> MutableSet<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return this.getMutableDoubleSet().collect(function);
    }

    @Override
    public LazyIterable<DoubleDoublePair> cartesianProduct(DoubleSet set)
    {
        return DoubleSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object otherSet)
    {
        return this.getMutableDoubleSet().equals(otherSet);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableDoubleSet().hashCode();
    }

    @Override
    public MutableDoubleSet asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableDoubleSet asSynchronized()
    {
        return new SynchronizedDoubleSet(this);
    }

    public DoubleSet freeze()
    {
        return this.getMutableDoubleSet().freeze();
    }

    @Override
    public ImmutableDoubleSet toImmutable()
    {
        return this.getMutableDoubleSet().toImmutable();
    }

    /**
     * @since 9.2.
     */
    public MutableDoubleSet newEmpty()
    {
        return this.getMutableDoubleSet().newEmpty();
    }
}
