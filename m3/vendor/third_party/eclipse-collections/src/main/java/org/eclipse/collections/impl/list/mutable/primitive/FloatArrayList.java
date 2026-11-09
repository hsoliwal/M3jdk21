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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.comparator.primitive.FloatComparator;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.list.primitive.ImmutableFloatList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.FloatFloatPair;
import org.eclipse.collections.api.tuple.primitive.FloatObjectPair;
import org.eclipse.collections.impl.factory.primitive.FloatLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseFloatIterable;
import org.eclipse.collections.impl.primitive.AbstractFloatIterable;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.primitive.FloatQuickSort;

/**
 * FloatArrayList is similar to FastList, and is memory-optimized for float primitives.
 * This file was automatically generated from template file primitiveArrayList.stg.
 *
 * @since 3.0.
 */
public class FloatArrayList extends AbstractFloatIterable
        implements MutableFloatList, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final float[] DEFAULT_SIZED_EMPTY_ARRAY = {};
    private static final float[] ZERO_SIZED_ARRAY = {};
    private static final int MAXIMUM_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    protected int size;
    protected transient float[] items = DEFAULT_SIZED_EMPTY_ARRAY;

    public FloatArrayList()
    {
    }

    public FloatArrayList(int initialCapacity)
    {
        this.items = initialCapacity == 0 ? ZERO_SIZED_ARRAY : new float[initialCapacity];
    }

    public FloatArrayList(float... array)
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
    public static FloatArrayList newListWith(float... elements)
    {
        return new FloatArrayList(elements);
    }

    public static FloatArrayList newList(FloatIterable source)
    {
        return FloatArrayList.newListWith(source.toArray());
    }

    public static FloatArrayList newWithNValues(int size, float value)
    {
        FloatArrayList newList = new FloatArrayList(size);
        newList.size = size;
        Arrays.fill(newList.items, value);
        return newList;
    }

    /**
     * Creates a new list by first copying the array passed in.
     */
    public static FloatArrayList wrapCopy(float... array)
    {
        float[] newArray = new float[array.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return new FloatArrayList(newArray);
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public void clear()
    {
        Arrays.fill(this.items, 0, size, 0.0f);
        this.size = 0;
    }

    @Override
    public boolean contains(float value)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (Float.compare(this.items[i], value) == 0)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public float get(int index)
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
    public float getFirst()
    {
        this.checkEmpty();
        return this.items[0];
    }

    @Override
    public float getLast()
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
    public int indexOf(float value)
    {
        for (int i = 0; i < this.size; i++)
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
        for (int i = this.size - 1; i >= 0; i--)
        {
            if (Float.compare(this.items[i], value) == 0)
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

    private float[] copyItemsWithNewCapacity(int newCapacity)
    {
        float[] newItems = new float[newCapacity];
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
            this.items = new float[10];
        }
        else
        {
            this.transferItemsToNewArrayWithCapacity(this.sizePlusFiftyPercent(this.size));
        }
    }

    @Override
    public boolean add(float newItem)
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
    public boolean addAll(float... source)
    {
        if (source.length < 1)
        {
            return false;
        }
        this.copyItems(source.length, source);
        return true;
    }

    @Override
    public boolean addAll(FloatIterable source)
    {
        if (source instanceof FloatArrayList)
        {
            if (source.isEmpty())
            {
                return false;
            }
            FloatArrayList other = (FloatArrayList) source;
            this.copyItems(other.size(), other.items);
            return true;
        }
        return this.addAll(source.toArray());
    }

    private void copyItems(int sourceSize, float[] source)
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
    public void addAtIndex(int index, float element)
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

    private void addAtIndexLessThanSize(int index, float element)
    {
        int oldSize = this.size;
        this.size++;
        if (this.items.length == oldSize)
        {
            float[] newItems = new float[this.sizePlusFiftyPercent(oldSize)];
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
    public boolean addAllAtIndex(int index, float... source)
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
    public boolean addAllAtIndex(int index, FloatIterable source)
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
    public boolean remove(float value)
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
    public boolean removeIf(FloatPredicate predicate)
    {
        int currentFilledIndex = 0;
        for (int i = 0; i < this.size; i++)
        {
            float item = this.items[i];
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
            this.items[i] = 0.0f;
        }
        this.size = newCurrentFilledIndex;
    }

    @Override
    public boolean removeAll(FloatIterable source)
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
    public boolean removeAll(float... source)
    {
        FloatHashSet set = FloatHashSet.newSetWith(source);
        float[] newItems = new float[this.size];
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
    public boolean retainAll(FloatIterable source)
    {
        int oldSize = this.size();
        final FloatSet sourceSet = source instanceof FloatSet ? (FloatSet) source : source.toSet();
        FloatArrayList retained = this.select(sourceSet::contains);
        this.size = retained.size;
        this.items = retained.items;
        return oldSize != this.size();
    }

    @Override
    public boolean retainAll(float... source)
    {
        return this.retainAll(FloatHashSet.newSetWith(source));
    }

    @Override
    public float removeAtIndex(int index)
    {
        float previous = this.get(index);
        int totalOffset = this.size - index - 1;
        if (totalOffset > 0)
        {
            System.arraycopy(this.items, index + 1, this.items, index, totalOffset);
        }
        --this.size;
        this.items[this.size] = 0.0f;
        return previous;
    }

    @Override
    public float set(int index, float element)
    {
        float previous = this.get(index);
        this.items[index] = element;
        return previous;
    }

    @Override
    public void swap(int index1, int index2)
    {
        float value = this.get(index1);
        this.items[index1] = this.items[index2];
        this.items[index2] = value;
    }

    @Override
    public FloatArrayList with(float element)
    {
        this.add(element);
        return this;
    }

    @Override
    public FloatArrayList without(float element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public FloatArrayList withAll(FloatIterable elements)
    {
        this.addAll(elements.toArray());
        return this;
    }

    @Override
    public FloatArrayList withoutAll(FloatIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    public FloatArrayList with(float element1, float element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public FloatArrayList with(float element1, float element2, float element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    public FloatArrayList with(float element1, float element2, float element3, float... elements)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this.withArrayCopy(elements, 0, elements.length);
    }

    private FloatArrayList withArrayCopy(float[] elements, int begin, int length)
    {
        this.ensureCapacity(this.size + length);
        System.arraycopy(elements, begin, this.items, this.size, length);
        this.size += length;
        return this;
    }

    @Override
    public MutableFloatIterator floatIterator()
    {
        return new InternalFloatIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i]);
        }
    }

    @Override
    public void forEachWithIndex(FloatIntProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectFloatIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
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
                result.add(FloatLists.mutable.withAll(this));
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
                    result.add(batch);
                }
            }
        }
        return result;
    }

    @Override
    public int count(FloatPredicate predicate)
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
    public boolean anySatisfy(FloatPredicate predicate)
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
    public boolean allSatisfy(FloatPredicate predicate)
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
    public FloatArrayList select(FloatPredicate predicate)
    {
        return this.select(predicate, new FloatArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableFloatCollection> R select(FloatPredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            float item = this.items[i];
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public FloatArrayList reject(FloatPredicate predicate)
    {
        return this.reject(predicate, new FloatArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableFloatCollection> R reject(FloatPredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            float item = this.items[i];
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
        for (int i = 0; i < this.size; i++)
        {
            float item = this.items[i];
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> MutableList<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return this.collect(function, Lists.mutable.withInitialCapacity(this.size));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(FloatToObjectFunction<? extends V> function, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(function.valueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public float max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        float max = this.items[0];
        for (int i = 1; i < this.size; i++)
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
    public float min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        float min = this.items[0];
        for (int i = 1; i < this.size; i++)
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
    public double dotProduct(FloatList list)
    {
        if (this.size != list.size())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        double sum = 0.0;
        for (int i = 0; i < this.size; i++)
        {
            sum += (double) this.items[i] * list.get(i);
        }
        return sum;
    }

    @Override
    public float[] toArray()
    {
        float[] newItems = new float[this.size];
        System.arraycopy(this.items, 0, newItems, 0, this.size);
        return newItems;
    }

    @Override
    public float[] toArray(float[] target)
    {
        if (target.length < this.size)
        {
            target = new float[this.size];
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
        if (!(otherList instanceof FloatList))
        {
            return false;
        }
        FloatList list = (FloatList) otherList;
        if (this.size != list.size())
        {
            return false;
        }
        for (int i = 0; i < this.size; i++)
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
        for (int i = 0; i < this.size; i++)
        {
            float item = this.items[i];
            hashCode = 31 * hashCode + Float.floatToIntBits(item);
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

    @Override
    public MutableFloatList asUnmodifiable()
    {
        return new UnmodifiableFloatList(this);
    }

    @Override
    public MutableFloatList asSynchronized()
    {
        return new SynchronizedFloatList(this);
    }

    @Override
    public ImmutableFloatList toImmutable()
    {
        return FloatLists.immutable.withAll(this);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size);
        for (int i = 0; i < this.size; i++)
        {
            out.writeFloat(this.items[i]);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        this.size = in.readInt();
        this.items = new float[this.size];
        for (int i = 0; i < this.size; i++)
        {
            this.items[i] = in.readFloat();
        }
    }

    @Override
    public LazyFloatIterable asReversed()
    {
        return ReverseFloatIterable.adapt(this);
    }

    @Override
    public FloatArrayList reverseThis()
    {
        int endIndex = this.size - 1;
        for (int i = 0; i < this.size / 2; i++)
        {
            float tempSwapValue = this.items[i];
            this.items[i] = this.items[endIndex - i];
            this.items[endIndex - i] = tempSwapValue;
        }
        return this;
    }

    @Override
    public FloatArrayList sortThis()
    {
        Arrays.sort(this.items, 0, this.size);
        return this;
    }

    @Override
    public FloatArrayList sortThis(FloatComparator comparator)
    {
        FloatQuickSort.sort(this.items, 0, this.size() - 1, comparator);
        return this;
    }

    @Override
    public FloatArrayList toReversed()
    {
        return FloatArrayList.newList(this.asReversed());
    }

    @Override
    public int binarySearch(float value)
    {
        return Arrays.binarySearch(this.items, 0, this.size, value);
    }

    @Override
    public MutableFloatList distinct()
    {
        FloatArrayList target = new FloatArrayList();
        MutableFloatSet seenSoFar = new FloatHashSet(this.size());

        for (int i = 0; i < this.size; i++)
        {
            float each = this.items[i];
            if (seenSoFar.add(each))
            {
                target.add(each);
            }
        }
        return target;
    }

    @Override
    public MutableFloatList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public MutableList<FloatFloatPair> zipFloat(FloatIterable iterable)
    {
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<FloatFloatPair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        FloatIterator iterator = iterable.floatIterator();
        for (int i = 0; i < size && i < otherSize; i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    /**
     * Creates a new empty FloatArrayList.
     *
     * @since 9.2.
     */
    public FloatArrayList newEmpty()
    {
        return new FloatArrayList();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> MutableList<FloatObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<FloatObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    private class InternalFloatIterator implements MutableFloatIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;
        private int lastIndex = -1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != FloatArrayList.this.size();
        }

        @Override
        public float next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            float next = FloatArrayList.this.items[this.currentIndex];
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
            FloatArrayList.this.removeAtIndex(this.lastIndex);
            this.currentIndex--;
            this.lastIndex = -1;
        }
    }
}
