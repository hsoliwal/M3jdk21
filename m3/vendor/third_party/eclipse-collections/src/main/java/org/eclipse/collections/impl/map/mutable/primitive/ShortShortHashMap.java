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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction0;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ShortShortToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortShortPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.factory.primitive.ShortShortMaps;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.iterator.MutableShortIterator;
import org.eclipse.collections.api.map.primitive.ShortShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortShortMap;
import org.eclipse.collections.api.map.primitive.MutableShortShortMap;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.tuple.primitive.ShortShortPair;
import org.eclipse.collections.impl.bag.mutable.primitive.ShortHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableShortIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyShortIterable;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file primitivePrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class ShortShortHashMap extends AbstractMutableShortValuesMap implements MutableShortShortMap, Externalizable, MutableShortKeysMap
{
    private static final short EMPTY_VALUE = (short) 0;
    private static final long serialVersionUID = 1L;
    private static final short EMPTY_KEY = (short) 0;
    private static final short REMOVED_KEY = (short) 1;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 2;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private short[] keysValues;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues sentinelValues;

    private boolean copyKeysOnWrite;

    public ShortShortHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ShortShortHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public ShortShortHashMap(ShortShortMap map)
    {
        if (map instanceof ShortShortHashMap && ((ShortShortHashMap) map).occupiedWithSentinels == 0)
        {
            ShortShortHashMap hashMap = (ShortShortHashMap) map;
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

    public static ShortShortHashMap newWithKeysValues(short key1, short value1)
    {
        return new ShortShortHashMap(1).withKeyValue(key1, value1);
    }

    public static ShortShortHashMap newWithKeysValues(short key1, short value1, short key2, short value2)
    {
        return new ShortShortHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static ShortShortHashMap newWithKeysValues(short key1, short value1, short key2, short value2, short key3, short value3)
    {
        return new ShortShortHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static ShortShortHashMap newWithKeysValues(short key1, short value1, short key2, short value2, short key3, short value3, short key4, short value4)
    {
        return new ShortShortHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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
    protected short getEmptyValue()
    {
        return EMPTY_VALUE;
    }

    @Override
    protected int getTableSize()
    {
        return this.keysValues.length / 2;
    }

    @Override
    protected short getValueAtIndex(int index)
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

        if (!(obj instanceof ShortShortMap))
        {
            return false;
        }

        ShortShortMap other = (ShortShortMap) obj;

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
            short key = this.keysValues[i];
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
            short key = this.keysValues[i];
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
    public MutableShortIterator shortIterator()
    {
        return new InternalShortIterator();
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectShortToObjectFunction<? super V, ? extends V> function)
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
        Arrays.fill(this.keysValues, (short) 0);
    }

    @Override
    public void put(short key, short value)
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
        short keyAtIndex = this.keysValues[index];
        if (keyAtIndex == key)
        {
            this.keysValues[index + 1] = value;
        }
        else
        {
            this.addKeyValueAtIndex(key, value, index);
        }
    }

    private void putForRemovedSentinel(short value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addRemovedKeyValue(value);
    }

    private void putForEmptySentinel(short value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addEmptyKeyValue(value);
    }

    @Override
    public void putAll(ShortShortMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ShortShortToShortFunction function)
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
    public void removeKey(short key)
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
    public void remove(short key)
    {
        this.removeKey(key);
    }

    @Override
    public short removeKeyIfAbsent(short key, short value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return value;
            }
            short oldValue = this.sentinelValues.zeroValue;
            this.removeEmptyKey();
            return oldValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                return value;
            }
            short oldValue = this.sentinelValues.oneValue;
            this.removeRemovedKey();
            return oldValue;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            short oldValue = this.keysValues[index + 1];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return value;
    }

    @Override
    public short getIfAbsentPut(short key, short value)
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
    public short getAndPut(short key, short putValue, short defaultValue)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                short existingValue = this.sentinelValues.zeroValue;
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
                short existingValue = this.sentinelValues.oneValue;
                this.sentinelValues.oneValue = putValue;
                return existingValue;
            }
            this.addRemovedKeyValue(putValue);
            return defaultValue;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            short existingValue = this.keysValues[index + 1];
            this.keysValues[index + 1] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public short getIfAbsentPut(short key, ShortFunction0 function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                short value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            short value = function.value();
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                short value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            short value = function.value();
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        short value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> short getIfAbsentPutWith(short key, ShortFunction<? super P> function, P parameter)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                short value = function.shortValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            short value = function.shortValueOf(parameter);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                short value = function.shortValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            short value = function.shortValueOf(parameter);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        short value = function.shortValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public short getIfAbsentPutWithKey(short key, ShortToShortFunction function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                short value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            short value = function.valueOf(key);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                short value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            short value = function.valueOf(key);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        short value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public short addToValue(short key, short toBeAdded)
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

    private void addKeyValueAtIndex(short key, short value, int index)
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
        short[] copy = new short[this.keysValues.length];
        System.arraycopy(this.keysValues, 0, copy, 0, this.keysValues.length);
        this.keysValues = copy;
        this.copyKeysOnWrite = false;
    }

    @Override
    public short updateValue(short key, short initialValueIfAbsent, ShortToShortFunction function)
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
        short value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public ShortShortHashMap withKeyValue(short key1, short value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ShortShortHashMap withKeysValues(short key1, short value1, short key2, short value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ShortShortHashMap withKeysValues(short key1, short value1, short key2, short value2, short key3, short value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ShortShortHashMap withKeysValues(short key1, short value1, short key2, short value2, short key3, short value3, short key4, short value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ShortShortHashMap withoutKey(short key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ShortShortHashMap withoutAllKeys(ShortIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    @Override
    public MutableShortShortMap asUnmodifiable()
    {
        return new UnmodifiableShortShortMap(this);
    }

    @Override
    public MutableShortShortMap asSynchronized()
    {
        return new SynchronizedShortShortMap(this);
    }

    @Override
    public ImmutableShortShortMap toImmutable()
    {
        return ShortShortMaps.immutable.ofAll(this);
    }

    @Override
    public short get(short key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public short getIfAbsent(short key, short ifAbsent)
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

    private short getForSentinel(short key, short ifAbsent)
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

    private short slowGetIfAbsent(short key, short ifAbsent)
    {
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        return ifAbsent;
    }

    private short fastGetIfAbsent(short key, short ifAbsent)
    {
        int index = this.mask((int) key) << 1;

        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            short keyAtIndex = this.keysValues[index];
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

    private short slowGetIfAbsentTwo(short key, short ifAbsent)
    {
        int index = this.probeTwo(key, -1);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        return ifAbsent;
    }

    @Override
    public short getOrThrow(short key)
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
    public boolean containsKey(short key)
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
    public void forEachKey(ShortProcedure procedure)
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
    public void forEachKeyValue(ShortShortProcedure procedure)
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
    public LazyShortIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ShortShortPair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableShortShortMap flipUniqueValues()
    {
        MutableShortShortMap result = ShortShortMaps.mutable.empty();
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
    public ShortShortHashMap select(ShortShortPredicate predicate)
    {
        ShortShortHashMap result = new ShortShortHashMap();

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
    public ShortShortHashMap reject(ShortShortPredicate predicate)
    {
        ShortShortHashMap result = new ShortShortHashMap();

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
                out.writeShort(EMPTY_KEY);
                out.writeShort(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeShort(REMOVED_KEY);
                out.writeShort(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                out.writeShort(this.keysValues[i]);
                out.writeShort(this.keysValues[i + 1]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readShort(), in.readShort());
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
        short[] old = this.keysValues;
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
    int probe(short element)
    {
        int index = this.mask((int) element) << 1;
        short keyAtIndex = this.keysValues[index];

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

    int probeTwo(short element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element) << 1;
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            int nextIndex = (index + i) & (this.keysValues.length - 1);
            short keyAtIndex = this.keysValues[nextIndex];
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

    int probeThree(short element, int removedIndex)
    {
        int nextIndex = (int) SpreadFunctions.shortSpreadOne(element) << 1;
        int spreadTwo = Integer.reverse(SpreadFunctions.shortSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask((nextIndex >> 1) + spreadTwo) << 1;
            short keyAtIndex = this.keysValues[nextIndex];
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
    int spreadAndMask(short element)
    {
        int code = SpreadFunctions.shortSpreadOne(element);
        return this.mask(code);
    }

    int spreadTwoAndMask(short element)
    {
        int code = SpreadFunctions.shortSpreadTwo(element);
        return this.mask(code);
    }

    private int mask(int spread)
    {
        return spread & ((this.keysValues.length >> 1) - 1);
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.keysValues = new short[sizeToAllocate << 1];
    }

    private static boolean isEmptyKey(short key)
    {
        return key == EMPTY_KEY;
    }

    private static boolean isRemovedKey(short key)
    {
        return key == REMOVED_KEY;
    }

    private static boolean isNonSentinel(short key)
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

    private class InternalShortIterator implements MutableShortIterator
    {
        private int count;
        private int position;
        private short lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < ShortShortHashMap.this.size();
        }

        @Override
        public short next()
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
                if (ShortShortHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return ShortShortHashMap.this.get(EMPTY_KEY);
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ShortShortHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return ShortShortHashMap.this.get(REMOVED_KEY);
                }
            }
            short[] keys = ShortShortHashMap.this.keysValues;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position += 2;
            }
            this.lastKey = keys[this.position];
            short result = ShortShortHashMap.this.keysValues[this.position + 1];
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
            ShortShortHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    private class KeysView extends AbstractLazyShortIterable
    {
        @Override
        public ShortIterator shortIterator()
        {
            return new UnmodifiableShortIterator(new KeySetIterator());
        }

        /**
         * @since 7.0.
         */
        @Override
        public void each(ShortProcedure procedure)
        {
            ShortShortHashMap.this.forEachKey(procedure);
        }
    }

    private class KeySetIterator implements MutableShortIterator
    {
        private int count;
        private int position;
        private short lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < ShortShortHashMap.this.size();
        }

        @Override
        public short next()
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
                if (ShortShortHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ShortShortHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            short[] keys = ShortShortHashMap.this.keysValues;
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
            ShortShortHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    @Override
    public MutableShortSet keySet()
    {
        return new KeySet();
    }

    private class KeySet extends AbstractMutableShortKeySet
    {
        @Override
        protected MutableShortKeysMap getOuter()
        {
            return ShortShortHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return ShortShortHashMap.this.sentinelValues;
        }

        @Override
        protected short getKeyAtIndex(int index)
        {
            return ShortShortHashMap.this.keysValues[index * 2];
        }

        @Override
        protected int getTableSize()
        {
            return ShortShortHashMap.this.keysValues.length / 2;
        }

        @Override
        public MutableShortIterator shortIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(ShortIterable source)
        {
            int oldSize = ShortShortHashMap.this.size();
            ShortSet sourceSet = source instanceof ShortSet ? (ShortSet) source : source.toSet();
            ShortShortHashMap retained = ShortShortHashMap.this.select((short key, short value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                ShortShortHashMap.this.keysValues = retained.keysValues;
                ShortShortHashMap.this.sentinelValues = retained.sentinelValues;
                ShortShortHashMap.this.occupiedWithData = retained.occupiedWithData;
                ShortShortHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(short... source)
        {
            return this.retainAll(ShortHashSet.newSetWith(source));
        }

        @Override
        public ShortSet freeze()
        {
            ShortShortHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (ShortShortHashMap.this.sentinelValues != null)
            {
                containsZeroKey = ShortShortHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = ShortShortHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableShortShortMapKeySet(ShortShortHashMap.this.keysValues, ShortShortHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableShortSet newEmpty()
        {
            return new ShortHashSet();
        }
    }

    @Override
    public MutableShortCollection values()
    {
        return new ValuesCollection();
    }

    private class ValuesCollection extends AbstractShortValuesCollection
    {
        @Override
        public MutableShortIterator shortIterator()
        {
            return ShortShortHashMap.this.shortIterator();
        }

        @Override
        public boolean remove(short item)
        {
            int oldSize = ShortShortHashMap.this.size();

            if (ShortShortHashMap.this.sentinelValues != null)
            {
                if (ShortShortHashMap.this.sentinelValues.containsZeroKey && item == ShortShortHashMap.this.sentinelValues.zeroValue)
                {
                    ShortShortHashMap.this.removeKey(EMPTY_KEY);
                }
            }
            if (ShortShortHashMap.this.sentinelValues != null)
            {
                if (ShortShortHashMap.this.sentinelValues.containsOneKey && item == ShortShortHashMap.this.sentinelValues.oneValue)
                {
                    ShortShortHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < ShortShortHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(ShortShortHashMap.this.keysValues[i]) && item == ShortShortHashMap.this.keysValues[i + 1])
                {
                    ShortShortHashMap.this.removeKey(ShortShortHashMap.this.keysValues[i]);
                }
            }
            return oldSize != ShortShortHashMap.this.size();
        }

        @Override
        public boolean retainAll(ShortIterable source)
        {
            int oldSize = ShortShortHashMap.this.size();
            ShortSet sourceSet = source instanceof ShortSet ? (ShortSet) source : source.toSet();
            ShortShortHashMap retained = ShortShortHashMap.this.select((short key, short value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ShortShortHashMap.this.keysValues = retained.keysValues;
                ShortShortHashMap.this.sentinelValues = retained.sentinelValues;
                ShortShortHashMap.this.occupiedWithData = retained.occupiedWithData;
                ShortShortHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableShortCollection newEmpty()
        {
            return new ShortHashBag();
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<ShortShortPair>
    {
        @Override
        public void each(Procedure<? super ShortShortPair> procedure)
        {
            if (ShortShortHashMap.this.sentinelValues != null)
            {
                if (ShortShortHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, ShortShortHashMap.this.sentinelValues.zeroValue));
                }
                if (ShortShortHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, ShortShortHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < ShortShortHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(ShortShortHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ShortShortHashMap.this.keysValues[i], ShortShortHashMap.this.keysValues[i + 1]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ShortShortPair> objectIntProcedure)
        {
            int index = 0;
            if (ShortShortHashMap.this.sentinelValues != null)
            {
                if (ShortShortHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, ShortShortHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (ShortShortHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, ShortShortHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < ShortShortHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(ShortShortHashMap.this.keysValues[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ShortShortHashMap.this.keysValues[i], ShortShortHashMap.this.keysValues[i + 1]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ShortShortPair, ? super P> procedure, P parameter)
        {
            if (ShortShortHashMap.this.sentinelValues != null)
            {
                if (ShortShortHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, ShortShortHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (ShortShortHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, ShortShortHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < ShortShortHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(ShortShortHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ShortShortHashMap.this.keysValues[i], ShortShortHashMap.this.keysValues[i + 1]), parameter);
                }
            }
        }

        @Override
        public Iterator<ShortShortPair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ShortShortPair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public ShortShortPair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (ShortShortHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, ShortShortHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (ShortShortHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, ShortShortHashMap.this.sentinelValues.oneValue);
                    }
                }

                short[] keys = ShortShortHashMap.this.keysValues;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position += 2;
                }
                ShortShortPair result = PrimitiveTuples.pair(keys[this.position], ShortShortHashMap.this.keysValues[this.position + 1]);
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
                return this.count != ShortShortHashMap.this.size();
            }
        }
    }
}

