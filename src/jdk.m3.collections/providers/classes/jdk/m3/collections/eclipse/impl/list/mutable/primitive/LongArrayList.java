/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project:
// avoid an intermediate primitive snapshot and repeated suffix copies for array-list insertion.

package jdk.m3.collections.eclipse.impl.list.mutable.primitive;

import java.io.Externalizable;
import java.io.NotSerializableException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.block.comparator.primitive.LongComparator;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectLongIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectLongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableLongCollection;
import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.iterator.MutableLongIterator;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.LongList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableLongList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.set.primitive.LongSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableLongStack;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongLongPair;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongObjectPair;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongStacks;
import jdk.m3.collections.eclipse.impl.lazy.primitive.ReverseLongIterable;
import jdk.m3.collections.eclipse.impl.primitive.AbstractLongIterable;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import jdk.m3.collections.eclipse.impl.utility.primitive.LongQuickSort;
import java.util.Spliterator;
import java.util.Spliterators;
import jdk.m3.collections.hppc.ArraySizingStrategy;

/**
 * LongArrayList is similar to FastList, and is memory-optimized for long primitives.
 * This file was automatically generated from template file primitiveArrayList.stg.
 *
 * @since 3.0.
 */
public class LongArrayList extends AbstractLongIterable
        implements MutableLongList, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final long[] DEFAULT_SIZED_EMPTY_ARRAY = {};
    private static final long[] ZERO_SIZED_ARRAY = {};
    private static final int MAXIMUM_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    protected int size;
    protected transient long[] items = DEFAULT_SIZED_EMPTY_ARRAY;
    // Modified 2026 by Synexia contributors: a growth policy may be given (ArraySizingStrategy).
    /** The growth policy given at construction, or null for this class's own. */
    private transient ArraySizingStrategy sizing;

    public LongArrayList()
    {
    }

    /**
     * Creates an empty list with the given initial capacity that grows by the given strategy,
     * as a {@link java.util.Comparator} orders a sorted collection: whenever the list must grow
     * to hold {@code n} elements, its new capacity is
     * {@code sizing.grow(capacity, size(), n - size())}. {@code ArraySizingStrategy.fastList()}
     * grows as this class does without a strategy. A list with a strategy is not serializable:
     * its strategy would be lost.
     *
     * @param initialCapacity the initial capacity
     * @param sizing the growth policy
     * @throws NullPointerException if {@code sizing} is null
     */
    public LongArrayList(int initialCapacity, ArraySizingStrategy sizing)
    {
        this(initialCapacity);
        this.sizing = java.util.Objects.requireNonNull(sizing, "sizing");
    }

    public LongArrayList(int initialCapacity)
    {
        this.items = initialCapacity == 0 ? ZERO_SIZED_ARRAY : new long[initialCapacity];
    }

    public LongArrayList(long... array)
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
    public static LongArrayList newListWith(long... elements)
    {
        return new LongArrayList(elements);
    }

    public static LongArrayList newList(LongIterable source)
    {
        return LongArrayList.newListWith(source.toArray());
    }

    public static LongArrayList newWithNValues(int size, long value)
    {
        LongArrayList newList = new LongArrayList(size);
        newList.size = size;
        Arrays.fill(newList.items, value);
        return newList;
    }

    /**
     * Creates a new list by first copying the array passed in.
     */
    public static LongArrayList wrapCopy(long... array)
    {
        long[] newArray = new long[array.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return new LongArrayList(newArray);
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public void clear()
    {
        Arrays.fill(this.items, 0, size, 0L);
        this.size = 0;
    }

    @Override
    public boolean contains(long value)
    {
        for (int i = 0; i < this.size; i++)
        {
            if (this.items[i] == value)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public long get(int index)
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
    public long getFirst()
    {
        this.checkEmpty();
        return this.items[0];
    }

    @Override
    public long getLast()
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
    public int indexOf(long value)
    {
        for (int i = 0; i < this.size; i++)
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
        for (int i = this.size - 1; i >= 0; i--)
        {
            if (this.items[i] == value)
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

    private long[] copyItemsWithNewCapacity(int newCapacity)
    {
        long[] newItems = new long[newCapacity];
        System.arraycopy(this.items, 0, newItems, 0, Math.min(this.size, newCapacity));
        return newItems;
    }

    /**
     * The capacity the strategy given at construction grows {@code capacity} to so that
     * {@code elements + additions} elements fit.
     */
    private int grownBySizing(int capacity, int elements, int additions)
    {
        int grown = this.sizing.grow(capacity, elements, additions);
        if (grown - additions < elements)
        {
            throw new IllegalStateException("ArraySizingStrategy " + this.sizing + " returned "
                    + grown + " for " + elements + " + " + additions + " elements");
        }
        return grown;
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
            int newCapacity = this.sizing == null
                    ? Math.max(this.sizePlusFiftyPercent(oldCapacity), minCapacity)
                    : this.grownBySizing(oldCapacity, this.size, minCapacity - this.size);
            this.transferItemsToNewArrayWithCapacity(newCapacity);
        }
    }

    private void ensureCapacityForAdd()
    {
        if (this.items == DEFAULT_SIZED_EMPTY_ARRAY)
        {
            this.items = new long[10];
        }
        else
        {
            this.transferItemsToNewArrayWithCapacity(this.sizing == null
                    ? this.sizePlusFiftyPercent(this.size)
                    : this.grownBySizing(this.items.length, this.size, 1));
        }
    }

    @Override
    public boolean add(long newItem)
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
    public boolean addAll(long... source)
    {
        if (source.length < 1)
        {
            return false;
        }
        this.copyItems(source.length, source);
        return true;
    }

    @Override
    public boolean addAll(LongIterable source)
    {
        if (source instanceof LongArrayList)
        {
            if (source.isEmpty())
            {
                return false;
            }
            LongArrayList other = (LongArrayList) source;
            this.copyItems(other.size(), other.items);
            return true;
        }
        return this.addAll(source.toArray());
    }

    private void copyItems(int sourceSize, long[] source)
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
    public void addAtIndex(int index, long element)
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

    private void addAtIndexLessThanSize(int index, long element)
    {
        int oldSize = this.size;
        // The strategy is asked before the list changes: a refusal leaves it unchanged.
        int grown = this.sizing == null || this.items.length != oldSize
                ? 0
                : this.grownBySizing(oldSize, oldSize, 1);
        this.size++;
        if (this.items.length == oldSize)
        {
            long[] newItems = new long[this.sizing == null
                    ? this.sizePlusFiftyPercent(oldSize)
                    : grown];
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
    public boolean addAllAtIndex(int index, long... source)
    {
        if (index > this.size || index < 0)
        {
            this.throwOutOfBounds(index);
        }
        if (source.length == 0)
        {
            return false;
        }
        this.copyItemsAtIndex(index, source.length, source);
        return true;
    }

    @Override
    public boolean addAllAtIndex(int index, LongIterable source)
    {
        if (this.getClass() == LongArrayList.class
                && source != null && source.getClass() == LongArrayList.class)
        {
            LongArrayList other = (LongArrayList) source;
            // Wrapped arrays may be shared by distinct lists. Retain the snapshot
            // path for aliases and for subclasses with observable export overrides.
            if (other.items != this.items)
            {
                if (index > this.size || index < 0)
                {
                    this.throwOutOfBounds(index);
                }
                if (other.size == 0)
                {
                    return false;
                }
                this.copyItemsAtIndex(index, other.size, other.items);
                return true;
            }
        }
        return this.addAllAtIndex(index, source.toArray());
    }

    private void copyItemsAtIndex(int index, int sourceSize, long[] source)
    {
        int newSize = this.size + sourceSize;
        // Subclasses retain ensureCapacity dispatch and its publication order.
        if (this.getClass() == LongArrayList.class && newSize > this.items.length && index < this.size)
        {
            int newCapacity = this.sizing == null
                    ? Math.max(this.sizePlusFiftyPercent(this.items.length), newSize)
                    : this.grownBySizing(this.items.length, this.size, sourceSize);
            long[] newItems = new long[newCapacity];
            if (index > 0)
            {
                System.arraycopy(this.items, 0, newItems, 0, index);
            }
            System.arraycopy(this.items, index, newItems, index + sourceSize, this.size - index);
            System.arraycopy(source, 0, newItems, index, sourceSize);
            this.items = newItems;
        }
        else
        {
            this.ensureCapacity(newSize);
            this.shiftElementsAtIndex(index, sourceSize);
            System.arraycopy(source, 0, this.items, index, sourceSize);
        }
        this.size = newSize;
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
    public boolean remove(long value)
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
    public boolean removeIf(LongPredicate predicate)
    {
        int currentFilledIndex = 0;
        for (int i = 0; i < this.size; i++)
        {
            long item = this.items[i];
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
            this.items[i] = 0L;
        }
        this.size = newCurrentFilledIndex;
    }

    @Override
    public boolean removeAll(LongIterable source)
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
    public boolean removeAll(long... source)
    {
        LongHashSet set = LongHashSet.newSetWith(source);
        long[] newItems = new long[this.size];
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
    public boolean retainAll(LongIterable source)
    {
        int oldSize = this.size();
        final LongSet sourceSet = source instanceof LongSet ? (LongSet) source : source.toSet();
        LongArrayList retained = this.select(sourceSet::contains);
        this.size = retained.size;
        this.items = retained.items;
        return oldSize != this.size();
    }

    @Override
    public boolean retainAll(long... source)
    {
        return this.retainAll(LongHashSet.newSetWith(source));
    }

    @Override
    public long removeAtIndex(int index)
    {
        long previous = this.get(index);
        int totalOffset = this.size - index - 1;
        if (totalOffset > 0)
        {
            System.arraycopy(this.items, index + 1, this.items, index, totalOffset);
        }
        --this.size;
        this.items[this.size] = 0L;
        return previous;
    }

    @Override
    public long set(int index, long element)
    {
        long previous = this.get(index);
        this.items[index] = element;
        return previous;
    }

    @Override
    public void swap(int index1, int index2)
    {
        long value = this.get(index1);
        this.items[index1] = this.items[index2];
        this.items[index2] = value;
    }

    @Override
    public LongArrayList with(long element)
    {
        this.add(element);
        return this;
    }

    @Override
    public LongArrayList without(long element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public LongArrayList withAll(LongIterable elements)
    {
        this.addAll(elements.toArray());
        return this;
    }

    @Override
    public LongArrayList withoutAll(LongIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    public LongArrayList with(long element1, long element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public LongArrayList with(long element1, long element2, long element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    public LongArrayList with(long element1, long element2, long element3, long... elements)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this.withArrayCopy(elements, 0, elements.length);
    }

    private LongArrayList withArrayCopy(long[] elements, int begin, int length)
    {
        this.ensureCapacity(this.size + length);
        System.arraycopy(elements, begin, this.items, this.size, length);
        this.size += length;
        return this;
    }

    @Override
    public MutableLongIterator longIterator()
    {
        return new InternalLongIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i]);
        }
    }

    @Override
    public void forEachWithIndex(LongIntProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectLongIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
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
                result.add(LongLists.mutable.withAll(this));
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
                    result.add(batch);
                }
            }
        }
        return result;
    }

    @Override
    public int count(LongPredicate predicate)
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
    public boolean anySatisfy(LongPredicate predicate)
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
    public boolean allSatisfy(LongPredicate predicate)
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
    public LongArrayList select(LongPredicate predicate)
    {
        return this.select(predicate, new LongArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableLongCollection> R select(LongPredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            long item = this.items[i];
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public LongArrayList reject(LongPredicate predicate)
    {
        return this.reject(predicate, new LongArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableLongCollection> R reject(LongPredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            long item = this.items[i];
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
        for (int i = 0; i < this.size; i++)
        {
            long item = this.items[i];
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> MutableList<V> collect(LongToObjectFunction<? extends V> function)
    {
        return this.collect(function, Lists.mutable.withInitialCapacity(this.size));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(LongToObjectFunction<? extends V> function, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(function.valueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public long max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        long max = this.items[0];
        for (int i = 1; i < this.size; i++)
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
    public long min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        long min = this.items[0];
        for (int i = 1; i < this.size; i++)
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
    public long sum()
    {
        long result = 0L;
        for (int i = 0; i < this.size; i++)
        {
            result += this.items[i];
        }
        return result;
    }

    @Override
    public long dotProduct(LongList list)
    {
        if (this.size != list.size())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        long sum = 0L;
        for (int i = 0; i < this.size; i++)
        {
            sum += this.items[i] * list.get(i);
        }
        return sum;
    }

    @Override
    public long[] toArray()
    {
        long[] newItems = new long[this.size];
        System.arraycopy(this.items, 0, newItems, 0, this.size);
        return newItems;
    }

    @Override
    public long[] toArray(long[] target)
    {
        if (target.length < this.size)
        {
            target = new long[this.size];
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
        if (!(otherList instanceof LongList))
        {
            return false;
        }
        LongList list = (LongList) otherList;
        if (this.size != list.size())
        {
            return false;
        }
        for (int i = 0; i < this.size; i++)
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
        for (int i = 0; i < this.size; i++)
        {
            long item = this.items[i];
            hashCode = 31 * hashCode + (int) (item ^ item >>> 32);
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

    @Override
    public MutableLongList asUnmodifiable()
    {
        return new UnmodifiableLongList(this);
    }

    @Override
    public MutableLongList asSynchronized()
    {
        return new SynchronizedLongList(this);
    }

    @Override
    public ImmutableLongList toImmutable()
    {
        return LongLists.immutable.withAll(this);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        if (this.sizing != null)
        {
            throw new NotSerializableException(
                    "a LongArrayList with an ArraySizingStrategy: the strategy would be lost");
        }
        out.writeInt(this.size);
        for (int i = 0; i < this.size; i++)
        {
            out.writeLong(this.items[i]);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        this.size = in.readInt();
        this.items = new long[this.size];
        for (int i = 0; i < this.size; i++)
        {
            this.items[i] = in.readLong();
        }
    }

    @Override
    public LazyLongIterable asReversed()
    {
        return ReverseLongIterable.adapt(this);
    }

    @Override
    public LongArrayList reverseThis()
    {
        int endIndex = this.size - 1;
        for (int i = 0; i < this.size / 2; i++)
        {
            long tempSwapValue = this.items[i];
            this.items[i] = this.items[endIndex - i];
            this.items[endIndex - i] = tempSwapValue;
        }
        return this;
    }

    @Override
    public LongArrayList sortThis()
    {
        Arrays.sort(this.items, 0, this.size);
        return this;
    }

    @Override
    public LongArrayList sortThis(LongComparator comparator)
    {
        LongQuickSort.sort(this.items, 0, this.size() - 1, comparator);
        return this;
    }

    @Override
    public LongArrayList toReversed()
    {
        return LongArrayList.newList(this.asReversed());
    }

    @Override
    public int binarySearch(long value)
    {
        return Arrays.binarySearch(this.items, 0, this.size, value);
    }

    @Override
    public MutableLongList distinct()
    {
        MutableLongSet seenSoFar = new LongHashSet(this.size());
        return this.select(seenSoFar::add);
    }

    @Override
    public MutableLongList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public MutableList<LongLongPair> zipLong(LongIterable iterable)
    {
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<LongLongPair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        LongIterator iterator = iterable.longIterator();
        for (int i = 0; i < size && i < otherSize; i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    /**
     * Creates a new empty LongArrayList.
     *
     * @since 9.2.
     */
    public LongArrayList newEmpty()
    {
        return new LongArrayList();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> MutableList<LongObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<LongObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    @Override
    public MutableList<Long> boxed()
    {
        return new BoxedMutableLongList(this);
    }

    @Override
    public MutableLongStack toStack()
    {
        return LongStacks.mutable.withAll(this);
    }

    private class InternalLongIterator implements MutableLongIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;
        private int lastIndex = -1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != LongArrayList.this.size();
        }

        @Override
        public long next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            long next = LongArrayList.this.items[this.currentIndex];
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
            LongArrayList.this.removeAtIndex(this.lastIndex);
            this.currentIndex--;
            this.lastIndex = -1;
        }
    }

    @Override
    public Spliterator.OfLong spliterator()
    {
        int characteristics = Spliterator.ORDERED;
        return Spliterators.spliterator(this.items, 0, size, characteristics);
    }
}
