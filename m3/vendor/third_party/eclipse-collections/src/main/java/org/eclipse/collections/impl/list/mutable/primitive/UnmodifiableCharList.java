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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.block.function.primitive.CharIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;
import org.eclipse.collections.api.tuple.primitive.CharObjectPair;
import org.eclipse.collections.api.list.primitive.CharList;
import org.eclipse.collections.api.list.primitive.ImmutableCharList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractUnmodifiableCharCollection;
import org.eclipse.collections.impl.factory.primitive.CharLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseCharIterable;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveList.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableCharList
        extends AbstractUnmodifiableCharCollection
        implements MutableCharList
{
    private static final long serialVersionUID = 1L;

    public UnmodifiableCharList(MutableCharList list)
    {
        super(list);
    }

    private MutableCharList getMutableCharList()
    {
        return (MutableCharList) this.getCharCollection();
    }

    @Override
    public char get(int index)
    {
        return this.getMutableCharList().get(index);
    }

    @Override
    public char getFirst()
    {
        return this.getMutableCharList().getFirst();
    }

    @Override
    public char getLast()
    {
        return this.getMutableCharList().getLast();
    }

    @Override
    public int indexOf(char value)
    {
        return this.getMutableCharList().indexOf(value);
    }

    @Override
    public int lastIndexOf(char value)
    {
        return this.getMutableCharList().lastIndexOf(value);
    }

    @Override
    public void addAtIndex(int index, char element)
    {
        throw new UnsupportedOperationException("Cannot call addAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, char... source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAllAtIndex(int index, CharIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAllAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public char removeAtIndex(int index)
    {
        throw new UnsupportedOperationException("Cannot call removeAtIndex() on " + this.getClass().getSimpleName());
    }

    @Override
    public char set(int index, char element)
    {
        throw new UnsupportedOperationException("Cannot call set() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharList with(char element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharList without(char element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharList withAll(CharIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public UnmodifiableCharList withoutAll(CharIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharList select(CharPredicate predicate)
    {
        return this.getMutableCharList().select(predicate);
    }

    @Override
    public MutableCharList reject(CharPredicate predicate)
    {
        return this.getMutableCharList().reject(predicate);
    }

    @Override
    public <V> MutableList<V> collect(CharToObjectFunction<? extends V> function)
    {
        return this.getMutableCharList().collect(function);
    }

    @Override
    public MutableCharList sortThis()
    {
        throw new UnsupportedOperationException("Cannot call sortThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharList shuffleThis()
    {
        throw new UnsupportedOperationException("Cannot call shuffleThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public int binarySearch(char value)
    {
        return this.getMutableCharList().binarySearch(value);
    }

    @Override
    public long dotProduct(CharList list)
    {
        return this.getMutableCharList().dotProduct(list);
    }

    @Override
    public boolean equals(Object otherList)
    {
        return this.getMutableCharList().equals(otherList);
    }

    @Override
    public int hashCode()
    {
        return this.getMutableCharList().hashCode();
    }

    @Override
    public MutableCharList asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableCharList asSynchronized()
    {
        return new SynchronizedCharList(this);
    }

    @Override
    public ImmutableCharList toImmutable()
    {
        int size = this.size();
        if (size == 0)
        {
            return CharLists.immutable.with();
        }
        if (size == 1)
        {
            return CharLists.immutable.with(this.getFirst());
        }
        return CharLists.immutable.with(this.toArray());
    }

    /**
     * @since 9.2.
     */
    public MutableCharList newEmpty()
    {
        return this.getMutableCharList().newEmpty();
    }

    @Override
    public MutableCharList reverseThis()
    {
        throw new UnsupportedOperationException("Cannot call reverseThis() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharList toReversed()
    {
        return this.getMutableCharList().toReversed();
    }

    @Override
    public void forEachWithIndex(CharIntProcedure procedure)
    {
        this.getMutableCharList().forEachWithIndex(procedure);
    }

    @Override
    public LazyCharIterable asReversed()
    {
        return ReverseCharIterable.adapt(this);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectCharIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.getMutableCharList().injectIntoWithIndex(injectedValue, function);
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableCharList distinct()
    {
        return this.getMutableCharList().distinct();
    }

    @Override
    public MutableCharList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<CharCharPair> zipChar(CharIterable iterable)
    {
        return this.getMutableCharList().zipChar(iterable);
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<CharObjectPair<T>> zip(Iterable<T> iterable)
    {
        return this.getMutableCharList().zip(iterable);
    }

    /**
     * Returns a new MutableCharList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableCharList selectWithIndex(CharIntPredicate predicate)
    {
        return this.getMutableCharList().selectWithIndex(predicate);
    }

    /**
     * Returns a new MutableCharCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableCharCollection> R selectWithIndex(CharIntPredicate predicate, R target)
    {
        return this.getMutableCharList().selectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableCharList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableCharList rejectWithIndex(CharIntPredicate predicate)
    {
        return this.getMutableCharList().rejectWithIndex(predicate);
    }

    /**
     * Returns a new MutableCharCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableCharCollection> R rejectWithIndex(CharIntPredicate predicate, R target)
    {
        return this.getMutableCharList().rejectWithIndex(predicate, target);
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(CharIntToObjectFunction<? extends V> function)
    {
        return this.getMutableCharList().collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(CharIntToObjectFunction<? extends V> function, R target)
    {
        return this.getMutableCharList().collectWithIndex(function, target);
    }
}
