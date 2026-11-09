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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.DoubleSets;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableDoubleSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableDoubleSetSerializationProxy;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.impl.set.immutable.primitive.AbstractImmutableDoubleSet;
import org.eclipse.collections.impl.set.primitive.AbstractDoubleSet;

/**
 * This file was automatically generated from template file primitiveHashSet.stg.
 *
 * @since 3.0.
 */
public class DoubleHashSet extends AbstractDoubleSet implements MutableDoubleSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final double EMPTY = 0.0;
    private static final double REMOVED = 1.0;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 8;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private double[] table;
    private int occupiedWithData;
    private int occupiedWithSentinels;
    // The 32 bits of this integer indicate whether the items 0.0 to 31.0 are present in the set.
    private int zeroToThirtyOne;
    private int zeroToThirtyOneOccupied;
    private transient boolean copyOnWrite;

    public DoubleHashSet()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY);
    }

    public DoubleHashSet(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public DoubleHashSet(double... elements)
    {
        this();
        this.addAll(elements);
    }

    public DoubleHashSet(DoubleIterable elements)
    {
        this();
        this.addAll(elements);
    }

    public DoubleHashSet(DoubleHashSet set)
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

    public static DoubleHashSet newSet(DoubleIterable source)
    {
        if (source instanceof DoubleHashSet)
        {
            return new DoubleHashSet((DoubleHashSet) source);
        }
        return new DoubleHashSet(source);
    }

    public static DoubleHashSet newSetWith(double... source)
    {
        return new DoubleHashSet(source);
    }

    private static boolean isBetweenZeroAndThirtyOne(double value)
    {
        return Double.compare(value, 0.0) >= 0 && Double.compare(value, 31.0) <= 0 && Double.compare(value, Math.floor(value)) == 0;
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result += (int) (Double.doubleToLongBits(value) ^ Double.doubleToLongBits(value) >>> 32);
            zeroToThirtyOne &= ~(1 << (int) value);
        }
        if (this.table != null)
        {
            for (int i = 0; i < this.table.length; i++)
            {
                if (isNonSentinel(this.table[i]))
                {
                    result += (int) (Double.doubleToLongBits(this.table[i]) ^ Double.doubleToLongBits(this.table[i]) >>> 32);
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
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (count > 0)
                {
                    appendable.append(separator);
                }
                count++;
                appendable.append(String.valueOf(value));
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (double value : this.table)
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
    public boolean add(double element)
    {
        if (isBetweenZeroAndThirtyOne(element))
        {
            int initial = this.zeroToThirtyOne;
            this.zeroToThirtyOne |= 1 << (int) element;
            if (this.zeroToThirtyOne != initial)
            {
                this.zeroToThirtyOneOccupied++;
                return true;
            }
            return false;
        }

        int index = this.probe(element);

        if (Double.compare(this.table[index], element) == 0)
        {
            // element already present in set
            return false;
        }

        if (this.copyOnWrite)
        {
            this.copyTable();
        }
        if (Double.compare(this.table[index], REMOVED) == 0)
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
    public boolean addAll(double... source)
    {
        int oldSize = this.size();
        for (double item : source)
        {
            this.add(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean addAll(DoubleIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof DoubleHashSet)
        {
            DoubleHashSet hashSet = (DoubleHashSet) source;
            this.zeroToThirtyOne |= hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (double item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.add(item);
                }
            }
        }
        else
        {
            DoubleIterator iterator = source.doubleIterator();
            while (iterator.hasNext())
            {
                double item = iterator.next();
                this.add(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean remove(double value)
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
        if (Double.compare(this.table[index], value) == 0)
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

    private boolean removeZeroToThirtyOne(double value)
    {
        int initial = this.zeroToThirtyOne;
        this.zeroToThirtyOne &= ~(1 << (int) value);
        if (this.zeroToThirtyOne == initial)
        {
            return false;
        }
        this.zeroToThirtyOneOccupied--;
        return true;
    }

    @Override
    public boolean removeAll(DoubleIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof DoubleHashSet)
        {
            DoubleHashSet hashSet = (DoubleHashSet) source;
            this.zeroToThirtyOne &= ~hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (double item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.remove(item);
                }
            }
        }
        else
        {
            DoubleIterator iterator = source.doubleIterator();
            while (iterator.hasNext())
            {
                double item = iterator.next();
                this.remove(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(double... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (double item : source)
        {
            this.remove(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(DoubleIterable source)
    {
        int oldSize = this.size();
        final DoubleSet sourceSet = source instanceof DoubleSet ? (DoubleSet) source : source.toSet();
        DoubleHashSet retained = this.select(sourceSet::contains);
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
    public boolean retainAll(double... source)
    {
        return this.retainAll(DoubleHashSet.newSetWith(source));
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
            this.table = new double[this.table.length];
            this.copyOnWrite = false;
        }
        else
        {
            Arrays.fill(this.table, EMPTY);
        }
    }

    @Override
    public DoubleHashSet with(double element)
    {
        this.add(element);
        return this;
    }

    @Override
    public DoubleHashSet without(double element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public DoubleHashSet withAll(DoubleIterable elements)
    {
        this.addAll(elements);
        return this;
    }

    @Override
    public DoubleHashSet withoutAll(DoubleIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    @Override
    public MutableDoubleSet asUnmodifiable()
    {
        return new UnmodifiableDoubleSet(this);
    }

    @Override
    public MutableDoubleSet asSynchronized()
    {
        return new SynchronizedDoubleSet(this);
    }

    @Override
    public ImmutableDoubleSet toImmutable()
    {
        if (this.size() == 0)
        {
            return DoubleSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return DoubleSets.immutable.with(this.doubleIterator().next());
        }
        DoubleHashSet mutableSet;
        if (this.table.length == smallestPowerOfTwoGreaterThan(this.size() << 1)) // table is not too big
        {
            mutableSet = this;
            this.copyOnWrite = true;
        }
        else
        {
            mutableSet = new DoubleHashSet(this.size());
            mutableSet.addAll(this);
        }
        return new ImmutableDoubleHashSet(mutableSet.table, mutableSet.occupiedWithData, mutableSet.zeroToThirtyOne, mutableSet.zeroToThirtyOneOccupied);
    }

    @Override
    public MutableDoubleIterator doubleIterator()
    {
        return new InternalDoubleIterator();
    }

    @Override
    public double[] toArray()
    {
        double[] array = new double[this.size()];

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            array[j] = value;
            j++;
            zeroToThirtyOne &= ~(1 << (int) value);
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
    public double[] toArray(double[] array)
    {
        if (array.length < this.size())
        {
            array = new double[this.size()];
        }

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            array[j] = value;
            j++;
            zeroToThirtyOne &= ~(1 << (int) value);
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
    public boolean contains(double value)
    {
        if (isBetweenZeroAndThirtyOne(value))
        {
            int temp = this.zeroToThirtyOne;
            return ((temp >>> (int) value) & 1) != 0;
        }
        return Double.compare(this.table[this.probe(value)], value) == 0;
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            procedure.value(value);
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
        {
            if (isNonSentinel(value))
            {
                procedure.value(value);
            }
        }
    }

    @Override
    public DoubleHashSet select(DoublePredicate predicate)
    {
        return this.select(predicate, new DoubleHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableDoubleCollection> R select(DoublePredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
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
    public DoubleHashSet reject(DoublePredicate predicate)
    {
        return this.reject(predicate, new DoubleHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableDoubleCollection> R reject(DoublePredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
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
    public <V> MutableSet<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return this.collect(function, UnifiedSet.newSet(this.size()));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(DoubleToObjectFunction<? extends V> function, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            target.add(function.valueOf(value));
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
        {
            if (isNonSentinel(value))
            {
                target.add(function.valueOf(value));
            }
        }
        return target;
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return value;
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
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
    public int count(DoublePredicate predicate)
    {
        int count = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                count++;
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
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
    public boolean anySatisfy(DoublePredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return true;
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
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
    public boolean allSatisfy(DoublePredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                return false;
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
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
    public double sum()
    {
        double result = 0.0;
        double compensation = 0.0;

        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            double adjustedValue = value - compensation;
            double nextSum = result + adjustedValue;
            compensation = nextSum - result - adjustedValue;
            result = nextSum;
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
        {
            if (isNonSentinel(value))
            {
                double adjustedValue = value - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
            }
        }
        return result;
    }

    @Override
    public double max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        double max = 31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne);
        boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

        for (double value : this.table)
        {
            if (isNonSentinel(value) && (!isMaxSet || Double.compare(max, value) < 0))
            {
                max = value;
                isMaxSet = true;
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
        double min = (double) Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
        boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

        for (double value : this.table)
        {
            if (isNonSentinel(value) && (!isMinSet || Double.compare(value, min) < 0))
            {
                min = value;
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public DoubleSet freeze()
    {
        if (this.size() == 0)
        {
            return DoubleSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return DoubleSets.immutable.with(this.doubleIterator().next());
        }
        this.copyOnWrite = true;
        return new ImmutableDoubleHashSet(this.table, this.occupiedWithData, this.zeroToThirtyOne, this.zeroToThirtyOneOccupied);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());

        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            out.writeDouble(value);
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
        {
            if (isNonSentinel(value))
            {
                out.writeDouble(value);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();

        for (int i = 0; i < size; i++)
        {
            this.add(in.readDouble());
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result = function.valueOf(result, value);
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (double value : this.table)
        {
            if (isNonSentinel(value))
            {
                result = function.valueOf(result, value);
            }
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
                result.add(DoubleSets.mutable.withAll(this));
            }
            else
            {
                DoubleIterator iterator = this.doubleIterator();
                while (iterator.hasNext())
                {
                    MutableDoubleSet batch = DoubleSets.mutable.empty();
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
     * Creates a new empty DoubleHashSet.
     *
     * @since 9.2.
     */
    public DoubleHashSet newEmpty()
    {
        return new DoubleHashSet();
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
        double[] old = this.table;
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
        this.table = new double[sizeToAllocate];
    }

    // exposed for testing
    int probe(double element)
    {
        int index = this.spreadAndMask(element);
        double valueAtIndex = this.table[index];

        if (Double.compare(valueAtIndex, element) == 0 || Double.compare(valueAtIndex, EMPTY) == 0)
        {
            return index;
        }

        int removedIndex = Double.compare(valueAtIndex, REMOVED) == 0 ? index : -1;
        for (int i = 1; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.table.length - 1);
            valueAtIndex = this.table[nextIndex];
            if (Double.compare(valueAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Double.compare(valueAtIndex, EMPTY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Double.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeTwo(element, removedIndex);
    }

    int probeTwo(double element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element);
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.table.length - 1);
            double valueAtIndex = this.table[nextIndex];
            if (Double.compare(valueAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Double.compare(valueAtIndex, EMPTY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Double.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeThree(element, removedIndex);
    }

    int probeThree(double element, int removedIndex)
    {
        int nextIndex = (int) Long.reverse(SpreadFunctions.doubleSpreadOne(element));
        int spreadTwo = (int) Long.reverse(SpreadFunctions.doubleSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask(nextIndex + spreadTwo);
            double valueAtIndex = this.table[nextIndex];
            if (Double.compare(valueAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Double.compare(valueAtIndex, EMPTY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Double.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
    }

    // exposed for testing
    int spreadAndMask(double element)
    {
        long code = SpreadFunctions.doubleSpreadOne(element);
        return this.mask((int) code);
    }

    int spreadTwoAndMask(double element)
    {
        long code = SpreadFunctions.doubleSpreadTwo(element);
        return this.mask((int) code);
    }

    private int mask(int spread)
    {
        return spread & (this.table.length - 1);
    }

    private void copyTable()
    {
        this.copyOnWrite = false;
        double[] copy = new double[this.table.length];
        System.arraycopy(this.table, 0, copy, 0, this.table.length);
        this.table = copy;
    }

    private int maxOccupiedWithData()
    {
        return this.table.length >> 1;
    }

    private static boolean isNonSentinel(double value)
    {
        return Double.compare(value, EMPTY) != 0 && Double.compare(value, REMOVED) != 0;
    }

    private static final class ImmutableDoubleHashSet extends AbstractImmutableDoubleSet implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private final double[] table;
        private final int occupied;
        // The 32 bits of this integer indicate whether the items 0.0 to 31.0 are present in the set.
        private final int zeroToThirtyOne;
        private final int zeroToThirtyOneOccupied;

        private ImmutableDoubleHashSet(double[] table, int occupied, int zeroToThirtyOne, int zeroToThirtyOneOccupied)
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
                throw new IllegalArgumentException("Use DoubleSets.immutable.with() to instantiate an optimized collection");
            }
        }

        public static ImmutableDoubleSet newSetWith(double... elements)
        {
            return DoubleHashSet.newSetWith(elements).toImmutable();
        }

        @Override
        public int hashCode()
        {
            int result = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result += (int) (Double.doubleToLongBits(value) ^ Double.doubleToLongBits(value) >>> 32);
                zeroToThirtyOne &= ~(1 << (int) value);
            }
            if (this.table != null)
            {
                for (int i = 0; i < this.table.length; i++)
                {
                    if (isNonSentinel(this.table[i]))
                    {
                        result += (int) (Double.doubleToLongBits(this.table[i]) ^ Double.doubleToLongBits(this.table[i]) >>> 32);
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
                    double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                    if (count > 0)
                    {
                        appendable.append(separator);
                    }
                    count++;
                    appendable.append(String.valueOf(value));
                    zeroToThirtyOne &= ~(1 << (int) value);
                }

                for (double value : this.table)
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
        public DoubleIterator doubleIterator()
        {
            return new InternalDoubleIterator();
        }

        @Override
        public double[] toArray()
        {
            double[] array = new double[this.size()];

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                array[j] = value;
                j++;
                zeroToThirtyOne &= ~(1 << (int) value);
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
        public double[] toArray(double[] array)
        {
            if (array.length < this.size())
            {
                array = new double[this.size()];
            }

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                array[j] = value;
                j++;
                zeroToThirtyOne &= ~(1 << (int) value);
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
        public boolean contains(double value)
        {
            if (isBetweenZeroAndThirtyOne(value))
            {
                int temp = this.zeroToThirtyOne;
                return ((temp >>> (int) value) & 1) != 0;
            }
            return Double.compare(this.table[this.probe(value)], value) == 0;
        }

        @Override
        public void each(DoubleProcedure procedure)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                procedure.value(value);
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (double value : this.table)
            {
                if (isNonSentinel(value))
                {
                    procedure.value(value);
                }
            }
        }

        @Override
        public ImmutableDoubleSet select(DoublePredicate predicate)
        {
            return this.select(predicate, new DoubleHashSet()).toImmutable();
        }

        @Override
        public ImmutableDoubleSet reject(DoublePredicate predicate)
        {
            return this.reject(predicate, new DoubleHashSet()).toImmutable();
        }

        @Override
        public <V> ImmutableSet<V> collect(DoubleToObjectFunction<? extends V> function)
        {
            MutableSet<V> set = this.collect(function, UnifiedSet.newSet(this.size()));
            return set.toImmutable();
        }

        @Override
        public double detectIfNone(DoublePredicate predicate, double ifNone)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return value;
                }
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (double value : this.table)
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
        public int count(DoublePredicate predicate)
        {
            int count = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    count++;
                }
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (double value : this.table)
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
        public boolean anySatisfy(DoublePredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return true;
                }
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (double value : this.table)
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
        public boolean allSatisfy(DoublePredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (!predicate.accept(value))
                {
                    return false;
                }
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (double value : this.table)
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
        public boolean noneSatisfy(DoublePredicate predicate)
        {
            return !this.anySatisfy(predicate);
        }

        @Override
        public double sum()
        {
            double result = 0.0;
            double compensation = 0.0;

            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                double adjustedValue = value - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (double value : this.table)
            {
                if (isNonSentinel(value))
                {
                    double adjustedValue = value - compensation;
                    double nextSum = result + adjustedValue;
                    compensation = nextSum - result - adjustedValue;
                    result = nextSum;
                }
            }
            return result;
        }

        @Override
        public double max()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            double max = 31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne);
            boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

            for (double value : this.table)
            {
                if (isNonSentinel(value) && (!isMaxSet || Double.compare(max, value) < 0))
                {
                    max = value;
                    isMaxSet = true;
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
            double min = (double) Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
            boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

            for (double value : this.table)
            {
                if (isNonSentinel(value) && (!isMinSet || Double.compare(value, min) < 0))
                {
                    min = value;
                    isMinSet = true;
                }
            }
            return min;
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
        {
            T result = injectedValue;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                double value = (double) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result = function.valueOf(result, value);
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (double value : this.table)
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
            return new ImmutableDoubleSetSerializationProxy(this);
        }

        // exposed for testing
        int probe(double element)
        {
            int index = this.spreadAndMask(element);
            double valueAtIndex = this.table[index];

            if (Double.compare(valueAtIndex, element) == 0 || Double.compare(valueAtIndex, EMPTY) == 0)
            {
                return index;
            }

            int removedIndex = Double.compare(valueAtIndex, REMOVED) == 0 ? index : -1;
            for (int i = 1; i < INITIAL_LINEAR_PROBE; i++)
            {
                int nextIndex = (index + i) & (this.table.length - 1);
                valueAtIndex = this.table[nextIndex];
                if (Double.compare(valueAtIndex, element) == 0)
                {
                    return nextIndex;
                }
                if (Double.compare(valueAtIndex, EMPTY) == 0)
                {
                    return removedIndex == -1 ? nextIndex : removedIndex;
                }
                if (Double.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            return this.probeTwo(element, removedIndex);
        }

        int probeTwo(double element, int removedIndex)
        {
            int index = this.spreadTwoAndMask(element);
            for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
            {
                int nextIndex = (index + i) & (this.table.length - 1);
                double valueAtIndex = this.table[nextIndex];
                if (Double.compare(valueAtIndex, element) == 0)
                {
                    return nextIndex;
                }
                if (Double.compare(valueAtIndex, EMPTY) == 0)
                {
                    return removedIndex == -1 ? nextIndex : removedIndex;
                }
                if (Double.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            return this.probeThree(element, removedIndex);
        }

        int probeThree(double element, int removedIndex)
        {
            int nextIndex = (int) Long.reverse(SpreadFunctions.doubleSpreadOne(element));
            int spreadTwo = (int) Long.reverse(SpreadFunctions.doubleSpreadTwo(element)) | 1;

            while (true)
            {
                nextIndex = this.mask(nextIndex + spreadTwo);
                double valueAtIndex = this.table[nextIndex];
                if (Double.compare(valueAtIndex, element) == 0)
                {
                    return nextIndex;
                }
                if (Double.compare(valueAtIndex, EMPTY) == 0)
                {
                    return removedIndex == -1 ? nextIndex : removedIndex;
                }
                if (Double.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
        }

        // exposed for testing
        int spreadAndMask(double element)
        {
            long code = SpreadFunctions.doubleSpreadOne(element);
            return this.mask((int) code);
        }

        int spreadTwoAndMask(double element)
        {
            long code = SpreadFunctions.doubleSpreadTwo(element);
            return this.mask((int) code);
        }

        private int mask(int spread)
        {
            return spread & (this.table.length - 1);
        }

        private class InternalDoubleIterator implements DoubleIterator
        {
            private int count;
            private int position;
            private double zeroToThirtyOne;

            public boolean hasNext()
            {
                return this.count < ImmutableDoubleHashSet.this.size();
            }

            public double next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                while (this.zeroToThirtyOne < 32)
                {
                    if (ImmutableDoubleHashSet.this.contains(this.zeroToThirtyOne))
                    {
                        double result = this.zeroToThirtyOne;
                        this.zeroToThirtyOne++;
                        return result;
                    }
                    this.zeroToThirtyOne++;
                }

                double[] table = ImmutableDoubleHashSet.this.table;
                while (!isNonSentinel(table[this.position]))
                {
                    this.position++;
                }
                double result = table[this.position];
                this.position++;
                return result;
            }
        }
    }

    private class InternalDoubleIterator implements MutableDoubleIterator
    {
        private int count;
        private int position;
        private double zeroToThirtyOne;

        @Override
        public boolean hasNext()
        {
            return this.count < DoubleHashSet.this.size();
        }

        @Override
        public double next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;

            while (this.zeroToThirtyOne < 32)
            {
                if (DoubleHashSet.this.contains(this.zeroToThirtyOne))
                {
                    double result = this.zeroToThirtyOne;
                    this.zeroToThirtyOne++;
                    return result;
                }
                this.zeroToThirtyOne++;
            }

            double[] table = DoubleHashSet.this.table;
            while (!isNonSentinel(table[this.position]))
            {
                this.position++;
            }
            double result = table[this.position];
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
            double removeValue;
            if (this.zeroToThirtyOne <= 32.0 && this.position == 0)
            {
                if (DoubleHashSet.this.zeroToThirtyOne != (DoubleHashSet.this.zeroToThirtyOne | 1 << ((int) this.zeroToThirtyOne - 1)))
                {
                    throw new IllegalStateException();
                }
                removeValue = this.zeroToThirtyOne - 1;
            }
            else if (Double.compare(DoubleHashSet.this.table[this.position - 1], REMOVED) == 0)
            {
                throw new IllegalStateException();
            }
            else
            {
                removeValue = DoubleHashSet.this.table[this.position - 1];
            }
            if (DoubleHashSet.isBetweenZeroAndThirtyOne(removeValue))
            {
                DoubleHashSet.this.removeZeroToThirtyOne(removeValue);
            }
            else if (Double.compare(DoubleHashSet.this.table[this.position - 1], removeValue) == 0)
            {
                if (DoubleHashSet.this.copyOnWrite)
                {
                    DoubleHashSet.this.copyTable();
                }
                DoubleHashSet.this.table[position - 1] = REMOVED;
                DoubleHashSet.this.occupiedWithData--;
                DoubleHashSet.this.occupiedWithSentinels++;
            }

            this.count--;
        }
    }
}
