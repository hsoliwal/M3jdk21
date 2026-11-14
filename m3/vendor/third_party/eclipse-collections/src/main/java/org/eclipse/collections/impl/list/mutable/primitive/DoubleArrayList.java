/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.mutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.comparator.primitive.DoubleComparator;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.api.list.primitive.ImmutableDoubleList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseDoubleIterable;
import org.eclipse.collections.impl.primitive.AbstractDoubleIterable;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.primitive.DoubleQuickSort;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * DoubleArrayList is similar to FastList, and is memory-optimized for double primitives.
 * This file was automatically generated from template file primitiveArrayList.stg.
 *
 * @since 3.0.
 */
public class DoubleArrayList extends AbstractDoubleIterable
        implements MutableDoubleList, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final double[] DEFAULT_SIZED_EMPTY_ARRAY = {};
    private static final double[] ZERO_SIZED_ARRAY = {};
    private static final int MAXIMUM_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    protected int size;
    protected transient double[] items = DEFAULT_SIZED_EMPTY_ARRAY;

    public DoubleArrayList()
    {
    }

    public DoubleArrayList(int initialCapacity)
    {
        this.items = initialCapacity == 0 ? ZERO_SIZED_ARRAY : new double[initialCapacity];
    }

    public DoubleArrayList(double... array)
    {
        this.size = array.length;
        this.items = array;
    }

    /**
     * Creates a new list using the passed {@code elements} argument as the backing store.
     * <p>
     * !!! WARNING: This method uses the passed in array, so can be very unsafe if the original
     * array is held onto anywhere else. !!!
     */
    public static DoubleArrayList newListWith(double... elements)
    {
        return new DoubleArrayList(elements);
    }

    public static DoubleArrayList newList(DoubleIterable source)
    {
        return DoubleArrayList.newListWith(source.toArray());
    }

    public static DoubleArrayList newWithNValues(int size, double value)
    {
        DoubleArrayList newList = new DoubleArrayList(size);
        newList.size = size;
        Arrays.fill(newList.items, value);
        return newList;
    }

    /**
     * Creates a new list by first copying the array passed in.
     */
    public static DoubleArrayList wrapCopy(double... array)
    {
        double[] newArray = new double[array.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return new DoubleArrayList(newArray);
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public void clear()
    {
        Arrays.fill(this.items, 0, size, 0.0);
        this.size = 0;
    }

    @Override
    public boolean contains(double value)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (Double.compare(this.items[i], value) == 0)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public double get(int index)
    {
        if (index < this.size)
        {
            return this.items[index];
        }
        throw this.newIndexOutOfBoundsException(index);
    }

    private IndexOutOfBoundsException newIndexOutOfBoundsException(int index)
    {
        return new IndexOutOfBoundsException("Index: " + index + " Size: " + this.size);
    }

    @Override
    public double getFirst()
    {
        this.checkEmpty();
        return this.items[0];
    }

    @Override
    public double getLast()
    {
        this.checkEmpty();
        return this.items[this.size() - 1];
    }

    private void checkEmpty()
    {
        if (this.isEmpty())
        {
            throw this.newIndexOutOfBoundsException(0);
        }
    }

    @Override
    public int indexOf(double value)
    {
        for (int i = 0; i < this.size; i++)
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
        for (int i = this.size - 1; i >= 0; i--)
        {
            if (Double.compare(this.items[i], value) == 0)
            {
                return i;
            }
        }
        return -1;
    }

    public void trimToSize()
    {
        if (this.size < this.items.length)
        {
            this.transferItemsToNewArrayWithCapacity(this.size);
        }
    }

    private void transferItemsToNewArrayWithCapacity(int newCapacity)
    {
        this.items = this.copyItemsWithNewCapacity(newCapacity);
    }

    private double[] copyItemsWithNewCapacity(int newCapacity)
    {
        double[] newItems = new double[newCapacity];
        System.arraycopy(this.items, 0, newItems, 0, Math.min(this.size, newCapacity));
        return newItems;
    }

    private int sizePlusFiftyPercent(int oldSize)
    {
        int result = oldSize + (oldSize >> 1) + 1;
        return result < oldSize ? MAXIMUM_ARRAY_SIZE : result;
    }

    public void ensureCapacity(int minCapacity)
    {
        int oldCapacity = this.items.length;
        if (minCapacity > oldCapacity)
        {
            int newCapacity = Math.max(this.sizePlusFiftyPercent(oldCapacity), minCapacity);
            this.transferItemsToNewArrayWithCapacity(newCapacity);
        }
    }

    private void ensureCapacityForAdd()
    {
        if (this.items == DEFAULT_SIZED_EMPTY_ARRAY)
        {
            this.items = new double[10];
        }
        else
        {
            this.transferItemsToNewArrayWithCapacity(this.sizePlusFiftyPercent(this.size));
        }
    }

    @Override
    public boolean add(double newItem)
    {
        if (this.items.length == this.size)
        {
            this.ensureCapacityForAdd();
        }
        this.items[this.size] = newItem;
        this.size++;
        return true;
    }

    @Override
    public boolean addAll(double... source)
    {
        if (source.length < 1)
        {
            return false;
        }
        this.copyItems(source.length, source);
        return true;
    }

    @Override
    public boolean addAll(DoubleIterable source)
    {
        if (source instanceof DoubleArrayList)
        {
            if (source.isEmpty())
            {
                return false;
            }
            DoubleArrayList other = (DoubleArrayList) source;
            this.copyItems(other.size(), other.items);
            return true;
        }
        return this.addAll(source.toArray());
    }

    private void copyItems(int sourceSize, double[] source)
    {
        int newSize = this.size + sourceSize;
        this.ensureCapacity(newSize);
        System.arraycopy(source, 0, this.items, this.size, sourceSize);
        this.size = newSize;
    }

    private void throwOutOfBounds(int index)
    {
        throw this.newIndexOutOfBoundsException(index);
    }

    @Override
    public void addAtIndex(int index, double element)
    {
        if (index > -1 && index < this.size)
        {
            this.addAtIndexLessThanSize(index, element);
        }
        else if (index == this.size)
        {
            this.add(element);
        }
        else
        {
            this.throwOutOfBounds(index);
        }
    }

    private void addAtIndexLessThanSize(int index, double element)
    {
        int oldSize = this.size;
        this.size++;
        if (this.items.length == oldSize)
        {
            double[] newItems = new double[this.sizePlusFiftyPercent(oldSize)];
            if (index > 0)
            {
                System.arraycopy(this.items, 0, newItems, 0, index);
            }
            System.arraycopy(this.items, index, newItems, index + 1, oldSize - index);
            this.items = newItems;
        }
        else
        {
            System.arraycopy(this.items, index, this.items, index + 1, oldSize - index);
        }
        this.items[index] = element;
    }

    @Override
    public boolean addAllAtIndex(int index, double... source)
    {
        if (index > this.size || index < 0)
        {
            this.throwOutOfBounds(index);
        }
        if (source.length == 0)
        {
            return false;
        }
        int sourceSize = source.length;
        int newSize = this.size + sourceSize;
        this.ensureCapacity(newSize);
        this.shiftElementsAtIndex(index, sourceSize);
        System.arraycopy(source, 0, this.items, index, sourceSize);
        this.size = newSize;
        return true;
    }

    @Override
    public boolean addAllAtIndex(int index, DoubleIterable source)
    {
        return this.addAllAtIndex(index, source.toArray());
    }

    private void shiftElementsAtIndex(int index, int sourceSize)
    {
        int numberToMove = this.size - index;
        if (numberToMove > 0)
        {
            System.arraycopy(this.items, index, this.items, index + sourceSize, numberToMove);
        }
    }

    @Override
    public boolean remove(double value)
    {
        int index = this.indexOf(value);
        if (index >= 0)
        {
            this.removeAtIndex(index);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeIf(DoublePredicate predicate)
    {
        int currentFilledIndex = 0;
        for (int i = 0; i < this.size; i++)
        {
            double item = this.items[i];
            if (!predicate.accept(item))
            {
                // keep it
                if (currentFilledIndex != i)
                {
                    this.items[currentFilledIndex] = item;
                }
                currentFilledIndex++;
            }
        }
        boolean changed = currentFilledIndex < this.size;
        this.wipeAndResetTheEnd(currentFilledIndex);
        return changed;
    }

    private void wipeAndResetTheEnd(int newCurrentFilledIndex)
    {
        for (int i = newCurrentFilledIndex; i < this.size; i++)
        {
            this.items[i] = 0.0;
        }
        this.size = newCurrentFilledIndex;
    }

    @Override
    public boolean removeAll(DoubleIterable source)
    {
        boolean modified = false;
        for (int index = 0; index < this.size; index++)
        {
            if (source.contains(this.get(index)))
            {
                this.removeAtIndex(index);
                index--;
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(double... source)
    {
        DoubleHashSet set = DoubleHashSet.newSetWith(source);
        double[] newItems = new double[this.size];
        int count = 0;
        int oldSize = this.size;
        for (int index = 0; index < this.size; index++)
        {
            if (!set.contains(this.items[index]))
            {
                newItems[count] = this.items[index];
                count++;
            }
        }
        this.items = newItems;
        this.size = count;
        return oldSize != this.size;
    }

    @Override
    public boolean retainAll(DoubleIterable source)
    {
        int oldSize = this.size();
        final DoubleSet sourceSet = source instanceof DoubleSet ? (DoubleSet) source : source.toSet();
        DoubleArrayList retained = this.select(sourceSet::contains);
        this.size = retained.size;
        this.items = retained.items;
        return oldSize != this.size();
    }

    @Override
    public boolean retainAll(double... source)
    {
        return this.retainAll(DoubleHashSet.newSetWith(source));
    }

    @Override
    public double removeAtIndex(int index)
    {
        double previous = this.get(index);
        int totalOffset = this.size - index - 1;
        if (totalOffset > 0)
        {
            System.arraycopy(this.items, index + 1, this.items, index, totalOffset);
        }
        --this.size;
        this.items[this.size] = 0.0;
        return previous;
    }

    @Override
    public double set(int index, double element)
    {
        double previous = this.get(index);
        this.items[index] = element;
        return previous;
    }

    @Override
    public void swap(int index1, int index2)
    {
        double value = this.get(index1);
        this.items[index1] = this.items[index2];
        this.items[index2] = value;
    }

    @Override
    public DoubleArrayList with(double element)
    {
        this.add(element);
        return this;
    }

    @Override
    public DoubleArrayList without(double element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public DoubleArrayList withAll(DoubleIterable elements)
    {
        this.addAll(elements.toArray());
        return this;
    }

    @Override
    public DoubleArrayList withoutAll(DoubleIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    public DoubleArrayList with(double element1, double element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public DoubleArrayList with(double element1, double element2, double element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    public DoubleArrayList with(double element1, double element2, double element3, double... elements)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this.withArrayCopy(elements, 0, elements.length);
    }

    private DoubleArrayList withArrayCopy(double[] elements, int begin, int length)
    {
        this.ensureCapacity(this.size + length);
        System.arraycopy(elements, begin, this.items, this.size, length);
        this.size += length;
        return this;
    }

    @Override
    public MutableDoubleIterator doubleIterator()
    {
        return new InternalDoubleIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i]);
        }
    }

    @Override
    public void forEachWithIndex(DoubleIntProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
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
                result.add(DoubleLists.mutable.withAll(this));
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
                    result.add(batch);
                }
            }
        }
        return result;
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        int count = 0;
        for (int i = 0; i < this.size; i++)
        {
            if (predicate.accept(this.items[i]))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (predicate.accept(this.items[i]))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (!predicate.accept(this.items[i]))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public DoubleArrayList select(DoublePredicate predicate)
    {
        return this.select(predicate, new DoubleArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableDoubleCollection> R select(DoublePredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            double item = this.items[i];
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public DoubleArrayList reject(DoublePredicate predicate)
    {
        return this.reject(predicate, new DoubleArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableDoubleCollection> R reject(DoublePredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            double item = this.items[i];
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
        for (int i = 0; i < this.size; i++)
        {
            double item = this.items[i];
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> MutableList<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return this.collect(function, Lists.mutable.withInitialCapacity(this.size));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(DoubleToObjectFunction<? extends V> function, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(function.valueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public double max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        double max = this.items[0];
        for (int i = 1; i < this.size; i++)
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
    public double min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        double min = this.items[0];
        for (int i = 1; i < this.size; i++)
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
    public double sum()
    {
        double result = 0.0;
        double compensation = 0.0;
        for (int i = 0; i < this.size; i++)
        {
            double adjustedValue = this.items[i] - compensation;
            double nextSum = result + adjustedValue;
            compensation = nextSum - result - adjustedValue;
            result = nextSum;
        }
        return result;
    }

    @Override
    public double dotProduct(DoubleList list)
    {
        if (this.size != list.size())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        double sum = 0.0;
        for (int i = 0; i < this.size; i++)
        {
            sum += this.items[i] * list.get(i);
        }
        return sum;
    }

    @Override
    public double[] toArray()
    {
        double[] newItems = new double[this.size];
        System.arraycopy(this.items, 0, newItems, 0, this.size);
        return newItems;
    }

    @Override
    public double[] toArray(double[] target)
    {
        if (target.length < this.size)
        {
            target = new double[this.size];
        }
        System.arraycopy(this.items, 0, target, 0, this.size);
        return target;
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
        if (this.size != list.size())
        {
            return false;
        }
        for (int i = 0; i < this.size; i++)
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
        for (int i = 0; i < this.size; i++)
        {
            double item = this.items[i];
            hashCode = 31 * hashCode + (int) (Double.doubleToLongBits(item) ^ Double.doubleToLongBits(item) >>> 32);
        }
        return hashCode;
    }

    @Override
    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        try
        {
            appendable.append(start);
            for (int i = 0; i < this.size; i++)
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

    @Override
    public MutableDoubleList asUnmodifiable()
    {
        return new UnmodifiableDoubleList(this);
    }

    @Override
    public MutableDoubleList asSynchronized()
    {
        return new SynchronizedDoubleList(this);
    }

    @Override
    public ImmutableDoubleList toImmutable()
    {
        return DoubleLists.immutable.withAll(this);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size);
        for (int i = 0; i < this.size; i++)
        {
            out.writeDouble(this.items[i]);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        this.size = in.readInt();
        this.items = new double[this.size];
        for (int i = 0; i < this.size; i++)
        {
            this.items[i] = in.readDouble();
        }
    }

    @Override
    public LazyDoubleIterable asReversed()
    {
        return ReverseDoubleIterable.adapt(this);
    }

    @Override
    public DoubleArrayList reverseThis()
    {
        int endIndex = this.size - 1;
        for (int i = 0; i < this.size / 2; i++)
        {
            double tempSwapValue = this.items[i];
            this.items[i] = this.items[endIndex - i];
            this.items[endIndex - i] = tempSwapValue;
        }
        return this;
    }

    @Override
    public DoubleArrayList sortThis()
    {
        Arrays.sort(this.items, 0, this.size);
        return this;
    }

    @Override
    public DoubleArrayList sortThis(DoubleComparator comparator)
    {
        DoubleQuickSort.sort(this.items, 0, this.size() - 1, comparator);
        return this;
    }

    @Override
    public DoubleArrayList toReversed()
    {
        return DoubleArrayList.newList(this.asReversed());
    }

    @Override
    public int binarySearch(double value)
    {
        return Arrays.binarySearch(this.items, 0, this.size, value);
    }

    @Override
    public MutableDoubleList distinct()
    {
        DoubleArrayList target = new DoubleArrayList();
        MutableDoubleSet seenSoFar = new DoubleHashSet(this.size());

        for (int i = 0; i < this.size; i++)
        {
            double each = this.items[i];
            if (seenSoFar.add(each))
            {
                target.add(each);
            }
        }
        return target;
    }

    @Override
    public MutableDoubleList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public MutableList<DoubleDoublePair> zipDouble(DoubleIterable iterable)
    {
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<DoubleDoublePair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        DoubleIterator iterator = iterable.doubleIterator();
        for (int i = 0; i < size && i < otherSize; i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    /**
     * Creates a new empty DoubleArrayList.
     *
     * @since 9.2.
     */
    public DoubleArrayList newEmpty()
    {
        return new DoubleArrayList();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> MutableList<DoubleObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<DoubleObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    private class InternalDoubleIterator implements MutableDoubleIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;
        private int lastIndex = -1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != DoubleArrayList.this.size();
        }

        @Override
        public double next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            double next = DoubleArrayList.this.items[this.currentIndex];
            this.lastIndex = this.currentIndex++;
            return next;
        }

        @Override
        public void remove()
        {
            if (this.lastIndex == -1)
            {
                throw new IllegalStateException();
            }
            DoubleArrayList.this.removeAtIndex(this.lastIndex);
            this.currentIndex--;
            this.lastIndex = -1;
        }
    }

    @Override
    public Spliterator.OfDouble spliterator()
    {
        int characteristics = Spliterator.ORDERED;
        return Spliterators.spliterator(this.items, 0, size, characteristics);
    }
}
