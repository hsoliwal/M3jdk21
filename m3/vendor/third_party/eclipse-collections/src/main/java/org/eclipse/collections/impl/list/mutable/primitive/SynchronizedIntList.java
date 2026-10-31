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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.block.comparator.primitive.IntComparator;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.collection.mutable.primitive.AbstractSynchronizedIntCollection;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseIntIterable;
import java.util.Spliterator;

/**
 * A synchronized view of a {@link MutableIntList}. It is imperative that the user manually synchronize on the on the
 * collection when iterating over it using an iterator or stream.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveList.stg.
 *
 * @see MutableIntList#asSynchronized()
 * @see MutableList#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedIntList
        extends AbstractSynchronizedIntCollection
        implements MutableIntList
{
    private static final long serialVersionUID = 1L;

    public SynchronizedIntList(MutableIntList list)
    {
        super(list);
    }

    public SynchronizedIntList(MutableIntList list, Object newLock)
    {
        super(list, newLock);
    }

    private MutableIntList getMutableIntList()
    {
        return (MutableIntList) this.getIntCollection();
    }

    @Override
    public int get(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().get(index);
        }
    }

    @Override
    public int getFirst()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().getFirst();
        }
    }

    @Override
    public int getLast()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().getLast();
        }
    }

    @Override
    public int indexOf(int value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().indexOf(value);
        }
    }

    @Override
    public int lastIndexOf(int value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().lastIndexOf(value);
        }
    }

    @Override
    public void addAtIndex(int index, int element)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().addAtIndex(index, element);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, int... source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().addAllAtIndex(index, source);
        }
    }

    @Override
    public boolean addAllAtIndex(int index, IntIterable source)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().addAllAtIndex(index, source);
        }
    }

    @Override
    public int removeAtIndex(int index)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().removeAtIndex(index);
        }
    }

    @Override
    public int set(int index, int element)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().set(index, element);
        }
    }

    @Override
    public SynchronizedIntList with(int element)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().add(element);
        }
        return this;
    }

    @Override
    public SynchronizedIntList without(int element)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().remove(element);
        }
        return this;
    }

    @Override
    public SynchronizedIntList withAll(IntIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().addAll(elements.toArray());
        }
        return this;
    }

    @Override
    public SynchronizedIntList withoutAll(IntIterable elements)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableIntList select(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().select(predicate);
        }
    }

    @Override
    public MutableIntList reject(IntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().reject(predicate);
        }
    }

    @Override
    public <V> MutableList<V> collect(IntToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().collect(function);
        }
    }

    @Override
    public MutableIntList sortThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().sortThis();
        }
        return this;
    }

    @Override
    public MutableIntList sortThis(IntComparator comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().sortThis(comparator);
        }
        return this;
    }

    @Override
    public <T> MutableIntList sortThisBy(IntToObjectFunction<T> function)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().sortThisBy(function);
        }
        return this;
    }

    @Override
    public <T> MutableIntList sortThisBy(IntToObjectFunction<T> function, Comparator<? super T> comparator)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().sortThisBy(function, comparator);
        }
        return this;
    }

    @Override
    public MutableIntList shuffleThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().shuffleThis();
        }
        return this;
    }

    @Override
    public MutableIntList shuffleThis(Random rnd)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().shuffleThis(rnd);
        }
        return this;
    }

    @Override
    public int binarySearch(int value)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().binarySearch(value);
        }
    }

    @Override
    public long dotProduct(IntList list)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().dotProduct(list);
        }
    }

    @Override
    public boolean equals(Object otherList)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().equals(otherList);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().hashCode();
        }
    }

    @Override
    public LazyIntIterable asLazy()
    {
        synchronized (this.getLock())
        {
            return new LazyIntIterableAdapter(this);
        }
    }

    @Override
    public MutableIntList asUnmodifiable()
    {
        synchronized (this.getLock())
        {
            return new UnmodifiableIntList(this);
        }
    }

    @Override
    public MutableIntList asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableIntList toImmutable()
    {
        synchronized (this.getLock())
        {
            int size = this.size();
            if (size == 0)
            {
                return IntLists.immutable.with();
            }
            if (size == 1)
            {
                return IntLists.immutable.with(this.getFirst());
            }
            return IntLists.immutable.with(this.toArray());
        }
    }

    /**
     * @since 9.2.
     */
    public MutableIntList newEmpty()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().newEmpty();
        }
    }

    @Override
    public MutableIntList reverseThis()
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().reverseThis();
        }
        return this;
    }

    @Override
    public MutableIntList toReversed()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().toReversed();
        }
    }

    @Override
    public LazyIntIterable asReversed()
    {
        return ReverseIntIterable.adapt(this);
    }

    @Override
    public void forEachInBoth(IntList other, IntIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().forEachInBoth(other, procedure);
        }
    }

    @Override
    public void forEachWithIndex(IntIntProcedure procedure)
    {
        synchronized (this.getLock())
        {
            this.getMutableIntList().forEachWithIndex(procedure);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectIntIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().injectIntoWithIndex(injectedValue, function);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public MutableIntList distinct()
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().distinct();
        }
    }

    @Override
    public MutableIntList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    public MutableList<IntIntPair> zipInt(IntIterable iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().zipInt(iterable);
        }
    }

    /**
     * @since 9.1.
     */
    public <T> MutableList<IntObjectPair<T>> zip(Iterable<T> iterable)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().zip(iterable);
        }
    }

    /**
     * Returns a new MutableIntList including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableIntList selectWithIndex(IntIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableIntCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableIntCollection> R selectWithIndex(IntIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableIntList excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableIntList rejectWithIndex(IntIntPredicate predicate)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableIntCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableIntCollection> R rejectWithIndex(IntIntPredicate predicate, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().rejectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableList using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableList<V> collectWithIndex(IntIntToObjectFunction<? extends V> function)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().collectWithIndex(function);
        }
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(IntIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.getLock())
        {
            return this.getMutableIntList().collectWithIndex(function, target);
        }
    }

    /**
    * This function needs to be synchronized manually
    */
    @Override
    public Spliterator.OfInt spliterator()
    {
        return this.getMutableIntList().spliterator();
    }
}
