/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableIntSetSerializationProxy;
import org.eclipse.collections.impl.set.immutable.primitive.AbstractImmutableIntSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

/**
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapKeySet.stg
 *
 * @since 6.0.
 */
class ImmutableIntIntMapKeySet extends AbstractImmutableIntSet implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final int EMPTY_KEY = 0;
    private static final int REMOVED_KEY = 1;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 4;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private final int[] keysValues;
    private final int occupiedWithData;
    private final boolean containsZeroKey;
    private final boolean containsOneKey;

    ImmutableIntIntMapKeySet(int[] keysValues, int occupiedWithData, boolean containsZeroKey, boolean containsOneKey)
    {
        this.keysValues = keysValues;
        this.occupiedWithData = occupiedWithData;
        this.containsZeroKey = containsZeroKey;
        this.containsOneKey = containsOneKey;
    }

    private static boolean isEmptyKey(int key)
    {
        return key == EMPTY_KEY;
    }

    private static boolean isRemovedKey(int key)
    {
        return key == REMOVED_KEY;
    }

    private static boolean isNonSentinel(int key)
    {
        return !isEmptyKey(key) && !isRemovedKey(key);
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        if (this.containsZeroKey)
        {
            result += EMPTY_KEY;
        }
        if (this.containsOneKey)
        {
            result += REMOVED_KEY;
        }
        if (this.keysValues != null)
        {
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    result += this.keysValues[i];
                }
            }
        }
        return result;
    }

    @Override
    public int size()
    {
        return this.occupiedWithData + (this.containsOneKey ? 1 : 0) + (this.containsZeroKey ? 1 : 0);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            int count = 0;
            if (this.containsZeroKey)
            {
                appendable.append(String.valueOf(EMPTY_KEY));
                count++;
            }
            if (this.containsOneKey)
            {
                if (count > 0)
                {
                    appendable.append(separator);
                }
                count++;
                appendable.append(String.valueOf(REMOVED_KEY));
            }
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    if (count > 0)
                    {
                        appendable.append(separator);
                    }
                    count++;
                    appendable.append(String.valueOf(this.keysValues[i]));
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
        int index = 0;
        if (this.containsZeroKey)
        {
            array[index] = EMPTY_KEY;
            index++;
        }
        if (this.containsOneKey)
        {
            array[index] = REMOVED_KEY;
            index++;
        }
        if (this.keysValues != null)
        {
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    array[index] = this.keysValues[i];
                    index++;
                }
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
        int index = 0;
        if (this.containsZeroKey)
        {
            array[index] = EMPTY_KEY;
            index++;
        }
        if (this.containsOneKey)
        {
            array[index] = REMOVED_KEY;
            index++;
        }
        if (this.keysValues != null)
        {
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    array[index] = this.keysValues[i];
                    index++;
                }
            }
        }
        return array;
    }

    @Override
    public boolean contains(int value)
    {
        if (value == EMPTY_KEY)
        {
            return this.containsZeroKey;
        }
        if (value == REMOVED_KEY)
        {
            return this.containsOneKey;
        }
        return this.keysValues[this.probe(value)] == value;
    }

    @Override
    public void forEach(IntProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        if (this.containsZeroKey)
        {
            procedure.value(EMPTY_KEY);
        }
        if (this.containsOneKey)
        {
            procedure.value(REMOVED_KEY);
        }
        if (this.keysValues != null)
        {
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    procedure.value(this.keysValues[i]);
                }
            }
        }
    }

    @Override
    public ImmutableIntSet select(IntPredicate predicate)
    {
        IntHashSet result = new IntHashSet();
        if (this.containsZeroKey && predicate.accept(EMPTY_KEY))
        {
            result.add(EMPTY_KEY);
        }
        if (this.containsOneKey && predicate.accept(REMOVED_KEY))
        {
            result.add(REMOVED_KEY);
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && predicate.accept(this.keysValues[i]))
            {
                result.add(this.keysValues[i]);
            }
        }
        return result.toImmutable();
    }

    @Override
    public ImmutableIntSet reject(IntPredicate predicate)
    {
        IntHashSet result = new IntHashSet();
        if (this.containsZeroKey && !predicate.accept(EMPTY_KEY))
        {
            result.add(EMPTY_KEY);
        }
        if (this.containsOneKey && !predicate.accept(REMOVED_KEY))
        {
            result.add(REMOVED_KEY);
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && !predicate.accept(this.keysValues[i]))
            {
                result.add(this.keysValues[i]);
            }
        }
        return result.toImmutable();
    }

    @Override
    public <V> ImmutableSet<V> collect(IntToObjectFunction<? extends V> function)
    {
        MutableSet<V> target = UnifiedSet.newSet(this.size());
        if (this.containsZeroKey)
        {
            target.add(function.valueOf(EMPTY_KEY));
        }
        if (this.containsOneKey)
        {
            target.add(function.valueOf(REMOVED_KEY));
        }
        if (this.keysValues != null)
        {
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    target.add(function.valueOf(this.keysValues[i]));
                }
            }
        }
        return target.toImmutable();
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        if (this.containsZeroKey && predicate.accept(EMPTY_KEY))
        {
            return EMPTY_KEY;
        }
        if (this.containsOneKey && predicate.accept(REMOVED_KEY))
        {
            return REMOVED_KEY;
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && predicate.accept(this.keysValues[i]))
            {
                return this.keysValues[i];
            }
        }
        return ifNone;
    }

    @Override
    public int count(IntPredicate predicate)
    {
        int count = 0;
        if (this.containsZeroKey && predicate.accept(EMPTY_KEY))
        {
            count++;
        }
        if (this.containsOneKey && predicate.accept(REMOVED_KEY))
        {
            count++;
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && predicate.accept(this.keysValues[i]))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        if (this.containsZeroKey && predicate.accept(EMPTY_KEY))
        {
            return true;
        }
        if (this.containsOneKey && predicate.accept(REMOVED_KEY))
        {
            return true;
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && predicate.accept(this.keysValues[i]))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        if (this.containsZeroKey && !predicate.accept(EMPTY_KEY))
        {
            return false;
        }
        if (this.containsOneKey && !predicate.accept(REMOVED_KEY))
        {
            return false;
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && !predicate.accept(this.keysValues[i]))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        if (this.containsZeroKey && predicate.accept(EMPTY_KEY))
        {
            return false;
        }
        if (this.containsOneKey && predicate.accept(REMOVED_KEY))
        {
            return false;
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && predicate.accept(this.keysValues[i]))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public long sum()
    {
        long result = 0L;
        long compensation = 0L;
        if (this.containsZeroKey)
        {
            long adjustedValue = EMPTY_KEY - compensation;
            long nextSum = result + adjustedValue;
            compensation = nextSum - result - adjustedValue;
            result = nextSum;
        }
        if (this.containsOneKey)
        {
            long adjustedValue = REMOVED_KEY - compensation;
            long nextSum = result + adjustedValue;
            compensation = nextSum - result - adjustedValue;
            result = nextSum;
        }
        if (this.keysValues != null)
        {
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    long adjustedValue = this.keysValues[i] - compensation;
                    long nextSum = result + adjustedValue;
                    compensation = nextSum - result - adjustedValue;
                    result = nextSum;
                }
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

        int max = 0;
        boolean isMaxSet = false;

        if (this.containsZeroKey)
        {
            max = EMPTY_KEY;
            isMaxSet = true;
        }
        if (this.containsOneKey && (!isMaxSet || max < REMOVED_KEY))
        {
            max = REMOVED_KEY;
            isMaxSet = true;
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && (!isMaxSet || max < this.keysValues[i]))
            {
                max = this.keysValues[i];
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

        int min = 0;
        boolean isMinSet = false;

        if (this.containsZeroKey)
        {
            min = EMPTY_KEY;
            isMinSet = true;
        }
        if (this.containsOneKey && (!isMinSet || REMOVED_KEY < min))
        {
            min = REMOVED_KEY;
            isMinSet = true;
        }

        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && (!isMinSet || this.keysValues[i] < min))
            {
                min = this.keysValues[i];
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        T result = injectedValue;
        if (this.containsZeroKey)
        {
            result = function.valueOf(result, EMPTY_KEY);
        }
        if (this.containsOneKey)
        {
            result = function.valueOf(result, REMOVED_KEY);
        }
        if (this.keysValues != null)
        {
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    result = function.valueOf(result, this.keysValues[i]);
                }
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
        int index = this.mask((int) element) << 1;
        int keyAtIndex = this.keysValues[index];

        if (keyAtIndex == element || keyAtIndex == EMPTY_KEY)
        {
            return index;
        }

        int removedIndex = keyAtIndex == REMOVED_KEY ? index : -1;
        for (int i = 2; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            int nextIndex = (index + i) & (this.keysValues.length - 1);
            keyAtIndex = this.keysValues[nextIndex];
            if (keyAtIndex == element)
            {
                return nextIndex;
            }
            if (keyAtIndex == EMPTY_KEY)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (keyAtIndex == REMOVED_KEY && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeTwo(element, removedIndex);
    }

    int probeTwo(int element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element) << 1;
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            int nextIndex = (index + i) & (this.keysValues.length - 1);
            int keyAtIndex = this.keysValues[nextIndex];
            if (keyAtIndex == element)
            {
                return nextIndex;
            }
            if (keyAtIndex == EMPTY_KEY)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (keyAtIndex == REMOVED_KEY && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeThree(element, removedIndex);
    }

    int probeThree(int element, int removedIndex)
    {
        int nextIndex = (int) SpreadFunctions.intSpreadOne(element) << 1;
        int spreadTwo = Integer.reverse(SpreadFunctions.intSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask((nextIndex >> 1) + spreadTwo) << 1;
            int keyAtIndex = this.keysValues[nextIndex];
            if (keyAtIndex == element)
            {
                return nextIndex;
            }
            if (keyAtIndex == EMPTY_KEY)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (keyAtIndex == REMOVED_KEY && removedIndex == -1)
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
        return spread & ((this.keysValues.length >> 1) - 1);
    }

    private class InternalIntIterator implements IntIterator
    {
        private int count;
        private int position;
        private boolean handledZero;
        private boolean handledOne;

        @Override
        public boolean hasNext()
        {
            return this.count < ImmutableIntIntMapKeySet.this.size();
        }

        @Override
        public int next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;

            if (!this.handledZero)
            {
                this.handledZero = true;
                if (ImmutableIntIntMapKeySet.this.containsZeroKey)
                {
                    return ImmutableIntIntMapKeySet.EMPTY_KEY;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ImmutableIntIntMapKeySet.this.containsOneKey)
                {
                    return ImmutableIntIntMapKeySet.REMOVED_KEY;
                }
            }

            int[] keysValues = ImmutableIntIntMapKeySet.this.keysValues;
            while (!ImmutableIntIntMapKeySet.isNonSentinel(keysValues[this.position]))
            {
                this.position += 2;
            }
            int result = ImmutableIntIntMapKeySet.this.keysValues[this.position];
            this.position += 2;
            return result;
        }
    }
}
