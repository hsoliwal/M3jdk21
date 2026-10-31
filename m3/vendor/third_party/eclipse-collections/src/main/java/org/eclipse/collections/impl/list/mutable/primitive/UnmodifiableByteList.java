/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.mutable.primitive;

import java.util.Collection;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.block.function.primitive.ByteIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ByteIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;
import org.eclipse.collections.api.tuple.primitive.ByteObjectPair;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableByteCollection;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseByteIterable;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveList.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableByteList
        extends AbstractUnmodifiableByteCollection
        implements MutableByteList
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableByteList(MutableByteList list)
    {
        super(list);
    }

    private MutableByteList getMutableByteList()
    {
        return (MutableByteList) this.getByteCollection();
    }

    @Override
    public byte get(int index)
    {
        return this.getMutableByteList().get(index);
    }

    @Override
    public byte getFirst()
    {
        return this.getMutableByteList().getFirst();
    }

    @Override
    public byte getLast()
    {
        return this.getMutableByteList().getLast();
    }

    @Override
    public int indexOf(byte value)
    {
        return this.getMutableByteList().indexOf(value);
    }

    @Override
    public int lastIndexOf(byte value)
    {
        return this.getMutableByteList().lastIndexOf(value);
    }

    @Override
    public void addAtIndex(int index, byte element)
    {
        throw new UnsupportedOperationException("Cannot call addAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, byte... source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, ByteIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte removeAtIndex(int index)
    {
        throw new UnsupportedOperationException("Cannot call removeAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public byte set(int index, byte element)
    {
        throw new UnsupportedOperationException("Cannot call set() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableByteList with(byte element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableByteList without(byte element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableByteList withAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableByteList withoutAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteList select(BytePredicate predicate)
    {
        return this.getMutableByteList().select(predicate);
    }

    @Override
    public MutableByteList reject(BytePredicate predicate)
    {
        return this.getMutableByteList().reject(predicate);
    }

    @Override
    public <V> MutableList<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return this.getMutableByteList().collect(function);
    }

    @Override
    public MutableByteList sortThis()
    {
        throw new UnsupportedOperationException("Cannot call sortThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteList shuffleThis()
    {
        throw new UnsupportedOperationException("Cannot call shuffleThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public int binarySearch(byte value)
    {
        return this.getMutableByteList().binarySearch(value);
    }

    @Override
    public long dotProduct(ByteList list)
    {
        return this.getMutableByteList().dotProduct(list);
    }

    @Override
    public boolean equals(Object otherList)
    {
        return this.getMutableByteList().equals(otherList);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableByteList().hashCode();
    }

    @Override
    public MutableByteList asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableByteList asSynchronized()
    {
        return new SynchronizedByteList(this);
    }

    @Override
    public ImmutableByteList toImmutable()
    {
        int size = this.size();
        if (size == 0)
        {
            return ByteLists.immutable.with();
        }
        if (size == 1)
        {
            return ByteLists.immutable.with(this.getFirst());
        }
        return ByteLists.immutable.with(this.toArray());
    }

    /**
     * @since 9.2.
     */
    public MutableByteList newEmpty()
    {
        return this.getMutableByteList().newEmpty();
    }

    @Override
    public MutableByteList reverseThis()
    {
        throw new UnsupportedOperationException("Cannot call reverseThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteList toReversed()
    {
        return this.getMutableByteList().toReversed();
    }

    @Override
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
        this.getMutableByteList().forEachWithIndex(procedure);
    }

    @Override
    public LazyByteIterable asReversed()
    {
        return ReverseByteIterable.adapt(this);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.getMutableByteList().injectIntoWithIndex(injectedValue, function);
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableByteList distinct()
    {
        return this.getMutableByteList().distinct();
    }

    @Override
    public MutableByteList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<ByteBytePair> zipByte(ByteIterable iterable)
    {
        return this.getMutableByteList().zipByte(iterable);
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<ByteObjectPair<T>> zip(Iterable<T> iterable)
    {
        return this.getMutableByteList().zip(iterable);
    }

    /**
     * Returns a new MutableByteList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableByteList selectWithIndex(ByteIntPredicate predicate)
    {
        return this.getMutableByteList().selectWithIndex(predicate);
    }

    /**
     * Returns a new MutableByteCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableByteCollection> R selectWithIndex(ByteIntPredicate predicate, R target)
    {
        return this.getMutableByteList().selectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableByteList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableByteList rejectWithIndex(ByteIntPredicate predicate)
    {
        return this.getMutableByteList().rejectWithIndex(predicate);
    }

    /**
     * Returns a new MutableByteCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableByteCollection> R rejectWithIndex(ByteIntPredicate predicate, R target)
    {
        return this.getMutableByteList().rejectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(ByteIntToObjectFunction<? extends V> function)
    {
        return this.getMutableByteList().collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(ByteIntToObjectFunction<? extends V> function, R target)
    {
        return this.getMutableByteList().collectWithIndex(function, target);
    }
}
