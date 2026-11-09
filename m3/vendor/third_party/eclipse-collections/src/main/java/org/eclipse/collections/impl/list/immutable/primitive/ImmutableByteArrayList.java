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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;
import org.eclipse.collections.api.tuple.primitive.ByteObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseByteIterable;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * ImmutableByteArrayList is the non-modifiable equivalent of {@link ByteArrayList}.
 * It wraps a Java byte array.
 * This file was automatically generated from template file immutablePrimitiveArrayList.stg.
 *
 * @since 3.2.
 */
final class ImmutableByteArrayList
        implements ImmutableByteList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final byte[] items;

    private ImmutableByteArrayList(byte[] newElements)
    {
        if (newElements.length <= 1)
        {
            throw new IllegalArgumentException("Use ByteLists.immutable.with() to instantiate an optimized collection");
        }
        this.items = newElements;
    }

    public static ImmutableByteArrayList newList(ByteIterable iterable)
    {
        return new ImmutableByteArrayList(iterable.toArray());
    }

    public static ImmutableByteArrayList newListWith(byte... elements)
    {
        byte[] newArray = new byte[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableByteArrayList(newArray);
    }

    @Override
    public byte get(int index)
    {
        return this.items[index];
    }

    @Override
    public byte getFirst()
    {
        return this.items[0];
    }

    @Override
    public byte getLast()
    {
        return this.items[this.items.length - 1];
    }

    @Override
    public int indexOf(byte value)
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
    public int lastIndexOf(byte value)
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
    public ByteIterator byteIterator()
    {
        return new InternalByteIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        for (byte item : this.items)
        {
            procedure.value(item);
        }
    }

    @Override
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
        for (int i = 0; i < this.items.length; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public int count(BytePredicate predicate)
    {
        int count = 0;
        for (byte item : this.items)
        {
            if (predicate.accept(item))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        for (byte item : this.items)
        {
            if (predicate.accept(item))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        for (byte item : this.items)
        {
            if (!predicate.accept(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ImmutableByteList select(BytePredicate predicate)
    {
        return this.select(predicate, new ByteArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableByteCollection> R select(BytePredicate predicate, R target)
    {
        for (byte item : this.items)
        {
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public ImmutableByteList reject(BytePredicate predicate)
    {
        return this.reject(predicate, new ByteArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableByteCollection> R reject(BytePredicate predicate, R target)
    {
        for (byte item : this.items)
        {
            if (!predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        for (byte item : this.items)
        {
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(ByteToObjectFunction<? extends V> function)
    {
        MutableList<V> list = this.collect(function, Lists.mutable.withInitialCapacity(this.items.length));
        return list.toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(ByteToObjectFunction<? extends V> function, R target)
    {
        for (byte item : this.items)
        {
            target.add(function.valueOf(item));
        }
        return target;
    }

    @Override
    public long sum()
    {
        long result = 0L;
        for (byte item : this.items)
        {
            result += item;
        }
        return result;
    }

    @Override
    public byte max()
    {
        byte max = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            byte value = this.items[i];
            if (max < value)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.max();
    }

    @Override
    public byte min()
    {
        byte min = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            byte value = this.items[i];
            if (value < min)
            {
                min = value;
            }
        }
        return min;
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
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
        byte[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            byte first = sortedArray[middleIndex];
            byte second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public byte[] toSortedArray()
    {
        byte[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public long dotProduct(ByteList list)
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
    public LazyByteIterable asReversed()
    {
        return ReverseByteIterable.adapt(this);
    }

    @Override
    public MutableByteList toSortedList()
    {
        return ByteArrayList.newList(this).sortThis();
    }

    @Override
    public int binarySearch(byte value)
    {
        return Arrays.binarySearch(this.items, value);
    }

    @Override
    public byte[] toArray()
    {
        byte[] newItems = new byte[this.items.length];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        return newItems;
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        if (target.length < this.items.length)
        {
            target = new byte[this.items.length];
        }
        System.arraycopy(this.items, 0, target, 0, this.items.length);
        return target;
    }

    @Override
    public boolean contains(byte value)
    {
        for (byte item : this.items)
        {
            if (item == value)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(byte... source)
    {
        for (byte value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        for (ByteIterator iterator = source.byteIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableByteList toList()
    {
        return ByteArrayList.newList(this);
    }

    @Override
    public MutableByteSet toSet()
    {
        return ByteHashSet.newSet(this);
    }

    @Override
    public MutableByteBag toBag()
    {
        return ByteHashBag.newBag(this);
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public ImmutableByteList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableByteArrayList toReversed()
    {
        return ImmutableByteArrayList.newList(this.asReversed());
    }

    @Override
    public ImmutableByteList newWith(byte element)
    {
        byte[] newItems = new byte[this.items.length + 1];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        newItems[this.items.length] = element;
        return new ImmutableByteArrayList(newItems);
    }

    @Override
    public ImmutableByteList newWithout(byte element)
    {
        int index = this.indexOf(element);
        if (index != -1)
        {
            byte[] newItems = new byte[this.items.length - 1];
            System.arraycopy(this.items, 0, newItems, 0, index);
            System.arraycopy(this.items, index + 1, newItems, index, this.items.length - index - 1);
            return ByteLists.immutable.with(newItems);
        }
        return this;
    }

    @Override
    public ImmutableByteList newWithAll(ByteIterable elements)
    {
        byte[] newItems = new byte[this.items.length + elements.size()];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        int index = 0;
        for (ByteIterator iterator = elements.byteIterator(); iterator.hasNext(); index++)
        {
            newItems[this.items.length + index] = iterator.next();
        }
        return new ImmutableByteArrayList(newItems);
    }

    @Override
    public ImmutableByteList newWithoutAll(ByteIterable elements)
    {
        MutableByteList mutableByteList = this.toList();
        mutableByteList.removeAll(elements);
        return mutableByteList.toImmutable();
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
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i], i);
        }
        return result;
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ByteIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                ByteIterator iterator = this.byteIterator();
                while (iterator.hasNext())
                {
                    MutableByteList batch = ByteLists.mutable.empty();
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
        if (!(otherList instanceof ByteList))
        {
            return false;
        }
        ByteList list = (ByteList) otherList;
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
        for (byte item : this.items)
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
                byte value = this.items[i];
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
    public ImmutableByteList distinct()
    {
        ByteArrayList target = new ByteArrayList();
        MutableByteSet seenSoFar = new ByteHashSet(this.size());

        for (byte each : this.items)
        {
            if (seenSoFar.add(each))
            {
                target.add(each);
            }
        }
        return target.toImmutable();
    }

    @Override
    public ImmutableByteList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<ByteBytePair> zipByte(ByteIterable iterable)
    {
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<ByteBytePair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        ByteIterator iterator = iterable.byteIterator();
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
    public <T> ImmutableList<ByteObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<ByteObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target.toImmutable();
    }

    private class InternalByteIterator implements ByteIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != ImmutableByteArrayList.this.items.length;
        }

        @Override
        public byte next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            byte next = ImmutableByteArrayList.this.items[this.currentIndex];
            this.currentIndex++;
            return next;
        }
    }
}
