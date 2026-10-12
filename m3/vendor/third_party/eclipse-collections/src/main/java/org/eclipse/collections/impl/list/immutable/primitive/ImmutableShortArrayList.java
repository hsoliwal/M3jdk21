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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableShortList;
import org.eclipse.collections.api.list.primitive.ShortList;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.tuple.primitive.ShortShortPair;
import org.eclipse.collections.api.tuple.primitive.ShortObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.ShortHashBag;
import org.eclipse.collections.impl.factory.primitive.ShortLists;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseShortIterable;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * ImmutableShortArrayList is the non-modifiable equivalent of {@link ShortArrayList}.
 * It wraps a Java short array.
 * This file was automatically generated from template file immutablePrimitiveArrayList.stg.
 *
 * @since 3.2.
 */
final class ImmutableShortArrayList
        implements ImmutableShortList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final short[] items;

    private ImmutableShortArrayList(short[] newElements)
    {
        if (newElements.length <= 1)
        {
            throw new IllegalArgumentException("Use ShortLists.immutable.with() to instantiate an optimized collection");
        }
        this.items = newElements;
    }

    public static ImmutableShortArrayList newList(ShortIterable iterable)
    {
        return new ImmutableShortArrayList(iterable.toArray());
    }

    public static ImmutableShortArrayList newListWith(short... elements)
    {
        short[] newArray = new short[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableShortArrayList(newArray);
    }

    @Override
    public short get(int index)
    {
        return this.items[index];
    }

    @Override
    public short getFirst()
    {
        return this.items[0];
    }

    @Override
    public short getLast()
    {
        return this.items[this.items.length - 1];
    }

    @Override
    public int indexOf(short value)
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
    public int lastIndexOf(short value)
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
    public ShortIterator shortIterator()
    {
        return new InternalShortIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        for (short item : this.items)
        {
            procedure.value(item);
        }
    }

    @Override
    public void forEachWithIndex(ShortIntProcedure procedure)
    {
        for (int i = 0; i < this.items.length; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        int count = 0;
        for (short item : this.items)
        {
            if (predicate.accept(item))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        for (short item : this.items)
        {
            if (predicate.accept(item))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        for (short item : this.items)
        {
            if (!predicate.accept(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ImmutableShortList select(ShortPredicate predicate)
    {
        return this.select(predicate, new ShortArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableShortCollection> R select(ShortPredicate predicate, R target)
    {
        for (short item : this.items)
        {
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public ImmutableShortList reject(ShortPredicate predicate)
    {
        return this.reject(predicate, new ShortArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableShortCollection> R reject(ShortPredicate predicate, R target)
    {
        for (short item : this.items)
        {
            if (!predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        for (short item : this.items)
        {
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(ShortToObjectFunction<? extends V> function)
    {
        MutableList<V> list = this.collect(function, Lists.mutable.withInitialCapacity(this.items.length));
        return list.toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(ShortToObjectFunction<? extends V> function, R target)
    {
        for (short item : this.items)
        {
            target.add(function.valueOf(item));
        }
        return target;
    }

    @Override
    public long sum()
    {
        long result = 0L;
        for (short item : this.items)
        {
            result += item;
        }
        return result;
    }

    @Override
    public short max()
    {
        short max = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            short value = this.items[i];
            if (max < value)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        return this.max();
    }

    @Override
    public short min()
    {
        short min = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            short value = this.items[i];
            if (value < min)
            {
                min = value;
            }
        }
        return min;
    }

    @Override
    public short minIfEmpty(short defaultValue)
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
        short[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            short first = sortedArray[middleIndex];
            short second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public short[] toSortedArray()
    {
        short[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public long dotProduct(ShortList list)
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
    public LazyShortIterable asReversed()
    {
        return ReverseShortIterable.adapt(this);
    }

    @Override
    public MutableShortList toSortedList()
    {
        return ShortArrayList.newList(this).sortThis();
    }

    @Override
    public int binarySearch(short value)
    {
        return Arrays.binarySearch(this.items, value);
    }

    @Override
    public short[] toArray()
    {
        short[] newItems = new short[this.items.length];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        return newItems;
    }

    @Override
    public short[] toArray(short[] target)
    {
        if (target.length < this.items.length)
        {
            target = new short[this.items.length];
        }
        System.arraycopy(this.items, 0, target, 0, this.items.length);
        return target;
    }

    @Override
    public boolean contains(short value)
    {
        for (short item : this.items)
        {
            if (item == value)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(short... source)
    {
        for (short value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        for (ShortIterator iterator = source.shortIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableShortList toList()
    {
        return ShortArrayList.newList(this);
    }

    @Override
    public MutableShortSet toSet()
    {
        return ShortHashSet.newSet(this);
    }

    @Override
    public MutableShortBag toBag()
    {
        return ShortHashBag.newBag(this);
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public ImmutableShortList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableShortArrayList toReversed()
    {
        return ImmutableShortArrayList.newList(this.asReversed());
    }

    @Override
    public ImmutableShortList newWith(short element)
    {
        short[] newItems = new short[this.items.length + 1];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        newItems[this.items.length] = element;
        return new ImmutableShortArrayList(newItems);
    }

    @Override
    public ImmutableShortList newWithout(short element)
    {
        int index = this.indexOf(element);
        if (index != -1)
        {
            short[] newItems = new short[this.items.length - 1];
            System.arraycopy(this.items, 0, newItems, 0, index);
            System.arraycopy(this.items, index + 1, newItems, index, this.items.length - index - 1);
            return ShortLists.immutable.with(newItems);
        }
        return this;
    }

    @Override
    public ImmutableShortList newWithAll(ShortIterable elements)
    {
        short[] newItems = new short[this.items.length + elements.size()];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        int index = 0;
        for (ShortIterator iterator = elements.shortIterator(); iterator.hasNext(); index++)
        {
            newItems[this.items.length + index] = iterator.next();
        }
        return new ImmutableShortArrayList(newItems);
    }

    @Override
    public ImmutableShortList newWithoutAll(ShortIterable elements)
    {
        MutableShortList mutableShortList = this.toList();
        mutableShortList.removeAll(elements);
        return mutableShortList.toImmutable();
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
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectShortIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i], i);
        }
        return result;
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ShortIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                ShortIterator iterator = this.shortIterator();
                while (iterator.hasNext())
                {
                    MutableShortList batch = ShortLists.mutable.empty();
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
        if (!(otherList instanceof ShortList))
        {
            return false;
        }
        ShortList list = (ShortList) otherList;
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
        for (short item : this.items)
        {
            hashCode = 31 * hashCode + (int) item;
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
                short value = this.items[i];
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
    public ImmutableShortList distinct()
    {
        ShortArrayList target = new ShortArrayList();
        MutableShortSet seenSoFar = new ShortHashSet(this.size());

        for (short each : this.items)
        {
            if (seenSoFar.add(each))
            {
                target.add(each);
            }
        }
        return target.toImmutable();
    }

    @Override
    public ImmutableShortList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<ShortShortPair> zipShort(ShortIterable iterable)
    {
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<ShortShortPair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        ShortIterator iterator = iterable.shortIterator();
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
    public <T> ImmutableList<ShortObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<ShortObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target.toImmutable();
    }

    private class InternalShortIterator implements ShortIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != ImmutableShortArrayList.this.items.length;
        }

        @Override
        public short next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            short next = ImmutableShortArrayList.this.items[this.currentIndex];
            this.currentIndex++;
            return next;
        }
    }
}
