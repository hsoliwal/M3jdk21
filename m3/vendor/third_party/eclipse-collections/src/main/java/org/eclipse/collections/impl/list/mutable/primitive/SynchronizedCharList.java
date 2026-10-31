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
import java.util.Comparator;
import java.util.Random;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.block.comparator.primitive.CharComparator;
import org.eclipse.collections.api.block.function.primitive.CharIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharCharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.CharList;
import org.eclipse.collections.api.list.primitive.ImmutableCharList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;
import org.eclipse.collections.api.tuple.primitive.CharObjectPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedCharCollection;
import org.eclipse.collections.impl.factory.primitive.CharLists;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseCharIterable;

/**
 * A synchronized view of a {@link MutableCharList}. It is imperative that the user manually synchronize on the on the
 * collection when iterating over it using an iterator or stream.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveList.stg.
 *
 * @see MutableCharList#asSynchronized()
 * @see MutableList#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedCharList
        extends AbstractSynchronizedCharCollection
        implements MutableCharList
{
    private static final long serialVersionUID = 1L;

    public SynchronizedCharList(MutableCharList list)
    {
        super(list);
    }

    public SynchronizedCharList(MutableCharList list, Object newLock)
    {
        super(list, newLock);
    }

    private MutableCharList getMutableCharList()
    {
        return (MutableCharList) this.getCharCollection();
    }

    @Override
    public char get(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().get(index);
        }
    }

    @Override
    public char getFirst()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().getFirst();
        }
    }

    @Override
    public char getLast()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().getLast();
        }
    }

    @Override
    public int indexOf(char value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().indexOf(value);
        }
    }

    @Override
    public int lastIndexOf(char value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().lastIndexOf(value);
        }
    }

    @Override
    public void addAtIndex(int index, char element)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().addAtIndex(index, element);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, char... source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().addAllAtIndex(index, source);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, CharIterable source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().addAllAtIndex(index, source);
        }
    }

    @Override
    public char removeAtIndex(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().removeAtIndex(index);
        }
    }

    @Override
    public char set(int index, char element)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().set(index, element);
        }
    }

    @Override
    public SynchronizedCharList with(char element)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedCharList without(char element)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedCharList withAll(CharIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedCharList withoutAll(CharIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableCharList select(CharPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().select(predicate);
        }
    }

    @Override
    public MutableCharList reject(CharPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().reject(predicate);
        }
    }

    @Override
    public <V> MutableList<V> collect(CharToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().collect(function);
        }
    }

    @Override
    public MutableCharList sortThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().sortThis();
        }
        return this;
    }

    @Override
    public MutableCharList sortThis(CharComparator comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().sortThis(comparator);
        }
        return this;
    }

    @Override
    public <T> MutableCharList sortThisBy(CharToObjectFunction<T> function)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().sortThisBy(function);
        }
        return this;
    }

    @Override
    public <T> MutableCharList sortThisBy(CharToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().sortThisBy(function, comparator);
        }
        return this;
    }

    @Override
    public MutableCharList shuffleThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().shuffleThis();
        }
        return this;
    }

    @Override
    public MutableCharList shuffleThis(Random rnd)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().shuffleThis(rnd);
        }
        return this;
    }

    @Override
    public int binarySearch(char value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().binarySearch(value);
        }
    }

    @Override
    public long dotProduct(CharList list)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().dotProduct(list);
        }
    }

    @Override
    public boolean equals(Object otherList)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().equals(otherList);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().hashCode();
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
    public MutableCharList asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableCharList(this);
        }
    }

    @Override
    public MutableCharList asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableCharList toImmutable()
    {
        synchronized (this.getLock())
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
    }

    /**
     * @since 9.2.
     */
    public MutableCharList newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().newEmpty();
        }
    }

    @Override
    public MutableCharList reverseThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().reverseThis();
        }
        return this;
    }

    @Override
    public MutableCharList toReversed()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().toReversed();
        }
    }

    @Override
    public LazyCharIterable asReversed()
    {
        return ReverseCharIterable.adapt(this);
    }

    @Override
    public void forEachInBoth(CharList other, CharCharProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().forEachInBoth(other, procedure);
        }
    }

    @Override
    public void forEachWithIndex(CharIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableCharList().forEachWithIndex(procedure);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectCharIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().injectIntoWithIndex(injectedValue, function);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableCharList distinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().distinct();
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableCharList().zipChar(iterable);
        }
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<CharObjectPair<T>> zip(Iterable<T> iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().zip(iterable);
        }
    }

    /**
     * Returns a new MutableCharList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableCharList selectWithIndex(CharIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableCharCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableCharCollection> R selectWithIndex(CharIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableCharList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableCharList rejectWithIndex(CharIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableCharCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableCharCollection> R rejectWithIndex(CharIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().rejectWithIndex(predicate, target);
        }
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
        synchronized (this.getLock())
        {
            return this.getMutableCharList().collectWithIndex(function);
        }
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(CharIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableCharList().collectWithIndex(function, target);
        }
    }
}
