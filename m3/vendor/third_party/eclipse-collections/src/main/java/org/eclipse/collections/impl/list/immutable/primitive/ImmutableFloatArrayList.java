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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableFloatList;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.api.tuple.primitive.FloatObjectPair;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.factory.primitive.FloatLists;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.impl.lazy.primitive.ReverseFloatIterable;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * ImmutableFloatArrayList is the non-modifiable equivalent of {@link FloatArrayList}.
 * It wraps a Java float array.
 * This file was automatically generated from template file immutablePrimitiveArrayList.stg.
 *
 * @since 3.2.
 */
final class ImmutableFloatArrayList
        implements ImmutableFloatList, Serializable
{
    private static final long serialVersionUID = 1L;
    private final float[] items;

    private ImmutableFloatArrayList(float[] newElements)
    {
        if (newElements.length <= 1)
        {
            throw new IllegalArgumentException("Use FloatLists.immutable.with() to instantiate an optimized collection");
        }
        this.items = newElements;
    }

    public static ImmutableFloatArrayList newList(FloatIterable iterable)
    {
        return new ImmutableFloatArrayList(iterable.toArray());
    }

    public static ImmutableFloatArrayList newListWith(float... elements)
    {
        float[] newArray = new float[elements.length];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        return new ImmutableFloatArrayList(newArray);
    }

    @Override
    public float get(int index)
    {
        return this.items[index];
    }

    @Override
    public float getFirst()
    {
        return this.items[0];
    }

    @Override
    public float getLast()
    {
        return this.items[this.items.length - 1];
    }

    @Override
    public int indexOf(float value)
    {
        for (int i = 0; i < this.items.length; i++)
        {
            if (Float.compare(this.items[i], value) == 0)
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(float value)
    {
        for (int i = this.items.length - 1; i >= 0; i--)
        {
            if (Float.compare(this.items[i], value) == 0)
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new InternalFloatIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        for (float item : this.items)
        {
            procedure.value(item);
        }
    }

    @Override
    public void forEachWithIndex(FloatIntProcedure procedure)
    {
        for (int i = 0; i < this.items.length; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        int count = 0;
        for (float item : this.items)
        {
            if (predicate.accept(item))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        for (float item : this.items)
        {
            if (predicate.accept(item))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        for (float item : this.items)
        {
            if (!predicate.accept(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ImmutableFloatList select(FloatPredicate predicate)
    {
        return this.select(predicate, new FloatArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableFloatCollection> R select(FloatPredicate predicate, R target)
    {
        for (float item : this.items)
        {
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public ImmutableFloatList reject(FloatPredicate predicate)
    {
        return this.reject(predicate, new FloatArrayList()).toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableFloatCollection> R reject(FloatPredicate predicate, R target)
    {
        for (float item : this.items)
        {
            if (!predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        for (float item : this.items)
        {
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> ImmutableList<V> collect(FloatToObjectFunction<? extends V> function)
    {
        MutableList<V> list = this.collect(function, Lists.mutable.withInitialCapacity(this.items.length));
        return list.toImmutable();
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(FloatToObjectFunction<? extends V> function, R target)
    {
        for (float item : this.items)
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
        for (float item : this.items)
        {
            double adjustedValue = item - compensation;
            double nextSum = result + adjustedValue;
            compensation = nextSum - result - adjustedValue;
            result = nextSum;
        }
        return result;
    }

    @Override
    public float max()
    {
        float max = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            float value = this.items[i];
            if (Float.compare(max, value) < 0)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.max();
    }

    @Override
    public float min()
    {
        float min = this.items[0];
        for (int i = 1; i < this.items.length; i++)
        {
            float value = this.items[i];
            if (Float.compare(value, min) < 0)
            {
                min = value;
            }
        }
        return min;
    }

    @Override
    public float minIfEmpty(float defaultValue)
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
        float[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            float first = sortedArray[middleIndex];
            float second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public float[] toSortedArray()
    {
        float[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public double dotProduct(FloatList list)
    {
        if (this.size() != list.size())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        double sum = 0.0;
        for (int i = 0; i < this.size(); i++)
        {
            sum += (double) this.items[i] * list.get(i);
        }
        return sum;
    }

    @Override
    public LazyFloatIterable asReversed()
    {
        return ReverseFloatIterable.adapt(this);
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return FloatArrayList.newList(this).sortThis();
    }

    @Override
    public int binarySearch(float value)
    {
        return Arrays.binarySearch(this.items, value);
    }

    @Override
    public float[] toArray()
    {
        float[] newItems = new float[this.items.length];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        return newItems;
    }

    @Override
    public float[] toArray(float[] target)
    {
        if (target.length < this.items.length)
        {
            target = new float[this.items.length];
        }
        System.arraycopy(this.items, 0, target, 0, this.items.length);
        return target;
    }

    @Override
    public boolean contains(float value)
    {
        for (float item : this.items)
        {
            if (Float.compare(item, value) == 0)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float value : source)
        {
            if (!this.contains(value))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        for (FloatIterator iterator = source.floatIterator(); iterator.hasNext(); )
        {
            if (!this.contains(iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatArrayList.newList(this);
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatHashSet.newSet(this);
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatHashBag.newBag(this);
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public ImmutableFloatList toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableFloatArrayList toReversed()
    {
        return ImmutableFloatArrayList.newList(this.asReversed());
    }

    @Override
    public ImmutableFloatList newWith(float element)
    {
        float[] newItems = new float[this.items.length + 1];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        newItems[this.items.length] = element;
        return new ImmutableFloatArrayList(newItems);
    }

    @Override
    public ImmutableFloatList newWithout(float element)
    {
        int index = this.indexOf(element);
        if (index != -1)
        {
            float[] newItems = new float[this.items.length - 1];
            System.arraycopy(this.items, 0, newItems, 0, index);
            System.arraycopy(this.items, index + 1, newItems, index, this.items.length - index - 1);
            return FloatLists.immutable.with(newItems);
        }
        return this;
    }

    @Override
    public ImmutableFloatList newWithAll(FloatIterable elements)
    {
        float[] newItems = new float[this.items.length + elements.size()];
        System.arraycopy(this.items, 0, newItems, 0, this.items.length);
        int index = 0;
        for (FloatIterator iterator = elements.floatIterator(); iterator.hasNext(); index++)
        {
            newItems[this.items.length + index] = iterator.next();
        }
        return new ImmutableFloatArrayList(newItems);
    }

    @Override
    public ImmutableFloatList newWithoutAll(FloatIterable elements)
    {
        MutableFloatList mutableFloatList = this.toList();
        mutableFloatList.removeAll(elements);
        return mutableFloatList.toImmutable();
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
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectFloatIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.items.length; i++)
        {
            result = function.valueOf(result, this.items[i], i);
        }
        return result;
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<FloatIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                FloatIterator iterator = this.floatIterator();
                while (iterator.hasNext())
                {
                    MutableFloatList batch = FloatLists.mutable.empty();
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
        if (!(otherList instanceof FloatList))
        {
            return false;
        }
        FloatList list = (FloatList) otherList;
        if (this.items.length != list.size())
        {
            return false;
        }
        for (int i = 0; i < this.items.length; i++)
        {
            if (Float.compare(this.items[i], list.get(i)) != 0)
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
        for (float item : this.items)
        {
            hashCode = 31 * hashCode + Float.floatToIntBits(item);
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
                float value = this.items[i];
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
    public ImmutableFloatList distinct()
    {
        FloatArrayList target = new FloatArrayList();
        MutableFloatSet seenSoFar = new FloatHashSet(this.size());

        for (float each : this.items)
        {
            if (seenSoFar.add(each))
            {
                target.add(each);
            }
        }
        return target.toImmutable();
    }

    @Override
    public ImmutableFloatList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public ImmutableList<FloatFloatPair> zipFloat(FloatIterable iterable)
    {
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<FloatFloatPair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        FloatIterator iterator = iterable.floatIterator();
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
    public <T> ImmutableList<FloatObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<FloatObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target.toImmutable();
    }

    private class InternalFloatIterator implements FloatIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != ImmutableFloatArrayList.this.items.length;
        }

        @Override
        public float next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            float next = ImmutableFloatArrayList.this.items[this.currentIndex];
            this.currentIndex++;
            return next;
        }
    }
}
