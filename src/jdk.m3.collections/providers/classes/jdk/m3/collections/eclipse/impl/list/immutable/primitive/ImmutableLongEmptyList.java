/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableLongBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectLongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectLongIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongIntProcedure;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableLongList;
import jdk.m3.collections.eclipse.api.list.primitive.LongList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableLongStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongLongPair;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongObjectPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.LongHashBag;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongStacks;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyLongIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyLongIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseLongIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.LongArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
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
    public MutableLongStack toStack()
    {
        return LongStacks.mutable.empty();
    }

    @Override
    public Spliterator.OfLong spliterator()
    {
        return Spliterators.emptyLongSpliterator();
    }
}
