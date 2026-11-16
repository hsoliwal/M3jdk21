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

import jdk.m3.collections.eclipse.api.ByteIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BytePredicate;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableByteSet;
import jdk.m3.collections.eclipse.api.set.primitive.ByteSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableByteSet;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractUnmodifiableByteCollection;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.tuple.primitive.ByteBytePair;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteSets;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveSet.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableByteSet
        extends AbstractUnmodifiableByteCollection
        implements MutableByteSet
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableByteSet(MutableByteSet set)
    {
        super(set);
    }

    /**
     * This method will take a MutableByteSet and wrap it directly in a UnmodifiableByteSet.
     */
    public static UnmodifiableByteSet of(MutableByteSet set)
    {
        if (set == null)
        {
            throw new IllegalArgumentException("cannot create an UnmodifiableByteSet for null");
        }
        return new UnmodifiableByteSet(set);
    }

    private MutableByteSet getMutableByteSet()
    {
        return (MutableByteSet) this.getByteCollection();
    }

    @Override
    public UnmodifiableByteSet with(byte element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableSet<Byte> boxed()
    {
        return new BoxedMutableByteSet(this);
    }

    @Override
    public UnmodifiableByteSet without(byte element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableByteSet withAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableByteSet withoutAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteSet select(BytePredicate predicate)
    {
        return this.getMutableByteSet().select(predicate);
    }

    @Override
    public MutableByteSet reject(BytePredicate predicate)
    {
        return this.getMutableByteSet().reject(predicate);
    }

    @Override
    public <V> MutableSet<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return this.getMutableByteSet().collect(function);
    }

    @Override
    public LazyIterable<ByteBytePair> cartesianProduct(ByteSet set)
    {
        return ByteSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object otherSet)
    {
        return this.getMutableByteSet().equals(otherSet);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableByteSet().hashCode();
    }

    @Override
    public MutableByteSet asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableByteSet asSynchronized()
    {
        return new SynchronizedByteSet(this);
    }

    public ByteSet freeze()
    {
        return this.getMutableByteSet().freeze();
    }

    @Override
    public ImmutableByteSet toImmutable()
    {
        return this.getMutableByteSet().toImmutable();
    }

    /**
     * @since 9.2.
     */
    public MutableByteSet newEmpty()
    {
        return this.getMutableByteSet().newEmpty();
    }
}
