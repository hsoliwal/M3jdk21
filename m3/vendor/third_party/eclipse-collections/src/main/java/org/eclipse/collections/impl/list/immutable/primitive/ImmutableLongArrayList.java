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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableLongList;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseLongIterable;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * ImmutableLongArrayList is the non-modifiable equivalent of {@link LongArrayList}.
 * It wraps a Java long array.
 * This file was automatically generated from template file immutablePrimitiveArrayList.stg.
 *
 * @since 3.2.
 */
final class ImmutableLongArrayList
        implements ImmutableLongList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final long[] items;

    private ImmutableLongArrayList(long[] newElements)
    {
        if (newElements.length <= 1)
        {
            throw new IllegalArgumentException("Use LongLists.immutable.with() to instantiate an optimized collection");
        }
        this.items = newElements;
    }

    public static ImmutableLongArrayList newList(LongIterable iterable)
    {
        return new ImmutableLongArrayList(iterable.toArray());
    }

    public static ImmutableLongArrayList newListWith(long... elements)
    {
        long[] newArray = new long[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableLongArrayList(newArray);
    }

    @Override
    public long get(int index)
    {
        return this.items[index];
    }

    @Override
    public long getFirst()
    {
        return this.items[0];
    }

    @Override
    public long getLast()
    {
        return this.items[this.items.length - 1];
    }

    @Override
    public int indexOf(long value)
    {
        for (int i = 0; i < this.items.length; i++)
        {
            if (this.items[i] == value)
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(long value)
    {
        for (int i = this.items.length - 1; i >= 0; i--)
        {
            if (this.items[i] == value)
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public LongIterator longIterator()
    {
        return new InternalLongIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        for (long item : this.items)
        {
            procedure.value(item);
        }
    }

    @Override
    public void forEachWithIndex(LongIntProcedure procedure)
    {
        for (int i = 0; i < this.items.length; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public int count(LongPredicate predicate)
    {
        int count = 0;
        for (long item : this.items)
        {
            if (predicate.accept(item))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        for (long item : this.items)
        {
            if (predicate.accept(item))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        for (long item : this.items)
        {
            if (!predicate.accept(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ImmutableLongList select(LongPredicate predicate)
    {
        return this.select(predicate, new LongArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableLongCollection> R select(LongPredicate predicate, R target)
    {
        for (long item : this.items)
        {
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public ImmutableLongList reject(LongPredicate predicate)
    {
        return this.reject(predicate, new LongArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableLongCollection> R reject(LongPredicate predicate, R target)
    {
        for (long item : this.items)
        {
            if (!predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        for (long item : this.items)
        {
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(LongToObjectFunction<? extends V> function)
    {
        MutableList<V> list = this.collect(function, Lists.mutable.withInitialCapacity(this.items.length));
        return list.toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(LongToObjectFunction<? extends V> function, R target)
    {
        for (long item : this.items)
        {
            target.add(function.valueOf(item));
        }
        return target;
    }

    @Override
    public long sum()
    {
        long result = 0L;
        for (long item : this.items)
        {
            result += item;
        }
        return result;
    }

    @Override
    public long max()
    {
        long max = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            long value = this.items[i];
            if (max < value)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.max();
    }

    @Override
    public long min()
    {
        long min = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            long value = this.items[i];
            if (value < min)
            {
                min = value;
            }
        }
        return min;
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        return this.min();
    }

    @Override
    public double average()
    {
        return (double) this.sum() / (double) this.size();
    }

    @Override
    public double median()
    {
        long[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            long first = sortedArray[middleIndex];
            long second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public long[] toSortedArray()
    {
        long[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public long dotProduct(LongList list)
    {
        if (this.size() != list.size())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        long sum = 0L;
        for (int i = 0; i < this.size(); i++)
        {
            sum += this.items[i] * list.get(i);
        }
        return sum;
    }

    @Override
    public LazyLongIterable asReversed()
    {
        return ReverseLongIterable.adapt(this);
    }

    @Override
    public MutableLongList toSortedList()
    {
        return LongArrayList.newList(this).sortThis();
    }

    @Override
    public int binarySearch(long value)
    {
        return Arrays.binarySearch(this.items, value);
    }

    @Override
    public long[] toArray()
    {
        long[] newItems = new long[this.items.length];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        return newItems;
    }

    @Override
    public long[] toArray(long[] target)
    {
        if (target.length < this.items.length)
        {
            target = new long[this.items.length];
        }
        System.arraycopy(this.items, 0, target, 0, this.items.length);
        return target;
    }

    @Override
    public boolean contains(long value)
    {
        for (long item : this.items)
        {
            if (item == value)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(long... source)
    {
        for (long value : source)
        {
            if (!this.contains(value))
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
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableLongList toList()
    {
        return LongArrayList.newList(this);
    }

    @Override
    public MutableLongSet toSet()
    {
        return LongHashSet.newSet(this);
    }

    @Override
    public MutableLongBag toBag()
    {
        return LongHashBag.newBag(this);
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
    public ImmutableLongArrayList toReversed()
    {
        return ImmutableLongArrayList.newList(this.asReversed());
    }

    @Override
    public ImmutableLongList newWith(long element)
    {
        long[] newItems = new long[this.items.length + 1];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        newItems[this.items.length] = element;
        return new ImmutableLongArrayList(newItems);
    }

    @Override
    public ImmutableLongList newWithout(long element)
    {
        int index = this.indexOf(element);
        if (index != -1)
        {
            long[] newItems = new long[this.items.length - 1];
            System.arraycopy(this.items, 0, newItems, 0, index);
            System.arraycopy(this.items, index + 1, newItems, index, this.items.length - index - 1);
            return LongLists.immutable.with(newItems);
        }
        return this;
    }

    @Override
    public ImmutableLongList newWithAll(LongIterable elements)
    {
        long[] newItems = new long[this.items.length + elements.size()];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        int index = 0;
        for (LongIterator iterator = elements.longIterator(); iterator.hasNext(); index++)
        {
            newItems[this.items.length + index] = iterator.next();
        }
        return new ImmutableLongArrayList(newItems);
    }

    @Override
    public ImmutableLongList newWithoutAll(LongIterable elements)
    {
        MutableLongList mutableLongList = this.toList();
        mutableLongList.removeAll(elements);
        return mutableLongList.toImmutable();
    }

    @Override
    public int size()
    {
        return this.items.length;
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
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectLongIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i], i);
        }
        return result;
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<LongIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                LongIterator iterator = this.longIterator();
                while (iterator.hasNext())
                {
                    MutableLongList batch = LongLists.mutable.empty();
                    for (int i = 0; i < size && iterator.hasNext(); i++)
                    {
                        batch.add(iterator.next());
                    }
                    result.add(batch.toImmutable());
                }
            }
        }
        return result.toImmutable();
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
        if (this.items.length != list.size())
        {
            return false;
        }
        for (int i = 0; i < this.items.length; i++)
        {
            if (this.items[i] != list.get(i))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hashCode = 1;
        for (long item : this.items)
        {
            hashCode = 31 * hashCode + (int) (item ^ item >>> 32);
        }
        return hashCode;
    }

    @Override
    public String toString()
    {
        return this.makeString("[", ", ", "]");
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
            for (int i = 0; i < this.items.length; i++)
            {
                if (i > 0)
                {
                    appendable.append(separator);
                }
                long value = this.items[i];
                appendable.append(String.valueOf(value));
            }
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
        LongArrayList target = new LongArrayList();
        MutableLongSet seenSoFar = new LongHashSet(this.size());

        for (long each : this.items)
        {
            if (seenSoFar.add(each))
            {
                target.add(each);
            }
        }
        return target.toImmutable();
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
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<LongLongPair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        LongIterator iterator = iterable.longIterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target.toImmutable();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> ImmutableList<LongObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<LongObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target.toImmutable();
    }

    private class InternalLongIterator implements LongIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != ImmutableLongArrayList.this.items.length;
        }

        @Override
        public long next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            long next = ImmutableLongArrayList.this.items[this.currentIndex];
            this.currentIndex++;
            return next;
        }
    }

    @Override
    public Spliterator.OfLong spliterator()
    {
        int characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
        return Spliterators.spliterator(this.items, characteristics);
    }
}
