/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleBytePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.factory.primitive.DoubleByteMaps;
import org.eclipse.collections.api.factory.primitive.ByteDoubleMaps;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.map.primitive.DoubleByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleByteMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleByteMap;
import org.eclipse.collections.api.map.primitive.MutableByteDoubleMap;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.DoubleBytePair;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableDoubleIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyDoubleIterable;
import org.eclipse.collections.impl.set.mutable.primitive.DoubleHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file primitivePrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class DoubleByteHashMap extends AbstractMutableByteValuesMap implements MutableDoubleByteMap, Externalizable, MutableDoubleKeysMap
{
    private static final byte EMPTY_VALUE = (byte) 0;
    private static final long serialVersionUID = 1L;
    private static final double EMPTY_KEY = 0.0;
    private static final double REMOVED_KEY = 1.0;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 8;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private double[] keys;
    private byte[] values;
    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues sentinelValues;

    private boolean copyKeysOnWrite;

    public DoubleByteHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public DoubleByteHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public DoubleByteHashMap(DoubleByteMap map)
    {
        if (map instanceof DoubleByteHashMap && ((DoubleByteHashMap) map).occupiedWithSentinels == 0)
        {
            DoubleByteHashMap hashMap = (DoubleByteHashMap) map;
            this.occupiedWithData = hashMap.occupiedWithData;
            if (hashMap.sentinelValues != null)
            {
                this.sentinelValues = hashMap.sentinelValues.copy();
            }
            this.keys = Arrays.copyOf(hashMap.keys, hashMap.keys.length);
            this.values = Arrays.copyOf(hashMap.values, hashMap.values.length);
        }
        else
        {
            int capacity = this.smallestPowerOfTwoGreaterThan(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY) << 1);
            this.allocateTable(capacity);
            this.putAll(map);
        }
    }

    public static DoubleByteHashMap newWithKeysValues(double key1, byte value1)
    {
        return new DoubleByteHashMap(1).withKeyValue(key1, value1);
    }

    public static DoubleByteHashMap newWithKeysValues(double key1, byte value1, double key2, byte value2)
    {
        return new DoubleByteHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static DoubleByteHashMap newWithKeysValues(double key1, byte value1, double key2, byte value2, double key3, byte value3)
    {
        return new DoubleByteHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static DoubleByteHashMap newWithKeysValues(double key1, byte value1, double key2, byte value2, double key3, byte value3, double key4, byte value4)
    {
        return new DoubleByteHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    private int smallestPowerOfTwoGreaterThan(int n)
    {
        return n > 1 ? Integer.highestOneBit(n - 1) << 1 : 1;
    }

    @Override
    protected int getOccupiedWithData()
    {
        return this.occupiedWithData;
    }

    @Override
    protected SentinelValues getSentinelValues()
    {
        return this.sentinelValues;
    }

    @Override
    protected void setSentinelValuesNull()
    {
        this.sentinelValues = null;
    }

    @Override
    protected byte getEmptyValue()
    {
        return EMPTY_VALUE;
    }

    @Override
    protected int getTableSize()
    {
        return this.values.length;
    }

    @Override
    protected byte getValueAtIndex(int index)
    {
        return this.values[index];
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof DoubleByteMap))
        {
            return false;
        }

        DoubleByteMap other = (DoubleByteMap) obj;

        if (this.size() != other.size())
        {
            return false;
        }

        if (this.sentinelValues == null)
        {
            if (other.containsKey(EMPTY_KEY) || other.containsKey(REMOVED_KEY))
            {
                return false;
            }
        }
        else
        {
            if (this.sentinelValues.containsZeroKey && (!other.containsKey(EMPTY_KEY) || this.sentinelValues.zeroValue != other.getOrThrow(EMPTY_KEY)))
            {
                return false;
            }

            if (this.sentinelValues.containsOneKey && (!other.containsKey(REMOVED_KEY) || this.sentinelValues.oneValue != other.getOrThrow(REMOVED_KEY)))
            {
                return false;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            double key = this.keys[i];
            if (isNonSentinel(key) && (!other.containsKey(key) || this.values[i] != other.getOrThrow(key)))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 0;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                result += (int) (Double.doubleToLongBits(EMPTY_KEY) ^ Double.doubleToLongBits(EMPTY_KEY) >>> 32) ^ (int) this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey)
            {
                result += (int) (Double.doubleToLongBits(REMOVED_KEY) ^ Double.doubleToLongBits(REMOVED_KEY) >>> 32) ^ (int) this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result += (int) (Double.doubleToLongBits(this.keys[i]) ^ Double.doubleToLongBits(this.keys[i]) >>> 32) ^ (int) this.values[i];
            }
        }

        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder appendable = new StringBuilder();

        appendable.append("{");

        boolean first = true;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                appendable.append(EMPTY_KEY).append("=").append(this.sentinelValues.zeroValue);
                first = false;
            }
            if (this.sentinelValues.containsOneKey)
            {
                if (!first)
                {
                    appendable.append(", ");
                }
                appendable.append(REMOVED_KEY).append("=").append(this.sentinelValues.oneValue);
                first = false;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            double key = this.keys[i];
            if (isNonSentinel(key))
            {
                if (!first)
                {
                    appendable.append(", ");
                }
                appendable.append(key).append("=").append(this.values[i]);
                first = false;
            }
        }
        appendable.append("}");

        return appendable.toString();
    }

    @Override
    public MutableByteIterator byteIterator()
    {
        return new InternalByteIterator();
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectByteToObjectFunction<? super V, ? extends V> function)
    {
        V result = injectedValue;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                result = function.valueOf(result, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result = function.valueOf(result, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result = function.valueOf(result, this.values[i]);
            }
        }

        return result;
    }

    @Override
    public void clear()
    {
        this.sentinelValues = null;
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;
        if (this.copyKeysOnWrite)
        {
            this.copyKeys();
        }
        Arrays.fill(this.keys, EMPTY_KEY);
        Arrays.fill(this.values, EMPTY_VALUE);
    }

    @Override
    public void put(double key, byte value)
    {
        if (isEmptyKey(key))
        {
            this.putForEmptySentinel(value);
            return;
        }

        if (isRemovedKey(key))
        {
            this.putForRemovedSentinel(value);
            return;
        }

        int index = this.probe(key);
        double keyAtIndex = this.keys[index];
        if (Double.compare(keyAtIndex, key) == 0)
        {
            this.values[index] = value;
        }
        else
        {
            this.addKeyValueAtIndex(key, value, index);
        }
    }

    private void putForRemovedSentinel(byte value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addRemovedKeyValue(value);
    }

    private void putForEmptySentinel(byte value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addEmptyKeyValue(value);
    }

    @Override
    public void putAll(DoubleByteMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(DoubleByteToByteFunction function)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                this.sentinelValues.zeroValue = function.valueOf(EMPTY_KEY, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                this.sentinelValues.oneValue = function.valueOf(REMOVED_KEY, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                this.values[i] = function.valueOf(this.keys[i], this.values[i]);
            }
        }
    }

    @Override
    public void removeKey(double key)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return;
            }
            this.removeEmptyKey();
            return;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                return;
            }
            this.removeRemovedKey();
            return;
        }
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            this.removeKeyAtIndex(index);
        }
    }

    @Override
    public void remove(double key)
    {
        this.removeKey(key);
    }

    @Override
    public byte removeKeyIfAbsent(double key, byte value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return value;
            }
            byte oldValue = this.sentinelValues.zeroValue;
            this.removeEmptyKey();
            return oldValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                return value;
            }
            byte oldValue = this.sentinelValues.oneValue;
            this.removeRemovedKey();
            return oldValue;
        }
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            byte oldValue = this.values[index];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return value;
    }

    @Override
    public byte getIfAbsentPut(double key, byte value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public byte getAndPut(double key, byte putValue, byte defaultValue)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                byte existingValue = this.sentinelValues.zeroValue;
                this.sentinelValues.zeroValue = putValue;
                return existingValue;
            }
            this.addEmptyKeyValue(putValue);
            return defaultValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
            }
            else if (this.sentinelValues.containsOneKey)
            {
                byte existingValue = this.sentinelValues.oneValue;
                this.sentinelValues.oneValue = putValue;
                return existingValue;
            }
            this.addRemovedKeyValue(putValue);
            return defaultValue;
        }
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            byte existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public byte getIfAbsentPut(double key, ByteFunction0 function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                byte value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            byte value = function.value();
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                byte value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            byte value = function.value();
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        byte value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> byte getIfAbsentPutWith(double key, ByteFunction<? super P> function, P parameter)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                byte value = function.byteValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            byte value = function.byteValueOf(parameter);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                byte value = function.byteValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            byte value = function.byteValueOf(parameter);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        byte value = function.byteValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public byte getIfAbsentPutWithKey(double key, DoubleToByteFunction function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                byte value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            byte value = function.valueOf(key);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                byte value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            byte value = function.valueOf(key);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        byte value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public byte addToValue(double key, byte toBeAdded)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(toBeAdded);
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                this.sentinelValues.zeroValue += toBeAdded;
            }
            else
            {
                this.addEmptyKeyValue(toBeAdded);
            }
            return this.sentinelValues.zeroValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(toBeAdded);
            }
            else if (this.sentinelValues.containsOneKey)
            {
                this.sentinelValues.oneValue += toBeAdded;
            }
            else
            {
                this.addRemovedKeyValue(toBeAdded);
            }
            return this.sentinelValues.oneValue;
        }
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            this.values[index] += toBeAdded;
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, toBeAdded, index);
        return toBeAdded;
    }

    private void addKeyValueAtIndex(double key, byte value, int index)
    {
        if (Double.compare(this.keys[index], REMOVED_KEY) == 0)
        {
            this.occupiedWithSentinels--;
        }
        if (this.copyKeysOnWrite)
        {
            this.copyKeys();
        }
        this.keys[index] = key;
        this.values[index] = value;
        this.occupiedWithData++;
        if (this.occupiedWithData + this.occupiedWithSentinels > this.maxOccupiedWithData())
        {
            this.rehashAndGrow();
        }
    }

    private void removeKeyAtIndex(int index)
    {
        if (this.copyKeysOnWrite)
        {
            this.copyKeys();
        }
        this.keys[index] = REMOVED_KEY;
        this.values[index] = EMPTY_VALUE;
        this.occupiedWithData--;
        this.occupiedWithSentinels++;
    }

    private void copyKeys()
    {
        double[] copy = new double[this.keys.length];
        System.arraycopy(this.keys, 0, copy, 0, this.keys.length);
        this.keys = copy;
        this.copyKeysOnWrite = false;
    }

    @Override
    public byte updateValue(double key, byte initialValueIfAbsent, ByteToByteFunction function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(function.valueOf(initialValueIfAbsent));
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                this.sentinelValues.zeroValue = function.valueOf(this.sentinelValues.zeroValue);
            }
            else
            {
                this.addEmptyKeyValue(function.valueOf(initialValueIfAbsent));
            }
            return this.sentinelValues.zeroValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(function.valueOf(initialValueIfAbsent));
            }
            else if (this.sentinelValues.containsOneKey)
            {
                this.sentinelValues.oneValue = function.valueOf(this.sentinelValues.oneValue);
            }
            else
            {
                this.addRemovedKeyValue(function.valueOf(initialValueIfAbsent));
            }
            return this.sentinelValues.oneValue;
        }
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        byte value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public DoubleByteHashMap withKeyValue(double key1, byte value1)
    {
        this.put(key1, value1);
        return this;
    }

    public DoubleByteHashMap withKeysValues(double key1, byte value1, double key2, byte value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public DoubleByteHashMap withKeysValues(double key1, byte value1, double key2, byte value2, double key3, byte value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public DoubleByteHashMap withKeysValues(double key1, byte value1, double key2, byte value2, double key3, byte value3, double key4, byte value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public DoubleByteHashMap withoutKey(double key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public DoubleByteHashMap withoutAllKeys(DoubleIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    @Override
    public MutableDoubleByteMap asUnmodifiable()
    {
        return new UnmodifiableDoubleByteMap(this);
    }

    @Override
    public MutableDoubleByteMap asSynchronized()
    {
        return new SynchronizedDoubleByteMap(this);
    }

    @Override
    public ImmutableDoubleByteMap toImmutable()
    {
        return DoubleByteMaps.immutable.ofAll(this);
    }

    @Override
    public byte get(double key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public byte getIfAbsent(double key, byte ifAbsent)
    {
        if (isEmptyKey(key) || isRemovedKey(key))
        {
            return this.getForSentinel(key, ifAbsent);
        }
        if (this.occupiedWithSentinels == 0)
        {
            return this.fastGetIfAbsent(key, ifAbsent);
        }
        return this.slowGetIfAbsent(key, ifAbsent);
    }

    private byte getForSentinel(double key, byte ifAbsent)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return ifAbsent;
            }
            return this.sentinelValues.zeroValue;
        }
        if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
        {
            return ifAbsent;
        }
        return this.sentinelValues.oneValue;
    }

    private byte slowGetIfAbsent(double key, byte ifAbsent)
    {
        int index = this.probe(key);
        if (Double.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    private byte fastGetIfAbsent(double key, byte ifAbsent)
    {
        int index = this.mask((int) key);

        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            double keyAtIndex = this.keys[index];
            if (Double.compare(keyAtIndex, key) == 0)
            {
                return this.values[index];
            }
            if (Double.compare(keyAtIndex, EMPTY_KEY) == 0)
            {
                return ifAbsent;
            }
            index = (index + 1) & (this.keys.length - 1);
        }
        return this.slowGetIfAbsentTwo(key, ifAbsent);
    }

    private byte slowGetIfAbsentTwo(double key, byte ifAbsent)
    {
        int index = this.probeTwo(key, -1);
        if (Double.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    @Override
    public byte getOrThrow(double key)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                throw new IllegalStateException("Key " + key + " not present.");
            }
            return this.sentinelValues.zeroValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                throw new IllegalStateException("Key " + key + " not present.");
            }
            return this.sentinelValues.oneValue;
        }
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]))
        {
            return this.values[index];
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(double key)
    {
        if (isEmptyKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsZeroKey;
        }
        if (isRemovedKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsOneKey;
        }
        return Double.compare(this.keys[this.probe(key)], key) == 0;
    }

    @Override
    public void forEachKey(DoubleProcedure procedure)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                procedure.value(EMPTY_KEY);
            }
            if (this.sentinelValues.containsOneKey)
            {
                procedure.value(REMOVED_KEY);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                procedure.value(this.keys[i]);
            }
        }
    }

    @Override
    public void forEachKeyValue(DoubleByteProcedure procedure)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                procedure.value(EMPTY_KEY, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                procedure.value(REMOVED_KEY, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                procedure.value(this.keys[i], this.values[i]);
            }
        }
    }

    @Override
    public LazyDoubleIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<DoubleBytePair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableByteDoubleMap flipUniqueValues()
    {
        MutableByteDoubleMap result = ByteDoubleMaps.mutable.empty();
        this.forEachKeyValue((key, value) ->
        {
            if (result.containsKey(value))
            {
                throw new IllegalStateException("Duplicate value: " + value + " found at key: " + result.get(value) + " and key: " + key);
            }
            result.put(value, key);
        });
        return result;
    }

    @Override
    public DoubleByteHashMap select(DoubleBytePredicate predicate)
    {
        DoubleByteHashMap result = new DoubleByteHashMap();

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(EMPTY_KEY, this.sentinelValues.zeroValue))
            {
                result.put(EMPTY_KEY, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(REMOVED_KEY, this.sentinelValues.oneValue))
            {
                result.put(REMOVED_KEY, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.keys[i], this.values[i]))
            {
                result.put(this.keys[i], this.values[i]);
            }
        }

        return result;
    }

    @Override
    public DoubleByteHashMap reject(DoubleBytePredicate predicate)
    {
        DoubleByteHashMap result = new DoubleByteHashMap();

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && !predicate.accept(EMPTY_KEY, this.sentinelValues.zeroValue))
            {
                result.put(EMPTY_KEY, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey && !predicate.accept(REMOVED_KEY, this.sentinelValues.oneValue))
            {
                result.put(REMOVED_KEY, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.keys[i], this.values[i]))
            {
                result.put(this.keys[i], this.values[i]);
            }
        }
        return result;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                out.writeDouble(EMPTY_KEY);
                out.writeByte(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeDouble(REMOVED_KEY);
                out.writeByte(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeDouble(this.keys[i]);
                out.writeByte(this.values[i]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readDouble(), in.readByte());
        }
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
        int oldLength = this.keys.length;
        double[] old = this.keys;
        byte[] oldValues = this.values;
        this.allocateTable(newCapacity);
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;

        for (int i = 0; i < oldLength; i++)
        {
            if (isNonSentinel(old[i]))
            {
                this.put(old[i], oldValues[i]);
            }
        }
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

    protected void allocateTable(int sizeToAllocate)
    {
        this.keys = new double[sizeToAllocate];
        this.values = new byte[sizeToAllocate];
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
    protected boolean isNonSentinelAtIndex(int index)
    {
        return !isEmptyKey(this.keys[index]) && !isRemovedKey(this.keys[index]);
    }

    private int maxOccupiedWithData()
    {
        return this.keys.length >> 1;
    }

    private class InternalByteIterator implements MutableByteIterator
    {
        private int count;
        private int position;
        private double lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < DoubleByteHashMap.this.size();
        }

        @Override
        public byte next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;
            this.canRemove = true;

            if (!this.handledZero)
            {
                this.handledZero = true;
                if (DoubleByteHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return DoubleByteHashMap.this.get(EMPTY_KEY);
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (DoubleByteHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return DoubleByteHashMap.this.get(REMOVED_KEY);
                }
            }
            double[] keys = DoubleByteHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.lastKey = keys[this.position];
            byte result = DoubleByteHashMap.this.values[this.position];
            this.position++;

            return result;
        }

        @Override
        public void remove()
        {
            if (!this.canRemove)
            {
                throw new IllegalStateException();
            }
            DoubleByteHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    private class KeysView extends AbstractLazyDoubleIterable
    {
        @Override
        public DoubleIterator doubleIterator()
        {
            return new UnmodifiableDoubleIterator(new KeySetIterator());
        }

        /**
         * @since 7.0.
         */
        @Override
        public void each(DoubleProcedure procedure)
        {
            DoubleByteHashMap.this.forEachKey(procedure);
        }
    }

    private class KeySetIterator implements MutableDoubleIterator
    {
        private int count;
        private int position;
        private double lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < DoubleByteHashMap.this.size();
        }

        @Override
        public double next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;
            this.canRemove = true;

            if (!this.handledZero)
            {
                this.handledZero = true;
                if (DoubleByteHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (DoubleByteHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            double[] keys = DoubleByteHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.lastKey = keys[this.position];
            this.position++;

            return this.lastKey;
        }

        @Override
        public void remove()
        {
            if (!this.canRemove)
            {
                throw new IllegalStateException();
            }
            DoubleByteHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    @Override
    public MutableDoubleSet keySet()
    {
        return new KeySet();
    }

    private class KeySet extends AbstractMutableDoubleKeySet
    {
        @Override
        protected MutableDoubleKeysMap getOuter()
        {
            return DoubleByteHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return DoubleByteHashMap.this.sentinelValues;
        }

        @Override
        protected double getKeyAtIndex(int index)
        {
            return DoubleByteHashMap.this.keys[index];
        }

        @Override
        protected int getTableSize()
        {
            return DoubleByteHashMap.this.keys.length;
        }

        @Override
        public MutableDoubleIterator doubleIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(DoubleIterable source)
        {
            int oldSize = DoubleByteHashMap.this.size();
            DoubleSet sourceSet = source instanceof DoubleSet ? (DoubleSet) source : source.toSet();
            DoubleByteHashMap retained = DoubleByteHashMap.this.select((double key, byte value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                DoubleByteHashMap.this.keys = retained.keys;
                DoubleByteHashMap.this.values = retained.values;
                DoubleByteHashMap.this.sentinelValues = retained.sentinelValues;
                DoubleByteHashMap.this.occupiedWithData = retained.occupiedWithData;
                DoubleByteHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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
        public DoubleSet freeze()
        {
            DoubleByteHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (DoubleByteHashMap.this.sentinelValues != null)
            {
                containsZeroKey = DoubleByteHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = DoubleByteHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableDoubleMapKeySet(DoubleByteHashMap.this.keys, DoubleByteHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableDoubleSet newEmpty()
        {
            return new DoubleHashSet();
        }
    }

    @Override
    public MutableByteCollection values()
    {
        return new ValuesCollection();
    }

    private class ValuesCollection extends AbstractByteValuesCollection
    {
        @Override
        public MutableByteIterator byteIterator()
        {
            return DoubleByteHashMap.this.byteIterator();
        }

        @Override
        public boolean remove(byte item)
        {
            int oldSize = DoubleByteHashMap.this.size();

            if (DoubleByteHashMap.this.sentinelValues != null)
            {
                if (DoubleByteHashMap.this.sentinelValues.containsZeroKey && item == DoubleByteHashMap.this.sentinelValues.zeroValue)
                {
                    DoubleByteHashMap.this.removeKey(EMPTY_KEY);
                }
            }
            if (DoubleByteHashMap.this.sentinelValues != null)
            {
                if (DoubleByteHashMap.this.sentinelValues.containsOneKey && item == DoubleByteHashMap.this.sentinelValues.oneValue)
                {
                    DoubleByteHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < DoubleByteHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(DoubleByteHashMap.this.keys[i]) && item == DoubleByteHashMap.this.values[i])
                {
                    DoubleByteHashMap.this.removeKey(DoubleByteHashMap.this.keys[i]);
                }
            }
            return oldSize != DoubleByteHashMap.this.size();
        }

        @Override
        public boolean retainAll(ByteIterable source)
        {
            int oldSize = DoubleByteHashMap.this.size();
            ByteSet sourceSet = source instanceof ByteSet ? (ByteSet) source : source.toSet();
            DoubleByteHashMap retained = DoubleByteHashMap.this.select((double key, byte value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                DoubleByteHashMap.this.keys = retained.keys;
                DoubleByteHashMap.this.values = retained.values;
                DoubleByteHashMap.this.sentinelValues = retained.sentinelValues;
                DoubleByteHashMap.this.occupiedWithData = retained.occupiedWithData;
                DoubleByteHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableByteCollection newEmpty()
        {
            return new ByteHashBag();
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<DoubleBytePair>
    {
        @Override
        public void each(Procedure<? super DoubleBytePair> procedure)
        {
            if (DoubleByteHashMap.this.sentinelValues != null)
            {
                if (DoubleByteHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, DoubleByteHashMap.this.sentinelValues.zeroValue));
                }
                if (DoubleByteHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, DoubleByteHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < DoubleByteHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(DoubleByteHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(DoubleByteHashMap.this.keys[i], DoubleByteHashMap.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super DoubleBytePair> objectIntProcedure)
        {
            int index = 0;
            if (DoubleByteHashMap.this.sentinelValues != null)
            {
                if (DoubleByteHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, DoubleByteHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (DoubleByteHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, DoubleByteHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < DoubleByteHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(DoubleByteHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(DoubleByteHashMap.this.keys[i], DoubleByteHashMap.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super DoubleBytePair, ? super P> procedure, P parameter)
        {
            if (DoubleByteHashMap.this.sentinelValues != null)
            {
                if (DoubleByteHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, DoubleByteHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (DoubleByteHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, DoubleByteHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < DoubleByteHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(DoubleByteHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(DoubleByteHashMap.this.keys[i], DoubleByteHashMap.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<DoubleBytePair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<DoubleBytePair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public DoubleBytePair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (DoubleByteHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, DoubleByteHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (DoubleByteHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, DoubleByteHashMap.this.sentinelValues.oneValue);
                    }
                }

                double[] keys = DoubleByteHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                DoubleBytePair result = PrimitiveTuples.pair(keys[this.position], DoubleByteHashMap.this.values[this.position]);
                this.position++;
                return result;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
            }

            @Override
            public boolean hasNext()
            {
                return this.count != DoubleByteHashMap.this.size();
            }
        }
    }
}

