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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.FloatToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.FloatByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatBytePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.factory.primitive.FloatByteMaps;
import org.eclipse.collections.api.factory.primitive.ByteFloatMaps;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.map.primitive.FloatByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatByteMap;
import org.eclipse.collections.api.map.primitive.MutableFloatByteMap;
import org.eclipse.collections.api.map.primitive.MutableByteFloatMap;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.FloatBytePair;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyFloatIterable;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file primitivePrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class FloatByteHashMap extends AbstractMutableByteValuesMap implements MutableFloatByteMap, Externalizable, MutableFloatKeysMap
{
    private static final byte EMPTY_VALUE = (byte) 0;
    private static final long serialVersionUID = 1L;
    private static final float EMPTY_KEY = 0.0f;
    private static final float REMOVED_KEY = 1.0f;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 4;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private float[] keys;
    private byte[] values;
    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues sentinelValues;

    private boolean copyKeysOnWrite;

    public FloatByteHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public FloatByteHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public FloatByteHashMap(FloatByteMap map)
    {
        if (map instanceof FloatByteHashMap && ((FloatByteHashMap) map).occupiedWithSentinels == 0)
        {
            FloatByteHashMap hashMap = (FloatByteHashMap) map;
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

    public static FloatByteHashMap newWithKeysValues(float key1, byte value1)
    {
        return new FloatByteHashMap(1).withKeyValue(key1, value1);
    }

    public static FloatByteHashMap newWithKeysValues(float key1, byte value1, float key2, byte value2)
    {
        return new FloatByteHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static FloatByteHashMap newWithKeysValues(float key1, byte value1, float key2, byte value2, float key3, byte value3)
    {
        return new FloatByteHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static FloatByteHashMap newWithKeysValues(float key1, byte value1, float key2, byte value2, float key3, byte value3, float key4, byte value4)
    {
        return new FloatByteHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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

        if (!(obj instanceof FloatByteMap))
        {
            return false;
        }

        FloatByteMap other = (FloatByteMap) obj;

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
            float key = this.keys[i];
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
                result += Float.floatToIntBits(EMPTY_KEY) ^ (int) this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey)
            {
                result += Float.floatToIntBits(REMOVED_KEY) ^ (int) this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result += Float.floatToIntBits(this.keys[i]) ^ (int) this.values[i];
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
            float key = this.keys[i];
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
    public void put(float key, byte value)
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
        float keyAtIndex = this.keys[index];
        if (Float.compare(keyAtIndex, key) == 0)
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
    public void putAll(FloatByteMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(FloatByteToByteFunction function)
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
    public void removeKey(float key)
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
        if (Float.compare(this.keys[index], key) == 0)
        {
            this.removeKeyAtIndex(index);
        }
    }

    @Override
    public void remove(float key)
    {
        this.removeKey(key);
    }

    @Override
    public byte removeKeyIfAbsent(float key, byte value)
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
        if (Float.compare(this.keys[index], key) == 0)
        {
            byte oldValue = this.values[index];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return value;
    }

    @Override
    public byte getIfAbsentPut(float key, byte value)
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
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public byte getAndPut(float key, byte putValue, byte defaultValue)
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
        if (Float.compare(this.keys[index], key) == 0)
        {
            byte existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public byte getIfAbsentPut(float key, ByteFunction0 function)
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
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        byte value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> byte getIfAbsentPutWith(float key, ByteFunction<? super P> function, P parameter)
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
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        byte value = function.byteValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public byte getIfAbsentPutWithKey(float key, FloatToByteFunction function)
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
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        byte value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public byte addToValue(float key, byte toBeAdded)
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
        if (Float.compare(this.keys[index], key) == 0)
        {
            this.values[index] += toBeAdded;
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, toBeAdded, index);
        return toBeAdded;
    }

    private void addKeyValueAtIndex(float key, byte value, int index)
    {
        if (Float.compare(this.keys[index], REMOVED_KEY) == 0)
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
        float[] copy = new float[this.keys.length];
        System.arraycopy(this.keys, 0, copy, 0, this.keys.length);
        this.keys = copy;
        this.copyKeysOnWrite = false;
    }

    @Override
    public byte updateValue(float key, byte initialValueIfAbsent, ByteToByteFunction function)
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
        if (Float.compare(this.keys[index], key) == 0)
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        byte value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public FloatByteHashMap withKeyValue(float key1, byte value1)
    {
        this.put(key1, value1);
        return this;
    }

    public FloatByteHashMap withKeysValues(float key1, byte value1, float key2, byte value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public FloatByteHashMap withKeysValues(float key1, byte value1, float key2, byte value2, float key3, byte value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public FloatByteHashMap withKeysValues(float key1, byte value1, float key2, byte value2, float key3, byte value3, float key4, byte value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public FloatByteHashMap withoutKey(float key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public FloatByteHashMap withoutAllKeys(FloatIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    @Override
    public MutableFloatByteMap asUnmodifiable()
    {
        return new UnmodifiableFloatByteMap(this);
    }

    @Override
    public MutableFloatByteMap asSynchronized()
    {
        return new SynchronizedFloatByteMap(this);
    }

    @Override
    public ImmutableFloatByteMap toImmutable()
    {
        return FloatByteMaps.immutable.ofAll(this);
    }

    @Override
    public byte get(float key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public byte getIfAbsent(float key, byte ifAbsent)
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

    private byte getForSentinel(float key, byte ifAbsent)
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

    private byte slowGetIfAbsent(float key, byte ifAbsent)
    {
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    private byte fastGetIfAbsent(float key, byte ifAbsent)
    {
        int index = this.mask((int) key);

        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            float keyAtIndex = this.keys[index];
            if (Float.compare(keyAtIndex, key) == 0)
            {
                return this.values[index];
            }
            if (Float.compare(keyAtIndex, EMPTY_KEY) == 0)
            {
                return ifAbsent;
            }
            index = (index + 1) & (this.keys.length - 1);
        }
        return this.slowGetIfAbsentTwo(key, ifAbsent);
    }

    private byte slowGetIfAbsentTwo(float key, byte ifAbsent)
    {
        int index = this.probeTwo(key, -1);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    @Override
    public byte getOrThrow(float key)
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
    public boolean containsKey(float key)
    {
        if (isEmptyKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsZeroKey;
        }
        if (isRemovedKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsOneKey;
        }
        return Float.compare(this.keys[this.probe(key)], key) == 0;
    }

    @Override
    public void forEachKey(FloatProcedure procedure)
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
    public void forEachKeyValue(FloatByteProcedure procedure)
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
    public LazyFloatIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<FloatBytePair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableByteFloatMap flipUniqueValues()
    {
        MutableByteFloatMap result = ByteFloatMaps.mutable.empty();
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
    public FloatByteHashMap select(FloatBytePredicate predicate)
    {
        FloatByteHashMap result = new FloatByteHashMap();

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
    public FloatByteHashMap reject(FloatBytePredicate predicate)
    {
        FloatByteHashMap result = new FloatByteHashMap();

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
                out.writeFloat(EMPTY_KEY);
                out.writeByte(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeFloat(REMOVED_KEY);
                out.writeByte(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeFloat(this.keys[i]);
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
            this.put(in.readFloat(), in.readByte());
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
        float[] old = this.keys;
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
    int probe(float element)
    {
        int index = this.mask((int) element);
        float keyAtIndex = this.keys[index];

        if (Float.compare(keyAtIndex, element) == 0 || Float.compare(keyAtIndex, EMPTY_KEY) == 0)
        {
            return index;
        }

        int removedIndex = Float.compare(keyAtIndex, REMOVED_KEY) == 0 ? index : -1;
        for (int i = 1; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.keys.length - 1);
            keyAtIndex = this.keys[nextIndex];
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
        int index = this.spreadTwoAndMask(element);
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.keys.length - 1);
            float keyAtIndex = this.keys[nextIndex];
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
        int nextIndex = (int) SpreadFunctions.floatSpreadOne(element);
        int spreadTwo = Integer.reverse(SpreadFunctions.floatSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask(nextIndex + spreadTwo);
            float keyAtIndex = this.keys[nextIndex];
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
        return spread & (this.keys.length - 1);
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.keys = new float[sizeToAllocate];
        this.values = new byte[sizeToAllocate];
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
        private float lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < FloatByteHashMap.this.size();
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
                if (FloatByteHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return FloatByteHashMap.this.get(EMPTY_KEY);
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (FloatByteHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return FloatByteHashMap.this.get(REMOVED_KEY);
                }
            }
            float[] keys = FloatByteHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.lastKey = keys[this.position];
            byte result = FloatByteHashMap.this.values[this.position];
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
            FloatByteHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    private class KeysView extends AbstractLazyFloatIterable
    {
        @Override
        public FloatIterator floatIterator()
        {
            return new UnmodifiableFloatIterator(new KeySetIterator());
        }

        /**
         * @since 7.0.
         */
        @Override
        public void each(FloatProcedure procedure)
        {
            FloatByteHashMap.this.forEachKey(procedure);
        }
    }

    private class KeySetIterator implements MutableFloatIterator
    {
        private int count;
        private int position;
        private float lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < FloatByteHashMap.this.size();
        }

        @Override
        public float next()
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
                if (FloatByteHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (FloatByteHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            float[] keys = FloatByteHashMap.this.keys;
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
            FloatByteHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    @Override
    public MutableFloatSet keySet()
    {
        return new KeySet();
    }

    private class KeySet extends AbstractMutableFloatKeySet
    {
        @Override
        protected MutableFloatKeysMap getOuter()
        {
            return FloatByteHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return FloatByteHashMap.this.sentinelValues;
        }

        @Override
        protected float getKeyAtIndex(int index)
        {
            return FloatByteHashMap.this.keys[index];
        }

        @Override
        protected int getTableSize()
        {
            return FloatByteHashMap.this.keys.length;
        }

        @Override
        public MutableFloatIterator floatIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(FloatIterable source)
        {
            int oldSize = FloatByteHashMap.this.size();
            FloatSet sourceSet = source instanceof FloatSet ? (FloatSet) source : source.toSet();
            FloatByteHashMap retained = FloatByteHashMap.this.select((float key, byte value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                FloatByteHashMap.this.keys = retained.keys;
                FloatByteHashMap.this.values = retained.values;
                FloatByteHashMap.this.sentinelValues = retained.sentinelValues;
                FloatByteHashMap.this.occupiedWithData = retained.occupiedWithData;
                FloatByteHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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
        public FloatSet freeze()
        {
            FloatByteHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (FloatByteHashMap.this.sentinelValues != null)
            {
                containsZeroKey = FloatByteHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = FloatByteHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableFloatMapKeySet(FloatByteHashMap.this.keys, FloatByteHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableFloatSet newEmpty()
        {
            return new FloatHashSet();
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
            return FloatByteHashMap.this.byteIterator();
        }

        @Override
        public boolean remove(byte item)
        {
            int oldSize = FloatByteHashMap.this.size();

            if (FloatByteHashMap.this.sentinelValues != null)
            {
                if (FloatByteHashMap.this.sentinelValues.containsZeroKey && item == FloatByteHashMap.this.sentinelValues.zeroValue)
                {
                    FloatByteHashMap.this.removeKey(EMPTY_KEY);
                }
            }
            if (FloatByteHashMap.this.sentinelValues != null)
            {
                if (FloatByteHashMap.this.sentinelValues.containsOneKey && item == FloatByteHashMap.this.sentinelValues.oneValue)
                {
                    FloatByteHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < FloatByteHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatByteHashMap.this.keys[i]) && item == FloatByteHashMap.this.values[i])
                {
                    FloatByteHashMap.this.removeKey(FloatByteHashMap.this.keys[i]);
                }
            }
            return oldSize != FloatByteHashMap.this.size();
        }

        @Override
        public boolean retainAll(ByteIterable source)
        {
            int oldSize = FloatByteHashMap.this.size();
            ByteSet sourceSet = source instanceof ByteSet ? (ByteSet) source : source.toSet();
            FloatByteHashMap retained = FloatByteHashMap.this.select((float key, byte value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                FloatByteHashMap.this.keys = retained.keys;
                FloatByteHashMap.this.values = retained.values;
                FloatByteHashMap.this.sentinelValues = retained.sentinelValues;
                FloatByteHashMap.this.occupiedWithData = retained.occupiedWithData;
                FloatByteHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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

    private class KeyValuesView extends AbstractLazyIterable<FloatBytePair>
    {
        @Override
        public void each(Procedure<? super FloatBytePair> procedure)
        {
            if (FloatByteHashMap.this.sentinelValues != null)
            {
                if (FloatByteHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, FloatByteHashMap.this.sentinelValues.zeroValue));
                }
                if (FloatByteHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, FloatByteHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < FloatByteHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatByteHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(FloatByteHashMap.this.keys[i], FloatByteHashMap.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super FloatBytePair> objectIntProcedure)
        {
            int index = 0;
            if (FloatByteHashMap.this.sentinelValues != null)
            {
                if (FloatByteHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, FloatByteHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (FloatByteHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, FloatByteHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < FloatByteHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatByteHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(FloatByteHashMap.this.keys[i], FloatByteHashMap.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super FloatBytePair, ? super P> procedure, P parameter)
        {
            if (FloatByteHashMap.this.sentinelValues != null)
            {
                if (FloatByteHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, FloatByteHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (FloatByteHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, FloatByteHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < FloatByteHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatByteHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(FloatByteHashMap.this.keys[i], FloatByteHashMap.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<FloatBytePair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<FloatBytePair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public FloatBytePair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (FloatByteHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, FloatByteHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (FloatByteHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, FloatByteHashMap.this.sentinelValues.oneValue);
                    }
                }

                float[] keys = FloatByteHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                FloatBytePair result = PrimitiveTuples.pair(keys[this.position], FloatByteHashMap.this.values[this.position]);
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
                return this.count != FloatByteHashMap.this.size();
            }
        }
    }
}

