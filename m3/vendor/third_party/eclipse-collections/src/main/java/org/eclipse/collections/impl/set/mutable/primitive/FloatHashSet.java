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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.FloatSets;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableFloatSetSerializationProxy;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.impl.set.immutable.primitive.AbstractImmutableFloatSet;
import org.eclipse.collections.impl.set.primitive.AbstractFloatSet;

/**
 * This file was automatically generated from template file primitiveHashSet.stg.
 *
 * @since 3.0.
 */
public class FloatHashSet extends AbstractFloatSet implements MutableFloatSet, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float EMPTY = 0.0f;
    private static final float REMOVED = 1.0f;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 4;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private float[] table;
    private int occupiedWithData;
    private int occupiedWithSentinels;
    // The 32 bits of this integer indicate whether the items 0.0f to 31.0f are present in the set.
    private int zeroToThirtyOne;
    private int zeroToThirtyOneOccupied;
    private transient boolean copyOnWrite;

    public FloatHashSet()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY);
    }

    public FloatHashSet(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public FloatHashSet(float... elements)
    {
        this();
        this.addAll(elements);
    }

    public FloatHashSet(FloatIterable elements)
    {
        this();
        this.addAll(elements);
    }

    public FloatHashSet(FloatHashSet set)
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

    public static FloatHashSet newSet(FloatIterable source)
    {
        if (source instanceof FloatHashSet)
        {
            return new FloatHashSet((FloatHashSet) source);
        }
        return new FloatHashSet(source);
    }

    public static FloatHashSet newSetWith(float... source)
    {
        return new FloatHashSet(source);
    }

    private static boolean isBetweenZeroAndThirtyOne(float value)
    {
        return Float.compare(value, 0.0f) >= 0 && Float.compare(value, 31.0f) <= 0 && Double.compare(value, Math.floor(value)) == 0;
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result += Float.floatToIntBits(value);
            zeroToThirtyOne &= ~(1 << (int) value);
        }
        if (this.table != null)
        {
            for (int i = 0; i < this.table.length; i++)
            {
                if (isNonSentinel(this.table[i]))
                {
                    result += Float.floatToIntBits(this.table[i]);
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
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (count > 0)
                {
                    appendable.append(separator);
                }
                count++;
                appendable.append(String.valueOf(value));
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (float value : this.table)
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
    public boolean add(float element)
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

        if (Float.compare(this.table[index], element) == 0)
        {
            // element already present in set
            return false;
        }

        if (this.copyOnWrite)
        {
            this.copyTable();
        }
        if (Float.compare(this.table[index], REMOVED) == 0)
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
    public boolean addAll(float... source)
    {
        int oldSize = this.size();
        for (float item : source)
        {
            this.add(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean addAll(FloatIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof FloatHashSet)
        {
            FloatHashSet hashSet = (FloatHashSet) source;
            this.zeroToThirtyOne |= hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (float item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.add(item);
                }
            }
        }
        else
        {
            FloatIterator iterator = source.floatIterator();
            while (iterator.hasNext())
            {
                float item = iterator.next();
                this.add(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean remove(float value)
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
        if (Float.compare(this.table[index], value) == 0)
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

    private boolean removeZeroToThirtyOne(float value)
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
    public boolean removeAll(FloatIterable source)
    {
        if (source.isEmpty())
        {
            return false;
        }
        int oldSize = this.size();
        if (source instanceof FloatHashSet)
        {
            FloatHashSet hashSet = (FloatHashSet) source;
            this.zeroToThirtyOne &= ~hashSet.zeroToThirtyOne;
            this.zeroToThirtyOneOccupied = Integer.bitCount(this.zeroToThirtyOne);
            for (float item : hashSet.table)
            {
                if (isNonSentinel(item))
                {
                    this.remove(item);
                }
            }
        }
        else
        {
            FloatIterator iterator = source.floatIterator();
            while (iterator.hasNext())
            {
                float item = iterator.next();
                this.remove(item);
            }
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean removeAll(float... source)
    {
        if (source.length == 0)
        {
            return false;
        }
        int oldSize = this.size();
        for (float item : source)
        {
            this.remove(item);
        }
        return this.size() != oldSize;
    }

    @Override
    public boolean retainAll(FloatIterable source)
    {
        int oldSize = this.size();
        final FloatSet sourceSet = source instanceof FloatSet ? (FloatSet) source : source.toSet();
        FloatHashSet retained = this.select(sourceSet::contains);
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
    public boolean retainAll(float... source)
    {
        return this.retainAll(FloatHashSet.newSetWith(source));
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
            this.table = new float[this.table.length];
            this.copyOnWrite = false;
        }
        else
        {
            Arrays.fill(this.table, EMPTY);
        }
    }

    @Override
    public FloatHashSet with(float element)
    {
        this.add(element);
        return this;
    }

    @Override
    public FloatHashSet without(float element)
    {
        this.remove(element);
        return this;
    }

    @Override
    public FloatHashSet withAll(FloatIterable elements)
    {
        this.addAll(elements);
        return this;
    }

    @Override
    public FloatHashSet withoutAll(FloatIterable elements)
    {
        this.removeAll(elements);
        return this;
    }

    @Override
    public MutableFloatSet asUnmodifiable()
    {
        return new UnmodifiableFloatSet(this);
    }

    @Override
    public MutableFloatSet asSynchronized()
    {
        return new SynchronizedFloatSet(this);
    }

    @Override
    public ImmutableFloatSet toImmutable()
    {
        if (this.size() == 0)
        {
            return FloatSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return FloatSets.immutable.with(this.floatIterator().next());
        }
        FloatHashSet mutableSet;
        if (this.table.length == smallestPowerOfTwoGreaterThan(this.size() << 1)) // table is not too big
        {
            mutableSet = this;
            this.copyOnWrite = true;
        }
        else
        {
            mutableSet = new FloatHashSet(this.size());
            mutableSet.addAll(this);
        }
        return new ImmutableFloatHashSet(mutableSet.table, mutableSet.occupiedWithData, mutableSet.zeroToThirtyOne, mutableSet.zeroToThirtyOneOccupied);
    }

    @Override
    public MutableFloatIterator floatIterator()
    {
        return new InternalFloatIterator();
    }

    @Override
    public float[] toArray()
    {
        float[] array = new float[this.size()];

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
    public float[] toArray(float[] array)
    {
        if (array.length < this.size())
        {
            array = new float[this.size()];
        }

        int j = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
    public boolean contains(float value)
    {
        if (isBetweenZeroAndThirtyOne(value))
        {
            int temp = this.zeroToThirtyOne;
            return ((temp >>> (int) value) & 1) != 0;
        }
        return Float.compare(this.table[this.probe(value)], value) == 0;
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            procedure.value(value);
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
        {
            if (isNonSentinel(value))
            {
                procedure.value(value);
            }
        }
    }

    @Override
    public FloatHashSet select(FloatPredicate predicate)
    {
        return this.select(predicate, new FloatHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableFloatCollection> R select(FloatPredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
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
    public FloatHashSet reject(FloatPredicate predicate)
    {
        return this.reject(predicate, new FloatHashSet());
    }

    /**
     * @since 8.1.
     */
    @Override
    public <R extends MutableFloatCollection> R reject(FloatPredicate predicate, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                target.add(value);
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
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
    public <V> MutableSet<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return this.collect(function, UnifiedSet.newSet(this.size()));
    }

    /**
     * @since 8.1.
     */
    @Override
    public <V, R extends Collection<V>> R collect(FloatToObjectFunction<? extends V> function, R target)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            target.add(function.valueOf(value));
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
        {
            if (isNonSentinel(value))
            {
                target.add(function.valueOf(value));
            }
        }
        return target;
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return value;
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
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
    public int count(FloatPredicate predicate)
    {
        int count = 0;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                count++;
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
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
    public boolean anySatisfy(FloatPredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (predicate.accept(value))
            {
                return true;
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
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
    public boolean allSatisfy(FloatPredicate predicate)
    {
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            if (!predicate.accept(value))
            {
                return false;
            }
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
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
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            double adjustedValue = value - compensation;
            double nextSum = result + adjustedValue;
            compensation = nextSum - result - adjustedValue;
            result = nextSum;
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
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
    public float max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        float max = 31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne);
        boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

        for (float value : this.table)
        {
            if (isNonSentinel(value) && (!isMaxSet || Float.compare(max, value) < 0))
            {
                max = value;
                isMaxSet = true;
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
        float min = (float) Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
        boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

        for (float value : this.table)
        {
            if (isNonSentinel(value) && (!isMinSet || Float.compare(value, min) < 0))
            {
                min = value;
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public FloatSet freeze()
    {
        if (this.size() == 0)
        {
            return FloatSets.immutable.with();
        }
        if (this.size() == 1)
        {
            return FloatSets.immutable.with(this.floatIterator().next());
        }
        this.copyOnWrite = true;
        return new ImmutableFloatHashSet(this.table, this.occupiedWithData, this.zeroToThirtyOne, this.zeroToThirtyOneOccupied);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());

        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            out.writeFloat(value);
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
        {
            if (isNonSentinel(value))
            {
                out.writeFloat(value);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException
    {
        int size = in.readInt();

        for (int i = 0; i < size; i++)
        {
            this.add(in.readFloat());
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        int zeroToThirtyOne = this.zeroToThirtyOne;
        while (zeroToThirtyOne != 0)
        {
            float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
            result = function.valueOf(result, value);
            zeroToThirtyOne &= ~(1 << (int) value);
        }

        for (float value : this.table)
        {
            if (isNonSentinel(value))
            {
                result = function.valueOf(result, value);
            }
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
                result.add(FloatSets.mutable.withAll(this));
            }
            else
            {
                FloatIterator iterator = this.floatIterator();
                while (iterator.hasNext())
                {
                    MutableFloatSet batch = FloatSets.mutable.empty();
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
     * Creates a new empty FloatHashSet.
     *
     * @since 9.2.
     */
    public FloatHashSet newEmpty()
    {
        return new FloatHashSet();
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
        float[] old = this.table;
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
        this.table = new float[sizeToAllocate];
    }

    // exposed for testing
    int probe(float element)
    {
        int index = this.spreadAndMask(element);
        float valueAtIndex = this.table[index];

        if (Float.compare(valueAtIndex, element) == 0 || Float.compare(valueAtIndex, EMPTY) == 0)
        {
            return index;
        }

        int removedIndex = Float.compare(valueAtIndex, REMOVED) == 0 ? index : -1;
        for (int i = 1; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.table.length - 1);
            valueAtIndex = this.table[nextIndex];
            if (Float.compare(valueAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Float.compare(valueAtIndex, EMPTY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Float.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeTwo(element, removedIndex);
    }

    int probeTwo(float element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element);
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.table.length - 1);
            float valueAtIndex = this.table[nextIndex];
            if (Float.compare(valueAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Float.compare(valueAtIndex, EMPTY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Float.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeThree(element, removedIndex);
    }

    int probeThree(float element, int removedIndex)
    {
        int nextIndex = Integer.reverse(SpreadFunctions.floatSpreadOne(element));
        int spreadTwo = Integer.reverse(SpreadFunctions.floatSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask(nextIndex + spreadTwo);
            float valueAtIndex = this.table[nextIndex];
            if (Float.compare(valueAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Float.compare(valueAtIndex, EMPTY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Float.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
    }

    // exposed for testing
    int spreadAndMask(float element)
    {
        int code = SpreadFunctions.floatSpreadOne(element);
        return this.mask(code);
    }

    int spreadTwoAndMask(float element)
    {
        int code = SpreadFunctions.floatSpreadTwo(element);
        return this.mask(code);
    }

    private int mask(int spread)
    {
        return spread & (this.table.length - 1);
    }

    private void copyTable()
    {
        this.copyOnWrite = false;
        float[] copy = new float[this.table.length];
        System.arraycopy(this.table, 0, copy, 0, this.table.length);
        this.table = copy;
    }

    private int maxOccupiedWithData()
    {
        return this.table.length >> 1;
    }

    private static boolean isNonSentinel(float value)
    {
        return Float.compare(value, EMPTY) != 0 && Float.compare(value, REMOVED) != 0;
    }

    private static final class ImmutableFloatHashSet extends AbstractImmutableFloatSet implements Serializable
    {
        private static final long serialVersionUID = 1L;
        private final float[] table;
        private final int occupied;
        // The 32 bits of this integer indicate whether the items 0.0f to 31.0f are present in the set.
        private final int zeroToThirtyOne;
        private final int zeroToThirtyOneOccupied;

        private ImmutableFloatHashSet(float[] table, int occupied, int zeroToThirtyOne, int zeroToThirtyOneOccupied)
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
                throw new IllegalArgumentException("Use FloatSets.immutable.with() to instantiate an optimized collection");
            }
        }

        public static ImmutableFloatSet newSetWith(float... elements)
        {
            return FloatHashSet.newSetWith(elements).toImmutable();
        }

        @Override
        public int hashCode()
        {
            int result = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result += Float.floatToIntBits(value);
                zeroToThirtyOne &= ~(1 << (int) value);
            }
            if (this.table != null)
            {
                for (int i = 0; i < this.table.length; i++)
                {
                    if (isNonSentinel(this.table[i]))
                    {
                        result += Float.floatToIntBits(this.table[i]);
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
                    float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                    if (count > 0)
                    {
                        appendable.append(separator);
                    }
                    count++;
                    appendable.append(String.valueOf(value));
                    zeroToThirtyOne &= ~(1 << (int) value);
                }

                for (float value : this.table)
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
        public FloatIterator floatIterator()
        {
            return new InternalFloatIterator();
        }

        @Override
        public float[] toArray()
        {
            float[] array = new float[this.size()];

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
        public float[] toArray(float[] array)
        {
            if (array.length < this.size())
            {
                array = new float[this.size()];
            }

            int j = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
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
        public boolean contains(float value)
        {
            if (isBetweenZeroAndThirtyOne(value))
            {
                int temp = this.zeroToThirtyOne;
                return ((temp >>> (int) value) & 1) != 0;
            }
            return Float.compare(this.table[this.probe(value)], value) == 0;
        }

        @Override
        public void each(FloatProcedure procedure)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                procedure.value(value);
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (float value : this.table)
            {
                if (isNonSentinel(value))
                {
                    procedure.value(value);
                }
            }
        }

        @Override
        public ImmutableFloatSet select(FloatPredicate predicate)
        {
            return this.select(predicate, new FloatHashSet()).toImmutable();
        }

        @Override
        public ImmutableFloatSet reject(FloatPredicate predicate)
        {
            return this.reject(predicate, new FloatHashSet()).toImmutable();
        }

        @Override
        public <V> ImmutableSet<V> collect(FloatToObjectFunction<? extends V> function)
        {
            MutableSet<V> set = this.collect(function, UnifiedSet.newSet(this.size()));
            return set.toImmutable();
        }

        @Override
        public float detectIfNone(FloatPredicate predicate, float ifNone)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return value;
                }
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (float value : this.table)
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
        public int count(FloatPredicate predicate)
        {
            int count = 0;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    count++;
                }
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (float value : this.table)
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
        public boolean anySatisfy(FloatPredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (predicate.accept(value))
                {
                    return true;
                }
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (float value : this.table)
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
        public boolean allSatisfy(FloatPredicate predicate)
        {
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                if (!predicate.accept(value))
                {
                    return false;
                }
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (float value : this.table)
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
        public boolean noneSatisfy(FloatPredicate predicate)
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
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                double adjustedValue = value - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (float value : this.table)
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
        public float max()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            float max = 31 - Integer.numberOfLeadingZeros(this.zeroToThirtyOne);
            boolean isMaxSet = this.zeroToThirtyOneOccupied != 0;

            for (float value : this.table)
            {
                if (isNonSentinel(value) && (!isMaxSet || Float.compare(max, value) < 0))
                {
                    max = value;
                    isMaxSet = true;
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
            float min = (float) Integer.numberOfTrailingZeros(this.zeroToThirtyOne);
            boolean isMinSet = this.zeroToThirtyOneOccupied != 0;

            for (float value : this.table)
            {
                if (isNonSentinel(value) && (!isMinSet || Float.compare(value, min) < 0))
                {
                    min = value;
                    isMinSet = true;
                }
            }
            return min;
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
        {
            T result = injectedValue;
            int zeroToThirtyOne = this.zeroToThirtyOne;
            while (zeroToThirtyOne != 0)
            {
                float value = (float) Integer.numberOfTrailingZeros(zeroToThirtyOne);
                result = function.valueOf(result, value);
                zeroToThirtyOne &= ~(1 << (int) value);
            }

            for (float value : this.table)
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
            return new ImmutableFloatSetSerializationProxy(this);
        }

        // exposed for testing
        int probe(float element)
        {
            int index = this.spreadAndMask(element);
            float valueAtIndex = this.table[index];

            if (Float.compare(valueAtIndex, element) == 0 || Float.compare(valueAtIndex, EMPTY) == 0)
            {
                return index;
            }

            int removedIndex = Float.compare(valueAtIndex, REMOVED) == 0 ? index : -1;
            for (int i = 1; i < INITIAL_LINEAR_PROBE; i++)
            {
                int nextIndex = (index + i) & (this.table.length - 1);
                valueAtIndex = this.table[nextIndex];
                if (Float.compare(valueAtIndex, element) == 0)
                {
                    return nextIndex;
                }
                if (Float.compare(valueAtIndex, EMPTY) == 0)
                {
                    return removedIndex == -1 ? nextIndex : removedIndex;
                }
                if (Float.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            return this.probeTwo(element, removedIndex);
        }

        int probeTwo(float element, int removedIndex)
        {
            int index = this.spreadTwoAndMask(element);
            for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
            {
                int nextIndex = (index + i) & (this.table.length - 1);
                float valueAtIndex = this.table[nextIndex];
                if (Float.compare(valueAtIndex, element) == 0)
                {
                    return nextIndex;
                }
                if (Float.compare(valueAtIndex, EMPTY) == 0)
                {
                    return removedIndex == -1 ? nextIndex : removedIndex;
                }
                if (Float.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            return this.probeThree(element, removedIndex);
        }

        int probeThree(float element, int removedIndex)
        {
            int nextIndex = Integer.reverse(SpreadFunctions.floatSpreadOne(element));
            int spreadTwo = Integer.reverse(SpreadFunctions.floatSpreadTwo(element)) | 1;

            while (true)
            {
                nextIndex = this.mask(nextIndex + spreadTwo);
                float valueAtIndex = this.table[nextIndex];
                if (Float.compare(valueAtIndex, element) == 0)
                {
                    return nextIndex;
                }
                if (Float.compare(valueAtIndex, EMPTY) == 0)
                {
                    return removedIndex == -1 ? nextIndex : removedIndex;
                }
                if (Float.compare(valueAtIndex, REMOVED) == 0 && removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
        }

        // exposed for testing
        int spreadAndMask(float element)
        {
            int code = SpreadFunctions.floatSpreadOne(element);
            return this.mask(code);
        }

        int spreadTwoAndMask(float element)
        {
            int code = SpreadFunctions.floatSpreadTwo(element);
            return this.mask(code);
        }

        private int mask(int spread)
        {
            return spread & (this.table.length - 1);
        }

        private class InternalFloatIterator implements FloatIterator
        {
            private int count;
            private int position;
            private float zeroToThirtyOne;

            public boolean hasNext()
            {
                return this.count < ImmutableFloatHashSet.this.size();
            }

            public float next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                while (this.zeroToThirtyOne < 32)
                {
                    if (ImmutableFloatHashSet.this.contains(this.zeroToThirtyOne))
                    {
                        float result = this.zeroToThirtyOne;
                        this.zeroToThirtyOne++;
                        return result;
                    }
                    this.zeroToThirtyOne++;
                }

                float[] table = ImmutableFloatHashSet.this.table;
                while (!isNonSentinel(table[this.position]))
                {
                    this.position++;
                }
                float result = table[this.position];
                this.position++;
                return result;
            }
        }
    }

    private class InternalFloatIterator implements MutableFloatIterator
    {
        private int count;
        private int position;
        private float zeroToThirtyOne;

        @Override
        public boolean hasNext()
        {
            return this.count < FloatHashSet.this.size();
        }

        @Override
        public float next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;

            while (this.zeroToThirtyOne < 32)
            {
                if (FloatHashSet.this.contains(this.zeroToThirtyOne))
                {
                    float result = this.zeroToThirtyOne;
                    this.zeroToThirtyOne++;
                    return result;
                }
                this.zeroToThirtyOne++;
            }

            float[] table = FloatHashSet.this.table;
            while (!isNonSentinel(table[this.position]))
            {
                this.position++;
            }
            float result = table[this.position];
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
            float removeValue;
            if (this.zeroToThirtyOne <= 32.0f && this.position == 0)
            {
                if (FloatHashSet.this.zeroToThirtyOne != (FloatHashSet.this.zeroToThirtyOne | 1 << ((int) this.zeroToThirtyOne - 1)))
                {
                    throw new IllegalStateException();
                }
                removeValue = this.zeroToThirtyOne - 1;
            }
            else if (Float.compare(FloatHashSet.this.table[this.position - 1], REMOVED) == 0)
            {
                throw new IllegalStateException();
            }
            else
            {
                removeValue = FloatHashSet.this.table[this.position - 1];
            }
            if (FloatHashSet.isBetweenZeroAndThirtyOne(removeValue))
            {
                FloatHashSet.this.removeZeroToThirtyOne(removeValue);
            }
            else if (Float.compare(FloatHashSet.this.table[this.position - 1], removeValue) == 0)
            {
                if (FloatHashSet.this.copyOnWrite)
                {
                    FloatHashSet.this.copyTable();
                }
                FloatHashSet.this.table[position - 1] = REMOVED;
                FloatHashSet.this.occupiedWithData--;
                FloatHashSet.this.occupiedWithSentinels++;
            }

            this.count--;
        }
    }
}
