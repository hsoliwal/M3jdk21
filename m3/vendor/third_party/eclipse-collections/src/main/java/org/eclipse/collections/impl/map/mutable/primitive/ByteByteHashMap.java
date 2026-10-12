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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ByteByteToByteFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ByteBytePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.factory.primitive.ByteByteMaps;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.map.primitive.ByteByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteByteMap;
import org.eclipse.collections.api.map.primitive.MutableByteByteMap;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyByteIterable;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file primitivePrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class ByteByteHashMap extends AbstractMutableByteValuesMap implements MutableByteByteMap, Externalizable, MutableByteKeysMap
{
    private static final byte EMPTY_VALUE = (byte) 0;
    private static final long serialVersionUID = 1L;
    private static final byte EMPTY_KEY = (byte) 0;
    private static final byte REMOVED_KEY = (byte) 1;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 1;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private byte[] keysValues;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues sentinelValues;

    private boolean copyKeysOnWrite;

    public ByteByteHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ByteByteHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public ByteByteHashMap(ByteByteMap map)
    {
        if (map instanceof ByteByteHashMap && ((ByteByteHashMap) map).occupiedWithSentinels == 0)
        {
            ByteByteHashMap hashMap = (ByteByteHashMap) map;
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

    public static ByteByteHashMap newWithKeysValues(byte key1, byte value1)
    {
        return new ByteByteHashMap(1).withKeyValue(key1, value1);
    }

    public static ByteByteHashMap newWithKeysValues(byte key1, byte value1, byte key2, byte value2)
    {
        return new ByteByteHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static ByteByteHashMap newWithKeysValues(byte key1, byte value1, byte key2, byte value2, byte key3, byte value3)
    {
        return new ByteByteHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static ByteByteHashMap newWithKeysValues(byte key1, byte value1, byte key2, byte value2, byte key3, byte value3, byte key4, byte value4)
    {
        return new ByteByteHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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
        return this.keysValues.length / 2;
    }

    @Override
    protected byte getValueAtIndex(int index)
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

        if (!(obj instanceof ByteByteMap))
        {
            return false;
        }

        ByteByteMap other = (ByteByteMap) obj;

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
            byte key = this.keysValues[i];
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
                result += (int) EMPTY_KEY ^ (int) this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey)
            {
                result += (int) REMOVED_KEY ^ (int) this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                result += (int) this.keysValues[i] ^ (int) this.keysValues[i + 1];
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
            byte key = this.keysValues[i];
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
        Arrays.fill(this.keysValues, (byte) 0);
    }

    @Override
    public void put(byte key, byte value)
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
        byte keyAtIndex = this.keysValues[index];
        if (keyAtIndex == key)
        {
            this.keysValues[index + 1] = value;
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
    public void putAll(ByteByteMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ByteByteToByteFunction function)
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
    public void removeKey(byte key)
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
    public void remove(byte key)
    {
        this.removeKey(key);
    }

    @Override
    public byte removeKeyIfAbsent(byte key, byte value)
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
        if (this.keysValues[index] == key)
        {
            byte oldValue = this.keysValues[index + 1];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return value;
    }

    @Override
    public byte getIfAbsentPut(byte key, byte value)
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
    public byte getAndPut(byte key, byte putValue, byte defaultValue)
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
        if (this.keysValues[index] == key)
        {
            byte existingValue = this.keysValues[index + 1];
            this.keysValues[index + 1] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public byte getIfAbsentPut(byte key, ByteFunction0 function)
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
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        byte value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> byte getIfAbsentPutWith(byte key, ByteFunction<? super P> function, P parameter)
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
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        byte value = function.byteValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public byte getIfAbsentPutWithKey(byte key, ByteToByteFunction function)
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
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        byte value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public byte addToValue(byte key, byte toBeAdded)
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

    private void addKeyValueAtIndex(byte key, byte value, int index)
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
        byte[] copy = new byte[this.keysValues.length];
        System.arraycopy(this.keysValues, 0, copy, 0, this.keysValues.length);
        this.keysValues = copy;
        this.copyKeysOnWrite = false;
    }

    @Override
    public byte updateValue(byte key, byte initialValueIfAbsent, ByteToByteFunction function)
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
        byte value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public ByteByteHashMap withKeyValue(byte key1, byte value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ByteByteHashMap withKeysValues(byte key1, byte value1, byte key2, byte value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ByteByteHashMap withKeysValues(byte key1, byte value1, byte key2, byte value2, byte key3, byte value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ByteByteHashMap withKeysValues(byte key1, byte value1, byte key2, byte value2, byte key3, byte value3, byte key4, byte value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ByteByteHashMap withoutKey(byte key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ByteByteHashMap withoutAllKeys(ByteIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    @Override
    public MutableByteByteMap asUnmodifiable()
    {
        return new UnmodifiableByteByteMap(this);
    }

    @Override
    public MutableByteByteMap asSynchronized()
    {
        return new SynchronizedByteByteMap(this);
    }

    @Override
    public ImmutableByteByteMap toImmutable()
    {
        return ByteByteMaps.immutable.ofAll(this);
    }

    @Override
    public byte get(byte key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public byte getIfAbsent(byte key, byte ifAbsent)
    {
        if (isEmptyKey(key) || isRemovedKey(key))
        {
            return this.getForSentinel(key, ifAbsent);
        }
        return this.slowGetIfAbsent(key, ifAbsent);
    }

    private byte getForSentinel(byte key, byte ifAbsent)
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

    private byte slowGetIfAbsent(byte key, byte ifAbsent)
    {
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        return ifAbsent;
    }

    @Override
    public byte getOrThrow(byte key)
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
    public boolean containsKey(byte key)
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
    public void forEachKey(ByteProcedure procedure)
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
    public void forEachKeyValue(ByteByteProcedure procedure)
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
    public LazyByteIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ByteBytePair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableByteByteMap flipUniqueValues()
    {
        MutableByteByteMap result = ByteByteMaps.mutable.empty();
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
    public ByteByteHashMap select(ByteBytePredicate predicate)
    {
        ByteByteHashMap result = new ByteByteHashMap();

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
    public ByteByteHashMap reject(ByteBytePredicate predicate)
    {
        ByteByteHashMap result = new ByteByteHashMap();

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
                out.writeByte(EMPTY_KEY);
                out.writeByte(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeByte(REMOVED_KEY);
                out.writeByte(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                out.writeByte(this.keysValues[i]);
                out.writeByte(this.keysValues[i + 1]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readByte(), in.readByte());
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
        byte[] old = this.keysValues;
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
    int probe(byte element)
    {
        int index = this.spreadAndMask(element) << 1;
        byte keyAtIndex = this.keysValues[index];

        if (keyAtIndex == element || keyAtIndex == EMPTY_KEY)
        {
            return index;
        }

        int removedIndex = keyAtIndex == REMOVED_KEY ? index : -1;
        int nextIndex = index;
        int probe = 34;

        // loop until an empty slot is reached
        while (true)
        {
            // Probe algorithm: 17*n*(n+1)/2 where n = number of collisions
            nextIndex += probe;
            probe += 34;
            nextIndex &= this.keysValues.length - 1;

            if (this.keysValues[nextIndex] == element)
            {
                return nextIndex;
            }
            if (this.keysValues[nextIndex] == REMOVED_KEY)
            {
                if (removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            else if (this.keysValues[nextIndex] == EMPTY_KEY)
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
        return spread & ((this.keysValues.length >> 1) - 1);
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.keysValues = new byte[sizeToAllocate << 1];
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
    protected boolean isNonSentinelAtIndex(int index)
    {
        return !isEmptyKey(this.keysValues[index * 2]) && !isRemovedKey(this.keysValues[index * 2]);
    }

    private int maxOccupiedWithData()
    {
        return this.keysValues.length >> 2;
    }

    private class InternalByteIterator implements MutableByteIterator
    {
        private int count;
        private int position;
        private byte lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < ByteByteHashMap.this.size();
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
                if (ByteByteHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return ByteByteHashMap.this.get(EMPTY_KEY);
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ByteByteHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return ByteByteHashMap.this.get(REMOVED_KEY);
                }
            }
            byte[] keys = ByteByteHashMap.this.keysValues;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position += 2;
            }
            this.lastKey = keys[this.position];
            byte result = ByteByteHashMap.this.keysValues[this.position + 1];
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
            ByteByteHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    private class KeysView extends AbstractLazyByteIterable
    {
        @Override
        public ByteIterator byteIterator()
        {
            return new UnmodifiableByteIterator(new KeySetIterator());
        }

        /**
         * @since 7.0.
         */
        @Override
        public void each(ByteProcedure procedure)
        {
            ByteByteHashMap.this.forEachKey(procedure);
        }
    }

    private class KeySetIterator implements MutableByteIterator
    {
        private int count;
        private int position;
        private byte lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < ByteByteHashMap.this.size();
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
                if (ByteByteHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ByteByteHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            byte[] keys = ByteByteHashMap.this.keysValues;
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
            ByteByteHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    @Override
    public MutableByteSet keySet()
    {
        return new KeySet();
    }

    private class KeySet extends AbstractMutableByteKeySet
    {
        @Override
        protected MutableByteKeysMap getOuter()
        {
            return ByteByteHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return ByteByteHashMap.this.sentinelValues;
        }

        @Override
        protected byte getKeyAtIndex(int index)
        {
            return ByteByteHashMap.this.keysValues[index * 2];
        }

        @Override
        protected int getTableSize()
        {
            return ByteByteHashMap.this.keysValues.length / 2;
        }

        @Override
        public MutableByteIterator byteIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(ByteIterable source)
        {
            int oldSize = ByteByteHashMap.this.size();
            ByteSet sourceSet = source instanceof ByteSet ? (ByteSet) source : source.toSet();
            ByteByteHashMap retained = ByteByteHashMap.this.select((byte key, byte value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                ByteByteHashMap.this.keysValues = retained.keysValues;
                ByteByteHashMap.this.sentinelValues = retained.sentinelValues;
                ByteByteHashMap.this.occupiedWithData = retained.occupiedWithData;
                ByteByteHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(byte... source)
        {
            return this.retainAll(ByteHashSet.newSetWith(source));
        }

        @Override
        public ByteSet freeze()
        {
            ByteByteHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (ByteByteHashMap.this.sentinelValues != null)
            {
                containsZeroKey = ByteByteHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = ByteByteHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableByteByteMapKeySet(ByteByteHashMap.this.keysValues, ByteByteHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableByteSet newEmpty()
        {
            return new ByteHashSet();
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
            return ByteByteHashMap.this.byteIterator();
        }

        @Override
        public boolean remove(byte item)
        {
            int oldSize = ByteByteHashMap.this.size();

            if (ByteByteHashMap.this.sentinelValues != null)
            {
                if (ByteByteHashMap.this.sentinelValues.containsZeroKey && item == ByteByteHashMap.this.sentinelValues.zeroValue)
                {
                    ByteByteHashMap.this.removeKey(EMPTY_KEY);
                }
            }
            if (ByteByteHashMap.this.sentinelValues != null)
            {
                if (ByteByteHashMap.this.sentinelValues.containsOneKey && item == ByteByteHashMap.this.sentinelValues.oneValue)
                {
                    ByteByteHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < ByteByteHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(ByteByteHashMap.this.keysValues[i]) && item == ByteByteHashMap.this.keysValues[i + 1])
                {
                    ByteByteHashMap.this.removeKey(ByteByteHashMap.this.keysValues[i]);
                }
            }
            return oldSize != ByteByteHashMap.this.size();
        }

        @Override
        public boolean retainAll(ByteIterable source)
        {
            int oldSize = ByteByteHashMap.this.size();
            ByteSet sourceSet = source instanceof ByteSet ? (ByteSet) source : source.toSet();
            ByteByteHashMap retained = ByteByteHashMap.this.select((byte key, byte value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ByteByteHashMap.this.keysValues = retained.keysValues;
                ByteByteHashMap.this.sentinelValues = retained.sentinelValues;
                ByteByteHashMap.this.occupiedWithData = retained.occupiedWithData;
                ByteByteHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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

    private class KeyValuesView extends AbstractLazyIterable<ByteBytePair>
    {
        @Override
        public void each(Procedure<? super ByteBytePair> procedure)
        {
            if (ByteByteHashMap.this.sentinelValues != null)
            {
                if (ByteByteHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, ByteByteHashMap.this.sentinelValues.zeroValue));
                }
                if (ByteByteHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, ByteByteHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < ByteByteHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(ByteByteHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ByteByteHashMap.this.keysValues[i], ByteByteHashMap.this.keysValues[i + 1]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ByteBytePair> objectIntProcedure)
        {
            int index = 0;
            if (ByteByteHashMap.this.sentinelValues != null)
            {
                if (ByteByteHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, ByteByteHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (ByteByteHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, ByteByteHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < ByteByteHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(ByteByteHashMap.this.keysValues[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ByteByteHashMap.this.keysValues[i], ByteByteHashMap.this.keysValues[i + 1]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ByteBytePair, ? super P> procedure, P parameter)
        {
            if (ByteByteHashMap.this.sentinelValues != null)
            {
                if (ByteByteHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, ByteByteHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (ByteByteHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, ByteByteHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < ByteByteHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(ByteByteHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ByteByteHashMap.this.keysValues[i], ByteByteHashMap.this.keysValues[i + 1]), parameter);
                }
            }
        }

        @Override
        public Iterator<ByteBytePair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ByteBytePair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public ByteBytePair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (ByteByteHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, ByteByteHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (ByteByteHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, ByteByteHashMap.this.sentinelValues.oneValue);
                    }
                }

                byte[] keys = ByteByteHashMap.this.keysValues;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position += 2;
                }
                ByteBytePair result = PrimitiveTuples.pair(keys[this.position], ByteByteHashMap.this.keysValues[this.position + 1]);
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
                return this.count != ByteByteHashMap.this.size();
            }
        }
    }
}

