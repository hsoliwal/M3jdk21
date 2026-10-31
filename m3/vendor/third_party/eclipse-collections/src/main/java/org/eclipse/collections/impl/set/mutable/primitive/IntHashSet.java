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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableIntSetSerializationProxy;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.impl.set.immutable.primitive.AbstractImmutableIntSet;
import org.eclipse.collections.impl.set.primitive.AbstractIntSet;

/**
 * This file was automatically generated from template file primitiveHashSet.stg.
 *
 * @since 3.0.
 */
public class IntHashSet extends AbstractIntSet implements MutableIntSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int EMPTY = 0;
    private static final int REMOVED = 1;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 4;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private int[] table;
    private int occupiedWithData;
    private int occupiedWithSentinels;
    // The 32 bits of this integer indicate whether the items 0 to 31 are present in the set.
    private int zeroToThirtyOne;
    private int zeroToThirtyOneOccupied;
    private transient boolean copyOnWrite;

    public IntHashSet()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY);
    }

    public IntHashSet(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public IntHashSet(int... elements)
    {
        this();
        this.addAll(elements);
    }

    public IntHashSet(IntIterable elements)
    {
        this();
        this.addAll(elements);
    }

    public IntHashSet(IntHashSet set)
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

    public static IntHashSet newSet(IntIterable source)
    {
        if (source instanceof IntHashSet)
        {
            return new IntHashSet((IntHashSet) source);
        }
        return new IntHashSet(source);
    }

    public static IntHashSet newSetWith(int... source)
    {
        return new IntHashSet(source);
    }

    private static boolean isBetweenZeroAndThirtyOne(int value)
    {
        return value >= 0 && value <= 31;
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result += value;
            zeroToThirtyOne &= ~(1 << value);
        }
        if (this.table != null)
        {
            for (int i = 0; i < this.table.length; i++)
            {
                if (isNonSentinel(this.table[i]))
                {
                    result += this.table[i];
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
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (count > 0)
                {
                    appendable.append(separator);
                }
                count++;
                appendable.append(String.valueOf(value));
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int value : this.table)
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
    public boolean add(int element)
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
    public boolean addAll(int... source)
    {
        int oldSize = this.size();
        for (int item : source)
        {
            this.add(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean addAll(IntIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof IntHashSet)
        {
            IntHashSet hashSet = (IntHashSet) source;
            this.zeroToThirtyOne |= hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (int item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.add(item);
                }
            }
        }
        else
        {
            IntIterator iterator = source.intIterator();
            while (iterator.hasNext())
            {
                int item = iterator.next();
                this.add(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean remove(int value)
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

    private boolean removeZeroToThirtyOne(int value)
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
    public boolean removeAll(IntIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof IntHashSet)
        {
            IntHashSet hashSet = (IntHashSet) source;
            this.zeroToThirtyOne &= ~hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (int item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.remove(item);
                }
            }
        }
        else
        {
            IntIterator iterator = source.intIterator();
            while (iterator.hasNext())
            {
                int item = iterator.next();
                this.remove(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(int... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (int item : source)
        {
            this.remove(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(IntIterable source)
    {
        int oldSize = this.size();
        final IntSet sourceSet = source instanceof IntSet ? (IntSet) source : source.toSet();
        IntHashSet retained = this.select(sourceSet::contains);
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
    public boolean retainAll(int... source)
    {
        return this.retainAll(IntHashSet.newSetWith(source));
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
            this.table = new int[this.table.length];
            this.copyOnWrite = false;
        }
        else
        {
            Arrays.fill(this.table, EMPTY);
        }
    }

    @Override
    public IntHashSet with(int element)
    {
        this.add(element);
        return this;
    }

    @Override
    public IntHashSet without(int element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public IntHashSet withAll(IntIterable elements)
    {
        this.addAll(elements);
        return this;
    }

    @Override
    public IntHashSet withoutAll(IntIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    @Override
    public MutableIntSet asUnmodifiable()
    {
        return new UnmodifiableIntSet(this);
    }

    @Override
    public MutableIntSet asSynchronized()
    {
        return new SynchronizedIntSet(this);
    }

    @Override
    public ImmutableIntSet toImmutable()
    {
        if (this.size() == 0)
        {
            return IntSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return IntSets.immutable.with(this.intIterator().next());
        }
        IntHashSet mutableSet;
        if (this.table.length == smallestPowerOfTwoGreaterThan(this.size() << 1)) // table is not too big
        {
            mutableSet = this;
            this.copyOnWrite = true;
        }
        else
        {
            mutableSet = new IntHashSet(this.size());
            mutableSet.addAll(this);
        }
        return new ImmutableIntHashSet(mutableSet.table, mutableSet.occupiedWithData, mutableSet.zeroToThirtyOne, mutableSet.zeroToThirtyOneOccupied);
    }

    @Override
    public MutableIntIterator intIterator()
    {
        return new InternalIntIterator();
    }

    @Override
    public int[] toArray()
    {
        int[] array = new int[this.size()];

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
    public int[] toArray(int[] array)
    {
        if (array.length < this.size())
        {
            array = new int[this.size()];
        }

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
    public boolean contains(int value)
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
    public void each(IntProcedure procedure)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            procedure.value(value);
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
        {
            if (isNonSentinel(value))
            {
                procedure.value(value);
            }
        }
    }

    @Override
    public IntHashSet select(IntPredicate predicate)
    {
        return this.select(predicate, new IntHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableIntCollection> R select(IntPredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
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
    public IntHashSet reject(IntPredicate predicate)
    {
        return this.reject(predicate, new IntHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableIntCollection> R reject(IntPredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
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
    public <V> MutableSet<V> collect(IntToObjectFunction<? extends V> function)
    {
        return this.collect(function, UnifiedSet.newSet(this.size()));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(IntToObjectFunction<? extends V> function, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            target.add(function.valueOf(value));
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
        {
            if (isNonSentinel(value))
            {
                target.add(function.valueOf(value));
            }
        }
        return target;
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return value;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
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
    public int count(IntPredicate predicate)
    {
        int count = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                count++;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
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
    public boolean anySatisfy(IntPredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return true;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
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
    public boolean allSatisfy(IntPredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                return false;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
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
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result += value;
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
        {
            if (isNonSentinel(value))
            {
                result += value;
            }
        }
        return result;
    }

    @Override
    public int max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        int max = 31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne);
        boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

        for (int value : this.table)
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
    public int min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        int min = Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
        boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

        for (int value : this.table)
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
    public IntSet freeze()
    {
        if (this.size() == 0)
        {
            return IntSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return IntSets.immutable.with(this.intIterator().next());
        }
        this.copyOnWrite = true;
        return new ImmutableIntHashSet(this.table, this.occupiedWithData, this.zeroToThirtyOne, this.zeroToThirtyOneOccupied);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());

        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            out.writeInt(value);
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
        {
            if (isNonSentinel(value))
            {
                out.writeInt(value);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();

        for (int i = 0; i < size; i++)
        {
            this.add(in.readInt());
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result = function.valueOf(result, value);
            zeroToThirtyOne &= ~(1 << value);
        }

        for (int value : this.table)
        {
            if (isNonSentinel(value))
            {
                result = function.valueOf(result, value);
            }
        }
        return result;
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<IntIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            if (this.size() <= size)
            {
                result.add(IntSets.mutable.withAll(this));
            }
            else
            {
                IntIterator iterator = this.intIterator();
                while (iterator.hasNext())
                {
                    MutableIntSet batch = IntSets.mutable.empty();
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
     * Creates a new empty IntHashSet.
     *
     * @since 9.2.
     */
    public IntHashSet newEmpty()
    {
        return new IntHashSet();
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
        int[] old = this.table;
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
        this.table = new int[sizeToAllocate];
    }

    // exposed for testing
    int probe(int element)
    {
        int index = this.spreadAndMask(element);
        int valueAtIndex = this.table[index];

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

    int probeTwo(int element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element);
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.table.length - 1);
            int valueAtIndex = this.table[nextIndex];
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

    int probeThree(int element, int removedIndex)
    {
        int nextIndex = Integer.reverse(SpreadFunctions.intSpreadOne(element));
        int spreadTwo = Integer.reverse(SpreadFunctions.intSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask(nextIndex + spreadTwo);
            int valueAtIndex = this.table[nextIndex];
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
    int spreadAndMask(int element)
    {
        int code = SpreadFunctions.intSpreadOne(element);
        return this.mask(code);
    }

    int spreadTwoAndMask(int element)
    {
        int code = SpreadFunctions.intSpreadTwo(element);
        return this.mask(code);
    }

    private int mask(int spread)
    {
        return spread & (this.table.length - 1);
    }

    private void copyTable()
    {
        this.copyOnWrite = false;
        int[] copy = new int[this.table.length];
        System.arraycopy(this.table, 0, copy, 0, this.table.length);
        this.table = copy;
    }

    private int maxOccupiedWithData()
    {
        return this.table.length >> 1;
    }

    private static boolean isNonSentinel(int value)
    {
        return value != EMPTY && value != REMOVED;
    }

    private static final class ImmutableIntHashSet extends AbstractImmutableIntSet implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private final int[] table;
        private final int occupied;
        // The 32 bits of this integer indicate whether the items 0 to 31 are present in the set.
        private final int zeroToThirtyOne;
        private final int zeroToThirtyOneOccupied;

        private ImmutableIntHashSet(int[] table, int occupied, int zeroToThirtyOne, int zeroToThirtyOneOccupied)
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
                throw new IllegalArgumentException("Use IntSets.immutable.with() to instantiate an optimized collection");
            }
        }

        public static ImmutableIntSet newSetWith(int... elements)
        {
            return IntHashSet.newSetWith(elements).toImmutable();
        }

        @Override
        public int hashCode()
        {
            int result = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result += value;
                zeroToThirtyOne &= ~(1 << value);
            }
            if (this.table != null)
            {
                for (int i = 0; i < this.table.length; i++)
                {
                    if (isNonSentinel(this.table[i]))
                    {
                        result += this.table[i];
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
                    int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                    if (count > 0)
                    {
                        appendable.append(separator);
                    }
                    count++;
                    appendable.append(String.valueOf(value));
                    zeroToThirtyOne &= ~(1 << value);
                }

                for (int value : this.table)
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
        public IntIterator intIterator()
        {
            return new InternalIntIterator();
        }

        @Override
        public int[] toArray()
        {
            int[] array = new int[this.size()];

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
        public int[] toArray(int[] array)
        {
            if (array.length < this.size())
            {
                array = new int[this.size()];
            }

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
        public boolean contains(int value)
        {
            if (isBetweenZeroAndThirtyOne(value))
            {
                int temp = this.zeroToThirtyOne;
                return ((temp >>> value) & 1) != 0;
            }
            return this.table[this.probe(value)] == value;
        }

        @Override
        public void each(IntProcedure procedure)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                procedure.value(value);
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int value : this.table)
            {
                if (isNonSentinel(value))
                {
                    procedure.value(value);
                }
            }
        }

        @Override
        public ImmutableIntSet select(IntPredicate predicate)
        {
            return this.select(predicate, new IntHashSet()).toImmutable();
        }

        @Override
        public ImmutableIntSet reject(IntPredicate predicate)
        {
            return this.reject(predicate, new IntHashSet()).toImmutable();
        }

        @Override
        public <V> ImmutableSet<V> collect(IntToObjectFunction<? extends V> function)
        {
            MutableSet<V> set = this.collect(function, UnifiedSet.newSet(this.size()));
            return set.toImmutable();
        }

        @Override
        public int detectIfNone(IntPredicate predicate, int ifNone)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return value;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int value : this.table)
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
        public int count(IntPredicate predicate)
        {
            int count = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    count++;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int value : this.table)
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
        public boolean anySatisfy(IntPredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return true;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int value : this.table)
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
        public boolean allSatisfy(IntPredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (!predicate.accept(value))
                {
                    return false;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int value : this.table)
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
        public boolean noneSatisfy(IntPredicate predicate)
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
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result += value;
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int value : this.table)
            {
                if (isNonSentinel(value))
                {
                    result += value;
                }
            }
            return result;
        }

        @Override
        public int max()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            int max = 31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne);
            boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

            for (int value : this.table)
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
        public int min()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            int min = Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
            boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

            for (int value : this.table)
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
        public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
        {
            T result = injectedValue;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                int value = Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result = function.valueOf(result, value);
                zeroToThirtyOne &= ~(1 << value);
            }

            for (int value : this.table)
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
            return new ImmutableIntSetSerializationProxy(this);
        }

        // exposed for testing
        int probe(int element)
        {
            int index = this.spreadAndMask(element);
            int valueAtIndex = this.table[index];

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

        int probeTwo(int element, int removedIndex)
        {
            int index = this.spreadTwoAndMask(element);
            for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
            {
                int nextIndex = (index + i) & (this.table.length - 1);
                int valueAtIndex = this.table[nextIndex];
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

        int probeThree(int element, int removedIndex)
        {
            int nextIndex = Integer.reverse(SpreadFunctions.intSpreadOne(element));
            int spreadTwo = Integer.reverse(SpreadFunctions.intSpreadTwo(element)) | 1;

            while (true)
            {
                nextIndex = this.mask(nextIndex + spreadTwo);
                int valueAtIndex = this.table[nextIndex];
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
        int spreadAndMask(int element)
        {
            int code = SpreadFunctions.intSpreadOne(element);
            return this.mask(code);
        }

        int spreadTwoAndMask(int element)
        {
            int code = SpreadFunctions.intSpreadTwo(element);
            return this.mask(code);
        }

        private int mask(int spread)
        {
            return spread & (this.table.length - 1);
        }

        private class InternalIntIterator implements IntIterator
        {
            private int count;
            private int position;
            private int zeroToThirtyOne;

            public boolean hasNext()
            {
                return this.count < ImmutableIntHashSet.this.size();
            }

            public int next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                while (this.zeroToThirtyOne < 32)
                {
                    if (ImmutableIntHashSet.this.contains(this.zeroToThirtyOne))
                    {
                        int result = this.zeroToThirtyOne;
                        this.zeroToThirtyOne++;
                        return result;
                    }
                    this.zeroToThirtyOne++;
                }

                int[] table = ImmutableIntHashSet.this.table;
                while (!isNonSentinel(table[this.position]))
                {
                    this.position++;
                }
                int result = table[this.position];
                this.position++;
                return result;
            }
        }
    }

    private class InternalIntIterator implements MutableIntIterator
    {
        private int count;
        private int position;
        private int zeroToThirtyOne;

        @Override
        public boolean hasNext()
        {
            return this.count < IntHashSet.this.size();
        }

        @Override
        public int next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;

            while (this.zeroToThirtyOne < 32)
            {
                if (IntHashSet.this.contains(this.zeroToThirtyOne))
                {
                    int result = this.zeroToThirtyOne;
                    this.zeroToThirtyOne++;
                    return result;
                }
                this.zeroToThirtyOne++;
            }

            int[] table = IntHashSet.this.table;
            while (!isNonSentinel(table[this.position]))
            {
                this.position++;
            }
            int result = table[this.position];
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
            int removeValue;
            if (this.zeroToThirtyOne <= 32 && this.position == 0)
            {
                if (IntHashSet.this.zeroToThirtyOne != (IntHashSet.this.zeroToThirtyOne | 1 << (this.zeroToThirtyOne - 1)))
                {
                    throw new IllegalStateException();
                }
                removeValue = this.zeroToThirtyOne - 1;
            }
            else if (IntHashSet.this.table[this.position - 1] == REMOVED)
            {
                throw new IllegalStateException();
            }
            else
            {
                removeValue = IntHashSet.this.table[this.position - 1];
            }
            if (IntHashSet.isBetweenZeroAndThirtyOne(removeValue))
            {
                IntHashSet.this.removeZeroToThirtyOne(removeValue);
            }
            else if (IntHashSet.this.table[this.position - 1] == removeValue)
            {
                if (IntHashSet.this.copyOnWrite)
                {
                    IntHashSet.this.copyTable();
                }
                IntHashSet.this.table[position - 1] = REMOVED;
                IntHashSet.this.occupiedWithData--;
                IntHashSet.this.occupiedWithSentinels++;
            }

            this.count--;
        }
    }
}
