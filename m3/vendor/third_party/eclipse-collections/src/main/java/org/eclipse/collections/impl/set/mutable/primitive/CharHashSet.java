/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.mutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.CharSets;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableCharSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableCharSetSerializationProxy;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.impl.set.immutable.primitive.AbstractImmutableCharSet;
import org.eclipse.collections.impl.set.primitive.AbstractCharSet;

/**
 * This file was automatically generated from template file primitiveHashSet.stg.
 *
 * @since 3.0.
 */
public class CharHashSet extends AbstractCharSet implements MutableCharSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final char EMPTY = (char) 0;
    private static final char REMOVED = (char) 1;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 2;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private char[] table;
    private int occupiedWithData;
    private int occupiedWithSentinels;
    // The 32 bits of this integer indicate whether the items (char) 0 to (char) 31 are present in the set.
    private int zeroToThirtyOne;
    private int zeroToThirtyOneOccupied;
    private transient boolean copyOnWrite;

    public CharHashSet()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY);
    }

    public CharHashSet(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public CharHashSet(char... elements)
    {
        this();
        this.addAll(elements);
    }

    public CharHashSet(CharIterable elements)
    {
        this();
        this.addAll(elements);
    }

    public CharHashSet(CharHashSet set)
    {
        this.occupiedWithData = set.occupiedWithData;
        this.occupiedWithSentinels = set.occupiedWithSentinels;
        this.zeroToThirtyOneOccupied = set.zeroToThirtyOneOccupied;
        this.zeroToThirtyOne = set.zeroToThirtyOne;
        this.allocateTable(set.table.length);

        System.arraycopy(set.table, 0, this.table, 0, set.table.length);
    }

    private int smallestPowerOfTwoGreaterThan(int n)
    {
        return n > 1 ? Integer.highestOneBit(n - 1) << 1 : 1;
    }

    public static CharHashSet newSet(CharIterable source)
    {
        if (source instanceof CharHashSet)
        {
            return new CharHashSet((CharHashSet) source);
        }
        return new CharHashSet(source);
    }

    public static CharHashSet newSetWith(char... source)
    {
        return new CharHashSet(source);
    }

    private static boolean isBetweenZeroAndThirtyOne(char value)
    {
        return value >= '\0' && value <= (char) 31;
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result += (int) value;
            zeroToThirtyOne &= ~(1 << value);
        }
        if (this.table != null)
        {
            for (int i = 0; i < this.table.length; i++)
            {
                if (isNonSentinel(this.table[i]))
                {
                    result += (int) this.table[i];
                }
            }
        }
        return result;
    }

    @Override
    public int size()
    {
        return this.occupiedWithData + this.zeroToThirtyOneOccupied;
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);

            int count = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (count > 0)
                {
                    appendable.append(separator);
                }
                count++;
                appendable.append(String.valueOf(value));
                zeroToThirtyOne &= ~(1 << value);
            }

            for (char value : this.table)
            {
                if (isNonSentinel(value))
                {
                    if (count > 0)
                    {
                        appendable.append(separator);
                    }
                    count++;
                    appendable.append(String.valueOf(value));
                }
            }
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean add(char element)
    {
        if (isBetweenZeroAndThirtyOne(element))
        {
            int initial = this.zeroToThirtyOne;
            this.zeroToThirtyOne |= 1 << element;
            if (this.zeroToThirtyOne != initial)
            {
                this.zeroToThirtyOneOccupied++;
                return true;
            }
            return false;
        }

        int index = this.probe(element);

        if (this.table[index] == element)
        {
            // element already present in set
            return false;
        }

        if (this.copyOnWrite)
        {
            this.copyTable();
        }
        if (this.table[index] == REMOVED)
        {
            --this.occupiedWithSentinels;
        }
        this.table[index] = element;
        ++this.occupiedWithData;
        if (this.occupiedWithData + this.occupiedWithSentinels > this.maxOccupiedWithData())
        {
            this.rehashAndGrow();
        }
        return true;
    }

    @Override
    public boolean addAll(char... source)
    {
        int oldSize = this.size();
        for (char item : source)
        {
            this.add(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean addAll(CharIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof CharHashSet)
        {
            CharHashSet hashSet = (CharHashSet) source;
            this.zeroToThirtyOne |= hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (char item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.add(item);
                }
            }
        }
        else
        {
            CharIterator iterator = source.charIterator();
            while (iterator.hasNext())
            {
                char item = iterator.next();
                this.add(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean remove(char value)
    {
        if (isBetweenZeroAndThirtyOne(value))
        {
            return this.removeZeroToThirtyOne(value);
        }
        if (this.occupiedWithData == 0)
        {
            return false;
        }
        int index = this.probe(value);
        if (this.table[index] == value)
        {
            if (this.copyOnWrite)
            {
                this.copyTable();
            }
            this.table[index] = REMOVED;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;
            return true;
        }
        return false;
    }

    private boolean removeZeroToThirtyOne(char value)
    {
        int initial = this.zeroToThirtyOne;
        this.zeroToThirtyOne &= ~(1 << value);
        if (this.zeroToThirtyOne == initial)
        {
            return false;
        }
        this.zeroToThirtyOneOccupied--;
        return true;
    }

    @Override
    public boolean removeAll(CharIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof CharHashSet)
        {
            CharHashSet hashSet = (CharHashSet) source;
            this.zeroToThirtyOne &= ~hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (char item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.remove(item);
                }
            }
        }
        else
        {
            CharIterator iterator = source.charIterator();
            while (iterator.hasNext())
            {
                char item = iterator.next();
                this.remove(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(char... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (char item : source)
        {
            this.remove(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(CharIterable source)
    {
        int oldSize = this.size();
        final CharSet sourceSet = source instanceof CharSet ? (CharSet) source : source.toSet();
        CharHashSet retained = this.select(sourceSet::contains);
        if (retained.size() != oldSize)
        {
            this.zeroToThirtyOne = retained.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = retained.zeroToThirtyOneOccupied;
            this.occupiedWithData = retained.occupiedWithData;
            this.occupiedWithSentinels = retained.occupiedWithSentinels;
            this.table = retained.table;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(char... source)
    {
        return this.retainAll(CharHashSet.newSetWith(source));
    }

    @Override
    public void clear()
    {
        this.zeroToThirtyOneOccupied = 0;
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;

        this.zeroToThirtyOne = 0;
        if (this.copyOnWrite)
        {
            this.table = new char[this.table.length];
            this.copyOnWrite = false;
        }
        else
        {
            Arrays.fill(this.table, EMPTY);
        }
    }

    @Override
    public CharHashSet with(char element)
    {
        this.add(element);
        return this;
    }

    @Override
    public CharHashSet without(char element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public CharHashSet withAll(CharIterable elements)
    {
        this.addAll(elements);
        return this;
    }

    @Override
    public CharHashSet withoutAll(CharIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    @Override
    public MutableCharSet asUnmodifiable()
    {
        return new UnmodifiableCharSet(this);
    }

    @Override
    public MutableCharSet asSynchronized()
    {
        return new SynchronizedCharSet(this);
    }

    @Override
    public ImmutableCharSet toImmutable()
    {
        if (this.size() == 0)
        {
            return CharSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return CharSets.immutable.with(this.charIterator().next());
        }
        CharHashSet mutableSet;
        if (this.table.length == smallestPowerOfTwoGreaterThan(this.size() << 1)) // table is not too big
        {
            mutableSet = this;
            this.copyOnWrite = true;
        }
        else
        {
            mutableSet = new CharHashSet(this.size());
            mutableSet.addAll(this);
        }
        return new ImmutableCharHashSet(mutableSet.table, mutableSet.occupiedWithData, mutableSet.zeroToThirtyOne, mutableSet.zeroToThirtyOneOccupied);
    }

    @Override
    public MutableCharIterator charIterator()
    {
        return new InternalCharIterator();
    }

    @Override
    public char[] toArray()
    {
        char[] array = new char[this.size()];

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            array[j] = value;
            j++;
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int i = 0; i < this.table.length && j < this.size(); i++)
        {
            if (isNonSentinel(this.table[i]))
            {
                array[j] = this.table[i];
                j++;
            }
        }
        return array;
    }

    @Override
    public char[] toArray(char[] array)
    {
        if (array.length < this.size())
        {
            array = new char[this.size()];
        }

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            array[j] = value;
            j++;
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int i = 0; i < this.table.length && j < this.size(); i++)
        {
            if (isNonSentinel(this.table[i]))
            {
                array[j] = this.table[i];
                j++;
            }
        }
        return array;
    }

    @Override
    public boolean contains(char value)
    {
        if (isBetweenZeroAndThirtyOne(value))
        {
            int temp = this.zeroToThirtyOne;
            return ((temp >>> value) & 1) != 0;
        }
        return this.table[this.probe(value)] == value;
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            procedure.value(value);
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                procedure.value(value);
            }
        }
    }

    @Override
    public CharHashSet select(CharPredicate predicate)
    {
        return this.select(predicate, new CharHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableCharCollection> R select(CharPredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                if (predicate.accept(value))
                {
                    target.add(value);
                }
            }
        }
        return target;
    }

    @Override
    public CharHashSet reject(CharPredicate predicate)
    {
        return this.reject(predicate, new CharHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableCharCollection> R reject(CharPredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                if (!predicate.accept(value))
                {
                    target.add(value);
                }
            }
        }
        return target;
    }

    @Override
    public <V> MutableSet<V> collect(CharToObjectFunction<? extends V> function)
    {
        return this.collect(function, UnifiedSet.newSet(this.size()));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(CharToObjectFunction<? extends V> function, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            target.add(function.valueOf(value));
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                target.add(function.valueOf(value));
            }
        }
        return target;
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return value;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                if (predicate.accept(value))
                {
                    return value;
                }
            }
        }
        return ifNone;
    }

    @Override
    public int count(CharPredicate predicate)
    {
        int count = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                count++;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                if (predicate.accept(value))
                {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return true;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                if (predicate.accept(value))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                return false;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                if (!predicate.accept(value))
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public long sum()
    {
        long result = 0L;

        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result += value;
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                result += value;
            }
        }
        return result;
    }

    @Override
    public char max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        char max = (char) (31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne));
        boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

        for (char value : this.table)
        {
            if (isNonSentinel(value) && (!isMaxSet || max < value))
            {
                max = value;
                isMaxSet = true;
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
        char min = (char) Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
        boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

        for (char value : this.table)
        {
            if (isNonSentinel(value) && (!isMinSet || value < min))
            {
                min = value;
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public CharSet freeze()
    {
        if (this.size() == 0)
        {
            return CharSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return CharSets.immutable.with(this.charIterator().next());
        }
        this.copyOnWrite = true;
        return new ImmutableCharHashSet(this.table, this.occupiedWithData, this.zeroToThirtyOne, this.zeroToThirtyOneOccupied);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());

        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            out.writeChar(value);
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                out.writeChar(value);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();

        for (int i = 0; i < size; i++)
        {
            this.add(in.readChar());
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result = function.valueOf(result, value);
            zeroToThirtyOne &= ~(1 << value);
        }

        for (char value : this.table)
        {
            if (isNonSentinel(value))
            {
                result = function.valueOf(result, value);
            }
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
                result.add(CharSets.mutable.withAll(this));
            }
            else
            {
                CharIterator iterator = this.charIterator();
                while (iterator.hasNext())
                {
                    MutableCharSet batch = CharSets.mutable.empty();
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

    /**
     * Creates a new empty CharHashSet.
     *
     * @since 9.2.
     */
    public CharHashSet newEmpty()
    {
        return new CharHashSet();
    }

    /**
     * Rehashes every element in the set into a new backing table of the smallest possible size and eliminating removed sentinels.
     */
    public void compact()
    {
        this.rehash(this.smallestPowerOfTwoGreaterThan(this.size()));
    }

    private void rehashAndGrow()
    {
        int max = this.maxOccupiedWithData();
        int newCapacity = Math.max(max, smallestPowerOfTwoGreaterThan((this.occupiedWithData + 1) << 1));
        if (this.occupiedWithSentinels > 0 && (max >> 1) + (max >> 2) < this.occupiedWithData)
        {
            newCapacity <<= 1;
        }
        this.rehash(newCapacity);
    }

    private void rehash(int newCapacity)
    {
        int oldLength = this.table.length;
        char[] old = this.table;
        this.allocateTable(newCapacity);
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;

        for (int i = 0; i < oldLength; i++)
        {
            if (isNonSentinel(old[i]))
            {
                this.add(old[i]);
            }
        }
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.table = new char[sizeToAllocate];
    }

    // exposed for testing
    int probe(char element)
    {
        int index = this.spreadAndMask(element);
        char valueAtIndex = this.table[index];

        if (valueAtIndex == element || valueAtIndex == EMPTY)
        {
            return index;
        }

        int removedIndex = valueAtIndex == REMOVED ? index : -1;
        for (int i = 1; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.table.length - 1);
            valueAtIndex = this.table[nextIndex];
            if (valueAtIndex == element)
            {
                return nextIndex;
            }
            if (valueAtIndex == EMPTY)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (valueAtIndex == REMOVED && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeTwo(element, removedIndex);
    }

    int probeTwo(char element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element);
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.table.length - 1);
            char valueAtIndex = this.table[nextIndex];
            if (valueAtIndex == element)
            {
                return nextIndex;
            }
            if (valueAtIndex == EMPTY)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (valueAtIndex == REMOVED && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeThree(element, removedIndex);
    }

    int probeThree(char element, int removedIndex)
    {
        int nextIndex = Integer.reverse(SpreadFunctions.charSpreadOne(element));
        int spreadTwo = Integer.reverse(SpreadFunctions.charSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask(nextIndex + spreadTwo);
            char valueAtIndex = this.table[nextIndex];
            if (valueAtIndex == element)
            {
                return nextIndex;
            }
            if (valueAtIndex == EMPTY)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (valueAtIndex == REMOVED && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
    }

    // exposed for testing
    int spreadAndMask(char element)
    {
        int code = SpreadFunctions.charSpreadOne(element);
        return this.mask(code);
    }

    int spreadTwoAndMask(char element)
    {
        int code = SpreadFunctions.charSpreadTwo(element);
        return this.mask(code);
    }

    private int mask(int spread)
    {
        return spread & (this.table.length - 1);
    }

    private void copyTable()
    {
        this.copyOnWrite = false;
        char[] copy = new char[this.table.length];
        System.arraycopy(this.table, 0, copy, 0, this.table.length);
        this.table = copy;
    }

    private int maxOccupiedWithData()
    {
        return this.table.length >> 1;
    }

    private static boolean isNonSentinel(char value)
    {
        return value != EMPTY && value != REMOVED;
    }

    private static final class ImmutableCharHashSet extends AbstractImmutableCharSet implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private final char[] table;
        private final int occupied;
        // The 32 bits of this integer indicate whether the items (char) 0 to (char) 31 are present in the set.
        private final int zeroToThirtyOne;
        private final int zeroToThirtyOneOccupied;

        private ImmutableCharHashSet(char[] table, int occupied, int zeroToThirtyOne, int zeroToThirtyOneOccupied)
        {
            this.checkOptimizedSize(occupied + zeroToThirtyOneOccupied);
            this.occupied = occupied;
            this.zeroToThirtyOneOccupied = zeroToThirtyOneOccupied;
            this.zeroToThirtyOne = zeroToThirtyOne;
            this.table = table;
        }

        private void checkOptimizedSize(int length)
        {
            if (length <= 1)
            {
                throw new IllegalArgumentException("Use CharSets.immutable.with() to instantiate an optimized collection");
            }
        }

        public static ImmutableCharSet newSetWith(char... elements)
        {
            return CharHashSet.newSetWith(elements).toImmutable();
        }

        @Override
        public int hashCode()
        {
            int result = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result += (int) value;
                zeroToThirtyOne &= ~(1 << value);
            }
            if (this.table != null)
            {
                for (int i = 0; i < this.table.length; i++)
                {
                    if (isNonSentinel(this.table[i]))
                    {
                        result += (int) this.table[i];
                    }
                }
            }
            return result;
        }

        @Override
        public int size()
        {
            return this.occupied + this.zeroToThirtyOneOccupied;
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            try
            {
                appendable.append(start);

                int count = 0;
                int zeroToThirtyOne = this.zeroToThirtyOne;
                while (zeroToThirtyOne != 0)
                {
                    char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                    if (count > 0)
                    {
                        appendable.append(separator);
                    }
                    count++;
                    appendable.append(String.valueOf(value));
                    zeroToThirtyOne &= ~(1 << value);
                }

                for (char value : this.table)
                {
                    if (isNonSentinel(value))
                    {
                        if (count > 0)
                        {
                            appendable.append(separator);
                        }
                        count++;
                        appendable.append(String.valueOf(value));
                    }
                }
                appendable.append(end);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public CharIterator charIterator()
        {
            return new InternalCharIterator();
        }

        @Override
        public char[] toArray()
        {
            char[] array = new char[this.size()];

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                array[j] = value;
                j++;
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int i = 0; i < this.table.length && j < this.size(); i++)
            {
                if (isNonSentinel(this.table[i]))
                {
                    array[j] = this.table[i];
                    j++;
                }
            }
            return array;
        }

        @Override
        public char[] toArray(char[] array)
        {
            if (array.length < this.size())
            {
                array = new char[this.size()];
            }

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                array[j] = value;
                j++;
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int i = 0; i < this.table.length && j < this.size(); i++)
            {
                if (isNonSentinel(this.table[i]))
                {
                    array[j] = this.table[i];
                    j++;
                }
            }
            return array;
        }

        @Override
        public boolean contains(char value)
        {
            if (isBetweenZeroAndThirtyOne(value))
            {
                int temp = this.zeroToThirtyOne;
                return ((temp >>> value) & 1) != 0;
            }
            return this.table[this.probe(value)] == value;
        }

        @Override
        public void each(CharProcedure procedure)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                procedure.value(value);
                zeroToThirtyOne &= ~(1 << value);
            }

            for (char value : this.table)
            {
                if (isNonSentinel(value))
                {
                    procedure.value(value);
                }
            }
        }

        @Override
        public ImmutableCharSet select(CharPredicate predicate)
        {
            return this.select(predicate, new CharHashSet()).toImmutable();
        }

        @Override
        public ImmutableCharSet reject(CharPredicate predicate)
        {
            return this.reject(predicate, new CharHashSet()).toImmutable();
        }

        @Override
        public <V> ImmutableSet<V> collect(CharToObjectFunction<? extends V> function)
        {
            MutableSet<V> set = this.collect(function, UnifiedSet.newSet(this.size()));
            return set.toImmutable();
        }

        @Override
        public char detectIfNone(CharPredicate predicate, char ifNone)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return value;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (char value : this.table)
            {
                if (isNonSentinel(value))
                {
                    if (predicate.accept(value))
                    {
                        return value;
                    }
                }
            }
            return ifNone;
        }

        @Override
        public int count(CharPredicate predicate)
        {
            int count = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    count++;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (char value : this.table)
            {
                if (isNonSentinel(value))
                {
                    if (predicate.accept(value))
                    {
                        count++;
                    }
                }
            }
            return count;
        }

        @Override
        public boolean anySatisfy(CharPredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return true;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (char value : this.table)
            {
                if (isNonSentinel(value))
                {
                    if (predicate.accept(value))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean allSatisfy(CharPredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (!predicate.accept(value))
                {
                    return false;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (char value : this.table)
            {
                if (isNonSentinel(value))
                {
                    if (!predicate.accept(value))
                    {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean noneSatisfy(CharPredicate predicate)
        {
            return !this.anySatisfy(predicate);
        }

        @Override
        public long sum()
        {
            long result = 0L;

            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result += value;
                zeroToThirtyOne &= ~(1 << value);
            }

            for (char value : this.table)
            {
                if (isNonSentinel(value))
                {
                    result += value;
                }
            }
            return result;
        }

        @Override
        public char max()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            char max = (char) (31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne));
            boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

            for (char value : this.table)
            {
                if (isNonSentinel(value) && (!isMaxSet || max < value))
                {
                    max = value;
                    isMaxSet = true;
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
            char min = (char) Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
            boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

            for (char value : this.table)
            {
                if (isNonSentinel(value) && (!isMinSet || value < min))
                {
                    min = value;
                    isMinSet = true;
                }
            }
            return min;
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
        {
            T result = injectedValue;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                char value = (char) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result = function.valueOf(result, value);
                zeroToThirtyOne &= ~(1 << value);
            }

            for (char value : this.table)
            {
                if (isNonSentinel(value))
                {
                    result = function.valueOf(result, value);
                }
            }
            return result;
        }

        private Object writeReplace()
        {
            return new ImmutableCharSetSerializationProxy(this);
        }

        // exposed for testing
        int probe(char element)
        {
            int index = this.spreadAndMask(element);
            char valueAtIndex = this.table[index];

            if (valueAtIndex == element || valueAtIndex == EMPTY)
            {
                return index;
            }

            int removedIndex = valueAtIndex == REMOVED ? index : -1;
            for (int i = 1; i < INITIAL_LINEAR_PROBE; i++)
            {
                int nextIndex = (index + i) & (this.table.length - 1);
                valueAtIndex = this.table[nextIndex];
                if (valueAtIndex == element)
                {
                    return nextIndex;
                }
                if (valueAtIndex == EMPTY)
                {
                    return removedIndex == -1 ? nextIndex : removedIndex;
                }
                if (valueAtIndex == REMOVED && removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            return this.probeTwo(element, removedIndex);
        }

        int probeTwo(char element, int removedIndex)
        {
            int index = this.spreadTwoAndMask(element);
            for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
            {
                int nextIndex = (index + i) & (this.table.length - 1);
                char valueAtIndex = this.table[nextIndex];
                if (valueAtIndex == element)
                {
                    return nextIndex;
                }
                if (valueAtIndex == EMPTY)
                {
                    return removedIndex == -1 ? nextIndex : removedIndex;
                }
                if (valueAtIndex == REMOVED && removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            return this.probeThree(element, removedIndex);
        }

        int probeThree(char element, int removedIndex)
        {
            int nextIndex = Integer.reverse(SpreadFunctions.charSpreadOne(element));
            int spreadTwo = Integer.reverse(SpreadFunctions.charSpreadTwo(element)) | 1;

            while (true)
            {
                nextIndex = this.mask(nextIndex + spreadTwo);
                char valueAtIndex = this.table[nextIndex];
                if (valueAtIndex == element)
                {
                    return nextIndex;
                }
                if (valueAtIndex == EMPTY)
                {
                    return removedIndex == -1 ? nextIndex : removedIndex;
                }
                if (valueAtIndex == REMOVED && removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
        }

        // exposed for testing
        int spreadAndMask(char element)
        {
            int code = SpreadFunctions.charSpreadOne(element);
            return this.mask(code);
        }

        int spreadTwoAndMask(char element)
        {
            int code = SpreadFunctions.charSpreadTwo(element);
            return this.mask(code);
        }

        private int mask(int spread)
        {
            return spread & (this.table.length - 1);
        }

        private class InternalCharIterator implements CharIterator
        {
            private int count;
            private int position;
            private char zeroToThirtyOne;

            public boolean hasNext()
            {
                return this.count < ImmutableCharHashSet.this.size();
            }

            public char next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                while (this.zeroToThirtyOne < 32)
                {
                    if (ImmutableCharHashSet.this.contains(this.zeroToThirtyOne))
                    {
                        char result = this.zeroToThirtyOne;
                        this.zeroToThirtyOne++;
                        return result;
                    }
                    this.zeroToThirtyOne++;
                }

                char[] table = ImmutableCharHashSet.this.table;
                while (!isNonSentinel(table[this.position]))
                {
                    this.position++;
                }
                char result = table[this.position];
                this.position++;
                return result;
            }
        }
    }

    private class InternalCharIterator implements MutableCharIterator
    {
        private int count;
        private int position;
        private char zeroToThirtyOne;

        @Override
        public boolean hasNext()
        {
            return this.count < CharHashSet.this.size();
        }

        @Override
        public char next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;

            while (this.zeroToThirtyOne < 32)
            {
                if (CharHashSet.this.contains(this.zeroToThirtyOne))
                {
                    char result = this.zeroToThirtyOne;
                    this.zeroToThirtyOne++;
                    return result;
                }
                this.zeroToThirtyOne++;
            }

            char[] table = CharHashSet.this.table;
            while (!isNonSentinel(table[this.position]))
            {
                this.position++;
            }
            char result = table[this.position];
            this.position++;
            return result;
        }

        @Override
        public void remove()
        {
            if (this.count == 0)
            {
                throw new IllegalStateException();
            }
            char removeValue;
            if (this.zeroToThirtyOne <= (char) 32 && this.position == 0)
            {
                if (CharHashSet.this.zeroToThirtyOne != (CharHashSet.this.zeroToThirtyOne | 1 << (this.zeroToThirtyOne - 1)))
                {
                    throw new IllegalStateException();
                }
                removeValue = (char) (this.zeroToThirtyOne - 1);
            }
            else if (CharHashSet.this.table[this.position - 1] == REMOVED)
            {
                throw new IllegalStateException();
            }
            else
            {
                removeValue = CharHashSet.this.table[this.position - 1];
            }
            if (CharHashSet.isBetweenZeroAndThirtyOne(removeValue))
            {
                CharHashSet.this.removeZeroToThirtyOne(removeValue);
            }
            else if (CharHashSet.this.table[this.position - 1] == removeValue)
            {
                if (CharHashSet.this.copyOnWrite)
                {
                    CharHashSet.this.copyTable();
                }
                CharHashSet.this.table[position - 1] = REMOVED;
                CharHashSet.this.occupiedWithData--;
                CharHashSet.this.occupiedWithSentinels++;
            }

            this.count--;
        }
    }
}
