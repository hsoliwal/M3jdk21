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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.comparator.primitive.ByteComparator;
import org.eclipse.collections.api.block.function.primitive.ObjectByteIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;
import org.eclipse.collections.api.tuple.primitive.ByteObjectPair;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseByteIterable;
import org.eclipse.collections.impl.primitive.AbstractByteIterable;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.primitive.ByteQuickSort;

/**
 * ByteArrayList is similar to FastList, and is memory-optimized for byte primitives.
 * This file was automatically generated from template file primitiveArrayList.stg.
 *
 * @since 3.0.
 */
public class ByteArrayList extends AbstractByteIterable
        implements MutableByteList, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final byte[] DEFAULT_SIZED_EMPTY_ARRAY = {};
    private static final byte[] ZERO_SIZED_ARRAY = {};
    private static final int MAXIMUM_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    protected int size;
    protected transient byte[] items = DEFAULT_SIZED_EMPTY_ARRAY;

    public ByteArrayList()
    {
    }

    public ByteArrayList(int initialCapacity)
    {
        this.items = initialCapacity == 0 ? ZERO_SIZED_ARRAY : new byte[initialCapacity];
    }

    public ByteArrayList(byte... array)
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
    public static ByteArrayList newListWith(byte... elements)
    {
        return new ByteArrayList(elements);
    }

    public static ByteArrayList newList(ByteIterable source)
    {
        return ByteArrayList.newListWith(source.toArray());
    }

    public static ByteArrayList newWithNValues(int size, byte value)
    {
        ByteArrayList newList = new ByteArrayList(size);
        newList.size = size;
        Arrays.fill(newList.items, value);
        return newList;
    }

    /**
     * Creates a new list by first copying the array passed in.
     */
    public static ByteArrayList wrapCopy(byte... array)
    {
        byte[] newArray = new byte[array.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return new ByteArrayList(newArray);
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public void clear()
    {
        Arrays.fill(this.items, 0, size, (byte) 0);
        this.size = 0;
    }

    @Override
    public boolean contains(byte value)
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
    public byte get(int index)
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
    public byte getFirst()
    {
        this.checkEmpty();
        return this.items[0];
    }

    @Override
    public byte getLast()
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
    public int indexOf(byte value)
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
    public int lastIndexOf(byte value)
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

    private byte[] copyItemsWithNewCapacity(int newCapacity)
    {
        byte[] newItems = new byte[newCapacity];
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
            this.items = new byte[10];
        }
        else
        {
            this.transferItemsToNewArrayWithCapacity(this.sizePlusFiftyPercent(this.size));
        }
    }

    @Override
    public boolean add(byte newItem)
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
    public boolean addAll(byte... source)
    {
        if (source.length < 1)
        {
            return false;
        }
        this.copyItems(source.length, source);
        return true;
    }

    @Override
    public boolean addAll(ByteIterable source)
    {
        if (source instanceof ByteArrayList)
        {
            if (source.isEmpty())
            {
                return false;
            }
            ByteArrayList other = (ByteArrayList) source;
            this.copyItems(other.size(), other.items);
            return true;
        }
        return this.addAll(source.toArray());
    }

    private void copyItems(int sourceSize, byte[] source)
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
    public void addAtIndex(int index, byte element)
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

    private void addAtIndexLessThanSize(int index, byte element)
    {
        int oldSize = this.size;
        this.size++;
        if (this.items.length == oldSize)
        {
            byte[] newItems = new byte[this.sizePlusFiftyPercent(oldSize)];
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
    public boolean addAllAtIndex(int index, byte... source)
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
    public boolean addAllAtIndex(int index, ByteIterable source)
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
    public boolean remove(byte value)
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
    public boolean removeIf(BytePredicate predicate)
    {
        int currentFilledIndex = 0;
        for (int i = 0; i < this.size; i++)
        {
            byte item = this.items[i];
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
            this.items[i] = (byte) 0;
        }
        this.size = newCurrentFilledIndex;
    }

    @Override
    public boolean removeAll(ByteIterable source)
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
    public boolean removeAll(byte... source)
    {
        ByteHashSet set = ByteHashSet.newSetWith(source);
        byte[] newItems = new byte[this.size];
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
    public boolean retainAll(ByteIterable source)
    {
        int oldSize = this.size();
        final ByteSet sourceSet = source instanceof ByteSet ? (ByteSet) source : source.toSet();
        ByteArrayList retained = this.select(sourceSet::contains);
        this.size = retained.size;
        this.items = retained.items;
        return oldSize != this.size();
    }

    @Override
    public boolean retainAll(byte... source)
    {
        return this.retainAll(ByteHashSet.newSetWith(source));
    }

    @Override
    public byte removeAtIndex(int index)
    {
        byte previous = this.get(index);
        int totalOffset = this.size - index - 1;
        if (totalOffset > 0)
        {
            System.arraycopy(this.items, index + 1, this.items, index, totalOffset);
        }
        --this.size;
        this.items[this.size] = (byte) 0;
        return previous;
    }

    @Override
    public byte set(int index, byte element)
    {
        byte previous = this.get(index);
        this.items[index] = element;
        return previous;
    }

    @Override
    public void swap(int index1, int index2)
    {
        byte value = this.get(index1);
        this.items[index1] = this.items[index2];
        this.items[index2] = value;
    }

    @Override
    public ByteArrayList with(byte element)
    {
        this.add(element);
        return this;
    }

    @Override
    public ByteArrayList without(byte element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public ByteArrayList withAll(ByteIterable elements)
    {
        this.addAll(elements.toArray());
        return this;
    }

    @Override
    public ByteArrayList withoutAll(ByteIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    public ByteArrayList with(byte element1, byte element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public ByteArrayList with(byte element1, byte element2, byte element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    public ByteArrayList with(byte element1, byte element2, byte element3, byte... elements)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this.withArrayCopy(elements, 0, elements.length);
    }

    private ByteArrayList withArrayCopy(byte[] elements, int begin, int length)
    {
        this.ensureCapacity(this.size + length);
        System.arraycopy(elements, begin, this.items, this.size, length);
        this.size += length;
        return this;
    }

    @Override
    public MutableByteIterator byteIterator()
    {
        return new InternalByteIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i]);
        }
    }

    @Override
    public void forEachWithIndex(ByteIntProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectByteIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
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
                result.add(ByteLists.mutable.withAll(this));
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
                    result.add(batch);
                }
            }
        }
        return result;
    }

    @Override
    public int count(BytePredicate predicate)
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
    public boolean anySatisfy(BytePredicate predicate)
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
    public boolean allSatisfy(BytePredicate predicate)
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
    public ByteArrayList select(BytePredicate predicate)
    {
        return this.select(predicate, new ByteArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableByteCollection> R select(BytePredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            byte item = this.items[i];
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public ByteArrayList reject(BytePredicate predicate)
    {
        return this.reject(predicate, new ByteArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableByteCollection> R reject(BytePredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            byte item = this.items[i];
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
        for (int i = 0; i < this.size; i++)
        {
            byte item = this.items[i];
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> MutableList<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return this.collect(function, Lists.mutable.withInitialCapacity(this.size));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(ByteToObjectFunction<? extends V> function, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(function.valueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public byte max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        byte max = this.items[0];
        for (int i = 1; i < this.size; i++)
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
    public byte min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        byte min = this.items[0];
        for (int i = 1; i < this.size; i++)
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
    public long dotProduct(ByteList list)
    {
        if (this.size != list.size())
        {
            throw new IllegalArgumentException("Lists used in dotProduct must be the same size");
        }
        long sum = 0L;
        for (int i = 0; i < this.size; i++)
        {
            sum += (long) this.items[i] * list.get(i);
        }
        return sum;
    }

    @Override
    public byte[] toArray()
    {
        byte[] newItems = new byte[this.size];
        System.arraycopy(this.items, 0, newItems, 0, this.size);
        return newItems;
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        if (target.length < this.size)
        {
            target = new byte[this.size];
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
        if (!(otherList instanceof ByteList))
        {
            return false;
        }
        ByteList list = (ByteList) otherList;
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
            byte item = this.items[i];
            hashCode = 31 * hashCode + (int) item;
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

    @Override
    public MutableByteList asUnmodifiable()
    {
        return new UnmodifiableByteList(this);
    }

    @Override
    public MutableByteList asSynchronized()
    {
        return new SynchronizedByteList(this);
    }

    @Override
    public ImmutableByteList toImmutable()
    {
        return ByteLists.immutable.withAll(this);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size);
        for (int i = 0; i < this.size; i++)
        {
            out.writeByte(this.items[i]);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        this.size = in.readInt();
        this.items = new byte[this.size];
        for (int i = 0; i < this.size; i++)
        {
            this.items[i] = in.readByte();
        }
    }

    @Override
    public LazyByteIterable asReversed()
    {
        return ReverseByteIterable.adapt(this);
    }

    @Override
    public ByteArrayList reverseThis()
    {
        int endIndex = this.size - 1;
        for (int i = 0; i < this.size / 2; i++)
        {
            byte tempSwapValue = this.items[i];
            this.items[i] = this.items[endIndex - i];
            this.items[endIndex - i] = tempSwapValue;
        }
        return this;
    }

    @Override
    public ByteArrayList sortThis()
    {
        Arrays.sort(this.items, 0, this.size);
        return this;
    }

    @Override
    public ByteArrayList sortThis(ByteComparator comparator)
    {
        ByteQuickSort.sort(this.items, 0, this.size() - 1, comparator);
        return this;
    }

    @Override
    public ByteArrayList toReversed()
    {
        return ByteArrayList.newList(this.asReversed());
    }

    @Override
    public int binarySearch(byte value)
    {
        return Arrays.binarySearch(this.items, 0, this.size, value);
    }

    @Override
    public MutableByteList distinct()
    {
        ByteArrayList target = new ByteArrayList();
        MutableByteSet seenSoFar = new ByteHashSet(this.size());

        for (int i = 0; i < this.size; i++)
        {
            byte each = this.items[i];
            if (seenSoFar.add(each))
            {
                target.add(each);
            }
        }
        return target;
    }

    @Override
    public MutableByteList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public MutableList<ByteBytePair> zipByte(ByteIterable iterable)
    {
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<ByteBytePair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        ByteIterator iterator = iterable.byteIterator();
        for (int i = 0; i < size && i < otherSize; i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    /**
     * Creates a new empty ByteArrayList.
     *
     * @since 9.2.
     */
    public ByteArrayList newEmpty()
    {
        return new ByteArrayList();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> MutableList<ByteObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<ByteObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    private class InternalByteIterator implements MutableByteIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;
        private int lastIndex = -1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != ByteArrayList.this.size();
        }

        @Override
        public byte next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            byte next = ByteArrayList.this.items[this.currentIndex];
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
            ByteArrayList.this.removeAtIndex(this.lastIndex);
            this.currentIndex--;
            this.lastIndex = -1;
        }
    }
}
