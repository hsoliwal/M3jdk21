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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongIntProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableLongList;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.iterator.ImmutableEmptyLongIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseLongIterable;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * ImmutableLongEmptyList is an optimization for {@link ImmutableLongList} of size 0.
 * This file was automatically generated from template file immutablePrimitiveEmptyList.stg.
 */
final class ImmutableLongEmptyList implements ImmutableLongList, Serializable
{
    static final ImmutableLongList INSTANCE = new ImmutableLongEmptyList();
    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public long get(int index)
    {
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }

    @Override
    public long getFirst()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public long getLast()
    {
        throw new IndexOutOfBoundsException("Index: 0, Size: 0");
    }

    @Override
    public int indexOf(long value)
    {
        return -1;
    }

    @Override
    public int lastIndexOf(long value)
    {
        return -1;
    }

    @Override
    public LongIterator longIterator()
    {
        return ImmutableEmptyLongIterator.INSTANCE;
    }

    @Override
    public void forEach(LongProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
    }

    @Override
    public void forEachWithIndex(LongIntProcedure procedure)
    {
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableLongList select(LongPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableLongList reject(LongPredicate predicate)
    {
        return this;
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of();
    }

    @Override
    public long sum()
    {
        return 0;
    }

    @Override
    public long max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return defaultValue;
    }

    @Override
    public long min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public long minIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return new long[0];
    }

    @Override
    public MutableLongList toSortedList()
    {
        return new LongArrayList();
    }

    @Override
    public int binarySearch(long value)
    {
        return -1;
    }

    @Override
    public long dotProduct(LongList list)
    {
        if (!list.isEmpty())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return 0;
    }

    @Override
    public long[] toArray()
    {
        return new long[0];
    }

    @Override
    public long[] toArray(long[] target)
    {
        return target;
    }

    @Override
    public boolean contains(long value)
    {
        return false;
    }

    @Override
    public boolean containsAll(long... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public LazyLongIterable asReversed()
    {
        return ReverseLongIterable.adapt(this);
    }

    @Override
    public MutableLongList toList()
    {
        return new LongArrayList();
    }

    @Override
    public MutableLongSet toSet()
    {
        return new LongHashSet();
    }

    @Override
    public MutableLongBag toBag()
    {
        return new LongHashBag();
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
    }

    @Override
    public ImmutableLongList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableLongEmptyList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableLongList newWith(long element)
    {
        return LongLists.immutable.with(element);
    }

    @Override
    public ImmutableLongList newWithout(long element)
    {
        return this;
    }

    @Override
    public ImmutableLongList newWithAll(LongIterable elements)
    {
        return LongLists.immutable.withAll(elements);
    }

    @Override
    public ImmutableLongList newWithoutAll(LongIterable elements)
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
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectLongIntToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
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
        if (!(otherList instanceof LongList))
        {
            return false;
        }
        LongList list = (LongList) otherList;
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
    public ImmutableLongList distinct()
    {
        return INSTANCE;
    }

    @Override
    public ImmutableLongList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<LongLongPair> zipLong(LongIterable iterable)
    {
        return Lists.immutable.empty();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<LongObjectPair<T>> zip(Iterable<T> iterable)
    {
        return Lists.immutable.empty();
    }

    @Override
    public Spliterator.OfLong spliterator()
    {
        return Spliterators.emptyLongSpliterator();
    }
}
