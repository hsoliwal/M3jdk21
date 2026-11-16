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

// Modified 2026 by Hitesh Soliwal and Contributors: materialize immutable chunk ranges directly.

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableIntBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectIntIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntIntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableIntCollection;
import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.iterator.IntIterator;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableIntList;
import jdk.m3.collections.eclipse.api.list.primitive.IntList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableIntStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.IntIntPair;
import jdk.m3.collections.eclipse.api.tuple.primitive.IntObjectPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.IntHashBag;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntStacks;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyIntIterableAdapter;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseIntIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * ImmutableIntArrayList is the non-modifiable equivalent of {@link IntArrayList}.
 * It wraps a Java int array.
 * This file was automatically generated from template file immutablePrimitiveArrayList.stg.
 *
 * @since 3.2.
 */
final class ImmutableIntArrayList
        implements ImmutableIntList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final int[] items;

    private ImmutableIntArrayList(int[] newElements)
    {
        if (newElements.length <= 1)
        {
            throw new IllegalArgumentException("Use IntLists.immutable.with() to instantiate an optimized collection");
        }
        this.items = newElements;
    }

    public static ImmutableIntArrayList newList(IntIterable iterable)
    {
        return new ImmutableIntArrayList(iterable.toArray());
    }

    public static ImmutableIntArrayList newListWith(int... elements)
    {
        int[] newArray = new int[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableIntArrayList(newArray);
    }

    @Override
    public int get(int index)
    {
        return this.items[index];
    }

    @Override
    public int getFirst()
    {
        return this.items[0];
    }

    @Override
    public int getLast()
    {
        return this.items[this.items.length - 1];
    }

    @Override
    public int indexOf(int value)
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
    public int lastIndexOf(int value)
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
    public IntIterator intIterator()
    {
        return new InternalIntIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        for (int item : this.items)
        {
            procedure.value(item);
        }
    }

    @Override
    public void forEachWithIndex(IntIntProcedure procedure)
    {
        for (int i = 0; i < this.items.length; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public int count(IntPredicate predicate)
    {
        int count = 0;
        for (int item : this.items)
        {
            if (predicate.accept(item))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        for (int item : this.items)
        {
            if (predicate.accept(item))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        for (int item : this.items)
        {
            if (!predicate.accept(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ImmutableIntList select(IntPredicate predicate)
    {
        return this.select(predicate, new IntArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableIntCollection> R select(IntPredicate predicate, R target)
    {
        for (int item : this.items)
        {
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public ImmutableIntList reject(IntPredicate predicate)
    {
        return this.reject(predicate, new IntArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableIntCollection> R reject(IntPredicate predicate, R target)
    {
        for (int item : this.items)
        {
            if (!predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        for (int item : this.items)
        {
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(IntToObjectFunction<? extends V> function)
    {
        MutableList<V> list = this.collect(function, Lists.mutable.withInitialCapacity(this.items.length));
        return list.toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(IntToObjectFunction<? extends V> function, R target)
    {
        for (int item : this.items)
        {
            target.add(function.valueOf(item));
        }
        return target;
    }

    @Override
    public long sum()
    {
        long result = 0L;
        for (int item : this.items)
        {
            result += item;
        }
        return result;
    }

    @Override
    public int max()
    {
        int max = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            int value = this.items[i];
            if (max < value)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return this.max();
    }

    @Override
    public int min()
    {
        int min = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            int value = this.items[i];
            if (value < min)
            {
                min = value;
            }
        }
        return min;
    }

    @Override
    public int minIfEmpty(int defaultValue)
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
        int[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            int first = sortedArray[middleIndex];
            int second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public int[] toSortedArray()
    {
        int[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public long dotProduct(IntList list)
    {
        if (this.size() != list.size())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        long sum = 0L;
        for (int i = 0; i < this.size(); i++)
        {
            sum += (long) this.items[i] * list.get(i);
        }
        return sum;
    }

    @Override
    public LazyIntIterable asReversed()
    {
        return ReverseIntIterable.adapt(this);
    }

    @Override
    public MutableIntList toSortedList()
    {
        return IntArrayList.newList(this).sortThis();
    }

    @Override
    public int binarySearch(int value)
    {
        return Arrays.binarySearch(this.items, value);
    }

    @Override
    public int[] toArray()
    {
        int[] newItems = new int[this.items.length];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        return newItems;
    }

    @Override
    public int[] toArray(int[] target)
    {
        if (target.length < this.items.length)
        {
            target = new int[this.items.length];
        }
        System.arraycopy(this.items, 0, target, 0, this.items.length);
        return target;
    }

    @Override
    public boolean contains(int value)
    {
        for (int item : this.items)
        {
            if (item == value)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(int... source)
    {
        for (int value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        for (IntIterator iterator = source.intIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableIntList toList()
    {
        return IntArrayList.newList(this);
    }

    @Override
    public MutableIntSet toSet()
    {
        return IntHashSet.newSet(this);
    }

    @Override
    public MutableIntBag toBag()
    {
        return IntHashBag.newBag(this);
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
    public ImmutableIntArrayList toReversed()
    {
        return ImmutableIntArrayList.newList(this.asReversed());
    }

    @Override
    public ImmutableIntList newWith(int element)
    {
        int[] newItems = new int[this.items.length + 1];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        newItems[this.items.length] = element;
        return new ImmutableIntArrayList(newItems);
    }

    @Override
    public ImmutableIntList newWithout(int element)
    {
        int index = this.indexOf(element);
        if (index != -1)
        {
            int[] newItems = new int[this.items.length - 1];
            System.arraycopy(this.items, 0, newItems, 0, index);
            System.arraycopy(this.items, index + 1, newItems, index, this.items.length - index - 1);
            return IntLists.immutable.with(newItems);
        }
        return this;
    }

    @Override
    public ImmutableIntList newWithAll(IntIterable elements)
    {
        int[] newItems = new int[this.items.length + elements.size()];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        int index = 0;
        for (IntIterator iterator = elements.intIterator(); iterator.hasNext(); index++)
        {
            newItems[this.items.length + index] = iterator.next();
        }
        return new ImmutableIntArrayList(newItems);
    }

    @Override
    public ImmutableIntList newWithoutAll(IntIterable elements)
    {
        MutableIntList mutableIntList = this.toList();
        mutableIntList.removeAll(elements);
        return mutableIntList.toImmutable();
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
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectIntIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i], i);
        }
        return result;
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        if (this.items.length <= size)
        {
            return Lists.immutable.with(this);
        }
        // Materialize each immutable range once, without a temporary mutable primitive list.
        int chunkCount = 1 + (this.items.length - 1) / size;
        MutableList<IntIterable> result = Lists.mutable.withInitialCapacity(chunkCount);
        for (int offset = 0; offset < this.items.length; )
        {
            int length = Math.min(size, this.items.length - offset);
            if (length == 1)
            {
                result.add(IntLists.immutable.with(this.items[offset]));
            }
            else
            {
                int[] chunk = Arrays.copyOfRange(this.items, offset, offset + length);
                result.add(new ImmutableIntArrayList(chunk));
            }
            offset += length;
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
        if (!(otherList instanceof IntList))
        {
            return false;
        }
        IntList list = (IntList) otherList;
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
        for (int item : this.items)
        {
            hashCode = 31 * hashCode + item;
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
                int value = this.items[i];
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
    public ImmutableIntList distinct()
    {
        MutableIntSet seenSoFar = new IntHashSet(this.size());
        return this.select(seenSoFar::add);
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
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<IntIntPair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        IntIterator iterator = iterable.intIterator();
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
    public <T> ImmutableList<IntObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<IntObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target.toImmutable();
    }

    @Override
    public MutableIntStack toStack()
    {
        return IntStacks.mutable.withAll(this);
    }

    private class InternalIntIterator implements IntIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != ImmutableIntArrayList.this.items.length;
        }

        @Override
        public int next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            int next = ImmutableIntArrayList.this.items[this.currentIndex];
            this.currentIndex++;
            return next;
        }
    }

    @Override
    public Spliterator.OfInt spliterator()
    {
        int characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
        return Spliterators.spliterator(this.items, characteristics);
    }
}
