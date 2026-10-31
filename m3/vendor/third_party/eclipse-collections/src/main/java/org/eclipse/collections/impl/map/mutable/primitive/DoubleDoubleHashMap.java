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
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction0;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleDoublePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleDoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.factory.primitive.DoubleDoubleMaps;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.map.primitive.DoubleDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleDoubleMap;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.impl.bag.mutable.primitive.DoubleHashBag;
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
public class DoubleDoubleHashMap extends AbstractMutableDoubleValuesMap implements MutableDoubleDoubleMap, Externalizable, MutableDoubleKeysMap
{
    private static final double EMPTY_VALUE = 0.0;
    private static final long serialVersionUID = 1L;
    private static final double EMPTY_KEY = 0.0;
    private static final double REMOVED_KEY = 1.0;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 8;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private double[] keysValues;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues sentinelValues;

    private boolean copyKeysOnWrite;

    public DoubleDoubleHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public DoubleDoubleHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public DoubleDoubleHashMap(DoubleDoubleMap map)
    {
        if (map instanceof DoubleDoubleHashMap && ((DoubleDoubleHashMap) map).occupiedWithSentinels == 0)
        {
            DoubleDoubleHashMap hashMap = (DoubleDoubleHashMap) map;
            this.occupiedWithData = hashMap.occupiedWithData;
            if (hashMap.sentinelValues != null)
            {
                this.sentinelValues = hashMap.sentinelValues.copy();
            }
            this.keysValues = Arrays.copyOf(hashMap.keysValues, hashMap.keysValues.length);
        }
        else
        {
            int capacity = this.smallestPowerOfTwoGreaterThan(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY) << 1);
            this.allocateTable(capacity);
            this.putAll(map);
        }
    }

    public static DoubleDoubleHashMap newWithKeysValues(double key1, double value1)
    {
        return new DoubleDoubleHashMap(1).withKeyValue(key1, value1);
    }

    public static DoubleDoubleHashMap newWithKeysValues(double key1, double value1, double key2, double value2)
    {
        return new DoubleDoubleHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static DoubleDoubleHashMap newWithKeysValues(double key1, double value1, double key2, double value2, double key3, double value3)
    {
        return new DoubleDoubleHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static DoubleDoubleHashMap newWithKeysValues(double key1, double value1, double key2, double value2, double key3, double value3, double key4, double value4)
    {
        return new DoubleDoubleHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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
    protected double getEmptyValue()
    {
        return EMPTY_VALUE;
    }

    @Override
    protected int getTableSize()
    {
        return this.keysValues.length / 2;
    }

    @Override
    protected double getValueAtIndex(int index)
    {
        return this.keysValues[index * 2 + 1];
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof DoubleDoubleMap))
        {
            return false;
        }

        DoubleDoubleMap other = (DoubleDoubleMap) obj;

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
            if (this.sentinelValues.containsZeroKey && (!other.containsKey(EMPTY_KEY) || Double.compare(this.sentinelValues.zeroValue, other.getOrThrow(EMPTY_KEY)) != 0))
            {
                return false;
            }

            if (this.sentinelValues.containsOneKey && (!other.containsKey(REMOVED_KEY) || Double.compare(this.sentinelValues.oneValue, other.getOrThrow(REMOVED_KEY)) != 0))
            {
                return false;
            }
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            double key = this.keysValues[i];
            if (isNonSentinel(key) && (!other.containsKey(key) || Double.compare(this.keysValues[i + 1], other.getOrThrow(key)) != 0))
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
                result += (int) (Double.doubleToLongBits(EMPTY_KEY) ^ Double.doubleToLongBits(EMPTY_KEY) >>> 32) ^ (int) (Double.doubleToLongBits(this.sentinelValues.zeroValue) ^ Double.doubleToLongBits(this.sentinelValues.zeroValue) >>> 32);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result += (int) (Double.doubleToLongBits(REMOVED_KEY) ^ Double.doubleToLongBits(REMOVED_KEY) >>> 32) ^ (int) (Double.doubleToLongBits(this.sentinelValues.oneValue) ^ Double.doubleToLongBits(this.sentinelValues.oneValue) >>> 32);
            }
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                result += (int) (Double.doubleToLongBits(this.keysValues[i]) ^ Double.doubleToLongBits(this.keysValues[i]) >>> 32) ^ (int) (Double.doubleToLongBits(this.keysValues[i + 1]) ^ Double.doubleToLongBits(this.keysValues[i + 1]) >>> 32);
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
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            double key = this.keysValues[i];
            if (isNonSentinel(key))
            {
                if (!first)
                {
                    appendable.append(", ");
                }
                appendable.append(key).append("=").append(this.keysValues[i + 1]);
                first = false;
            }
        }
        appendable.append("}");

        return appendable.toString();
    }

    @Override
    public MutableDoubleIterator doubleIterator()
    {
        return new InternalDoubleIterator();
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectDoubleToObjectFunction<? super V, ? extends V> function)
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
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                result = function.valueOf(result, this.keysValues[i + 1]);
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
        Arrays.fill(this.keysValues, 0.0);
    }

    @Override
    public void put(double key, double value)
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
        double keyAtIndex = this.keysValues[index];
        if (Double.compare(keyAtIndex, key) == 0)
        {
            this.keysValues[index + 1] = value;
        }
        else
        {
            this.addKeyValueAtIndex(key, value, index);
        }
    }

    private void putForRemovedSentinel(double value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addRemovedKeyValue(value);
    }

    private void putForEmptySentinel(double value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addEmptyKeyValue(value);
    }

    @Override
    public void putAll(DoubleDoubleMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(DoubleDoubleToDoubleFunction function)
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
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                this.keysValues[i + 1] = function.valueOf(this.keysValues[i], this.keysValues[i + 1]);
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
        if (Double.compare(this.keysValues[index], key) == 0)
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
    public double removeKeyIfAbsent(double key, double value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return value;
            }
            double oldValue = this.sentinelValues.zeroValue;
            this.removeEmptyKey();
            return oldValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                return value;
            }
            double oldValue = this.sentinelValues.oneValue;
            this.removeRemovedKey();
            return oldValue;
        }
        int index = this.probe(key);
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            double oldValue = this.keysValues[index + 1];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return value;
    }

    @Override
    public double getIfAbsentPut(double key, double value)
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
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            return this.keysValues[index + 1];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public double getAndPut(double key, double putValue, double defaultValue)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                double existingValue = this.sentinelValues.zeroValue;
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
                double existingValue = this.sentinelValues.oneValue;
                this.sentinelValues.oneValue = putValue;
                return existingValue;
            }
            this.addRemovedKeyValue(putValue);
            return defaultValue;
        }
        int index = this.probe(key);
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            double existingValue = this.keysValues[index + 1];
            this.keysValues[index + 1] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public double getIfAbsentPut(double key, DoubleFunction0 function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                double value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            double value = function.value();
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                double value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            double value = function.value();
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            return this.keysValues[index + 1];
        }
        double value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> double getIfAbsentPutWith(double key, DoubleFunction<? super P> function, P parameter)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                double value = function.doubleValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            double value = function.doubleValueOf(parameter);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                double value = function.doubleValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            double value = function.doubleValueOf(parameter);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            return this.keysValues[index + 1];
        }
        double value = function.doubleValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public double getIfAbsentPutWithKey(double key, DoubleToDoubleFunction function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                double value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            double value = function.valueOf(key);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                double value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            double value = function.valueOf(key);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            return this.keysValues[index + 1];
        }
        double value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public double addToValue(double key, double toBeAdded)
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
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            this.keysValues[index + 1] += toBeAdded;
            return this.keysValues[index + 1];
        }
        this.addKeyValueAtIndex(key, toBeAdded, index);
        return toBeAdded;
    }

    private void addKeyValueAtIndex(double key, double value, int index)
    {
        if (Double.compare(this.keysValues[index], REMOVED_KEY) == 0)
        {
            this.occupiedWithSentinels--;
        }
        if (this.copyKeysOnWrite)
        {
            this.copyKeys();
        }
        this.keysValues[index] = key;
        this.keysValues[index + 1] = value;
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
        this.keysValues[index] = REMOVED_KEY;
        this.keysValues[index + 1] = EMPTY_VALUE;
        this.occupiedWithData--;
        this.occupiedWithSentinels++;
    }

    private void copyKeys()
    {
        double[] copy = new double[this.keysValues.length];
        System.arraycopy(this.keysValues, 0, copy, 0, this.keysValues.length);
        this.keysValues = copy;
        this.copyKeysOnWrite = false;
    }

    @Override
    public double updateValue(double key, double initialValueIfAbsent, DoubleToDoubleFunction function)
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
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            this.keysValues[index + 1] = function.valueOf(this.keysValues[index + 1]);
            return this.keysValues[index + 1];
        }
        double value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public DoubleDoubleHashMap withKeyValue(double key1, double value1)
    {
        this.put(key1, value1);
        return this;
    }

    public DoubleDoubleHashMap withKeysValues(double key1, double value1, double key2, double value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public DoubleDoubleHashMap withKeysValues(double key1, double value1, double key2, double value2, double key3, double value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public DoubleDoubleHashMap withKeysValues(double key1, double value1, double key2, double value2, double key3, double value3, double key4, double value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public DoubleDoubleHashMap withoutKey(double key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public DoubleDoubleHashMap withoutAllKeys(DoubleIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    @Override
    public MutableDoubleDoubleMap asUnmodifiable()
    {
        return new UnmodifiableDoubleDoubleMap(this);
    }

    @Override
    public MutableDoubleDoubleMap asSynchronized()
    {
        return new SynchronizedDoubleDoubleMap(this);
    }

    @Override
    public ImmutableDoubleDoubleMap toImmutable()
    {
        return DoubleDoubleMaps.immutable.ofAll(this);
    }

    @Override
    public double get(double key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public double getIfAbsent(double key, double ifAbsent)
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

    private double getForSentinel(double key, double ifAbsent)
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

    private double slowGetIfAbsent(double key, double ifAbsent)
    {
        int index = this.probe(key);
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            return this.keysValues[index + 1];
        }
        return ifAbsent;
    }

    private double fastGetIfAbsent(double key, double ifAbsent)
    {
        int index = this.mask((int) key) << 1;

        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            double keyAtIndex = this.keysValues[index];
            if (Double.compare(keyAtIndex, key) == 0)
            {
                return this.keysValues[index + 1];
            }
            if (Double.compare(keyAtIndex, EMPTY_KEY) == 0)
            {
                return ifAbsent;
            }
            index = (index + 2) & (this.keysValues.length - 1);
        }
        return this.slowGetIfAbsentTwo(key, ifAbsent);
    }

    private double slowGetIfAbsentTwo(double key, double ifAbsent)
    {
        int index = this.probeTwo(key, -1);
        if (Double.compare(this.keysValues[index], key) == 0)
        {
            return this.keysValues[index + 1];
        }
        return ifAbsent;
    }

    @Override
    public double getOrThrow(double key)
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
        if (isNonSentinel(this.keysValues[index]))
        {
            return this.keysValues[index + 1];
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
        return Double.compare(this.keysValues[this.probe(key)], key) == 0;
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
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                procedure.value(this.keysValues[i]);
            }
        }
    }

    @Override
    public void forEachKeyValue(DoubleDoubleProcedure procedure)
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
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                procedure.value(this.keysValues[i], this.keysValues[i + 1]);
            }
        }
    }

    @Override
    public LazyDoubleIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<DoubleDoublePair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableDoubleDoubleMap flipUniqueValues()
    {
        MutableDoubleDoubleMap result = DoubleDoubleMaps.mutable.empty();
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
    public DoubleDoubleHashMap select(DoubleDoublePredicate predicate)
    {
        DoubleDoubleHashMap result = new DoubleDoubleHashMap();

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
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && predicate.accept(this.keysValues[i], this.keysValues[i + 1]))
            {
                result.put(this.keysValues[i], this.keysValues[i + 1]);
            }
        }

        return result;
    }

    @Override
    public DoubleDoubleHashMap reject(DoubleDoublePredicate predicate)
    {
        DoubleDoubleHashMap result = new DoubleDoubleHashMap();

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
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]) && !predicate.accept(this.keysValues[i], this.keysValues[i + 1]))
            {
                result.put(this.keysValues[i], this.keysValues[i + 1]);
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
                out.writeDouble(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeDouble(REMOVED_KEY);
                out.writeDouble(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                out.writeDouble(this.keysValues[i]);
                out.writeDouble(this.keysValues[i + 1]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readDouble(), in.readDouble());
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
        int oldLength = this.keysValues.length;
        double[] old = this.keysValues;
        this.allocateTable(newCapacity);
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;

        for (int i = 0; i < oldLength; i += 2)
        {
            if (isNonSentinel(old[i]))
            {
                this.put(old[i], old[i + 1]);
            }
        }
    }

    // exposed for testing
    int probe(double element)
    {
        int index = this.mask((int) element) << 1;
        double keyAtIndex = this.keysValues[index];

        if (Double.compare(keyAtIndex, element) == 0 || Double.compare(keyAtIndex, EMPTY_KEY) == 0)
        {
            return index;
        }

        int removedIndex = Double.compare(keyAtIndex, REMOVED_KEY) == 0 ? index : -1;
        for (int i = 2; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            int nextIndex = (index + i) & (this.keysValues.length - 1);
            keyAtIndex = this.keysValues[nextIndex];
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
        int index = this.spreadTwoAndMask(element) << 1;
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            int nextIndex = (index + i) & (this.keysValues.length - 1);
            double keyAtIndex = this.keysValues[nextIndex];
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
        int nextIndex = (int) SpreadFunctions.doubleSpreadOne(element) << 1;
        int spreadTwo = (int) Long.reverse(SpreadFunctions.doubleSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask((nextIndex >> 1) + spreadTwo) << 1;
            double keyAtIndex = this.keysValues[nextIndex];
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
        return spread & ((this.keysValues.length >> 1) - 1);
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.keysValues = new double[sizeToAllocate << 1];
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
        return !isEmptyKey(this.keysValues[index * 2]) && !isRemovedKey(this.keysValues[index * 2]);
    }

    private int maxOccupiedWithData()
    {
        return this.keysValues.length >> 2;
    }

    private class InternalDoubleIterator implements MutableDoubleIterator
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
            return this.count < DoubleDoubleHashMap.this.size();
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
                if (DoubleDoubleHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return DoubleDoubleHashMap.this.get(EMPTY_KEY);
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (DoubleDoubleHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return DoubleDoubleHashMap.this.get(REMOVED_KEY);
                }
            }
            double[] keys = DoubleDoubleHashMap.this.keysValues;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position += 2;
            }
            this.lastKey = keys[this.position];
            double result = DoubleDoubleHashMap.this.keysValues[this.position + 1];
            this.position += 2;

            return result;
        }

        @Override
        public void remove()
        {
            if (!this.canRemove)
            {
                throw new IllegalStateException();
            }
            DoubleDoubleHashMap.this.removeKey(this.lastKey);
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
            DoubleDoubleHashMap.this.forEachKey(procedure);
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
            return this.count < DoubleDoubleHashMap.this.size();
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
                if (DoubleDoubleHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (DoubleDoubleHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            double[] keys = DoubleDoubleHashMap.this.keysValues;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position += 2;
            }
            this.lastKey = keys[this.position];
            this.position += 2;

            return this.lastKey;
        }

        @Override
        public void remove()
        {
            if (!this.canRemove)
            {
                throw new IllegalStateException();
            }
            DoubleDoubleHashMap.this.removeKey(this.lastKey);
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
            return DoubleDoubleHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return DoubleDoubleHashMap.this.sentinelValues;
        }

        @Override
        protected double getKeyAtIndex(int index)
        {
            return DoubleDoubleHashMap.this.keysValues[index * 2];
        }

        @Override
        protected int getTableSize()
        {
            return DoubleDoubleHashMap.this.keysValues.length / 2;
        }

        @Override
        public MutableDoubleIterator doubleIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(DoubleIterable source)
        {
            int oldSize = DoubleDoubleHashMap.this.size();
            DoubleSet sourceSet = source instanceof DoubleSet ? (DoubleSet) source : source.toSet();
            DoubleDoubleHashMap retained = DoubleDoubleHashMap.this.select((double key, double value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                DoubleDoubleHashMap.this.keysValues = retained.keysValues;
                DoubleDoubleHashMap.this.sentinelValues = retained.sentinelValues;
                DoubleDoubleHashMap.this.occupiedWithData = retained.occupiedWithData;
                DoubleDoubleHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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
            DoubleDoubleHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (DoubleDoubleHashMap.this.sentinelValues != null)
            {
                containsZeroKey = DoubleDoubleHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = DoubleDoubleHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableDoubleDoubleMapKeySet(DoubleDoubleHashMap.this.keysValues, DoubleDoubleHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
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
    public MutableDoubleCollection values()
    {
        return new ValuesCollection();
    }

    private class ValuesCollection extends AbstractDoubleValuesCollection
    {
        @Override
        public MutableDoubleIterator doubleIterator()
        {
            return DoubleDoubleHashMap.this.doubleIterator();
        }

        @Override
        public boolean remove(double item)
        {
            int oldSize = DoubleDoubleHashMap.this.size();

            if (DoubleDoubleHashMap.this.sentinelValues != null)
            {
                if (DoubleDoubleHashMap.this.sentinelValues.containsZeroKey && Double.compare(item, DoubleDoubleHashMap.this.sentinelValues.zeroValue) == 0)
                {
                    DoubleDoubleHashMap.this.removeKey(EMPTY_KEY);
                }
            }
            if (DoubleDoubleHashMap.this.sentinelValues != null)
            {
                if (DoubleDoubleHashMap.this.sentinelValues.containsOneKey && Double.compare(item, DoubleDoubleHashMap.this.sentinelValues.oneValue) == 0)
                {
                    DoubleDoubleHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < DoubleDoubleHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(DoubleDoubleHashMap.this.keysValues[i]) && Double.compare(item, DoubleDoubleHashMap.this.keysValues[i + 1]) == 0)
                {
                    DoubleDoubleHashMap.this.removeKey(DoubleDoubleHashMap.this.keysValues[i]);
                }
            }
            return oldSize != DoubleDoubleHashMap.this.size();
        }

        @Override
        public boolean retainAll(DoubleIterable source)
        {
            int oldSize = DoubleDoubleHashMap.this.size();
            DoubleSet sourceSet = source instanceof DoubleSet ? (DoubleSet) source : source.toSet();
            DoubleDoubleHashMap retained = DoubleDoubleHashMap.this.select((double key, double value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                DoubleDoubleHashMap.this.keysValues = retained.keysValues;
                DoubleDoubleHashMap.this.sentinelValues = retained.sentinelValues;
                DoubleDoubleHashMap.this.occupiedWithData = retained.occupiedWithData;
                DoubleDoubleHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableDoubleCollection newEmpty()
        {
            return new DoubleHashBag();
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<DoubleDoublePair>
    {
        @Override
        public void each(Procedure<? super DoubleDoublePair> procedure)
        {
            if (DoubleDoubleHashMap.this.sentinelValues != null)
            {
                if (DoubleDoubleHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, DoubleDoubleHashMap.this.sentinelValues.zeroValue));
                }
                if (DoubleDoubleHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, DoubleDoubleHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < DoubleDoubleHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(DoubleDoubleHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(DoubleDoubleHashMap.this.keysValues[i], DoubleDoubleHashMap.this.keysValues[i + 1]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super DoubleDoublePair> objectIntProcedure)
        {
            int index = 0;
            if (DoubleDoubleHashMap.this.sentinelValues != null)
            {
                if (DoubleDoubleHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, DoubleDoubleHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (DoubleDoubleHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, DoubleDoubleHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < DoubleDoubleHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(DoubleDoubleHashMap.this.keysValues[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(DoubleDoubleHashMap.this.keysValues[i], DoubleDoubleHashMap.this.keysValues[i + 1]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super DoubleDoublePair, ? super P> procedure, P parameter)
        {
            if (DoubleDoubleHashMap.this.sentinelValues != null)
            {
                if (DoubleDoubleHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, DoubleDoubleHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (DoubleDoubleHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, DoubleDoubleHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < DoubleDoubleHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(DoubleDoubleHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(DoubleDoubleHashMap.this.keysValues[i], DoubleDoubleHashMap.this.keysValues[i + 1]), parameter);
                }
            }
        }

        @Override
        public Iterator<DoubleDoublePair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<DoubleDoublePair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public DoubleDoublePair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (DoubleDoubleHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, DoubleDoubleHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (DoubleDoubleHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, DoubleDoubleHashMap.this.sentinelValues.oneValue);
                    }
                }

                double[] keys = DoubleDoubleHashMap.this.keysValues;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position += 2;
                }
                DoubleDoublePair result = PrimitiveTuples.pair(keys[this.position], DoubleDoubleHashMap.this.keysValues[this.position + 1]);
                this.position += 2;
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
                return this.count != DoubleDoubleHashMap.this.size();
            }
        }
    }
}

