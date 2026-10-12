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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableShortSet;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableShortCollection;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.ShortShortPair;
import org.eclipse.collections.impl.factory.primitive.ShortSets;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveSet.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableShortSet
        extends AbstractUnmodifiableShortCollection
        implements MutableShortSet
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableShortSet(MutableShortSet set)
    {
        super(set);
    }

    /**
     * This method will take a MutableShortSet and wrap it directly in a UnmodifiableShortSet.
     */
    public static UnmodifiableShortSet of(MutableShortSet set)
    {
        if (set == null)
        {
            throw new IllegalArgumentException("cannot create an UnmodifiableShortSet for null");
        }
        return new UnmodifiableShortSet(set);
    }

    private MutableShortSet getMutableShortSet()
    {
        return (MutableShortSet) this.getShortCollection();
    }

    @Override
    public UnmodifiableShortSet with(short element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortSet without(short element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortSet withAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableShortSet withoutAll(ShortIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortSet select(ShortPredicate predicate)
    {
        return this.getMutableShortSet().select(predicate);
    }

    @Override
    public MutableShortSet reject(ShortPredicate predicate)
    {
        return this.getMutableShortSet().reject(predicate);
    }

    @Override
    public <V> MutableSet<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return this.getMutableShortSet().collect(function);
    }

    @Override
    public LazyIterable<ShortShortPair> cartesianProduct(ShortSet set)
    {
        return ShortSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object otherSet)
    {
        return this.getMutableShortSet().equals(otherSet);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableShortSet().hashCode();
    }

    @Override
    public MutableShortSet asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableShortSet asSynchronized()
    {
        return new SynchronizedShortSet(this);
    }

    public ShortSet freeze()
    {
        return this.getMutableShortSet().freeze();
    }

    @Override
    public ImmutableShortSet toImmutable()
    {
        return this.getMutableShortSet().toImmutable();
    }

    /**
     * @since 9.2.
     */
    public MutableShortSet newEmpty()
    {
        return this.getMutableShortSet().newEmpty();
    }
}
