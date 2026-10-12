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
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.CharFunction0;
import org.eclipse.collections.api.block.function.primitive.CharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.CharCharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharCharPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharCharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.factory.primitive.CharCharMaps;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.map.primitive.CharCharMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharCharMap;
import org.eclipse.collections.api.map.primitive.MutableCharCharMap;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;
import org.eclipse.collections.impl.bag.mutable.primitive.CharHashBag;
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
public class CharCharHashMap extends AbstractMutableCharValuesMap implements MutableCharCharMap, Externalizable, MutableCharKeysMap
{
    private static final char EMPTY_VALUE = (char) 0;
    private static final long serialVersionUID = 1L;
    private static final char EMPTY_KEY = (char) 0;
    private static final char REMOVED_KEY = (char) 1;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 2;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private char[] keysValues;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues sentinelValues;

    private boolean copyKeysOnWrite;

    public CharCharHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public CharCharHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public CharCharHashMap(CharCharMap map)
    {
        if (map instanceof CharCharHashMap && ((CharCharHashMap) map).occupiedWithSentinels == 0)
        {
            CharCharHashMap hashMap = (CharCharHashMap) map;
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

    public static CharCharHashMap newWithKeysValues(char key1, char value1)
    {
        return new CharCharHashMap(1).withKeyValue(key1, value1);
    }

    public static CharCharHashMap newWithKeysValues(char key1, char value1, char key2, char value2)
    {
        return new CharCharHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static CharCharHashMap newWithKeysValues(char key1, char value1, char key2, char value2, char key3, char value3)
    {
        return new CharCharHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static CharCharHashMap newWithKeysValues(char key1, char value1, char key2, char value2, char key3, char value3, char key4, char value4)
    {
        return new CharCharHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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
    protected char getEmptyValue()
    {
        return EMPTY_VALUE;
    }

    @Override
    protected int getTableSize()
    {
        return this.keysValues.length / 2;
    }

    @Override
    protected char getValueAtIndex(int index)
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

        if (!(obj instanceof CharCharMap))
        {
            return false;
        }

        CharCharMap other = (CharCharMap) obj;

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
            char key = this.keysValues[i];
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
            char key = this.keysValues[i];
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
    public MutableCharIterator charIterator()
    {
        return new InternalCharIterator();
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectCharToObjectFunction<? super V, ? extends V> function)
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
        Arrays.fill(this.keysValues, (char) 0);
    }

    @Override
    public void put(char key, char value)
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
        char keyAtIndex = this.keysValues[index];
        if (keyAtIndex == key)
        {
            this.keysValues[index + 1] = value;
        }
        else
        {
            this.addKeyValueAtIndex(key, value, index);
        }
    }

    private void putForRemovedSentinel(char value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addRemovedKeyValue(value);
    }

    private void putForEmptySentinel(char value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addEmptyKeyValue(value);
    }

    @Override
    public void putAll(CharCharMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(CharCharToCharFunction function)
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
        if (this.keysValues[index] == key)
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
    public char removeKeyIfAbsent(char key, char value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return value;
            }
            char oldValue = this.sentinelValues.zeroValue;
            this.removeEmptyKey();
            return oldValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                return value;
            }
            char oldValue = this.sentinelValues.oneValue;
            this.removeRemovedKey();
            return oldValue;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            char oldValue = this.keysValues[index + 1];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return value;
    }

    @Override
    public char getIfAbsentPut(char key, char value)
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
    public char getAndPut(char key, char putValue, char defaultValue)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                char existingValue = this.sentinelValues.zeroValue;
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
                char existingValue = this.sentinelValues.oneValue;
                this.sentinelValues.oneValue = putValue;
                return existingValue;
            }
            this.addRemovedKeyValue(putValue);
            return defaultValue;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            char existingValue = this.keysValues[index + 1];
            this.keysValues[index + 1] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public char getIfAbsentPut(char key, CharFunction0 function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                char value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            char value = function.value();
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                char value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            char value = function.value();
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        char value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> char getIfAbsentPutWith(char key, CharFunction<? super P> function, P parameter)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                char value = function.charValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            char value = function.charValueOf(parameter);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                char value = function.charValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            char value = function.charValueOf(parameter);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        char value = function.charValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public char getIfAbsentPutWithKey(char key, CharToCharFunction function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                char value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            char value = function.valueOf(key);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                char value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            char value = function.valueOf(key);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        char value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public char addToValue(char key, char toBeAdded)
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

    private void addKeyValueAtIndex(char key, char value, int index)
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
        char[] copy = new char[this.keysValues.length];
        System.arraycopy(this.keysValues, 0, copy, 0, this.keysValues.length);
        this.keysValues = copy;
        this.copyKeysOnWrite = false;
    }

    @Override
    public char updateValue(char key, char initialValueIfAbsent, CharToCharFunction function)
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
        char value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public CharCharHashMap withKeyValue(char key1, char value1)
    {
        this.put(key1, value1);
        return this;
    }

    public CharCharHashMap withKeysValues(char key1, char value1, char key2, char value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public CharCharHashMap withKeysValues(char key1, char value1, char key2, char value2, char key3, char value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public CharCharHashMap withKeysValues(char key1, char value1, char key2, char value2, char key3, char value3, char key4, char value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public CharCharHashMap withoutKey(char key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public CharCharHashMap withoutAllKeys(CharIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    @Override
    public MutableCharCharMap asUnmodifiable()
    {
        return new UnmodifiableCharCharMap(this);
    }

    @Override
    public MutableCharCharMap asSynchronized()
    {
        return new SynchronizedCharCharMap(this);
    }

    @Override
    public ImmutableCharCharMap toImmutable()
    {
        return CharCharMaps.immutable.ofAll(this);
    }

    @Override
    public char get(char key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public char getIfAbsent(char key, char ifAbsent)
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

    private char getForSentinel(char key, char ifAbsent)
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

    private char slowGetIfAbsent(char key, char ifAbsent)
    {
        int index = this.probe(key);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        return ifAbsent;
    }

    private char fastGetIfAbsent(char key, char ifAbsent)
    {
        int index = this.mask((int) key) << 1;

        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            char keyAtIndex = this.keysValues[index];
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

    private char slowGetIfAbsentTwo(char key, char ifAbsent)
    {
        int index = this.probeTwo(key, -1);
        if (this.keysValues[index] == key)
        {
            return this.keysValues[index + 1];
        }
        return ifAbsent;
    }

    @Override
    public char getOrThrow(char key)
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
        return this.keysValues[this.probe(key)] == key;
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
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                procedure.value(this.keysValues[i]);
            }
        }
    }

    @Override
    public void forEachKeyValue(CharCharProcedure procedure)
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
    public LazyCharIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<CharCharPair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableCharCharMap flipUniqueValues()
    {
        MutableCharCharMap result = CharCharMaps.mutable.empty();
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
    public CharCharHashMap select(CharCharPredicate predicate)
    {
        CharCharHashMap result = new CharCharHashMap();

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
    public CharCharHashMap reject(CharCharPredicate predicate)
    {
        CharCharHashMap result = new CharCharHashMap();

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
                out.writeChar(EMPTY_KEY);
                out.writeChar(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeChar(REMOVED_KEY);
                out.writeChar(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keysValues.length; i += 2)
        {
            if (isNonSentinel(this.keysValues[i]))
            {
                out.writeChar(this.keysValues[i]);
                out.writeChar(this.keysValues[i + 1]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readChar(), in.readChar());
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
        char[] old = this.keysValues;
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
    int probe(char element)
    {
        int index = this.mask((int) element) << 1;
        char keyAtIndex = this.keysValues[index];

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

    int probeTwo(char element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element) << 1;
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i += 2)
        {
            int nextIndex = (index + i) & (this.keysValues.length - 1);
            char keyAtIndex = this.keysValues[nextIndex];
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
        int nextIndex = (int) SpreadFunctions.charSpreadOne(element) << 1;
        int spreadTwo = Integer.reverse(SpreadFunctions.charSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask((nextIndex >> 1) + spreadTwo) << 1;
            char keyAtIndex = this.keysValues[nextIndex];
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
        return spread & ((this.keysValues.length >> 1) - 1);
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.keysValues = new char[sizeToAllocate << 1];
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
        return !isEmptyKey(this.keysValues[index * 2]) && !isRemovedKey(this.keysValues[index * 2]);
    }

    private int maxOccupiedWithData()
    {
        return this.keysValues.length >> 2;
    }

    private class InternalCharIterator implements MutableCharIterator
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
            return this.count < CharCharHashMap.this.size();
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
                if (CharCharHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return CharCharHashMap.this.get(EMPTY_KEY);
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (CharCharHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return CharCharHashMap.this.get(REMOVED_KEY);
                }
            }
            char[] keys = CharCharHashMap.this.keysValues;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position += 2;
            }
            this.lastKey = keys[this.position];
            char result = CharCharHashMap.this.keysValues[this.position + 1];
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
            CharCharHashMap.this.removeKey(this.lastKey);
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
            CharCharHashMap.this.forEachKey(procedure);
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
            return this.count < CharCharHashMap.this.size();
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
                if (CharCharHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (CharCharHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            char[] keys = CharCharHashMap.this.keysValues;
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
            CharCharHashMap.this.removeKey(this.lastKey);
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
            return CharCharHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return CharCharHashMap.this.sentinelValues;
        }

        @Override
        protected char getKeyAtIndex(int index)
        {
            return CharCharHashMap.this.keysValues[index * 2];
        }

        @Override
        protected int getTableSize()
        {
            return CharCharHashMap.this.keysValues.length / 2;
        }

        @Override
        public MutableCharIterator charIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(CharIterable source)
        {
            int oldSize = CharCharHashMap.this.size();
            CharSet sourceSet = source instanceof CharSet ? (CharSet) source : source.toSet();
            CharCharHashMap retained = CharCharHashMap.this.select((char key, char value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                CharCharHashMap.this.keysValues = retained.keysValues;
                CharCharHashMap.this.sentinelValues = retained.sentinelValues;
                CharCharHashMap.this.occupiedWithData = retained.occupiedWithData;
                CharCharHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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
            CharCharHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (CharCharHashMap.this.sentinelValues != null)
            {
                containsZeroKey = CharCharHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = CharCharHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableCharCharMapKeySet(CharCharHashMap.this.keysValues, CharCharHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
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
    public MutableCharCollection values()
    {
        return new ValuesCollection();
    }

    private class ValuesCollection extends AbstractCharValuesCollection
    {
        @Override
        public MutableCharIterator charIterator()
        {
            return CharCharHashMap.this.charIterator();
        }

        @Override
        public boolean remove(char item)
        {
            int oldSize = CharCharHashMap.this.size();

            if (CharCharHashMap.this.sentinelValues != null)
            {
                if (CharCharHashMap.this.sentinelValues.containsZeroKey && item == CharCharHashMap.this.sentinelValues.zeroValue)
                {
                    CharCharHashMap.this.removeKey(EMPTY_KEY);
                }
            }
            if (CharCharHashMap.this.sentinelValues != null)
            {
                if (CharCharHashMap.this.sentinelValues.containsOneKey && item == CharCharHashMap.this.sentinelValues.oneValue)
                {
                    CharCharHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < CharCharHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(CharCharHashMap.this.keysValues[i]) && item == CharCharHashMap.this.keysValues[i + 1])
                {
                    CharCharHashMap.this.removeKey(CharCharHashMap.this.keysValues[i]);
                }
            }
            return oldSize != CharCharHashMap.this.size();
        }

        @Override
        public boolean retainAll(CharIterable source)
        {
            int oldSize = CharCharHashMap.this.size();
            CharSet sourceSet = source instanceof CharSet ? (CharSet) source : source.toSet();
            CharCharHashMap retained = CharCharHashMap.this.select((char key, char value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                CharCharHashMap.this.keysValues = retained.keysValues;
                CharCharHashMap.this.sentinelValues = retained.sentinelValues;
                CharCharHashMap.this.occupiedWithData = retained.occupiedWithData;
                CharCharHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableCharCollection newEmpty()
        {
            return new CharHashBag();
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<CharCharPair>
    {
        @Override
        public void each(Procedure<? super CharCharPair> procedure)
        {
            if (CharCharHashMap.this.sentinelValues != null)
            {
                if (CharCharHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, CharCharHashMap.this.sentinelValues.zeroValue));
                }
                if (CharCharHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, CharCharHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < CharCharHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(CharCharHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(CharCharHashMap.this.keysValues[i], CharCharHashMap.this.keysValues[i + 1]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super CharCharPair> objectIntProcedure)
        {
            int index = 0;
            if (CharCharHashMap.this.sentinelValues != null)
            {
                if (CharCharHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, CharCharHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (CharCharHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, CharCharHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < CharCharHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(CharCharHashMap.this.keysValues[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(CharCharHashMap.this.keysValues[i], CharCharHashMap.this.keysValues[i + 1]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super CharCharPair, ? super P> procedure, P parameter)
        {
            if (CharCharHashMap.this.sentinelValues != null)
            {
                if (CharCharHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, CharCharHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (CharCharHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, CharCharHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < CharCharHashMap.this.keysValues.length; i += 2)
            {
                if (isNonSentinel(CharCharHashMap.this.keysValues[i]))
                {
                    procedure.value(PrimitiveTuples.pair(CharCharHashMap.this.keysValues[i], CharCharHashMap.this.keysValues[i + 1]), parameter);
                }
            }
        }

        @Override
        public Iterator<CharCharPair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<CharCharPair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public CharCharPair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (CharCharHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, CharCharHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (CharCharHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, CharCharHashMap.this.sentinelValues.oneValue);
                    }
                }

                char[] keys = CharCharHashMap.this.keysValues;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position += 2;
                }
                CharCharPair result = PrimitiveTuples.pair(keys[this.position], CharCharHashMap.this.keysValues[this.position + 1]);
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
                return this.count != CharCharHashMap.this.size();
            }
        }
    }
}

