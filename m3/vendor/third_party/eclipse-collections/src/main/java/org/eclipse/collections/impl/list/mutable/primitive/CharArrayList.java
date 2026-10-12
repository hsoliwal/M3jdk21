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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.comparator.primitive.CharComparator;
import org.eclipse.collections.api.block.function.primitive.ObjectCharIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.CharList;
import org.eclipse.collections.api.list.primitive.ImmutableCharList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;
import org.eclipse.collections.api.tuple.primitive.CharObjectPair;
import org.eclipse.collections.impl.factory.primitive.CharLists;
import org.eclipse.collections.impl.lazy.primitive.ReverseCharIterable;
import org.eclipse.collections.impl.primitive.AbstractCharIterable;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.primitive.CharQuickSort;

/**
 * CharArrayList is similar to FastList, and is memory-optimized for char primitives.
 * This file was automatically generated from template file primitiveArrayList.stg.
 *
 * @since 3.0.
 */
public class CharArrayList extends AbstractCharIterable
        implements MutableCharList, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final char[] DEFAULT_SIZED_EMPTY_ARRAY = {};
    private static final char[] ZERO_SIZED_ARRAY = {};
    private static final int MAXIMUM_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    protected int size;
    protected transient char[] items = DEFAULT_SIZED_EMPTY_ARRAY;

    public CharArrayList()
    {
    }

    public CharArrayList(int initialCapacity)
    {
        this.items = initialCapacity == 0 ? ZERO_SIZED_ARRAY : new char[initialCapacity];
    }

    public CharArrayList(char... array)
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
    public static CharArrayList newListWith(char... elements)
    {
        return new CharArrayList(elements);
    }

    public static CharArrayList newList(CharIterable source)
    {
        return CharArrayList.newListWith(source.toArray());
    }

    public static CharArrayList newWithNValues(int size, char value)
    {
        CharArrayList newList = new CharArrayList(size);
        newList.size = size;
        Arrays.fill(newList.items, value);
        return newList;
    }

    /**
     * Creates a new list by first copying the array passed in.
     */
    public static CharArrayList wrapCopy(char... array)
    {
        char[] newArray = new char[array.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return new CharArrayList(newArray);
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public void clear()
    {
        Arrays.fill(this.items, 0, size, '\0');
        this.size = 0;
    }

    @Override
    public boolean contains(char value)
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
    public char get(int index)
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
    public char getFirst()
    {
        this.checkEmpty();
        return this.items[0];
    }

    @Override
    public char getLast()
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
    public int indexOf(char value)
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
    public int lastIndexOf(char value)
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

    private char[] copyItemsWithNewCapacity(int newCapacity)
    {
        char[] newItems = new char[newCapacity];
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
            this.items = new char[10];
        }
        else
        {
            this.transferItemsToNewArrayWithCapacity(this.sizePlusFiftyPercent(this.size));
        }
    }

    @Override
    public boolean add(char newItem)
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
    public boolean addAll(char... source)
    {
        if (source.length < 1)
        {
            return false;
        }
        this.copyItems(source.length, source);
        return true;
    }

    @Override
    public boolean addAll(CharIterable source)
    {
        if (source instanceof CharArrayList)
        {
            if (source.isEmpty())
            {
                return false;
            }
            CharArrayList other = (CharArrayList) source;
            this.copyItems(other.size(), other.items);
            return true;
        }
        return this.addAll(source.toArray());
    }

    private void copyItems(int sourceSize, char[] source)
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
    public void addAtIndex(int index, char element)
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

    private void addAtIndexLessThanSize(int index, char element)
    {
        int oldSize = this.size;
        this.size++;
        if (this.items.length == oldSize)
        {
            char[] newItems = new char[this.sizePlusFiftyPercent(oldSize)];
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
    public boolean addAllAtIndex(int index, char... source)
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
    public boolean addAllAtIndex(int index, CharIterable source)
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
    public boolean remove(char value)
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
    public boolean removeIf(CharPredicate predicate)
    {
        int currentFilledIndex = 0;
        for (int i = 0; i < this.size; i++)
        {
            char item = this.items[i];
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
            this.items[i] = '\0';
        }
        this.size = newCurrentFilledIndex;
    }

    @Override
    public boolean removeAll(CharIterable source)
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
    public boolean removeAll(char... source)
    {
        CharHashSet set = CharHashSet.newSetWith(source);
        char[] newItems = new char[this.size];
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
    public boolean retainAll(CharIterable source)
    {
        int oldSize = this.size();
        final CharSet sourceSet = source instanceof CharSet ? (CharSet) source : source.toSet();
        CharArrayList retained = this.select(sourceSet::contains);
        this.size = retained.size;
        this.items = retained.items;
        return oldSize != this.size();
    }

    @Override
    public boolean retainAll(char... source)
    {
        return this.retainAll(CharHashSet.newSetWith(source));
    }

    @Override
    public char removeAtIndex(int index)
    {
        char previous = this.get(index);
        int totalOffset = this.size - index - 1;
        if (totalOffset > 0)
        {
            System.arraycopy(this.items, index + 1, this.items, index, totalOffset);
        }
        --this.size;
        this.items[this.size] = '\0';
        return previous;
    }

    @Override
    public char set(int index, char element)
    {
        char previous = this.get(index);
        this.items[index] = element;
        return previous;
    }

    @Override
    public void swap(int index1, int index2)
    {
        char value = this.get(index1);
        this.items[index1] = this.items[index2];
        this.items[index2] = value;
    }

    @Override
    public CharArrayList with(char element)
    {
        this.add(element);
        return this;
    }

    @Override
    public CharArrayList without(char element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public CharArrayList withAll(CharIterable elements)
    {
        this.addAll(elements.toArray());
        return this;
    }

    @Override
    public CharArrayList withoutAll(CharIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    public CharArrayList with(char element1, char element2)
    {
        this.add(element1);
        this.add(element2);
        return this;
    }

    public CharArrayList with(char element1, char element2, char element3)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this;
    }

    public CharArrayList with(char element1, char element2, char element3, char... elements)
    {
        this.add(element1);
        this.add(element2);
        this.add(element3);
        return this.withArrayCopy(elements, 0, elements.length);
    }

    private CharArrayList withArrayCopy(char[] elements, int begin, int length)
    {
        this.ensureCapacity(this.size + length);
        System.arraycopy(elements, begin, this.items, this.size, length);
        this.size += length;
        return this;
    }

    @Override
    public MutableCharIterator charIterator()
    {
        return new InternalCharIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i]);
        }
    }

    @Override
    public void forEachWithIndex(CharIntProcedure procedure)
    {
        for (int i = 0; i < this.size; i++)
        {
            procedure.value(this.items[i], i);
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.valueOf(result, this.items[i]);
        }
        return result;
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectCharIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        for (int i = 0; i < this.size; i++)
        {
            result = function.valueOf(result, this.items[i], i);
        }
        return result;
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<CharIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(CharLists.mutable.withAll(this));
            }
            else
            {
                CharIterator iterator = this.charIterator();
                while (iterator.hasNext())
                {
                    MutableCharList batch = CharLists.mutable.empty();
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
    public int count(CharPredicate predicate)
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
    public boolean anySatisfy(CharPredicate predicate)
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
    public boolean allSatisfy(CharPredicate predicate)
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
    public CharArrayList select(CharPredicate predicate)
    {
        return this.select(predicate, new CharArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableCharCollection> R select(CharPredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            char item = this.items[i];
            if (predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public CharArrayList reject(CharPredicate predicate)
    {
        return this.reject(predicate, new CharArrayList());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableCharCollection> R reject(CharPredicate predicate, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            char item = this.items[i];
            if (!predicate.accept(item))
            {
                target.add(item);
            }
        }
        return target;
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        for (int i = 0; i < this.size; i++)
        {
            char item = this.items[i];
            if (predicate.accept(item))
            {
                return item;
            }
        }
        return ifNone;
    }

    @Override
    public <V> MutableList<V> collect(CharToObjectFunction<? extends V> function)
    {
        return this.collect(function, Lists.mutable.withInitialCapacity(this.size));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(CharToObjectFunction<? extends V> function, R target)
    {
        for (int i = 0; i < this.size; i++)
        {
            target.add(function.valueOf(this.items[i]));
        }
        return target;
    }

    @Override
    public char max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        char max = this.items[0];
        for (int i = 1; i < this.size; i++)
        {
            char value = this.items[i];
            if (max < value)
            {
                max = value;
            }
        }
        return max;
    }

    @Override
    public char min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        char min = this.items[0];
        for (int i = 1; i < this.size; i++)
        {
            char value = this.items[i];
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
    public long dotProduct(CharList list)
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
    public char[] toArray()
    {
        char[] newItems = new char[this.size];
        System.arraycopy(this.items, 0, newItems, 0, this.size);
        return newItems;
    }

    @Override
    public char[] toArray(char[] target)
    {
        if (target.length < this.size)
        {
            target = new char[this.size];
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
        if (!(otherList instanceof CharList))
        {
            return false;
        }
        CharList list = (CharList) otherList;
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
            char item = this.items[i];
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
                char value = this.items[i];
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
    public MutableCharList asUnmodifiable()
    {
        return new UnmodifiableCharList(this);
    }

    @Override
    public MutableCharList asSynchronized()
    {
        return new SynchronizedCharList(this);
    }

    @Override
    public ImmutableCharList toImmutable()
    {
        return CharLists.immutable.withAll(this);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size);
        for (int i = 0; i < this.size; i++)
        {
            out.writeChar(this.items[i]);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        this.size = in.readInt();
        this.items = new char[this.size];
        for (int i = 0; i < this.size; i++)
        {
            this.items[i] = in.readChar();
        }
    }

    @Override
    public LazyCharIterable asReversed()
    {
        return ReverseCharIterable.adapt(this);
    }

    @Override
    public CharArrayList reverseThis()
    {
        int endIndex = this.size - 1;
        for (int i = 0; i < this.size / 2; i++)
        {
            char tempSwapValue = this.items[i];
            this.items[i] = this.items[endIndex - i];
            this.items[endIndex - i] = tempSwapValue;
        }
        return this;
    }

    @Override
    public CharArrayList sortThis()
    {
        Arrays.sort(this.items, 0, this.size);
        return this;
    }

    @Override
    public CharArrayList sortThis(CharComparator comparator)
    {
        CharQuickSort.sort(this.items, 0, this.size() - 1, comparator);
        return this;
    }

    @Override
    public CharArrayList toReversed()
    {
        return CharArrayList.newList(this.asReversed());
    }

    @Override
    public int binarySearch(char value)
    {
        return Arrays.binarySearch(this.items, 0, this.size, value);
    }

    @Override
    public MutableCharList distinct()
    {
        CharArrayList target = new CharArrayList();
        MutableCharSet seenSoFar = new CharHashSet(this.size());

        for (int i = 0; i < this.size; i++)
        {
            char each = this.items[i];
            if (seenSoFar.add(each))
            {
                target.add(each);
            }
        }
        return target;
    }

    @Override
    public MutableCharList subList(int fromIndex, int toIndex)
    {
        throw new UnsupportedOperationException("subList not yet implemented!");
    }

    /**
     * @since 9.1.
     */
    @Override
    public MutableList<CharCharPair> zipChar(CharIterable iterable)
    {
        int size = this.size();
        int otherSize = iterable.size();
        MutableList<CharCharPair> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        CharIterator iterator = iterable.charIterator();
        for (int i = 0; i < size && i < otherSize; i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    /**
     * Creates a new empty CharArrayList.
     *
     * @since 9.2.
     */
    public CharArrayList newEmpty()
    {
        return new CharArrayList();
    }

    /**
     * @since 9.1.
     */
    @Override
    public <T> MutableList<CharObjectPair<T>> zip(Iterable<T> iterable)
    {
        int size = this.size();
        int otherSize = Iterate.sizeOf(iterable);
        MutableList<CharObjectPair<T>> target = Lists.mutable.withInitialCapacity(Math.min(size, otherSize));
        Iterator<T> iterator = iterable.iterator();
        for (int i = 0; i < size && iterator.hasNext(); i++)
        {
            target.add(PrimitiveTuples.pair(this.items[i], iterator.next()));
        }
        return target;
    }

    private class InternalCharIterator implements MutableCharIterator
    {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;
        private int lastIndex = -1;

        @Override
        public boolean hasNext()
        {
            return this.currentIndex != CharArrayList.this.size();
        }

        @Override
        public char next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            char next = CharArrayList.this.items[this.currentIndex];
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
            CharArrayList.this.removeAtIndex(this.lastIndex);
            this.currentIndex--;
            this.lastIndex = -1;
        }
    }
}
