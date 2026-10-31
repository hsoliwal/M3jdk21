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
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableByteSetSerializationProxy;
import org.eclipse.collections.impl.set.immutable.primitive.AbstractImmutableByteSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;

/**
 * This file was automatically generated from template file immutablePrimitiveMapKeySet.stg
 *
 * @since 6.0.
 */
class ImmutableByteMapKeySet extends AbstractImmutableByteSet implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final byte EMPTY_KEY = (byte) 0;
    private static final byte REMOVED_KEY = (byte) 1;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 1;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private final byte[] keys;
    private final int occupiedWithData;
    private final boolean containsZeroKey;
    private final boolean containsOneKey;

    ImmutableByteMapKeySet(byte[] keys, int occupiedWithData, boolean containsZeroKey, boolean containsOneKey)
    {
        this.keys = keys;
        this.occupiedWithData = occupiedWithData;
        this.containsZeroKey = containsZeroKey;
        this.containsOneKey = containsOneKey;
    }

    private static boolean isEmptyKey(byte key)
    {
        return key == EMPTY_KEY;
    }

    private static boolean isRemovedKey(byte key)
    {
        return key == REMOVED_KEY;
    }

    private static boolean isNonSentinel(byte key)
    {
        return !isEmptyKey(key) && !isRemovedKey(key);
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        if (this.containsZeroKey)
        {
            result += (int) EMPTY_KEY;
        }
        if (this.containsOneKey)
        {
            result += (int) REMOVED_KEY;
        }
        if (this.keys != null)
        {
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    result += (int) this.keys[i];
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
    public ByteIterator byteIterator()
    {
        return new InternalByteIterator();
    }

    @Override
    public byte[] toArray()
    {
        byte[] array = new byte[this.size()];
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
    public byte[] toArray(byte[] array)
    {
        if (array.length < this.size())
        {
            array = new byte[this.size()];
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
    public boolean contains(byte value)
    {
        if (value == EMPTY_KEY)
        {
            return this.containsZeroKey;
        }
        if (value == REMOVED_KEY)
        {
            return this.containsOneKey;
        }
        return this.keys[this.probe(value)] == value;
    }

    @Override
    public void forEach(ByteProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
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
    public ImmutableByteSet select(BytePredicate predicate)
    {
        ByteHashSet result = new ByteHashSet();
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
    public ImmutableByteSet reject(BytePredicate predicate)
    {
        ByteHashSet result = new ByteHashSet();
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
    public <V> ImmutableSet<V> collect(ByteToObjectFunction<? extends V> function)
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
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
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
    public int count(BytePredicate predicate)
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
    public boolean anySatisfy(BytePredicate predicate)
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
    public boolean allSatisfy(BytePredicate predicate)
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
    public boolean noneSatisfy(BytePredicate predicate)
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
        if (this.keys != null)
        {
            for (int i = 0; i < this.keys.length; i++)
            {
                if (isNonSentinel(this.keys[i]))
                {
                    long adjustedValue = this.keys[i] - compensation;
                    long nextSum = result + adjustedValue;
                    compensation = nextSum - result - adjustedValue;
                    result = nextSum;
                }
            }
        }
        return result;
    }

    @Override
    public byte max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }

        byte max = 0;
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
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMaxSet || max < this.keys[i]))
            {
                max = this.keys[i];
                isMaxSet = true;
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

        byte min = 0;
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
            if (isNonSentinel(this.keys[i]) && (!isMinSet || this.keys[i] < min))
            {
                min = this.keys[i];
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
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
        return new ImmutableByteSetSerializationProxy(this);
    }

    // exposed for testing
    int probe(byte element)
    {
        int index = this.spreadAndMask(element);
        byte keyAtIndex = this.keys[index];

        if (keyAtIndex == element || keyAtIndex == EMPTY_KEY)
        {
            return index;
        }

        int removedIndex = keyAtIndex == REMOVED_KEY ? index : -1;
        int nextIndex = index;
        int probe = 17;
        // loop until an empty slot is reached
        while (true)
        {
            // Probe algorithm: 17*n*(n+1)/2 where n = number of collisions
            nextIndex += probe;
            probe += 17;
            nextIndex &= this.keys.length - 1;
            if (this.keys[nextIndex] == element)
            {
                return nextIndex;
            }
            if (this.keys[nextIndex] == REMOVED_KEY)
            {
                if (removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            else if (this.keys[nextIndex] == EMPTY_KEY)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
        }
    }

    // exposed for testing
    int spreadAndMask(byte element)
    {
        // No spreading necessary for 8-bit types
        return this.mask(element);
    }

    private int mask(int spread)
    {
        return spread & (this.keys.length - 1);
    }

    private class InternalByteIterator implements ByteIterator
    {
        private int count;
        private int position;
        private boolean handledZero;
        private boolean handledOne;

        @Override
        public boolean hasNext()
        {
            return this.count < ImmutableByteMapKeySet.this.size();
        }

        @Override
        public byte next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;

            if (!this.handledZero)
            {
                this.handledZero = true;
                if (ImmutableByteMapKeySet.this.containsZeroKey)
                {
                    return ImmutableByteMapKeySet.EMPTY_KEY;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ImmutableByteMapKeySet.this.containsOneKey)
                {
                    return ImmutableByteMapKeySet.REMOVED_KEY;
                }
            }

            byte[] keys = ImmutableByteMapKeySet.this.keys;
            while (!ImmutableByteMapKeySet.isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            byte result = ImmutableByteMapKeySet.this.keys[this.position];
            this.position++;
            return result;
        }
    }
}
