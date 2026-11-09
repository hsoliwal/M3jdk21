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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableCharCollection;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;
import org.eclipse.collections.impl.factory.primitive.CharSets;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveSet.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableCharSet
        extends AbstractUnmodifiableCharCollection
        implements MutableCharSet
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableCharSet(MutableCharSet set)
    {
        super(set);
    }

    /**
     * This method will take a MutableCharSet and wrap it directly in a UnmodifiableCharSet.
     */
    public static UnmodifiableCharSet of(MutableCharSet set)
    {
        if (set == null)
        {
            throw new IllegalArgumentException("cannot create an UnmodifiableCharSet for null");
        }
        return new UnmodifiableCharSet(set);
    }

    private MutableCharSet getMutableCharSet()
    {
        return (MutableCharSet) this.getCharCollection();
    }

    @Override
    public UnmodifiableCharSet with(char element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharSet without(char element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharSet withAll(CharIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharSet withoutAll(CharIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharSet select(CharPredicate predicate)
    {
        return this.getMutableCharSet().select(predicate);
    }

    @Override
    public MutableCharSet reject(CharPredicate predicate)
    {
        return this.getMutableCharSet().reject(predicate);
    }

    @Override
    public <V> MutableSet<V> collect(CharToObjectFunction<? extends V> function)
    {
        return this.getMutableCharSet().collect(function);
    }

    @Override
    public LazyIterable<CharCharPair> cartesianProduct(CharSet set)
    {
        return CharSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object otherSet)
    {
        return this.getMutableCharSet().equals(otherSet);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableCharSet().hashCode();
    }

    @Override
    public MutableCharSet asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableCharSet asSynchronized()
    {
        return new SynchronizedCharSet(this);
    }

    public CharSet freeze()
    {
        return this.getMutableCharSet().freeze();
    }

    @Override
    public ImmutableCharSet toImmutable()
    {
        return this.getMutableCharSet().toImmutable();
    }

    /**
     * @since 9.2.
     */
    public MutableCharSet newEmpty()
    {
        return this.getMutableCharSet().newEmpty();
    }
}
