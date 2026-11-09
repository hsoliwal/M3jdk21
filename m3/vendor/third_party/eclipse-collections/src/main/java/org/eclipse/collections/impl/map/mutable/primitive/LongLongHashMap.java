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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction0;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongLongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.factory.primitive.LongLongMaps;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.map.primitive.LongLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongLongMap;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableLongIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyLongIterable;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file primitivePrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class LongLongHashMap extends AbstractMutableLongValuesMap implements MutableLongLongMap, Externalizable, MutableLongKeysMap
{
    private static final long EMPTY_VALUE = 0L;
    private static final long serialVersionUID = 1L;
    private static final long EMPTY_KEY = 0L;
    private static final long REMOVED_KEY = 1L;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 8;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private long[] keysValues;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues sentinelValues;

    private boolean copyKeysOnWrite;

    public LongLongHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public LongLongHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public LongLongHashMap(LongLongMap map)
    {
        if (map instanceof LongLongHashMap && ((LongLongHashMap) map).occupiedWithSentinels == 0)
        {
            LongLongHashMap hashMap = (LongLongHashMap) map;
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

    public static LongLongHashMap newWithKeysValues(long key1, long value1)
    {
        return new LongLongHashMap(1).withKeyValue(key1, value1);
    }

    public static LongLongHashMap newWithKeysValues(long key1, long value1, long key2, long value2)
    {
        return new LongLongHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static LongLongHashMap newWithKeysValues(long key1, long value1, long key2, long value2, long key3, long value3)
    {
        return new LongLongHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static LongLongHashMap newWithKeysValues(long key1, long value1, long key2, long value2, long key3, long value3, long key4, long value4)
    {
        return new LongLongHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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
    protected long getEmptyValue()
    {
        return EMPTY_VALUE;
    }

    @Override
    protected int getTableSize()
    {
        return this.keysValues.length / 2;
    }

    @Override
    protected long getValueAtIndex(int index)
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

        if (!(obj instanceof LongLongMap))
        {
            return false;
        }

        LongLongMap other = (LongLongMap) obj;

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
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            long key = this.keysValues[i];
            if (isNonSentinel(key) && (!other.containsKey(key) || this.keysValues[i + 1] != other.getOrThrow(key)))
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
                result += (int) (EMPTY_KEY ^ EMPTY_KEY >>> 32) ^ (int) (this.sentinelValues.zeroValue ^ this.sentinelValues.zeroValue >>> 32);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result += (int) (REMOVED_KEY ^ REMOVED_KEY >>> 32) ^ (int) (this.sentinelValues.oneValue ^ this.sentinelValues.oneValue >>> 32);
            }
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                result += (int) (this.keysValues[i] ^ this.keysValues[i] >>> 32) ^ (int) (this.keysValues[i + 1] ^ this.keysValues[i + 1] >>> 32);
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
            long key = this.keysValues[i];
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
    public MutableLongIterator longIterator()
    {
        return new InternalLongIterator();
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectLongToObjectFunction<? super V, ? extends V> function)
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
        Arrays.fill(this.keysValues, 0L);
    }

    @Override
    public void put(long key, long value)
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
        long keyAtIndex = this.keysValues[index];
        if (keyAtIndex == key)
        {
            this.keysValues[index + 1] = value;
        }
        else
        {
            this.addKeyValueAtIndex(key, value, index);
        }
    }

    private void putForRemovedSentinel(long value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addRemovedKeyValue(value);
    }

    private void putForEmptySentinel(long value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addEmptyKeyValue(value);
    }

    @Override
    public void putAll(LongLongMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(LongLongToLongFunction function)
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
    public void removeKey(long key)
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
        if (this.keysValues[index] == key)
        {
            this.removeKeyAtIndex(index);
        }
    }

    @Override
    public void remove(long key)
    {
        this.removeKey(key);
    }

    @Override
    public long removeKeyIfAbsent(long key, long value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return value;
            }
            long oldValue = this.sentinelValues.zeroValue;
            this.removeEmptyKey();
            return oldValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                return value;
            }
            long oldValue = this.sentinelValues.oneValue;
            this.removeRemovedKey();
            return oldValue;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            long oldValue = this.keysValues[index + 1];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return value;
    }

    @Override
    public long getIfAbsentPut(long key, long value)
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
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public long getAndPut(long key, long putValue, long defaultValue)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                long existingValue = this.sentinelValues.zeroValue;
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
                long existingValue = this.sentinelValues.oneValue;
                this.sentinelValues.oneValue = putValue;
                return existingValue;
            }
            this.addRemovedKeyValue(putValue);
            return defaultValue;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            long existingValue = this.keysValues[index + 1];
            this.keysValues[index + 1] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public long getIfAbsentPut(long key, LongFunction0 function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                long value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            long value = function.value();
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                long value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            long value = function.value();
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        long value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> long getIfAbsentPutWith(long key, LongFunction<? super P> function, P parameter)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                long value = function.longValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            long value = function.longValueOf(parameter);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                long value = function.longValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            long value = function.longValueOf(parameter);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        long value = function.longValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public long getIfAbsentPutWithKey(long key, LongToLongFunction function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                long value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            long value = function.valueOf(key);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                long value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            long value = function.valueOf(key);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        long value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public long addToValue(long key, long toBeAdded)
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
        if (this.keysValues[index] == key)
        {
            this.keysValues[index + 1] += toBeAdded;
            return this.keysValues[index + 1];
        }
        this.addKeyValueAtIndex(key, toBeAdded, index);
        return toBeAdded;
    }

    private void addKeyValueAtIndex(long key, long value, int index)
    {
        if (this.keysValues[index] == REMOVED_KEY)
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
        long[] copy = new long[this.keysValues.length];
        System.arraycopy(this.keysValues, 0, copy, 0, this.keysValues.length);
        this.keysValues = copy;
        this.copyKeysOnWrite = false;
    }

    @Override
    public long updateValue(long key, long initialValueIfAbsent, LongToLongFunction function)
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
        if (this.keysValues[index] == key)
        {
            this.keysValues[index + 1] = function.valueOf(this.keysValues[index + 1]);
            return this.keysValues[index + 1];
        }
        long value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public LongLongHashMap withKeyValue(long key1, long value1)
    {
        this.put(key1, value1);
        return this;
    }

    public LongLongHashMap withKeysValues(long key1, long value1, long key2, long value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public LongLongHashMap withKeysValues(long key1, long value1, long key2, long value2, long key3, long value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public LongLongHashMap withKeysValues(long key1, long value1, long key2, long value2, long key3, long value3, long key4, long value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public LongLongHashMap withoutKey(long key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public LongLongHashMap withoutAllKeys(LongIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    @Override
    public MutableLongLongMap asUnmodifiable()
    {
        return new UnmodifiableLongLongMap(this);
    }

    @Override
    public MutableLongLongMap asSynchronized()
    {
        return new SynchronizedLongLongMap(this);
    }

    @Override
    public ImmutableLongLongMap toImmutable()
    {
        return LongLongMaps.immutable.ofAll(this);
    }

    @Override
    public long get(long key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public long getIfAbsent(long key, long ifAbsent)
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

    private long getForSentinel(long key, long ifAbsent)
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

    private long slowGetIfAbsent(long key, long ifAbsent)
    {
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        return ifAbsent;
    }

    private long fastGetIfAbsent(long key, long ifAbsent)
    {
        int index = this.mask((int) key) << 1;

        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            long keyAtIndex = this.keysValues[index];
            if (keyAtIndex == key)
            {
                return this.keysValues[index + 1];
            }
            if (keyAtIndex == EMPTY_KEY)
            {
                return ifAbsent;
            }
            index = (index + 2) & (this.keysValues.length - 1);
        }
        return this.slowGetIfAbsentTwo(key, ifAbsent);
    }

    private long slowGetIfAbsentTwo(long key, long ifAbsent)
    {
        int index = this.probeTwo(key, -1);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        return ifAbsent;
    }

    @Override
    public long getOrThrow(long key)
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
    public boolean containsKey(long key)
    {
        if (isEmptyKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsZeroKey;
        }
        if (isRemovedKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsOneKey;
        }
        return this.keysValues[this.probe(key)] == key;
    }

    @Override
    public void forEachKey(LongProcedure procedure)
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
    public void forEachKeyValue(LongLongProcedure procedure)
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
    public LazyLongIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<LongLongPair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableLongLongMap flipUniqueValues()
    {
        MutableLongLongMap result = LongLongMaps.mutable.empty();
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
    public LongLongHashMap select(LongLongPredicate predicate)
    {
        LongLongHashMap result = new LongLongHashMap();

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
    public LongLongHashMap reject(LongLongPredicate predicate)
    {
        LongLongHashMap result = new LongLongHashMap();

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
                out.writeLong(EMPTY_KEY);
                out.writeLong(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeLong(REMOVED_KEY);
                out.writeLong(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                out.writeLong(this.keysValues[i]);
                out.writeLong(this.keysValues[i + 1]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readLong(), in.readLong());
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
        long[] old = this.keysValues;
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
    int probe(long element)
    {
        int index = this.mask((int) element) << 1;
        long keyAtIndex = this.keysValues[index];

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

    int probeTwo(long element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element) << 1;
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            int nextIndex = (index + i) & (this.keysValues.length - 1);
            long keyAtIndex = this.keysValues[nextIndex];
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

    int probeThree(long element, int removedIndex)
    {
        int nextIndex = (int) SpreadFunctions.longSpreadOne(element) << 1;
        int spreadTwo = (int) Long.reverse(SpreadFunctions.longSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask((nextIndex >> 1) + spreadTwo) << 1;
            long keyAtIndex = this.keysValues[nextIndex];
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
        return spread & ((this.keysValues.length >> 1) - 1);
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.keysValues = new long[sizeToAllocate << 1];
    }

    private static boolean isEmptyKey(long key)
    {
        return key == EMPTY_KEY;
    }

    private static boolean isRemovedKey(long key)
    {
        return key == REMOVED_KEY;
    }

    private static boolean isNonSentinel(long key)
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

    private class InternalLongIterator implements MutableLongIterator
    {
        private int count;
        private int position;
        private long lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < LongLongHashMap.this.size();
        }

        @Override
        public long next()
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
                if (LongLongHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return LongLongHashMap.this.get(EMPTY_KEY);
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (LongLongHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return LongLongHashMap.this.get(REMOVED_KEY);
                }
            }
            long[] keys = LongLongHashMap.this.keysValues;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position += 2;
            }
            this.lastKey = keys[this.position];
            long result = LongLongHashMap.this.keysValues[this.position + 1];
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
            LongLongHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    private class KeysView extends AbstractLazyLongIterable
    {
        @Override
        public LongIterator longIterator()
        {
            return new UnmodifiableLongIterator(new KeySetIterator());
        }

        /**
         * @since 7.0.
         */
        @Override
        public void each(LongProcedure procedure)
        {
            LongLongHashMap.this.forEachKey(procedure);
        }
    }

    private class KeySetIterator implements MutableLongIterator
    {
        private int count;
        private int position;
        private long lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < LongLongHashMap.this.size();
        }

        @Override
        public long next()
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
                if (LongLongHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (LongLongHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            long[] keys = LongLongHashMap.this.keysValues;
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
            LongLongHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    @Override
    public MutableLongSet keySet()
    {
        return new KeySet();
    }

    private class KeySet extends AbstractMutableLongKeySet
    {
        @Override
        protected MutableLongKeysMap getOuter()
        {
            return LongLongHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return LongLongHashMap.this.sentinelValues;
        }

        @Override
        protected long getKeyAtIndex(int index)
        {
            return LongLongHashMap.this.keysValues[index * 2];
        }

        @Override
        protected int getTableSize()
        {
            return LongLongHashMap.this.keysValues.length / 2;
        }

        @Override
        public MutableLongIterator longIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(LongIterable source)
        {
            int oldSize = LongLongHashMap.this.size();
            LongSet sourceSet = source instanceof LongSet ? (LongSet) source : source.toSet();
            LongLongHashMap retained = LongLongHashMap.this.select((long key, long value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                LongLongHashMap.this.keysValues = retained.keysValues;
                LongLongHashMap.this.sentinelValues = retained.sentinelValues;
                LongLongHashMap.this.occupiedWithData = retained.occupiedWithData;
                LongLongHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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
        public LongSet freeze()
        {
            LongLongHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (LongLongHashMap.this.sentinelValues != null)
            {
                containsZeroKey = LongLongHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = LongLongHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableLongLongMapKeySet(LongLongHashMap.this.keysValues, LongLongHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableLongSet newEmpty()
        {
            return new LongHashSet();
        }
    }

    @Override
    public MutableLongCollection values()
    {
        return new ValuesCollection();
    }

    private class ValuesCollection extends AbstractLongValuesCollection
    {
        @Override
        public MutableLongIterator longIterator()
        {
            return LongLongHashMap.this.longIterator();
        }

        @Override
        public boolean remove(long item)
        {
            int oldSize = LongLongHashMap.this.size();

            if (LongLongHashMap.this.sentinelValues != null)
            {
                if (LongLongHashMap.this.sentinelValues.containsZeroKey && item == LongLongHashMap.this.sentinelValues.zeroValue)
                {
                    LongLongHashMap.this.removeKey(EMPTY_KEY);
                }
            }
            if (LongLongHashMap.this.sentinelValues != null)
            {
                if (LongLongHashMap.this.sentinelValues.containsOneKey && item == LongLongHashMap.this.sentinelValues.oneValue)
                {
                    LongLongHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < LongLongHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(LongLongHashMap.this.keysValues[i]) && item == LongLongHashMap.this.keysValues[i + 1])
                {
                    LongLongHashMap.this.removeKey(LongLongHashMap.this.keysValues[i]);
                }
            }
            return oldSize != LongLongHashMap.this.size();
        }

        @Override
        public boolean retainAll(LongIterable source)
        {
            int oldSize = LongLongHashMap.this.size();
            LongSet sourceSet = source instanceof LongSet ? (LongSet) source : source.toSet();
            LongLongHashMap retained = LongLongHashMap.this.select((long key, long value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                LongLongHashMap.this.keysValues = retained.keysValues;
                LongLongHashMap.this.sentinelValues = retained.sentinelValues;
                LongLongHashMap.this.occupiedWithData = retained.occupiedWithData;
                LongLongHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableLongCollection newEmpty()
        {
            return new LongHashBag();
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<LongLongPair>
    {
        @Override
        public void each(Procedure<? super LongLongPair> procedure)
        {
            if (LongLongHashMap.this.sentinelValues != null)
            {
                if (LongLongHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, LongLongHashMap.this.sentinelValues.zeroValue));
                }
                if (LongLongHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, LongLongHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < LongLongHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(LongLongHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(LongLongHashMap.this.keysValues[i], LongLongHashMap.this.keysValues[i + 1]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super LongLongPair> objectIntProcedure)
        {
            int index = 0;
            if (LongLongHashMap.this.sentinelValues != null)
            {
                if (LongLongHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, LongLongHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (LongLongHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, LongLongHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < LongLongHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(LongLongHashMap.this.keysValues[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(LongLongHashMap.this.keysValues[i], LongLongHashMap.this.keysValues[i + 1]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super LongLongPair, ? super P> procedure, P parameter)
        {
            if (LongLongHashMap.this.sentinelValues != null)
            {
                if (LongLongHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, LongLongHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (LongLongHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, LongLongHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < LongLongHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(LongLongHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(LongLongHashMap.this.keysValues[i], LongLongHashMap.this.keysValues[i + 1]), parameter);
                }
            }
        }

        @Override
        public Iterator<LongLongPair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<LongLongPair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public LongLongPair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (LongLongHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, LongLongHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (LongLongHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, LongLongHashMap.this.sentinelValues.oneValue);
                    }
                }

                long[] keys = LongLongHashMap.this.keysValues;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position += 2;
                }
                LongLongPair result = PrimitiveTuples.pair(keys[this.position], LongLongHashMap.this.keysValues[this.position + 1]);
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
                return this.count != LongLongHashMap.this.size();
            }
        }
    }
}

