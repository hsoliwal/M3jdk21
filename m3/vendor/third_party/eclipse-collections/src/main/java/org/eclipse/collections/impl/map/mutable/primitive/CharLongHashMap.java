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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.CharToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction0;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.CharLongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharLongPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.factory.primitive.CharLongMaps;
import org.eclipse.collections.api.factory.primitive.LongCharMaps;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.map.primitive.CharLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharLongMap;
import org.eclipse.collections.api.map.primitive.MutableCharLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongCharMap;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.tuple.primitive.CharLongPair;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.iterator.UnmodifiableCharIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyCharIterable;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file primitivePrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class CharLongHashMap extends AbstractMutableLongValuesMap implements MutableCharLongMap, Externalizable, MutableCharKeysMap
{
    private static final long EMPTY_VALUE = 0L;
    private static final long serialVersionUID = 1L;
    private static final char EMPTY_KEY = (char) 0;
    private static final char REMOVED_KEY = (char) 1;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 2;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private char[] keys;
    private long[] values;
    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues sentinelValues;

    private boolean copyKeysOnWrite;

    public CharLongHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public CharLongHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public CharLongHashMap(CharLongMap map)
    {
        if (map instanceof CharLongHashMap && ((CharLongHashMap) map).occupiedWithSentinels == 0)
        {
            CharLongHashMap hashMap = (CharLongHashMap) map;
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

    public static CharLongHashMap newWithKeysValues(char key1, long value1)
    {
        return new CharLongHashMap(1).withKeyValue(key1, value1);
    }

    public static CharLongHashMap newWithKeysValues(char key1, long value1, char key2, long value2)
    {
        return new CharLongHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static CharLongHashMap newWithKeysValues(char key1, long value1, char key2, long value2, char key3, long value3)
    {
        return new CharLongHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static CharLongHashMap newWithKeysValues(char key1, long value1, char key2, long value2, char key3, long value3, char key4, long value4)
    {
        return new CharLongHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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
        return this.values.length;
    }

    @Override
    protected long getValueAtIndex(int index)
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

        if (!(obj instanceof CharLongMap))
        {
            return false;
        }

        CharLongMap other = (CharLongMap) obj;

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
            char key = this.keys[i];
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
                result += (int) EMPTY_KEY ^ (int) (this.sentinelValues.zeroValue ^ this.sentinelValues.zeroValue >>> 32);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result += (int) REMOVED_KEY ^ (int) (this.sentinelValues.oneValue ^ this.sentinelValues.oneValue >>> 32);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result += (int) this.keys[i] ^ (int) (this.values[i] ^ this.values[i] >>> 32);
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
            char key = this.keys[i];
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
    public void put(char key, long value)
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
        char keyAtIndex = this.keys[index];
        if (keyAtIndex == key)
        {
            this.values[index] = value;
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
    public void putAll(CharLongMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(CharLongToLongFunction function)
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
    public void removeKey(char key)
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
        if (this.keys[index] == key)
        {
            this.removeKeyAtIndex(index);
        }
    }

    @Override
    public void remove(char key)
    {
        this.removeKey(key);
    }

    @Override
    public long removeKeyIfAbsent(char key, long value)
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
        if (this.keys[index] == key)
        {
            long oldValue = this.values[index];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return value;
    }

    @Override
    public long getIfAbsentPut(char key, long value)
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
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public long getAndPut(char key, long putValue, long defaultValue)
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
        if (this.keys[index] == key)
        {
            long existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public long getIfAbsentPut(char key, LongFunction0 function)
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
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        long value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> long getIfAbsentPutWith(char key, LongFunction<? super P> function, P parameter)
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
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        long value = function.longValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public long getIfAbsentPutWithKey(char key, CharToLongFunction function)
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
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        long value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public long addToValue(char key, long toBeAdded)
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
        if (this.keys[index] == key)
        {
            this.values[index] += toBeAdded;
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, toBeAdded, index);
        return toBeAdded;
    }

    private void addKeyValueAtIndex(char key, long value, int index)
    {
        if (this.keys[index] == REMOVED_KEY)
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
        char[] copy = new char[this.keys.length];
        System.arraycopy(this.keys, 0, copy, 0, this.keys.length);
        this.keys = copy;
        this.copyKeysOnWrite = false;
    }

    @Override
    public long updateValue(char key, long initialValueIfAbsent, LongToLongFunction function)
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
        if (this.keys[index] == key)
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        long value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public CharLongHashMap withKeyValue(char key1, long value1)
    {
        this.put(key1, value1);
        return this;
    }

    public CharLongHashMap withKeysValues(char key1, long value1, char key2, long value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public CharLongHashMap withKeysValues(char key1, long value1, char key2, long value2, char key3, long value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public CharLongHashMap withKeysValues(char key1, long value1, char key2, long value2, char key3, long value3, char key4, long value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public CharLongHashMap withoutKey(char key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public CharLongHashMap withoutAllKeys(CharIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    @Override
    public MutableCharLongMap asUnmodifiable()
    {
        return new UnmodifiableCharLongMap(this);
    }

    @Override
    public MutableCharLongMap asSynchronized()
    {
        return new SynchronizedCharLongMap(this);
    }

    @Override
    public ImmutableCharLongMap toImmutable()
    {
        return CharLongMaps.immutable.ofAll(this);
    }

    @Override
    public long get(char key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public long getIfAbsent(char key, long ifAbsent)
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

    private long getForSentinel(char key, long ifAbsent)
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

    private long slowGetIfAbsent(char key, long ifAbsent)
    {
        int index = this.probe(key);
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    private long fastGetIfAbsent(char key, long ifAbsent)
    {
        int index = this.mask((int) key);

        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            char keyAtIndex = this.keys[index];
            if (keyAtIndex == key)
            {
                return this.values[index];
            }
            if (keyAtIndex == EMPTY_KEY)
            {
                return ifAbsent;
            }
            index = (index + 1) & (this.keys.length - 1);
        }
        return this.slowGetIfAbsentTwo(key, ifAbsent);
    }

    private long slowGetIfAbsentTwo(char key, long ifAbsent)
    {
        int index = this.probeTwo(key, -1);
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    @Override
    public long getOrThrow(char key)
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
    public boolean containsKey(char key)
    {
        if (isEmptyKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsZeroKey;
        }
        if (isRemovedKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsOneKey;
        }
        return this.keys[this.probe(key)] == key;
    }

    @Override
    public void forEachKey(CharProcedure procedure)
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
    public void forEachKeyValue(CharLongProcedure procedure)
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
    public LazyCharIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<CharLongPair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableLongCharMap flipUniqueValues()
    {
        MutableLongCharMap result = LongCharMaps.mutable.empty();
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
    public CharLongHashMap select(CharLongPredicate predicate)
    {
        CharLongHashMap result = new CharLongHashMap();

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
    public CharLongHashMap reject(CharLongPredicate predicate)
    {
        CharLongHashMap result = new CharLongHashMap();

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
                out.writeChar(EMPTY_KEY);
                out.writeLong(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeChar(REMOVED_KEY);
                out.writeLong(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeChar(this.keys[i]);
                out.writeLong(this.values[i]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readChar(), in.readLong());
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
        char[] old = this.keys;
        long[] oldValues = this.values;
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
    int probe(char element)
    {
        int index = this.mask((int) element);
        char keyAtIndex = this.keys[index];

        if (keyAtIndex == element || keyAtIndex == EMPTY_KEY)
        {
            return index;
        }

        int removedIndex = keyAtIndex == REMOVED_KEY ? index : -1;
        for (int i = 1; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.keys.length - 1);
            keyAtIndex = this.keys[nextIndex];
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

    int probeTwo(char element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element);
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.keys.length - 1);
            char keyAtIndex = this.keys[nextIndex];
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

    int probeThree(char element, int removedIndex)
    {
        int nextIndex = (int) SpreadFunctions.charSpreadOne(element);
        int spreadTwo = Integer.reverse(SpreadFunctions.charSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask(nextIndex + spreadTwo);
            char keyAtIndex = this.keys[nextIndex];
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
    int spreadAndMask(char element)
    {
        int code = SpreadFunctions.charSpreadOne(element);
        return this.mask(code);
    }

    int spreadTwoAndMask(char element)
    {
        int code = SpreadFunctions.charSpreadTwo(element);
        return this.mask(code);
    }

    private int mask(int spread)
    {
        return spread & (this.keys.length - 1);
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.keys = new char[sizeToAllocate];
        this.values = new long[sizeToAllocate];
    }

    private static boolean isEmptyKey(char key)
    {
        return key == EMPTY_KEY;
    }

    private static boolean isRemovedKey(char key)
    {
        return key == REMOVED_KEY;
    }

    private static boolean isNonSentinel(char key)
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

    private class InternalLongIterator implements MutableLongIterator
    {
        private int count;
        private int position;
        private char lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < CharLongHashMap.this.size();
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
                if (CharLongHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return CharLongHashMap.this.get(EMPTY_KEY);
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (CharLongHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return CharLongHashMap.this.get(REMOVED_KEY);
                }
            }
            char[] keys = CharLongHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.lastKey = keys[this.position];
            long result = CharLongHashMap.this.values[this.position];
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
            CharLongHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    private class KeysView extends AbstractLazyCharIterable
    {
        @Override
        public CharIterator charIterator()
        {
            return new UnmodifiableCharIterator(new KeySetIterator());
        }

        /**
         * @since 7.0.
         */
        @Override
        public void each(CharProcedure procedure)
        {
            CharLongHashMap.this.forEachKey(procedure);
        }
    }

    private class KeySetIterator implements MutableCharIterator
    {
        private int count;
        private int position;
        private char lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < CharLongHashMap.this.size();
        }

        @Override
        public char next()
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
                if (CharLongHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (CharLongHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            char[] keys = CharLongHashMap.this.keys;
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
            CharLongHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    @Override
    public MutableCharSet keySet()
    {
        return new KeySet();
    }

    private class KeySet extends AbstractMutableCharKeySet
    {
        @Override
        protected MutableCharKeysMap getOuter()
        {
            return CharLongHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return CharLongHashMap.this.sentinelValues;
        }

        @Override
        protected char getKeyAtIndex(int index)
        {
            return CharLongHashMap.this.keys[index];
        }

        @Override
        protected int getTableSize()
        {
            return CharLongHashMap.this.keys.length;
        }

        @Override
        public MutableCharIterator charIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(CharIterable source)
        {
            int oldSize = CharLongHashMap.this.size();
            CharSet sourceSet = source instanceof CharSet ? (CharSet) source : source.toSet();
            CharLongHashMap retained = CharLongHashMap.this.select((char key, long value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                CharLongHashMap.this.keys = retained.keys;
                CharLongHashMap.this.values = retained.values;
                CharLongHashMap.this.sentinelValues = retained.sentinelValues;
                CharLongHashMap.this.occupiedWithData = retained.occupiedWithData;
                CharLongHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(char... source)
        {
            return this.retainAll(CharHashSet.newSetWith(source));
        }

        @Override
        public CharSet freeze()
        {
            CharLongHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (CharLongHashMap.this.sentinelValues != null)
            {
                containsZeroKey = CharLongHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = CharLongHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableCharMapKeySet(CharLongHashMap.this.keys, CharLongHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableCharSet newEmpty()
        {
            return new CharHashSet();
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
            return CharLongHashMap.this.longIterator();
        }

        @Override
        public boolean remove(long item)
        {
            int oldSize = CharLongHashMap.this.size();

            if (CharLongHashMap.this.sentinelValues != null)
            {
                if (CharLongHashMap.this.sentinelValues.containsZeroKey && item == CharLongHashMap.this.sentinelValues.zeroValue)
                {
                    CharLongHashMap.this.removeKey(EMPTY_KEY);
                }
            }
            if (CharLongHashMap.this.sentinelValues != null)
            {
                if (CharLongHashMap.this.sentinelValues.containsOneKey && item == CharLongHashMap.this.sentinelValues.oneValue)
                {
                    CharLongHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < CharLongHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(CharLongHashMap.this.keys[i]) && item == CharLongHashMap.this.values[i])
                {
                    CharLongHashMap.this.removeKey(CharLongHashMap.this.keys[i]);
                }
            }
            return oldSize != CharLongHashMap.this.size();
        }

        @Override
        public boolean retainAll(LongIterable source)
        {
            int oldSize = CharLongHashMap.this.size();
            LongSet sourceSet = source instanceof LongSet ? (LongSet) source : source.toSet();
            CharLongHashMap retained = CharLongHashMap.this.select((char key, long value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                CharLongHashMap.this.keys = retained.keys;
                CharLongHashMap.this.values = retained.values;
                CharLongHashMap.this.sentinelValues = retained.sentinelValues;
                CharLongHashMap.this.occupiedWithData = retained.occupiedWithData;
                CharLongHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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

    private class KeyValuesView extends AbstractLazyIterable<CharLongPair>
    {
        @Override
        public void each(Procedure<? super CharLongPair> procedure)
        {
            if (CharLongHashMap.this.sentinelValues != null)
            {
                if (CharLongHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, CharLongHashMap.this.sentinelValues.zeroValue));
                }
                if (CharLongHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, CharLongHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < CharLongHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(CharLongHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(CharLongHashMap.this.keys[i], CharLongHashMap.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super CharLongPair> objectIntProcedure)
        {
            int index = 0;
            if (CharLongHashMap.this.sentinelValues != null)
            {
                if (CharLongHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, CharLongHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (CharLongHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, CharLongHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < CharLongHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(CharLongHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(CharLongHashMap.this.keys[i], CharLongHashMap.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super CharLongPair, ? super P> procedure, P parameter)
        {
            if (CharLongHashMap.this.sentinelValues != null)
            {
                if (CharLongHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, CharLongHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (CharLongHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, CharLongHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < CharLongHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(CharLongHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(CharLongHashMap.this.keys[i], CharLongHashMap.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<CharLongPair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<CharLongPair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public CharLongPair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (CharLongHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, CharLongHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (CharLongHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, CharLongHashMap.this.sentinelValues.oneValue);
                    }
                }

                char[] keys = CharLongHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                CharLongPair result = PrimitiveTuples.pair(keys[this.position], CharLongHashMap.this.values[this.position]);
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
                return this.count != CharLongHashMap.this.size();
            }
        }
    }
}

