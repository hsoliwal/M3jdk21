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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.LongSets;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableLongSetSerializationProxy;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.impl.set.immutable.primitive.AbstractImmutableLongSet;
import org.eclipse.collections.impl.set.primitive.AbstractLongSet;

/**
 * This file was automatically generated from template file primitiveHashSet.stg.
 *
 * @since 3.0.
 */
public class LongHashSet extends AbstractLongSet implements MutableLongSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final long EMPTY = 0L;
    private static final long REMOVED = 1L;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 8;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private long[] table;
    private int occupiedWithData;
    private int occupiedWithSentinels;
    // The 32 bits of this integer indicate whether the items 0L to 31L are present in the set.
    private int zeroToThirtyOne;
    private int zeroToThirtyOneOccupied;
    private transient boolean copyOnWrite;

    public LongHashSet()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY);
    }

    public LongHashSet(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public LongHashSet(long... elements)
    {
        this();
        this.addAll(elements);
    }

    public LongHashSet(LongIterable elements)
    {
        this();
        this.addAll(elements);
    }

    public LongHashSet(LongHashSet set)
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

    public static LongHashSet newSet(LongIterable source)
    {
        if (source instanceof LongHashSet)
        {
            return new LongHashSet((LongHashSet) source);
        }
        return new LongHashSet(source);
    }

    public static LongHashSet newSetWith(long... source)
    {
        return new LongHashSet(source);
    }

    private static boolean isBetweenZeroAndThirtyOne(long value)
    {
        return value >= 0L && value <= 31L;
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result += (int) (value ^ value >>> 32);
            zeroToThirtyOne &= ~(1 << value);
        }
        if (this.table != null)
        {
            for (int i = 0; i < this.table.length; i++)
            {
                if (isNonSentinel(this.table[i]))
                {
                    result += (int) (this.table[i] ^ this.table[i] >>> 32);
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
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (count > 0)
                {
                    appendable.append(separator);
                }
                count++;
                appendable.append(String.valueOf(value));
                zeroToThirtyOne &= ~(1 << value);
            }

            for (long value : this.table)
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
    public boolean add(long element)
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
    public boolean addAll(long... source)
    {
        int oldSize = this.size();
        for (long item : source)
        {
            this.add(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean addAll(LongIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof LongHashSet)
        {
            LongHashSet hashSet = (LongHashSet) source;
            this.zeroToThirtyOne |= hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (long item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.add(item);
                }
            }
        }
        else
        {
            LongIterator iterator = source.longIterator();
            while (iterator.hasNext())
            {
                long item = iterator.next();
                this.add(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean remove(long value)
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

    private boolean removeZeroToThirtyOne(long value)
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
    public boolean removeAll(LongIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof LongHashSet)
        {
            LongHashSet hashSet = (LongHashSet) source;
            this.zeroToThirtyOne &= ~hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (long item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.remove(item);
                }
            }
        }
        else
        {
            LongIterator iterator = source.longIterator();
            while (iterator.hasNext())
            {
                long item = iterator.next();
                this.remove(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(long... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (long item : source)
        {
            this.remove(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(LongIterable source)
    {
        int oldSize = this.size();
        final LongSet sourceSet = source instanceof LongSet ? (LongSet) source : source.toSet();
        LongHashSet retained = this.select(sourceSet::contains);
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
    public boolean retainAll(long... source)
    {
        return this.retainAll(LongHashSet.newSetWith(source));
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
            this.table = new long[this.table.length];
            this.copyOnWrite = false;
        }
        else
        {
            Arrays.fill(this.table, EMPTY);
        }
    }

    @Override
    public LongHashSet with(long element)
    {
        this.add(element);
        return this;
    }

    @Override
    public LongHashSet without(long element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public LongHashSet withAll(LongIterable elements)
    {
        this.addAll(elements);
        return this;
    }

    @Override
    public LongHashSet withoutAll(LongIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    @Override
    public MutableLongSet asUnmodifiable()
    {
        return new UnmodifiableLongSet(this);
    }

    @Override
    public MutableLongSet asSynchronized()
    {
        return new SynchronizedLongSet(this);
    }

    @Override
    public ImmutableLongSet toImmutable()
    {
        if (this.size() == 0)
        {
            return LongSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return LongSets.immutable.with(this.longIterator().next());
        }
        LongHashSet mutableSet;
        if (this.table.length == smallestPowerOfTwoGreaterThan(this.size() << 1)) // table is not too big
        {
            mutableSet = this;
            this.copyOnWrite = true;
        }
        else
        {
            mutableSet = new LongHashSet(this.size());
            mutableSet.addAll(this);
        }
        return new ImmutableLongHashSet(mutableSet.table, mutableSet.occupiedWithData, mutableSet.zeroToThirtyOne, mutableSet.zeroToThirtyOneOccupied);
    }

    @Override
    public MutableLongIterator longIterator()
    {
        return new InternalLongIterator();
    }

    @Override
    public long[] toArray()
    {
        long[] array = new long[this.size()];

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
    public long[] toArray(long[] array)
    {
        if (array.length < this.size())
        {
            array = new long[this.size()];
        }

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
    public boolean contains(long value)
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
    public void each(LongProcedure procedure)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            procedure.value(value);
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
        {
            if (isNonSentinel(value))
            {
                procedure.value(value);
            }
        }
    }

    @Override
    public LongHashSet select(LongPredicate predicate)
    {
        return this.select(predicate, new LongHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableLongCollection> R select(LongPredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
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
    public LongHashSet reject(LongPredicate predicate)
    {
        return this.reject(predicate, new LongHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableLongCollection> R reject(LongPredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
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
    public <V> MutableSet<V> collect(LongToObjectFunction<? extends V> function)
    {
        return this.collect(function, UnifiedSet.newSet(this.size()));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(LongToObjectFunction<? extends V> function, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            target.add(function.valueOf(value));
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
        {
            if (isNonSentinel(value))
            {
                target.add(function.valueOf(value));
            }
        }
        return target;
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return value;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
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
    public int count(LongPredicate predicate)
    {
        int count = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                count++;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
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
    public boolean anySatisfy(LongPredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return true;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
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
    public boolean allSatisfy(LongPredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                return false;
            }
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
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
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result += value;
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
        {
            if (isNonSentinel(value))
            {
                result += value;
            }
        }
        return result;
    }

    @Override
    public long max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        long max = 31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne);
        boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

        for (long value : this.table)
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
    public long min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        long min = (long) Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
        boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

        for (long value : this.table)
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
    public LongSet freeze()
    {
        if (this.size() == 0)
        {
            return LongSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return LongSets.immutable.with(this.longIterator().next());
        }
        this.copyOnWrite = true;
        return new ImmutableLongHashSet(this.table, this.occupiedWithData, this.zeroToThirtyOne, this.zeroToThirtyOneOccupied);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());

        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            out.writeLong(value);
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
        {
            if (isNonSentinel(value))
            {
                out.writeLong(value);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();

        for (int i = 0; i < size; i++)
        {
            this.add(in.readLong());
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result = function.valueOf(result, value);
            zeroToThirtyOne &= ~(1 << value);
        }

        for (long value : this.table)
        {
            if (isNonSentinel(value))
            {
                result = function.valueOf(result, value);
            }
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
                result.add(LongSets.mutable.withAll(this));
            }
            else
            {
                LongIterator iterator = this.longIterator();
                while (iterator.hasNext())
                {
                    MutableLongSet batch = LongSets.mutable.empty();
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
     * Creates a new empty LongHashSet.
     *
     * @since 9.2.
     */
    public LongHashSet newEmpty()
    {
        return new LongHashSet();
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
        long[] old = this.table;
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
        this.table = new long[sizeToAllocate];
    }

    // exposed for testing
    int probe(long element)
    {
        int index = this.spreadAndMask(element);
        long valueAtIndex = this.table[index];

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

    int probeTwo(long element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element);
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.table.length - 1);
            long valueAtIndex = this.table[nextIndex];
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

    int probeThree(long element, int removedIndex)
    {
        int nextIndex = (int) Long.reverse(SpreadFunctions.longSpreadOne(element));
        int spreadTwo = (int) Long.reverse(SpreadFunctions.longSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask(nextIndex + spreadTwo);
            long valueAtIndex = this.table[nextIndex];
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
    int spreadAndMask(long element)
    {
        long code = SpreadFunctions.longSpreadOne(element);
        return this.mask((int) code);
    }

    int spreadTwoAndMask(long element)
    {
        long code = SpreadFunctions.longSpreadTwo(element);
        return this.mask((int) code);
    }

    private int mask(int spread)
    {
        return spread & (this.table.length - 1);
    }

    private void copyTable()
    {
        this.copyOnWrite = false;
        long[] copy = new long[this.table.length];
        System.arraycopy(this.table, 0, copy, 0, this.table.length);
        this.table = copy;
    }

    private int maxOccupiedWithData()
    {
        return this.table.length >> 1;
    }

    private static boolean isNonSentinel(long value)
    {
        return value != EMPTY && value != REMOVED;
    }

    private static final class ImmutableLongHashSet extends AbstractImmutableLongSet implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private final long[] table;
        private final int occupied;
        // The 32 bits of this integer indicate whether the items 0L to 31L are present in the set.
        private final int zeroToThirtyOne;
        private final int zeroToThirtyOneOccupied;

        private ImmutableLongHashSet(long[] table, int occupied, int zeroToThirtyOne, int zeroToThirtyOneOccupied)
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
                throw new IllegalArgumentException("Use LongSets.immutable.with() to instantiate an optimized collection");
            }
        }

        public static ImmutableLongSet newSetWith(long... elements)
        {
            return LongHashSet.newSetWith(elements).toImmutable();
        }

        @Override
        public int hashCode()
        {
            int result = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result += (int) (value ^ value >>> 32);
                zeroToThirtyOne &= ~(1 << value);
            }
            if (this.table != null)
            {
                for (int i = 0; i < this.table.length; i++)
                {
                    if (isNonSentinel(this.table[i]))
                    {
                        result += (int) (this.table[i] ^ this.table[i] >>> 32);
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
                    long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                    if (count > 0)
                    {
                        appendable.append(separator);
                    }
                    count++;
                    appendable.append(String.valueOf(value));
                    zeroToThirtyOne &= ~(1 << value);
                }

                for (long value : this.table)
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
        public LongIterator longIterator()
        {
            return new InternalLongIterator();
        }

        @Override
        public long[] toArray()
        {
            long[] array = new long[this.size()];

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
        public long[] toArray(long[] array)
        {
            if (array.length < this.size())
            {
                array = new long[this.size()];
            }

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
        public boolean contains(long value)
        {
            if (isBetweenZeroAndThirtyOne(value))
            {
                int temp = this.zeroToThirtyOne;
                return ((temp >>> value) & 1) != 0;
            }
            return this.table[this.probe(value)] == value;
        }

        @Override
        public void each(LongProcedure procedure)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                procedure.value(value);
                zeroToThirtyOne &= ~(1 << value);
            }

            for (long value : this.table)
            {
                if (isNonSentinel(value))
                {
                    procedure.value(value);
                }
            }
        }

        @Override
        public ImmutableLongSet select(LongPredicate predicate)
        {
            return this.select(predicate, new LongHashSet()).toImmutable();
        }

        @Override
        public ImmutableLongSet reject(LongPredicate predicate)
        {
            return this.reject(predicate, new LongHashSet()).toImmutable();
        }

        @Override
        public <V> ImmutableSet<V> collect(LongToObjectFunction<? extends V> function)
        {
            MutableSet<V> set = this.collect(function, UnifiedSet.newSet(this.size()));
            return set.toImmutable();
        }

        @Override
        public long detectIfNone(LongPredicate predicate, long ifNone)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return value;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (long value : this.table)
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
        public int count(LongPredicate predicate)
        {
            int count = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    count++;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (long value : this.table)
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
        public boolean anySatisfy(LongPredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return true;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (long value : this.table)
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
        public boolean allSatisfy(LongPredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (!predicate.accept(value))
                {
                    return false;
                }
                zeroToThirtyOne &= ~(1 << value);
            }

            for (long value : this.table)
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
        public boolean noneSatisfy(LongPredicate predicate)
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
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result += value;
                zeroToThirtyOne &= ~(1 << value);
            }

            for (long value : this.table)
            {
                if (isNonSentinel(value))
                {
                    result += value;
                }
            }
            return result;
        }

        @Override
        public long max()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            long max = 31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne);
            boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

            for (long value : this.table)
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
        public long min()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            long min = (long) Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
            boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

            for (long value : this.table)
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
        public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
        {
            T result = injectedValue;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                long value = (long) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result = function.valueOf(result, value);
                zeroToThirtyOne &= ~(1 << value);
            }

            for (long value : this.table)
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
            return new ImmutableLongSetSerializationProxy(this);
        }

        // exposed for testing
        int probe(long element)
        {
            int index = this.spreadAndMask(element);
            long valueAtIndex = this.table[index];

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

        int probeTwo(long element, int removedIndex)
        {
            int index = this.spreadTwoAndMask(element);
            for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
            {
                int nextIndex = (index + i) & (this.table.length - 1);
                long valueAtIndex = this.table[nextIndex];
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

        int probeThree(long element, int removedIndex)
        {
            int nextIndex = (int) Long.reverse(SpreadFunctions.longSpreadOne(element));
            int spreadTwo = (int) Long.reverse(SpreadFunctions.longSpreadTwo(element)) | 1;

            while (true)
            {
                nextIndex = this.mask(nextIndex + spreadTwo);
                long valueAtIndex = this.table[nextIndex];
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
        int spreadAndMask(long element)
        {
            long code = SpreadFunctions.longSpreadOne(element);
            return this.mask((int) code);
        }

        int spreadTwoAndMask(long element)
        {
            long code = SpreadFunctions.longSpreadTwo(element);
            return this.mask((int) code);
        }

        private int mask(int spread)
        {
            return spread & (this.table.length - 1);
        }

        private class InternalLongIterator implements LongIterator
        {
            private int count;
            private int position;
            private long zeroToThirtyOne;

            public boolean hasNext()
            {
                return this.count < ImmutableLongHashSet.this.size();
            }

            public long next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                while (this.zeroToThirtyOne < 32)
                {
                    if (ImmutableLongHashSet.this.contains(this.zeroToThirtyOne))
                    {
                        long result = this.zeroToThirtyOne;
                        this.zeroToThirtyOne++;
                        return result;
                    }
                    this.zeroToThirtyOne++;
                }

                long[] table = ImmutableLongHashSet.this.table;
                while (!isNonSentinel(table[this.position]))
                {
                    this.position++;
                }
                long result = table[this.position];
                this.position++;
                return result;
            }
        }
    }

    private class InternalLongIterator implements MutableLongIterator
    {
        private int count;
        private int position;
        private long zeroToThirtyOne;

        @Override
        public boolean hasNext()
        {
            return this.count < LongHashSet.this.size();
        }

        @Override
        public long next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;

            while (this.zeroToThirtyOne < 32)
            {
                if (LongHashSet.this.contains(this.zeroToThirtyOne))
                {
                    long result = this.zeroToThirtyOne;
                    this.zeroToThirtyOne++;
                    return result;
                }
                this.zeroToThirtyOne++;
            }

            long[] table = LongHashSet.this.table;
            while (!isNonSentinel(table[this.position]))
            {
                this.position++;
            }
            long result = table[this.position];
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
            long removeValue;
            if (this.zeroToThirtyOne <= 32L && this.position == 0)
            {
                if (LongHashSet.this.zeroToThirtyOne != (LongHashSet.this.zeroToThirtyOne | 1 << (this.zeroToThirtyOne - 1)))
                {
                    throw new IllegalStateException();
                }
                removeValue = this.zeroToThirtyOne - 1;
            }
            else if (LongHashSet.this.table[this.position - 1] == REMOVED)
            {
                throw new IllegalStateException();
            }
            else
            {
                removeValue = LongHashSet.this.table[this.position - 1];
            }
            if (LongHashSet.isBetweenZeroAndThirtyOne(removeValue))
            {
                LongHashSet.this.removeZeroToThirtyOne(removeValue);
            }
            else if (LongHashSet.this.table[this.position - 1] == removeValue)
            {
                if (LongHashSet.this.copyOnWrite)
                {
                    LongHashSet.this.copyTable();
                }
                LongHashSet.this.table[position - 1] = REMOVED;
                LongHashSet.this.occupiedWithData--;
                LongHashSet.this.occupiedWithSentinels++;
            }

            this.count--;
        }
    }
}
