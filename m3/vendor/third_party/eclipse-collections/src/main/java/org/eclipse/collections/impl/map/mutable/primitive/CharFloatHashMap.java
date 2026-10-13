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
import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.CharToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction0;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.CharFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharFloatPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.factory.primitive.CharFloatMaps;
import org.eclipse.collections.api.factory.primitive.FloatCharMaps;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.map.primitive.CharFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharFloatMap;
import org.eclipse.collections.api.map.primitive.MutableCharFloatMap;
import org.eclipse.collections.api.map.primitive.MutableFloatCharMap;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.tuple.primitive.CharFloatPair;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
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
public class CharFloatHashMap extends AbstractMutableFloatValuesMap implements MutableCharFloatMap, Externalizable, MutableCharKeysMap
{
    private static final float EMPTY_VALUE = 0.0f;
    private static final long serialVersionUID = 1L;
    private static final char EMPTY_KEY = (char) 0;
    private static final char REMOVED_KEY = (char) 1;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 2;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private char[] keys;
    private float[] values;
    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues sentinelValues;

    private boolean copyKeysOnWrite;

    public CharFloatHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public CharFloatHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(initialCapacity << 1);
        this.allocateTable(capacity);
    }

    public CharFloatHashMap(CharFloatMap map)
    {
        if (map instanceof CharFloatHashMap && ((CharFloatHashMap) map).occupiedWithSentinels == 0)
        {
            CharFloatHashMap hashMap = (CharFloatHashMap) map;
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

    public static CharFloatHashMap newWithKeysValues(char key1, float value1)
    {
        return new CharFloatHashMap(1).withKeyValue(key1, value1);
    }

    public static CharFloatHashMap newWithKeysValues(char key1, float value1, char key2, float value2)
    {
        return new CharFloatHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static CharFloatHashMap newWithKeysValues(char key1, float value1, char key2, float value2, char key3, float value3)
    {
        return new CharFloatHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static CharFloatHashMap newWithKeysValues(char key1, float value1, char key2, float value2, char key3, float value3, char key4, float value4)
    {
        return new CharFloatHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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
    protected float getEmptyValue()
    {
        return EMPTY_VALUE;
    }

    @Override
    protected int getTableSize()
    {
        return this.values.length;
    }

    @Override
    protected float getValueAtIndex(int index)
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

        if (!(obj instanceof CharFloatMap))
        {
            return false;
        }

        CharFloatMap other = (CharFloatMap) obj;

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
            if (this.sentinelValues.containsZeroKey && (!other.containsKey(EMPTY_KEY) || Float.compare(this.sentinelValues.zeroValue, other.getOrThrow(EMPTY_KEY)) != 0))
            {
                return false;
            }

            if (this.sentinelValues.containsOneKey && (!other.containsKey(REMOVED_KEY) || Float.compare(this.sentinelValues.oneValue, other.getOrThrow(REMOVED_KEY)) != 0))
            {
                return false;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            char key = this.keys[i];
            if (isNonSentinel(key) && (!other.containsKey(key) || Float.compare(this.values[i], other.getOrThrow(key)) != 0))
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
                result += (int) EMPTY_KEY ^ Float.floatToIntBits(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result += (int) REMOVED_KEY ^ Float.floatToIntBits(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result += (int) this.keys[i] ^ Float.floatToIntBits(this.values[i]);
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
    public MutableFloatIterator floatIterator()
    {
        return new InternalFloatIterator();
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectFloatToObjectFunction<? super V, ? extends V> function)
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
    public void put(char key, float value)
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

    private void putForRemovedSentinel(float value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addRemovedKeyValue(value);
    }

    private void putForEmptySentinel(float value)
    {
        if (this.sentinelValues == null)
        {
            this.sentinelValues = new SentinelValues();
        }
        this.addEmptyKeyValue(value);
    }

    @Override
    public void putAll(CharFloatMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(CharFloatToFloatFunction function)
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
    public float removeKeyIfAbsent(char key, float value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return value;
            }
            float oldValue = this.sentinelValues.zeroValue;
            this.removeEmptyKey();
            return oldValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                return value;
            }
            float oldValue = this.sentinelValues.oneValue;
            this.removeRemovedKey();
            return oldValue;
        }
        int index = this.probe(key);
        if (this.keys[index] == key)
        {
            float oldValue = this.values[index];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return value;
    }

    @Override
    public float getIfAbsentPut(char key, float value)
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
    public float getAndPut(char key, float putValue, float defaultValue)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                float existingValue = this.sentinelValues.zeroValue;
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
                float existingValue = this.sentinelValues.oneValue;
                this.sentinelValues.oneValue = putValue;
                return existingValue;
            }
            this.addRemovedKeyValue(putValue);
            return defaultValue;
        }
        int index = this.probe(key);
        if (this.keys[index] == key)
        {
            float existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public float getIfAbsentPut(char key, FloatFunction0 function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                float value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            float value = function.value();
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                float value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            float value = function.value();
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        float value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> float getIfAbsentPutWith(char key, FloatFunction<? super P> function, P parameter)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                float value = function.floatValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            float value = function.floatValueOf(parameter);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                float value = function.floatValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            float value = function.floatValueOf(parameter);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        float value = function.floatValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public float getIfAbsentPutWithKey(char key, CharToFloatFunction function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                float value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            float value = function.valueOf(key);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                float value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            float value = function.valueOf(key);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        float value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public float addToValue(char key, float toBeAdded)
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

    private void addKeyValueAtIndex(char key, float value, int index)
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
    public float updateValue(char key, float initialValueIfAbsent, FloatToFloatFunction function)
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
        float value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public CharFloatHashMap withKeyValue(char key1, float value1)
    {
        this.put(key1, value1);
        return this;
    }

    public CharFloatHashMap withKeysValues(char key1, float value1, char key2, float value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public CharFloatHashMap withKeysValues(char key1, float value1, char key2, float value2, char key3, float value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public CharFloatHashMap withKeysValues(char key1, float value1, char key2, float value2, char key3, float value3, char key4, float value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public CharFloatHashMap withoutKey(char key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public CharFloatHashMap withoutAllKeys(CharIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    @Override
    public MutableCharFloatMap asUnmodifiable()
    {
        return new UnmodifiableCharFloatMap(this);
    }

    @Override
    public MutableCharFloatMap asSynchronized()
    {
        return new SynchronizedCharFloatMap(this);
    }

    @Override
    public ImmutableCharFloatMap toImmutable()
    {
        return CharFloatMaps.immutable.ofAll(this);
    }

    @Override
    public float get(char key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public float getIfAbsent(char key, float ifAbsent)
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

    private float getForSentinel(char key, float ifAbsent)
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

    private float slowGetIfAbsent(char key, float ifAbsent)
    {
        int index = this.probe(key);
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    private float fastGetIfAbsent(char key, float ifAbsent)
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

    private float slowGetIfAbsentTwo(char key, float ifAbsent)
    {
        int index = this.probeTwo(key, -1);
        if (this.keys[index] == key)
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    @Override
    public float getOrThrow(char key)
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
    public void forEachKeyValue(CharFloatProcedure procedure)
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
    public RichIterable<CharFloatPair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableFloatCharMap flipUniqueValues()
    {
        MutableFloatCharMap result = FloatCharMaps.mutable.empty();
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
    public CharFloatHashMap select(CharFloatPredicate predicate)
    {
        CharFloatHashMap result = new CharFloatHashMap();

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
    public CharFloatHashMap reject(CharFloatPredicate predicate)
    {
        CharFloatHashMap result = new CharFloatHashMap();

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
                out.writeFloat(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeChar(REMOVED_KEY);
                out.writeFloat(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeChar(this.keys[i]);
                out.writeFloat(this.values[i]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readChar(), in.readFloat());
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
        float[] oldValues = this.values;
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
        this.values = new float[sizeToAllocate];
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

    private class InternalFloatIterator implements MutableFloatIterator
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
            return this.count < CharFloatHashMap.this.size();
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
                if (CharFloatHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return CharFloatHashMap.this.get(EMPTY_KEY);
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (CharFloatHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return CharFloatHashMap.this.get(REMOVED_KEY);
                }
            }
            char[] keys = CharFloatHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.lastKey = keys[this.position];
            float result = CharFloatHashMap.this.values[this.position];
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
            CharFloatHashMap.this.removeKey(this.lastKey);
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
            CharFloatHashMap.this.forEachKey(procedure);
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
            return this.count < CharFloatHashMap.this.size();
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
                if (CharFloatHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (CharFloatHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            char[] keys = CharFloatHashMap.this.keys;
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
            CharFloatHashMap.this.removeKey(this.lastKey);
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
            return CharFloatHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return CharFloatHashMap.this.sentinelValues;
        }

        @Override
        protected char getKeyAtIndex(int index)
        {
            return CharFloatHashMap.this.keys[index];
        }

        @Override
        protected int getTableSize()
        {
            return CharFloatHashMap.this.keys.length;
        }

        @Override
        public MutableCharIterator charIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(CharIterable source)
        {
            int oldSize = CharFloatHashMap.this.size();
            CharSet sourceSet = source instanceof CharSet ? (CharSet) source : source.toSet();
            CharFloatHashMap retained = CharFloatHashMap.this.select((char key, float value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                CharFloatHashMap.this.keys = retained.keys;
                CharFloatHashMap.this.values = retained.values;
                CharFloatHashMap.this.sentinelValues = retained.sentinelValues;
                CharFloatHashMap.this.occupiedWithData = retained.occupiedWithData;
                CharFloatHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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
            CharFloatHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (CharFloatHashMap.this.sentinelValues != null)
            {
                containsZeroKey = CharFloatHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = CharFloatHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableCharMapKeySet(CharFloatHashMap.this.keys, CharFloatHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
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
    public MutableFloatCollection values()
    {
        return new ValuesCollection();
    }

    private class ValuesCollection extends AbstractFloatValuesCollection
    {
        @Override
        public MutableFloatIterator floatIterator()
        {
            return CharFloatHashMap.this.floatIterator();
        }

        @Override
        public boolean remove(float item)
        {
            int oldSize = CharFloatHashMap.this.size();

            if (CharFloatHashMap.this.sentinelValues != null)
            {
                if (CharFloatHashMap.this.sentinelValues.containsZeroKey && Float.compare(item, CharFloatHashMap.this.sentinelValues.zeroValue) == 0)
                {
                    CharFloatHashMap.this.removeKey(EMPTY_KEY);
                }
            }
            if (CharFloatHashMap.this.sentinelValues != null)
            {
                if (CharFloatHashMap.this.sentinelValues.containsOneKey && Float.compare(item, CharFloatHashMap.this.sentinelValues.oneValue) == 0)
                {
                    CharFloatHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < CharFloatHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(CharFloatHashMap.this.keys[i]) && Float.compare(item, CharFloatHashMap.this.values[i]) == 0)
                {
                    CharFloatHashMap.this.removeKey(CharFloatHashMap.this.keys[i]);
                }
            }
            return oldSize != CharFloatHashMap.this.size();
        }

        @Override
        public boolean retainAll(FloatIterable source)
        {
            int oldSize = CharFloatHashMap.this.size();
            FloatSet sourceSet = source instanceof FloatSet ? (FloatSet) source : source.toSet();
            CharFloatHashMap retained = CharFloatHashMap.this.select((char key, float value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                CharFloatHashMap.this.keys = retained.keys;
                CharFloatHashMap.this.values = retained.values;
                CharFloatHashMap.this.sentinelValues = retained.sentinelValues;
                CharFloatHashMap.this.occupiedWithData = retained.occupiedWithData;
                CharFloatHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableFloatCollection newEmpty()
        {
            return new FloatHashBag();
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<CharFloatPair>
    {
        @Override
        public void each(Procedure<? super CharFloatPair> procedure)
        {
            if (CharFloatHashMap.this.sentinelValues != null)
            {
                if (CharFloatHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, CharFloatHashMap.this.sentinelValues.zeroValue));
                }
                if (CharFloatHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, CharFloatHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < CharFloatHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(CharFloatHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(CharFloatHashMap.this.keys[i], CharFloatHashMap.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super CharFloatPair> objectIntProcedure)
        {
            int index = 0;
            if (CharFloatHashMap.this.sentinelValues != null)
            {
                if (CharFloatHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, CharFloatHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (CharFloatHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, CharFloatHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < CharFloatHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(CharFloatHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(CharFloatHashMap.this.keys[i], CharFloatHashMap.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super CharFloatPair, ? super P> procedure, P parameter)
        {
            if (CharFloatHashMap.this.sentinelValues != null)
            {
                if (CharFloatHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, CharFloatHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (CharFloatHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, CharFloatHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < CharFloatHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(CharFloatHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(CharFloatHashMap.this.keys[i], CharFloatHashMap.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<CharFloatPair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<CharFloatPair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public CharFloatPair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (CharFloatHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, CharFloatHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (CharFloatHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, CharFloatHashMap.this.sentinelValues.oneValue);
                    }
                }

                char[] keys = CharFloatHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                CharFloatPair result = PrimitiveTuples.pair(keys[this.position], CharFloatHashMap.this.values[this.position]);
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
                return this.count != CharFloatHashMap.this.size();
            }
        }
    }
}

