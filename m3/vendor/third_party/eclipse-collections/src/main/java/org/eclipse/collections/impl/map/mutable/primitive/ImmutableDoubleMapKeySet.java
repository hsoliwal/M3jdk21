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
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableDoubleSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableDoubleSetSerializationProxy;
import org.eclipse.collections.impl.set.immutable.primitive.AbstractImmutableDoubleSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;

/**
 * This file was automatically generated from template file immutablePrimitiveMapKeySet.stg
 *
 * @since 6.0.
 */
class ImmutableDoubleMapKeySet extends AbstractImmutableDoubleSet implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final double EMPTY_KEY = 0.0;
    private static final double REMOVED_KEY = 1.0;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 8;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private final double[] keys;
    private final int occupiedWithData;
    private final boolean containsZeroKey;
    private final boolean containsOneKey;

    ImmutableDoubleMapKeySet(double[] keys, int occupiedWithData, boolean containsZeroKey, boolean containsOneKey)
    {
        this.keys = keys;
        this.occupiedWithData = occupiedWithData;
        this.containsZeroKey = containsZeroKey;
        this.containsOneKey = containsOneKey;
    }

    private static boolean isEmptyKey(double key)
    {
        return Double.compare(key, EMPTY_KEY) == 0;
    }

    private static boolean isRemovedKey(double key)
    {
        return Double.compare(key, REMOVED_KEY) == 0;
    }

    private static boolean isNonSentinel(double key)
    {
        return !isEmptyKey(key) && !isRemovedKey(key);
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        if (this.containsZeroKey)
        {
            result += (int) (Double.doubleToLongBits(EMPTY_KEY) ^ Double.doubleToLongBits(EMPTY_KEY) >>> 32);
        }
        if (this.containsOneKey)
        {
            result += (int) (Double.doubleToLongBits(REMOVED_KEY) ^ Double.doubleToLongBits(REMOVED_KEY) >>> 32);
        }
        if (this.keys != null)
        {
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    result += (int) (Double.doubleToLongBits(this.keys[i]) ^ Double.doubleToLongBits(this.keys[i]) >>> 32);
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
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    if (count > 0)
                    {
                        appendable.append(separator);
                    }
                    count++;
                    appendable.append(String.valueOf(this.keys[i]));
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
        if (this.keys != null)
        {
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    array[index] = this.keys[i];
                    index++;
                }
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
        if (this.keys != null)
        {
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    array[index] = this.keys[i];
                    index++;
                }
            }
        }
        return array;
    }

    @Override
    public boolean contains(double value)
    {
        if (Double.compare(value, EMPTY_KEY) == 0)
        {
            return this.containsZeroKey;
        }
        if (Double.compare(value, REMOVED_KEY) == 0)
        {
            return this.containsOneKey;
        }
        return Double.compare(this.keys[this.probe(value)], value) == 0;
    }

    @Override
    public void forEach(DoubleProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        if (this.containsZeroKey)
        {
            procedure.value(EMPTY_KEY);
        }
        if (this.containsOneKey)
        {
            procedure.value(REMOVED_KEY);
        }
        if (this.keys != null)
        {
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    procedure.value(this.keys[i]);
                }
            }
        }
    }

    @Override
    public ImmutableDoubleSet select(DoublePredicate predicate)
    {
        DoubleHashSet result = new DoubleHashSet();
        if (this.containsZeroKey && predicate.accept(EMPTY_KEY))
        {
            result.add(EMPTY_KEY);
        }
        if (this.containsOneKey && predicate.accept(REMOVED_KEY))
        {
            result.add(REMOVED_KEY);
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.keys[i]))
            {
                result.add(this.keys[i]);
            }
        }
        return result.toImmutable();
    }

    @Override
    public ImmutableDoubleSet reject(DoublePredicate predicate)
    {
        DoubleHashSet result = new DoubleHashSet();
        if (this.containsZeroKey && !predicate.accept(EMPTY_KEY))
        {
            result.add(EMPTY_KEY);
        }
        if (this.containsOneKey && !predicate.accept(REMOVED_KEY))
        {
            result.add(REMOVED_KEY);
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.keys[i]))
            {
                result.add(this.keys[i]);
            }
        }
        return result.toImmutable();
    }

    @Override
    public <V> ImmutableSet<V> collect(DoubleToObjectFunction<? extends V> function)
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
        if (this.keys != null)
        {
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    target.add(function.valueOf(this.keys[i]));
                }
            }
        }
        return target.toImmutable();
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        if (this.containsZeroKey && predicate.accept(EMPTY_KEY))
        {
            return EMPTY_KEY;
        }
        if (this.containsOneKey && predicate.accept(REMOVED_KEY))
        {
            return REMOVED_KEY;
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.keys[i]))
            {
                return this.keys[i];
            }
        }
        return ifNone;
    }

    @Override
    public int count(DoublePredicate predicate)
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
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.keys[i]))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        if (this.containsZeroKey && predicate.accept(EMPTY_KEY))
        {
            return true;
        }
        if (this.containsOneKey && predicate.accept(REMOVED_KEY))
        {
            return true;
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.keys[i]))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        if (this.containsZeroKey && !predicate.accept(EMPTY_KEY))
        {
            return false;
        }
        if (this.containsOneKey && !predicate.accept(REMOVED_KEY))
        {
            return false;
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.keys[i]))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        if (this.containsZeroKey && predicate.accept(EMPTY_KEY))
        {
            return false;
        }
        if (this.containsOneKey && predicate.accept(REMOVED_KEY))
        {
            return false;
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.keys[i]))
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
        if (this.keys != null)
        {
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    double adjustedValue = this.keys[i] - compensation;
                    double nextSum = result + adjustedValue;
                    compensation = nextSum - result - adjustedValue;
                    result = nextSum;
                }
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

        double max = 0;
        boolean isMaxSet = false;

        if (this.containsZeroKey)
        {
            max = EMPTY_KEY;
            isMaxSet = true;
        }
        if (this.containsOneKey && (!isMaxSet || Double.compare(max, REMOVED_KEY) < 0))
        {
            max = REMOVED_KEY;
            isMaxSet = true;
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMaxSet || Double.compare(max, this.keys[i]) < 0))
            {
                max = this.keys[i];
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

        double min = 0;
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

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMinSet || Double.compare(this.keys[i], min) < 0))
            {
                min = this.keys[i];
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
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
        if (this.keys != null)
        {
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    result = function.valueOf(result, this.keys[i]);
                }
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
        int index = this.mask((int) element);
        double keyAtIndex = this.keys[index];

        if (Double.compare(keyAtIndex, element) == 0 || Double.compare(keyAtIndex, EMPTY_KEY) == 0)
        {
            return index;
        }

        int removedIndex = Double.compare(keyAtIndex, REMOVED_KEY) == 0 ? index : -1;
        for (int i = 1; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.keys.length - 1);
            keyAtIndex = this.keys[nextIndex];
            if (Double.compare(keyAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Double.compare(keyAtIndex, EMPTY_KEY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Double.compare(keyAtIndex, REMOVED_KEY) == 0 && removedIndex == -1)
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
            int nextIndex = (index + i) & (this.keys.length - 1);
            double keyAtIndex = this.keys[nextIndex];
            if (Double.compare(keyAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Double.compare(keyAtIndex, EMPTY_KEY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Double.compare(keyAtIndex, REMOVED_KEY) == 0 && removedIndex == -1)
            {
                removedIndex = nextIndex;
            }
        }
        return this.probeThree(element, removedIndex);
    }

    int probeThree(double element, int removedIndex)
    {
        int nextIndex = (int) SpreadFunctions.doubleSpreadOne(element);
        int spreadTwo = (int) Long.reverse(SpreadFunctions.doubleSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask(nextIndex + spreadTwo);
            double keyAtIndex = this.keys[nextIndex];
            if (Double.compare(keyAtIndex, element) == 0)
            {
                return nextIndex;
            }
            if (Double.compare(keyAtIndex, EMPTY_KEY) == 0)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
            if (Double.compare(keyAtIndex, REMOVED_KEY) == 0 && removedIndex == -1)
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
        return spread & (this.keys.length - 1);
    }

    private class InternalDoubleIterator implements DoubleIterator
    {
        private int count;
        private int position;
        private boolean handledZero;
        private boolean handledOne;

        @Override
        public boolean hasNext()
        {
            return this.count < ImmutableDoubleMapKeySet.this.size();
        }

        @Override
        public double next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;

            if (!this.handledZero)
            {
                this.handledZero = true;
                if (ImmutableDoubleMapKeySet.this.containsZeroKey)
                {
                    return ImmutableDoubleMapKeySet.EMPTY_KEY;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ImmutableDoubleMapKeySet.this.containsOneKey)
                {
                    return ImmutableDoubleMapKeySet.REMOVED_KEY;
                }
            }

            double[] keys = ImmutableDoubleMapKeySet.this.keys;
            while (!ImmutableDoubleMapKeySet.isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            double result = ImmutableDoubleMapKeySet.this.keys[this.position];
            this.position++;
            return result;
        }
    }
}
