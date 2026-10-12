/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntIntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.IntHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.iterator.ImmutableEmptyIntIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseIntIterable;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * ImmutableIntEmptyList is an optimization for {@link ImmutableIntList} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyList.stg.
 */
final class ImmutableIntEmptyList implements ImmutableIntList, Serializable
{
    static final ImmutableIntList INSTANCE = new ImmutableIntEmptyList();
    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public int get(int index)
    {
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }

    @Override
    public int getFirst()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public int getLast()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public int indexOf(int value)
    {
        return -1;
    }

    @Override
    public int lastIndexOf(int value)
    {
        return -1;
    }

    @Override
    public IntIterator intIterator()
    {
        return ImmutableEmptyIntIterator.INSTANCE;
    }

    @Override
    public void forEach(IntProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
    }

    @Override
    public void forEachWithIndex(IntIntProcedure procedure)
    {
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableIntList select(IntPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableIntList reject(IntPredicate predicate)
    {
        return this;
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(IntToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of();
    }

    @Override
    public long sum()
    {
        return 0;
    }

    @Override
    public int max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return defaultValue;
    }

    @Override
    public int min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public int minIfEmpty(int defaultValue)
    {
        return defaultValue;
    }

    @Override
    public double average()
    {
        throw new ArithmeticException();
    }

    @Override
    public double median()
    {
        throw new ArithmeticException();
    }

    @Override
    public int[] toSortedArray()
    {
        return new int[0];
    }

    @Override
    public MutableIntList toSortedList()
    {
        return new IntArrayList();
    }

    @Override
    public int binarySearch(int value)
    {
        return -1;
    }

    @Override
    public long dotProduct(IntList list)
    {
        if (!list.isEmpty())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return 0;
    }

    @Override
    public int[] toArray()
    {
        return new int[0];
    }

    @Override
    public int[] toArray(int[] target)
    {
        return target;
    }

    @Override
    public boolean contains(int value)
    {
        return false;
    }

    @Override
    public boolean containsAll(int... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public LazyIntIterable asReversed()
    {
        return ReverseIntIterable.adapt(this);
    }

    @Override
    public MutableIntList toList()
    {
        return new IntArrayList();
    }

    @Override
    public MutableIntSet toSet()
    {
        return new IntHashSet();
    }

    @Override
    public MutableIntBag toBag()
    {
        return new IntHashBag();
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
    }

    @Override
    public ImmutableIntList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableIntEmptyList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableIntList newWith(int element)
    {
        return IntLists.immutable.with(element);
    }

    @Override
    public ImmutableIntList newWithout(int element)
    {
        return this;
    }

    @Override
    public ImmutableIntList newWithAll(IntIterable elements)
    {
        return IntLists.immutable.withAll(elements);
    }

    @Override
    public ImmutableIntList newWithoutAll(IntIterable elements)
    {
        return this;
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean notEmpty()
    {
        return false;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectIntIntToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public boolean equals(Object otherList)
    {
        if (otherList == this)
        {
            return true;
        }
        if (!(otherList instanceof IntList))
        {
            return false;
        }
        IntList list = (IntList) otherList;
        return list.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 1;
    }

    @Override
    public String toString()
    {
        return "[]";
    }

    @Override
    public String makeString()
    {
        return "";
    }

    @Override
    public String makeString(String separator)
    {
        return "";
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return start + end;
    }

    @Override
    public void appendString(Appendable appendable)
    {
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * @since 6.0.
     */
    @Override
    public ImmutableIntList distinct()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableIntList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<IntIntPair> zipInt(IntIterable iterable)
    {
        return Lists.immutable.empty();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<IntObjectPair<T>> zip(Iterable<T> iterable)
    {
        return Lists.immutable.empty();
    }

    @Override
    public Spliterator.OfInt spliterator()
    {
        return Spliterators.emptyIntSpliterator();
    }
}
