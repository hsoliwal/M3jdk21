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

import java.util.Collection;
import java.util.Collections;

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableCharSet;
import jdk.m3.collections.eclipse.api.set.primitive.CharSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.CharCharPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.AbstractSynchronizedCharCollection;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyCharIterableAdapter;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharSets;
import jdk.m3.collections.eclipse.impl.set.mutable.SynchronizedMutableSet;

/**
 * A synchronized view of a {@link MutableCharSet}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link CharIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveSet.stg.
 *
 * @see MutableCharSet#asSynchronized()
 * @see MutableSet#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedCharSet
        extends AbstractSynchronizedCharCollection
        implements MutableCharSet
{
    private static final long serialVersionUID = 1L;

    public SynchronizedCharSet(MutableCharSet set)
    {
        super(set);
    }

    public SynchronizedCharSet(MutableCharSet set, Object newLock)
    {
        super(set, newLock);
    }

    /**
     * This method will take a MutableCharSet and wrap it directly in a SynchronizedCharSet.
     */
    public static SynchronizedCharSet of(MutableCharSet set)
    {
        return new SynchronizedCharSet(set);
    }

    /**
     * This method will take a MutableCharSet and wrap it directly in a SynchronizedCharSet.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedCharSet of(MutableCharSet set, Object lock)
    {
        return new SynchronizedCharSet(set, lock);
    }

    private MutableCharSet getMutableCharSet()
    {
        return (MutableCharSet) this.getCharCollection();
    }

    @Override
    public SynchronizedCharSet without(char element)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharSet().remove(element);
        }
        return this;
    }

    @Override
    public MutableSet<Character> boxed()
    {
        synchronized (this.getLock())
        {
            return SynchronizedMutableSet.of(new BoxedMutableCharSet(this), this.getLock());
        }
    }

    @Override
    public SynchronizedCharSet with(char element)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharSet().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedCharSet withAll(CharIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharSet().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedCharSet withoutAll(CharIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharSet().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableCharSet select(CharPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharSet().select(predicate);
        }
    }

    @Override
    public MutableCharSet reject(CharPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharSet().reject(predicate);
        }
    }

    @Override
    public <V> MutableSet<V> collect(CharToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharSet().collect(function);
        }
    }

    @Override
    public LazyIterable<CharCharPair> cartesianProduct(
            CharSet set)
    {
        synchronized (this.getLock())
        {
            return CharSets.cartesianProduct(this, set);
        }
    }

    @Override
    public boolean equals(Object otherSet)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharSet().equals(otherSet);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharSet().hashCode();
        }
    }

    @Override
    public LazyCharIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyCharIterableAdapter(this);
        }
    }

    @Override
    public MutableCharSet asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableCharSet(this);
        }
    }

    @Override
    public MutableCharSet asSynchronized()
    {
        return this;
    }

    public CharSet freeze()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharSet().freeze();
        }
    }

    @Override
    public ImmutableCharSet toImmutable()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharSet().toImmutable();
        }
    }

    /**
     * @since 9.2.
     */
    public MutableCharSet newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharSet().newEmpty();
        }
    }
}
