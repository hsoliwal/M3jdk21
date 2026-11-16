/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.mutable.primitive;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableLongSet;
import jdk.m3.collections.eclipse.api.set.primitive.LongSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractUnmodifiableLongCollection;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongLongPair;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongSets;

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
    public MutableSet<Long> boxed()
    {
        return new BoxedMutableLongSet(this);
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
