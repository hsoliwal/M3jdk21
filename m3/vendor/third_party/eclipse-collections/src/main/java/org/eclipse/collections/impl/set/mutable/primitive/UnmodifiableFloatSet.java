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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableFloatCollection;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.impl.factory.primitive.FloatSets;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveSet.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableFloatSet
        extends AbstractUnmodifiableFloatCollection
        implements MutableFloatSet
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableFloatSet(MutableFloatSet set)
    {
        super(set);
    }

    /**
     * This method will take a MutableFloatSet and wrap it directly in a UnmodifiableFloatSet.
     */
    public static UnmodifiableFloatSet of(MutableFloatSet set)
    {
        if (set == null)
        {
            throw new IllegalArgumentException("cannot create an UnmodifiableFloatSet for null");
        }
        return new UnmodifiableFloatSet(set);
    }

    private MutableFloatSet getMutableFloatSet()
    {
        return (MutableFloatSet) this.getFloatCollection();
    }

    @Override
    public UnmodifiableFloatSet with(float element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatSet without(float element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatSet withAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableFloatSet withoutAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatSet select(FloatPredicate predicate)
    {
        return this.getMutableFloatSet().select(predicate);
    }

    @Override
    public MutableFloatSet reject(FloatPredicate predicate)
    {
        return this.getMutableFloatSet().reject(predicate);
    }

    @Override
    public <V> MutableSet<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return this.getMutableFloatSet().collect(function);
    }

    @Override
    public LazyIterable<FloatFloatPair> cartesianProduct(FloatSet set)
    {
        return FloatSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object otherSet)
    {
        return this.getMutableFloatSet().equals(otherSet);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableFloatSet().hashCode();
    }

    @Override
    public MutableFloatSet asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableFloatSet asSynchronized()
    {
        return new SynchronizedFloatSet(this);
    }

    public FloatSet freeze()
    {
        return this.getMutableFloatSet().freeze();
    }

    @Override
    public ImmutableFloatSet toImmutable()
    {
        return this.getMutableFloatSet().toImmutable();
    }

    /**
     * @since 9.2.
     */
    public MutableFloatSet newEmpty()
    {
        return this.getMutableFloatSet().newEmpty();
    }
}
