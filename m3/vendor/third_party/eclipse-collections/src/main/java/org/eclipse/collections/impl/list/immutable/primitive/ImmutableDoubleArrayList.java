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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableDoubleList;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.DoubleHashBag;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseDoubleIterable;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * ImmutableDoubleArrayList is the non-modifiable equivalent of {@link DoubleArrayList}.
 * It wraps a Java double array.
 * This file was automatically generated from template file immutablePrimitiveArrayList.stg.
 *
 * @since 3.2.
 */
final class ImmutableDoubleArrayList
        implements ImmutableDoubleList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final double[] items;

    private ImmutableDoubleArrayList(double[] newElements)
    {
        if (newElements.length <= 1)
        {
            throw new IllegalArgumentException("Use DoubleLists.immutable.with() to instantiate an optimized collection");
        }
        this.items = newElements;
    }

    public static ImmutableDoubleArrayList newList(DoubleIterable iterable)
    {
        return new ImmutableDoubleArrayList(iterable.toArray());
    }

    public static ImmutableDoubleArrayList newListWith(double... elements)
    {
        double[] newArray = new double[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableDoubleArrayList(newArray);
    }

    @Override
    public double get(int index)
    {
        return this.items[index];
    }

    @Override
    public double getFirst()
    {
        return this.items[0];
    }

    @Override
    public double getLast()
    {
        return this.items[this.items.length - 1];
    }

    @Override
    public int indexOf(double value)
    {
        for (int i = 0; i < this.items.length; i++)
        {
            if (Double.compare(this.items[i], value) == 0)
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(double value)
    {
        for (int i = this.items.length - 1; i >= 0; i--)
        {
            if (Double.compare(this.items[i], value) == 0)
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new InternalDoubleIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        for (double item : this.items)
        {
            procedure.value(item);
        }
    }

    @Override
    public void forEachWithIndex(DoubleIntProcedure procedure)
    {
        for (int i = 0; i < this.items.length; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        int count = 0;
        for (double item : this.items)
        {
            if (predicate.accept(item))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        for (double item : this.items)
        {
            if (predicate.accept(item))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        for (double item : this.items)
        {
            if (!predicate.accept(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ImmutableDoubleList select(DoublePredicate predicate)
    {
        return this.select(predicate, new DoubleArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableDoubleCollection> R select(DoublePredicate predicate, R target)
    {
        for (double item : this.items)
        {
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public ImmutableDoubleList reject(DoublePredicate predicate)
    {
        return this.reject(predicate, new DoubleArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableDoubleCollection> R reject(DoublePredicate predicate, R target)
    {
        for (double item : this.items)
        {
            if (!predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        for (double item : this.items)
        {
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        MutableList<V> list = this.collect(function, Lists.mutable.withInitialCapacity(this.items.length));
        return list.toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(DoubleToObjectFunction<? extends V> function, R target)
    {
        for (double item : this.items)
        {
            target.add(function.valueOf(item));
        }
        return target;
    }

    @Override
    public double sum()
    {
        double result = 0.0;
        double compensation = 0.0;
        for (double item : this.items)
        {
            double adjustedValue = item - compensation;
            double nextSum = result + adjustedValue;
            compensation = nextSum - result - adjustedValue;
            result = nextSum;
        }
        return result;
    }

    @Override
    public double max()
    {
        double max = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            double value = this.items[i];
            if (Double.compare(max, value) < 0)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        return this.max();
    }

    @Override
    public double min()
    {
        double min = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            double value = this.items[i];
            if (Double.compare(value, min) < 0)
            {
                min = value;
            }
        }
        return min;
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        return this.min();
    }

    @Override
    public double average()
    {
        return this.sum() / (double) this.size();
    }

    @Override
    public double median()
    {
        double[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            double first = sortedArray[middleIndex];
            double second = sortedArray[middleIndex - 1];
            return (first + second) / 2.0;
        }
        return sortedArray[middleIndex];
    }

    @Override
    public double[] toSortedArray()
    {
        double[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public double dotProduct(DoubleList list)
    {
        if (this.size() != list.size())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        double sum = 0.0;
        for (int i = 0; i < this.size(); i++)
        {
            sum += this.items[i] * list.get(i);
        }
        return sum;
    }

    @Override
    public LazyDoubleIterable asReversed()
    {
        return ReverseDoubleIterable.adapt(this);
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return DoubleArrayList.newList(this).sortThis();
    }

    @Override
    public int binarySearch(double value)
    {
        return Arrays.binarySearch(this.items, value);
    }

    @Override
    public double[] toArray()
    {
        double[] newItems = new double[this.items.length];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        return newItems;
    }

    @Override
    public double[] toArray(double[] target)
    {
        if (target.length < this.items.length)
        {
            target = new double[this.items.length];
        }
        System.arraycopy(this.items, 0, target, 0, this.items.length);
        return target;
    }

    @Override
    public boolean contains(double value)
    {
        for (double item : this.items)
        {
            if (Double.compare(item, value) == 0)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(double... source)
    {
        for (double value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        for (DoubleIterator iterator = source.doubleIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableDoubleList toList()
    {
        return DoubleArrayList.newList(this);
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return DoubleHashSet.newSet(this);
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return DoubleHashBag.newBag(this);
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public ImmutableDoubleList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableDoubleArrayList toReversed()
    {
        return ImmutableDoubleArrayList.newList(this.asReversed());
    }

    @Override
    public ImmutableDoubleList newWith(double element)
    {
        double[] newItems = new double[this.items.length + 1];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        newItems[this.items.length] = element;
        return new ImmutableDoubleArrayList(newItems);
    }

    @Override
    public ImmutableDoubleList newWithout(double element)
    {
        int index = this.indexOf(element);
        if (index != -1)
        {
            double[] newItems = new double[this.items.length - 1];
            System.arraycopy(this.items, 0, newItems, 0, index);
            System.arraycopy(this.items, index + 1, newItems, index, this.items.length - index - 1);
            return DoubleLists.immutable.with(newItems);
        }
        return this;
    }

    @Override
    public ImmutableDoubleList newWithAll(DoubleIterable elements)
    {
        double[] newItems = new double[this.items.length + elements.size()];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        int index = 0;
        for (DoubleIterator iterator = elements.doubleIterator(); iterator.hasNext(); index++)
        {
            newItems[this.items.length + index] = iterator.next();
        }
        return new ImmutableDoubleArrayList(newItems);
    }

    @Override
    public ImmutableDoubleList newWithoutAll(DoubleIterable elements)
    {
        MutableDoubleList mutableDoubleList = this.toList();
        mutableDoubleList.removeAll(elements);
        return mutableDoubleList.toImmutable();
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
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i], i);
        }
        return result;
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<DoubleIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                DoubleIterator iterator = this.doubleIterator();
                while (iterator.hasNext())
                {
                    MutableDoubleList batch = DoubleLists.mutable.empty();
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
        if (!(otherList instanceof DoubleList))
        {
            return false;
        }
        DoubleList list = (DoubleList) otherList;
        if (this.items.length != list.size())
        {
            return false;
        }
        for (int i = 0; i < this.items.length; i++)
        {
            if (Double.compare(this.items[i], list.get(i)) != 0)
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
        for (double item : this.items)
        {
            hashCode = 31 * hashCode + (int) (Double.doubleToLongBits(item) ^ Double.doubleToLongBits(item) >>> 32);
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
                double value = this.items[i];
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
    public ImmutableDoubleList distinct()
    {
        DoubleArrayList target = new DoubleArrayList();
        MutableDoubleSet seenSoFar = new DoubleHashSet(this.size());

        for (double each : this.items)
        {
            if (seenSoFar.add(each))
            {
                target.add(each);
            }
        }
        return target.toImmutable();
    }

    @Override
    public ImmutableDoubleList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<DoubleDoublePair> zipDouble(DoubleIterable iterable)
    {
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<DoubleDoublePair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        DoubleIterator iterator = iterable.doubleIterator();
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
    public <T> ImmutableList<DoubleObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<DoubleObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target.toImmutable();
    }

    private class InternalDoubleIterator implements DoubleIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != ImmutableDoubleArrayList.this.items.length;
        }

        @Override
        public double next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            double next = ImmutableDoubleArrayList.this.items[this.currentIndex];
            this.currentIndex++;
            return next;
        }
    }

    @Override
    public Spliterator.OfDouble spliterator()
    {
        int characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
        return Spliterators.spliterator(this.items, characteristics);
    }
}
