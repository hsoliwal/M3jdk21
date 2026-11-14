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
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableFloatSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableFloatSetSerializationProxy;
import org.eclipse.collections.impl.set.immutable.primitive.AbstractImmutableFloatSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;

/**
 * This file was automatically generated from template file immutablePrimitivePrimitiveMapKeySet.stg
 *
 * @since 6.0.
 */
class ImmutableFloatFloatMapKeySet extends AbstractImmutableFloatSet implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final float EMPTY_KEY = 0.0f;
    private static final float REMOVED_KEY = 1.0f;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 4;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private final float[] keysValues;
    private final int occupiedWithData;
    private final boolean containsZeroKey;
    private final boolean containsOneKey;

    ImmutableFloatFloatMapKeySet(float[] keysValues, int occupiedWithData, boolean containsZeroKey, boolean containsOneKey)
    {
        this.keysValues = keysValues;
        this.occupiedWithData = occupiedWithData;
        this.containsZeroKey = containsZeroKey;
        this.containsOneKey = containsOneKey;
    }

    private static boolean isEmptyKey(float key)
    {
        return Float.compare(key, EMPTY_KEY) == 0;
    }

    private static boolean isRemovedKey(float key)
    {
        return Float.compare(key, REMOVED_KEY) == 0;
    }

    private static boolean isNonSentinel(float key)
    {
        return !isEmptyKey(key) && !isRemovedKey(key);
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        if (this.containsZeroKey)
        {
            result += Float.floatToIntBits(EMPTY_KEY);
        }
        if (this.containsOneKey)
        {
            result += Float.floatToIntBits(REMOVED_KEY);
        }
        if (this.keysValues != null)
        {
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    result += Float.floatToIntBits(this.keysValues[i]);
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
    public FloatIterator floatIterator()
    {
        return new InternalFloatIterator();
    }

    @Override
    public float[] toArray()
    {
        float[] array = new float[this.size()];
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
    public float[] toArray(float[] array)
    {
        if (array.length < this.size())
        {
            array = new float[this.size()];
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
    public boolean contains(float value)
    {
        if (Float.compare(value, EMPTY_KEY) == 0)
        {
            return this.containsZeroKey;
        }
        if (Float.compare(value, REMOVED_KEY) == 0)
        {
            return this.containsOneKey;
        }
        return Float.compare(this.keysValues[this.probe(value)], value) == 0;
    }

    @Override
    public void forEach(FloatProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
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
    public ImmutableFloatSet select(FloatPredicate predicate)
    {
        FloatHashSet result = new FloatHashSet();
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
    public ImmutableFloatSet reject(FloatPredicate predicate)
    {
        FloatHashSet result = new FloatHashSet();
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
    public <V> ImmutableSet<V> collect(FloatToObjectFunction<? extends V> function)
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
    public float detectIfNone(FloatPredicate predicate, float ifNone)
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
    public int count(FloatPredicate predicate)
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
    public boolean anySatisfy(FloatPredicate predicate)
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
    public boolean allSatisfy(FloatPredicate predicate)
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
    public boolean noneSatisfy(FloatPredicate predicate)
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
    public double sum()
    {
        double result = 0.0;
        double compensation = 0.0;
        if (this.containsZeroKey)
        {
            double adjustedValue = EMPTY_KEY - compensation;
            double nextSum = result + adjustedValue;
            compensation = nextSum - result - adjustedValue;
            result = nextSum;
        }
        if (this.containsOneKey)
        {
            double adjustedValue = REMOVED_KEY - compensation;
            double nextSum = result + adjustedValue;
            compensation = nextSum - result - adjustedValue;
            result = nextSum;
        }
        if (this.keysValues != null)
        {
            for (int i = 0; i < this.keysValues.length; i += 2)
            {
                if (isNonSentinel(this.keysValues[i]))
                {
                    double adjustedValue = this.keysValues[i] - compensation;
                    double nextSum = result + adjustedValue;
                    compensation = nextSum - result - adjustedValue;
                    result = nextSum;
                }
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

        float max = 0;
        boolean isMaxSet = false;

        if (this.containsZeroKey)
        {
            max = EMPTY_KEY;
            isMaxSet = true;
        }
        if (this.containsOneKey && (!isMaxSet || Float.compare(max, REMOVED_KEY) < 0))
        {
            max = REMOVED_KEY;
            isMaxSet = true;
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && (!isMaxSet || Float.compare(max, this.keysValues[i]) < 0))
            {
                max = this.keysValues[i];
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

        float min = 0;
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
            if (isNonSentinel(this.keysValues[i]) && (!isMinSet || Float.compare(this.keysValues[i], min) < 0))
            {
                min = this.keysValues[i];
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
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
        return new ImmutableFloatSetSerializationProxy(this);
    }

    // exposed for testing
    int probe(float element)
    {
        int index = this.mask((int) element) << 1;
        float keyAtIndex = this.keysValues[index];

        if (Float.compare(keyAtIndex, element) == 0 || Float.compare(keyAtIndex, EMPTY_KEY) == 0)
        {
            return index;
        }

        int removedIndex = Float.compare(keyAtIndex, REMOVED_KEY) == 0 ? index : -1;
        for (int i = 2; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            int nextIndex = (index + i) & (this.keysValues.length - 1);
            keyAtIndex = this.keysValues[nextIndex];
            if (Float.compare(keyAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Float.compare(keyAtIndex, EMPTY_KEY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Float.compare(keyAtIndex, REMOVED_KEY) == 0 && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeTwo(element, removedIndex);
    }

    int probeTwo(float element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element) << 1;
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            int nextIndex = (index + i) & (this.keysValues.length - 1);
            float keyAtIndex = this.keysValues[nextIndex];
            if (Float.compare(keyAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Float.compare(keyAtIndex, EMPTY_KEY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Float.compare(keyAtIndex, REMOVED_KEY) == 0 && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeThree(element, removedIndex);
    }

    int probeThree(float element, int removedIndex)
    {
        int nextIndex = (int) SpreadFunctions.floatSpreadOne(element) << 1;
        int spreadTwo = Integer.reverse(SpreadFunctions.floatSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask((nextIndex >> 1) + spreadTwo) << 1;
            float keyAtIndex = this.keysValues[nextIndex];
            if (Float.compare(keyAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Float.compare(keyAtIndex, EMPTY_KEY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Float.compare(keyAtIndex, REMOVED_KEY) == 0 && removedIndex == -1)
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
        return spread & ((this.keysValues.length >> 1) - 1);
    }

    private class InternalFloatIterator implements FloatIterator
    {
        private int count;
        private int position;
        private boolean handledZero;
        private boolean handledOne;

        @Override
        public boolean hasNext()
        {
            return this.count < ImmutableFloatFloatMapKeySet.this.size();
        }

        @Override
        public float next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;

            if (!this.handledZero)
            {
                this.handledZero = true;
                if (ImmutableFloatFloatMapKeySet.this.containsZeroKey)
                {
                    return ImmutableFloatFloatMapKeySet.EMPTY_KEY;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ImmutableFloatFloatMapKeySet.this.containsOneKey)
                {
                    return ImmutableFloatFloatMapKeySet.REMOVED_KEY;
                }
            }

            float[] keysValues = ImmutableFloatFloatMapKeySet.this.keysValues;
            while (!ImmutableFloatFloatMapKeySet.isNonSentinel(keysValues[this.position]))
            {
                this.position += 2;
            }
            float result = ImmutableFloatFloatMapKeySet.this.keysValues[this.position];
            this.position += 2;
            return result;
        }
    }
}
