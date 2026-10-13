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
import org.eclipse.collections.impl.iterator.SingletonLongIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseLongIterable;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import java.util.Spliterator;
import org.eclipse.collections.impl.stream.primitive.LongSingletonSpliterator;

/**
 * ImmutableLongSingletonList is an optimization for {@link ImmutableLongList} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonList.stg.
 */
final class ImmutableLongSingletonList implements ImmutableLongList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final long element1;

    ImmutableLongSingletonList(long element)
    {
        this.element1 = element;
    }

    @Override
    public long get(int index)
    {
        if (index == 0)
        {
            return this.element1;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
    }

    @Override
    public long getFirst()
    {
        return this.element1;
    }

    @Override
    public long getLast()
    {
        return this.element1;
    }

    @Override
    public int indexOf(long value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public int lastIndexOf(long value)
    {
        return this.element1 == value ? 0 : -1;
    }

    @Override
    public LongIterator longIterator()
    {
        return new SingletonLongIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public void forEachWithIndex(LongIntProcedure procedure)
    {
        procedure.value(this.element1, 0);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public ImmutableLongList select(LongPredicate predicate)
    {
        return predicate.accept(this.element1) ? LongArrayList.newListWith(this.element1).toImmutable()
                : new LongArrayList().toImmutable();
    }

    @Override
    public ImmutableLongList reject(LongPredicate predicate)
    {
        return predicate.accept(this.element1) ? new LongArrayList().toImmutable()
                : LongArrayList.newListWith(this.element1).toImmutable();
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Lists.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public long sum()
    {
        return this.element1;
    }

    @Override
    public long max()
    {
        return this.element1;
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.element1;
    }

    @Override
    public long min()
    {
        return this.element1;
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        return this.element1;
    }

    @Override
    public double average()
    {
        return this.element1;
    }

    @Override
    public double median()
    {
        return this.element1;
    }

    @Override
    public long[] toSortedArray()
    {
        return new long[]{this.element1};
    }

    @Override
    public MutableLongList toSortedList()
    {
        return LongArrayList.newListWith(this.element1);
    }

    @Override
    public int binarySearch(long value)
    {
        if (this.element1 == value)
        {
            return 0;
        }
        if (this.element1 < value)
        {
            return -2;
        }
        return -1;
    }

    @Override
    public long dotProduct(LongList list)
    {
        if (list.size() != 1)
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        return this.element1 * list.getFirst();
    }

    @Override
    public long[] toArray()
    {
        return new long[]{this.element1};
    }

    @Override
    public long[] toArray(long[] target)
    {
        if (target.length < 1)
        {
            target = new long[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(long value)
    {
        return this.element1 == value;
    }

    @Override
    public boolean containsAll(long... source)
    {
        for (long value : source)
        {
            if (this.element1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        for (LongIterator iterator = source.longIterator(); iterator.hasNext(); )
        {
            if (this.element1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public LazyLongIterable asReversed()
    {
        return ReverseLongIterable.adapt(this);
    }

    @Override
    public MutableLongList toList()
    {
        return LongArrayList.newListWith(this.element1);
    }

    @Override
    public MutableLongSet toSet()
    {
        return LongHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableLongBag toBag()
    {
        return LongHashBag.newBagWith(this.element1);
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
    public ImmutableLongSingletonList toReversed()
    {
        return this;
    }

    @Override
    public ImmutableLongList newWith(long element)
    {
        return LongLists.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableLongList newWithout(long element)
    {
        return this.element1 == element ? LongLists.immutable.with() : this;
    }

    @Override
    public ImmutableLongList newWithAll(LongIterable elements)
    {
        LongArrayList arrayList = LongArrayList.newListWith(this.element1);
        arrayList.addAll(elements);
        return arrayList.toImmutable();
    }

    @Override
    public ImmutableLongList newWithoutAll(LongIterable elements)
    {
        return elements.contains(this.element1) ? LongLists.immutable.with() : this;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectLongIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1, 0);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.immutable.with(this);
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
        if (list.size() != 1)
        {
            return false;
        }
        return this.element1 == list.get(0);
    }

    @Override
    public int hashCode()
    {
        return 31 + (int) (this.element1 ^ this.element1 >>> 32);
    }

    @Override
    public String toString()
    {
        return "[" + this.element1 + ']';
    }

    @Override
    public String makeString()
    {
        return this.makeString(", ");
    }

    @Override
    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(String.valueOf(this.element1));
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
        return this;
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
        if (iterable.isEmpty())
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, iterable.longIterator().next()));
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<LongObjectPair<T>> zip(Iterable<T> iterable)
    {
        if (Iterate.isEmpty(iterable))
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.with(PrimitiveTuples.pair(this.element1, Iterate.getFirst(iterable)));
    }

    @Override
    public Spliterator.OfLong spliterator()
    {
        return new LongSingletonSpliterator(this.element1);
    }
}
