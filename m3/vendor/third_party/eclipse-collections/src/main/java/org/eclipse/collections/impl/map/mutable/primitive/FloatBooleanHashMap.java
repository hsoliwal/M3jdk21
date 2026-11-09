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
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction0;
import org.eclipse.collections.api.block.function.primitive.BooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.FloatBooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatBooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.FloatBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.iterator.MutableBooleanIterator;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.map.primitive.FloatBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatBooleanMap;
import org.eclipse.collections.api.map.primitive.MutableFloatBooleanMap;
import org.eclipse.collections.api.set.primitive.BooleanSet;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.FloatBooleanPair;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.factory.primitive.FloatBooleanMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyFloatIterable;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file primitiveBooleanHashMap.stg.
 *
 * @since 3.0.
 */
public class FloatBooleanHashMap extends AbstractMutableBooleanValuesMap implements MutableFloatBooleanMap, MutableFloatKeysMap, Externalizable
{
    static final boolean EMPTY_VALUE = false;
    private static final long serialVersionUID = 1L;
    private static final float EMPTY_KEY = 0.0f;
    private static final float REMOVED_KEY = 1.0f;

    /**
     * @deprecated in 5.1.0.
     */
    @Deprecated
    private static final float DEFAULT_LOAD_FACTOR = 0.5f;
    private static final int OCCUPIED_DATA_RATIO = 2;
    private static final int OCCUPIED_SENTINEL_RATIO = 4;
    private static final int DEFAULT_INITIAL_CAPACITY = 8;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 4;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private float[] keys;
    private BitSet values;

    private int occupiedWithData;
    private int occupiedWithSentinels;
    private SentinelValues sentinelValues;

    public FloatBooleanHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public FloatBooleanHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity * OCCUPIED_DATA_RATIO));
        this.allocateTable(capacity);
    }

    public FloatBooleanHashMap(FloatBooleanMap map)
    {
        this(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }

    /**
     * @deprecated in 5.1.0.
     */
    @Deprecated
    public FloatBooleanHashMap(int initialCapacity, float loadFactor)
    {
        this(initialCapacity);
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
    protected boolean getEmptyValue()
    {
        return EMPTY_VALUE;
    }

    @Override
    protected int getTableSize()
    {
        return this.keys.length;
    }

    @Override
    protected boolean getValueAtIndex(int index)
    {
        return this.values.get(index);
    }

    @Override
    protected boolean isNonSentinelAtIndex(int index)
    {
        return !isEmptyKey(this.keys[index]) && !isRemovedKey(this.keys[index]);
    }

    private int smallestPowerOfTwoGreaterThan(int n)
    {
        return n > 1 ? Integer.highestOneBit(n - 1) << 1 : 1;
    }

    @Override
    public MutableFloatBooleanMap asUnmodifiable()
    {
        return new UnmodifiableFloatBooleanMap(this);
    }

    @Override
    public MutableFloatBooleanMap asSynchronized()
    {
        return new SynchronizedFloatBooleanMap(this);
    }

    @Override
    public ImmutableFloatBooleanMap toImmutable()
    {
        return FloatBooleanMaps.immutable.withAll(this);
    }

    public static FloatBooleanHashMap newWithKeysValues(float key1, boolean value1)
    {
        return new FloatBooleanHashMap(1).withKeyValue(key1, value1);
    }

    public static FloatBooleanHashMap newWithKeysValues(float key1, boolean value1, float key2, boolean value2)
    {
        return new FloatBooleanHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static FloatBooleanHashMap newWithKeysValues(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3)
    {
        return new FloatBooleanHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static FloatBooleanHashMap newWithKeysValues(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3, float key4, boolean value4)
    {
        return new FloatBooleanHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public FloatBooleanHashMap withKeyValue(float key1, boolean value1)
    {
        this.put(key1, value1);
        return this;
    }

    public FloatBooleanHashMap withKeysValues(float key1, boolean value1, float key2, boolean value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public FloatBooleanHashMap withKeysValues(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public FloatBooleanHashMap withKeysValues(float key1, boolean value1, float key2, boolean value2, float key3, boolean value3, float key4, boolean value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public FloatBooleanHashMap withoutKey(float key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public FloatBooleanHashMap withoutAllKeys(FloatIterable keys)
    {
        keys.forEach(this::removeKey);
        return this;
    }

    private int fastCeil(float v)
    {
        int possibleResult = (int) v;
        if (v - possibleResult > 0.0F)
        {
            possibleResult++;
        }
        return possibleResult;
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

    protected void allocateTable(int sizeToAllocate)
    {
        this.keys = new float[sizeToAllocate];
        this.values = new BitSet(sizeToAllocate);
    }

    /**
     * Rehashes every element in the set into a new backing table of the smallest possible size and eliminating removed sentinels.
     */
    public void compact()
    {
        this.rehash(this.smallestPowerOfTwoGreaterThan(this.size()));
    }

    private void rehash()
    {
        this.rehash(this.keys.length);
    }

    private void rehashAndGrow()
    {
        this.rehash(this.keys.length << 1);
    }

    private void rehash(int newCapacity)
    {
        int oldLength = this.keys.length;
        float[] old = this.keys;
        BitSet oldValues = this.values;
        this.allocateTable(newCapacity);
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;

        for (int i = 0; i < oldLength; i++)
        {
            if (isNonSentinel(old[i]))
            {
                this.put(old[i], oldValues.get(i));
            }
        }
    }

    // exposed for testing
    int probe(float element)
    {
        int index = this.spreadAndMask(element);
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
        int nextIndex = Integer.reverse(SpreadFunctions.floatSpreadOne(element));
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

    @Override
    public void clear()
    {
        this.sentinelValues = null;
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;
        Arrays.fill(this.keys, EMPTY_KEY);
        this.values.clear();
    }

    @Override
    public void put(float key, boolean value)
    {
        if (isEmptyKey(key))
        {
            if (this.getSentinelValues() == null)
            {
                this.sentinelValues = new SentinelValues();
            }
            this.getSentinelValues().containsZeroKey = true;
            this.getSentinelValues().zeroValue = value;
            return;
        }

        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
            }
            this.getSentinelValues().containsOneKey = true;
            this.getSentinelValues().oneValue = value;
            return;
        }

        int index = this.probe(key);

        if (Float.compare(this.keys[index], key) == 0)
        {
            // key already present in map
            this.values.set(index, value);
            return;
        }

        this.addKeyValueAtIndex(key, value, index);
    }

    @Override
    public void putAll(FloatBooleanMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(FloatBooleanToBooleanFunction function)
    {
        if (this.sentinelValues != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                this.getSentinelValues().zeroValue = function.valueOf(EMPTY_KEY, this.getSentinelValues().zeroValue);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                this.getSentinelValues().oneValue = function.valueOf(REMOVED_KEY, this.getSentinelValues().oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                this.values.set(i, function.valueOf(this.keys[i], this.getValueAtIndex(i)));
            }
        }
    }

    @Override
    public boolean containsKey(float key)
    {
        if (isEmptyKey(key))
        {
            return this.getSentinelValues() != null && this.getSentinelValues().containsZeroKey;
        }
        if (isRemovedKey(key))
        {
            return this.getSentinelValues() != null && this.getSentinelValues().containsOneKey;
        }
        return Float.compare(this.keys[this.probe(key)], key) == 0;
    }

    @Override
    public boolean containsValue(boolean value)
    {
        if (this.getSentinelValues() != null && this.getSentinelValues().containsValue(value))
        {
            return true;
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && this.getValueAtIndex(i) == value)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean get(float key)
    {
        return this.getIfAbsent(key, this.getEmptyValue());
    }

    @Override
    public boolean getIfAbsent(float key, boolean ifAbsent)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.getSentinelValues().containsZeroKey)
            {
                return ifAbsent;
            }
            return this.getSentinelValues().zeroValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.getSentinelValues().containsOneKey)
            {
                return ifAbsent;
            }
            return this.getSentinelValues().oneValue;
        }
        int index = this.probe(key);
        if (this.isNonSentinelAtIndex(index))
        {
            return this.values.get(index);
        }
        return ifAbsent;
    }

    @Override
    public boolean getOrThrow(float key)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.getSentinelValues().containsZeroKey)
            {
                throw new IllegalStateException("Key " + key + " not present.");
            }
            return this.getSentinelValues().zeroValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.getSentinelValues().containsOneKey)
            {
                throw new IllegalStateException("Key " + key + " not present.");
            }
            return this.getSentinelValues().oneValue;
        }
        int index = this.probe(key);
        if (this.isNonSentinelAtIndex(index))
        {
            return this.values.get(index);
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean getIfAbsentPut(float key, boolean value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.getSentinelValues().containsZeroKey)
            {
                return this.getSentinelValues().zeroValue;
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
            if (this.getSentinelValues().containsOneKey)
            {
                return this.getSentinelValues().oneValue;
            }
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values.get(index);
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public boolean getIfAbsentPut(float key, BooleanFunction0 function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                boolean value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.getSentinelValues().containsZeroKey)
            {
                return this.getSentinelValues().zeroValue;
            }
            boolean value = function.value();
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                boolean value = function.value();
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                return this.getSentinelValues().oneValue;
            }
            boolean value = function.value();
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values.get(index);
        }
        boolean value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> boolean getIfAbsentPutWith(float key, BooleanFunction<? super P> function, P parameter)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                boolean value = function.booleanValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.getSentinelValues().containsZeroKey)
            {
                return this.getSentinelValues().zeroValue;
            }
            boolean value = function.booleanValueOf(parameter);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                boolean value = function.booleanValueOf(parameter);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                return this.getSentinelValues().oneValue;
            }
            boolean value = function.booleanValueOf(parameter);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values.get(index);
        }
        boolean value = function.booleanValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public boolean getIfAbsentPutWithKey(float key, FloatToBooleanFunction function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                boolean value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(value);
                return value;
            }
            if (this.getSentinelValues().containsZeroKey)
            {
                return this.getSentinelValues().zeroValue;
            }
            boolean value = function.valueOf(key);
            this.addEmptyKeyValue(value);
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                boolean value = function.valueOf(key);
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(value);
                return value;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                return this.getSentinelValues().oneValue;
            }
            boolean value = function.valueOf(key);
            this.addRemovedKeyValue(value);
            return value;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values.get(index);
        }
        boolean value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public boolean updateValue(float key, boolean initialValueIfAbsent, BooleanToBooleanFunction function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
                this.addEmptyKeyValue(function.valueOf(initialValueIfAbsent));
            }
            else if (this.getSentinelValues().containsZeroKey)
            {
                this.getSentinelValues().zeroValue = function.valueOf(this.getSentinelValues().zeroValue);
            }
            else
            {
                this.addEmptyKeyValue(function.valueOf(initialValueIfAbsent));
            }
            return this.getSentinelValues().zeroValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues();
                this.addRemovedKeyValue(function.valueOf(initialValueIfAbsent));
            }
            else if (this.getSentinelValues().containsOneKey)
            {
                this.getSentinelValues().oneValue = function.valueOf(this.getSentinelValues().oneValue);
            }
            else
            {
                this.addRemovedKeyValue(function.valueOf(initialValueIfAbsent));
            }
            return this.getSentinelValues().oneValue;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            this.values.set(index, function.valueOf(this.values.get(index)));
            return this.values.get(index);
        }
        boolean value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(float key, boolean value, int index)
    {
        if (Float.compare(this.keys[index], REMOVED_KEY) == 0)
        {
            this.occupiedWithSentinels--;
        }
        this.keys[index] = key;
        this.values.set(index, value);
        this.occupiedWithData++;
        if (this.occupiedWithData > this.maxOccupiedWithData())
        {
            this.rehashAndGrow();
        }
    }

    @Override
    public void removeKey(float key)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.getSentinelValues().containsZeroKey)
            {
                return;
            }
            this.removeEmptyKey();
            return;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.getSentinelValues().containsOneKey)
            {
                return;
            }
            this.removeRemovedKey();
            return;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            this.keys[index] = REMOVED_KEY;
            this.values.set(index, this.getEmptyValue());
            this.occupiedWithData--;
            this.occupiedWithSentinels++;
            if (this.occupiedWithSentinels > this.maxOccupiedWithSentinels())
            {
                this.rehash();
            }
        }
    }

    @Override
    public void remove(float key)
    {
        this.removeKey(key);
    }

    @Override
    public boolean removeKeyIfAbsent(float key, boolean value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.getSentinelValues().containsZeroKey)
            {
                return value;
            }
            boolean oldValue = this.getSentinelValues().zeroValue;
            this.removeEmptyKey();
            return oldValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.getSentinelValues().containsOneKey)
            {
                return value;
            }
            boolean oldValue = this.getSentinelValues().oneValue;
            this.removeRemovedKey();
            return oldValue;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            this.keys[index] = REMOVED_KEY;
            boolean oldValue = this.values.get(index);
            this.values.set(index, this.getEmptyValue());
            this.occupiedWithData--;
            this.occupiedWithSentinels++;
            if (this.occupiedWithSentinels > this.maxOccupiedWithSentinels())
            {
                this.rehash();
            }

            return oldValue;
        }
        return value;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof FloatBooleanMap))
        {
            return false;
        }

        FloatBooleanMap other = (FloatBooleanMap) obj;

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
            if (this.getSentinelValues().containsZeroKey && (!other.containsKey(EMPTY_KEY) || this.getSentinelValues().zeroValue != other.getOrThrow(EMPTY_KEY)))
            {
                return false;
            }

            if (this.getSentinelValues().containsOneKey && (!other.containsKey(REMOVED_KEY) || this.getSentinelValues().oneValue != other.getOrThrow(REMOVED_KEY)))
            {
                return false;
            }
        }

        for (int i = 0; i < this.keys.length; i++)
        {
            if (this.isNonSentinelAtIndex(i) && (!other.containsKey(this.keys[i]) || this.getValueAtIndex(i) != other.getOrThrow(this.keys[i])))
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
            if (this.getSentinelValues().containsZeroKey)
            {
                result += Float.floatToIntBits(EMPTY_KEY) ^ (this.getSentinelValues().zeroValue ? 1231 : 1237);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result += Float.floatToIntBits(REMOVED_KEY) ^ (this.getSentinelValues().oneValue ? 1231 : 1237);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result += Float.floatToIntBits(this.keys[i]) ^ (this.getValueAtIndex(i) ? 1231 : 1237);
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
            if (this.getSentinelValues().containsZeroKey)
            {
                appendable.append(EMPTY_KEY).append("=").append(this.getSentinelValues().zeroValue);
                first = false;
            }
            if (this.getSentinelValues().containsOneKey)
            {
                if (!first)
                {
                    appendable.append(", ");
                }
                appendable.append(REMOVED_KEY).append("=").append(this.getSentinelValues().oneValue);
                first = false;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (this.isNonSentinelAtIndex(i))
            {
                if (!first)
                {
                    appendable.append(", ");
                }
                appendable.append(this.keys[i]).append("=").append(this.getValueAtIndex(i));
                first = false;
            }
        }
        appendable.append("}");

        return appendable.toString();
    }

    @Override
    public MutableBooleanIterator booleanIterator()
    {
        return new InternalBooleanIterator();
    }

    @Override
    public void forEachKey(FloatProcedure procedure)
    {
        if (this.sentinelValues != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                procedure.value(EMPTY_KEY);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                procedure.value(REMOVED_KEY);
            }
        }
        for (float key : this.keys)
        {
            if (isNonSentinel(key))
            {
                procedure.value(key);
            }
        }
    }

    @Override
    public void forEachKeyValue(FloatBooleanProcedure procedure)
    {
        if (this.sentinelValues != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                procedure.value(EMPTY_KEY, this.getSentinelValues().zeroValue);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                procedure.value(REMOVED_KEY, this.getSentinelValues().oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                procedure.value(this.keys[i], this.getValueAtIndex(i));
            }
        }
    }

    @Override
    public FloatBooleanHashMap select(FloatBooleanPredicate predicate)
    {
        FloatBooleanHashMap result = new FloatBooleanHashMap();

        if (this.sentinelValues != null)
        {
            if (this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY, this.getSentinelValues().zeroValue))
            {
                result.put(EMPTY_KEY, this.getSentinelValues().zeroValue);
            }
            if (this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY, this.getSentinelValues().oneValue))
            {
                result.put(REMOVED_KEY, this.getSentinelValues().oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.keys[i], this.getValueAtIndex(i)))
            {
                result.put(this.keys[i], this.getValueAtIndex(i));
            }
        }

        return result;
    }

    @Override
    public FloatBooleanHashMap reject(FloatBooleanPredicate predicate)
    {
        FloatBooleanHashMap result = new FloatBooleanHashMap();

        if (this.sentinelValues != null)
        {
            if (this.getSentinelValues().containsZeroKey && !predicate.accept(EMPTY_KEY, this.getSentinelValues().zeroValue))
            {
                result.put(EMPTY_KEY, this.getSentinelValues().zeroValue);
            }
            if (this.getSentinelValues().containsOneKey && !predicate.accept(REMOVED_KEY, this.getSentinelValues().oneValue))
            {
                result.put(REMOVED_KEY, this.getSentinelValues().oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.keys[i], this.getValueAtIndex(i)))
            {
                result.put(this.keys[i], this.getValueAtIndex(i));
            }
        }
        return result;
    }

    @Override
    public LazyFloatIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<FloatBooleanPair> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        /**
         * @deprecated in 5.1.0.
         */
        out.writeFloat(DEFAULT_LOAD_FACTOR);
        if (this.sentinelValues != null)
        {
            if (this.getSentinelValues().containsZeroKey)
            {
                out.writeFloat(EMPTY_KEY);
                out.writeBoolean(this.getSentinelValues().zeroValue);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                out.writeFloat(REMOVED_KEY);
                out.writeBoolean(this.getSentinelValues().oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeFloat(this.keys[i]);
                out.writeBoolean(this.getValueAtIndex(i));
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        /**
         * @deprecated in 5.1.0.
         */
        in.readFloat();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readFloat(), in.readBoolean());
        }
    }

    private int maxOccupiedWithData()
    {
        int capacity = this.keys.length;
        // need at least one free slot for open addressing
        return Math.min(capacity - 1, capacity / OCCUPIED_DATA_RATIO);
    }

    private int maxOccupiedWithSentinels()
    {
        return this.keys.length / OCCUPIED_SENTINEL_RATIO;
    }

    private class InternalBooleanIterator implements MutableBooleanIterator
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
            return this.count < FloatBooleanHashMap.this.size();
        }

        @Override
        public boolean next()
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
                if (FloatBooleanHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return FloatBooleanHashMap.this.getSentinelValues().zeroValue;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (FloatBooleanHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return FloatBooleanHashMap.this.getSentinelValues().oneValue;
                }
            }

            float[] keys = FloatBooleanHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.lastKey = keys[this.position];
            boolean result = FloatBooleanHashMap.this.values.get(this.position);
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
            FloatBooleanHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    private class KeysView extends AbstractLazyFloatIterable
    {
        @Override
        public boolean isEmpty()
        {
            return FloatBooleanHashMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return FloatBooleanHashMap.this.notEmpty();
        }

        @Override
        public int size()
        {
            return FloatBooleanHashMap.this.size();
        }

        @Override
        public boolean contains(float key)
        {
            return FloatBooleanHashMap.this.containsKey(key);
        }

        @Override
        public boolean containsAll(float... keys)
        {
            for (float key : keys)
            {
                if (!FloatBooleanHashMap.this.containsKey(key))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean containsAll(FloatIterable source)
        {
            return source.allSatisfy(FloatBooleanHashMap.this::containsKey);
        }

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
            FloatBooleanHashMap.this.forEachKey(procedure);
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            try
            {
                appendable.append(start);
                boolean first = true;
                if (FloatBooleanHashMap.this.sentinelValues != null)
                {
                    if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey)
                    {
                        appendable.append(String.valueOf(EMPTY_KEY));
                        first = false;
                    }
                    if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey)
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(REMOVED_KEY));
                        first = false;
                    }
                }
                for (float key : FloatBooleanHashMap.this.keys)
                {
                    if (isNonSentinel(key))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(key));
                        first = false;
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
        public int count(FloatPredicate predicate)
        {
            int count = 0;
            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
                {
                    count++;
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
                {
                    count++;
                }
            }
            for (float key : FloatBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && predicate.accept(key))
                {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean anySatisfy(FloatPredicate predicate)
        {
            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
                {
                    return true;
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
                {
                    return true;
                }
            }
            for (float key : FloatBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && predicate.accept(key))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean allSatisfy(FloatPredicate predicate)
        {
            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey && !predicate.accept(EMPTY_KEY))
                {
                    return false;
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey && !predicate.accept(REMOVED_KEY))
                {
                    return false;
                }
            }
            for (float key : FloatBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && !predicate.accept(key))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public float detectIfNone(FloatPredicate predicate, float value)
        {
            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
                {
                    return EMPTY_KEY;
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
                {
                    return REMOVED_KEY;
                }
            }
            for (float key : FloatBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && predicate.accept(key))
                {
                    return key;
                }
            }
            return value;
        }

        @Override
        public double sum()
        {
            double result = 0.0;

            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    result += EMPTY_KEY;
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    result += REMOVED_KEY;
                }
            }
            for (float key : FloatBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key))
                {
                    result += key;
                }
            }

            return result;
        }

        @Override
        public float max()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            FloatIterator iterator = this.floatIterator();
            float max = iterator.next();
            while (iterator.hasNext())
            {
                float value = iterator.next();
                if (Float.compare(max, value) < 0)
                {
                    max = value;
                }
            }
            return max;
        }

        @Override
        public float min()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            FloatIterator iterator = this.floatIterator();
            float min = iterator.next();
            while (iterator.hasNext())
            {
                float value = iterator.next();
                if (Float.compare(value, min) < 0)
                {
                    min = value;
                }
            }
            return min;
        }

        @Override
        public float[] toSortedArray()
        {
            float[] array = this.toArray();
            Arrays.sort(array);
            return array;
        }

        @Override
        public float[] toArray()
        {
            int size = FloatBooleanHashMap.this.size();
            final float[] result = new float[size];
            FloatBooleanHashMap.this.forEachKey(new FloatProcedure()
            {
                private int index;

                @Override
                public void value(float each)
                {
                    result[this.index] = each;
                    this.index++;
                }
            });
            return result;
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
        {
            T result = injectedValue;
            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    result = function.valueOf(result, EMPTY_KEY);
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    result = function.valueOf(result, REMOVED_KEY);
                }
            }
            for (int i = 0; i < FloatBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatBooleanHashMap.this.keys[i]))
                {
                    result = function.valueOf(result, FloatBooleanHashMap.this.keys[i]);
                }
            }
            return result;
        }

        @Override
        public MutableFloatList toList()
        {
            return FloatArrayList.newList(this);
        }

        @Override
        public MutableFloatSet toSet()
        {
            return FloatHashSet.newSet(this);
        }

        @Override
        public MutableFloatBag toBag()
        {
            return FloatHashBag.newBag(this);
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
        protected float getKeyAtIndex(int index)
        {
            return FloatBooleanHashMap.this.keys[index];
        }

        @Override
        protected int getTableSize()
        {
            return FloatBooleanHashMap.this.keys.length;
        }

        @Override
        protected MutableFloatKeysMap getOuter()
        {
            return FloatBooleanHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return FloatBooleanHashMap.this.sentinelValues;
        }

        @Override
        public MutableFloatIterator floatIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(FloatIterable source)
        {
            int oldSize = FloatBooleanHashMap.this.size();
            final FloatSet sourceSet = source instanceof FloatSet ? (FloatSet) source : source.toSet();
            FloatBooleanHashMap retained = FloatBooleanHashMap.this.select((float key, boolean value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                FloatBooleanHashMap.this.keys = retained.keys;
                FloatBooleanHashMap.this.values = retained.values;
                FloatBooleanHashMap.this.occupiedWithData = retained.occupiedWithData;
                FloatBooleanHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                FloatBooleanHashMap.this.sentinelValues = retained.sentinelValues;
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
            throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".freeze() not implemented yet");
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
    public MutableBooleanCollection values()
    {
        return new ValuesCollection();
    }

    private class ValuesCollection extends AbstractBooleanValuesCollection
    {
        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            try
            {
                appendable.append(start);

                boolean first = true;

                if (FloatBooleanHashMap.this.sentinelValues != null)
                {
                    if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey)
                    {
                        appendable.append(String.valueOf(FloatBooleanHashMap.this.getSentinelValues().zeroValue));
                        first = false;
                    }
                    if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey)
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(FloatBooleanHashMap.this.getSentinelValues().oneValue));
                        first = false;
                    }
                }
                for (int i = 0; i < FloatBooleanHashMap.this.keys.length; i++)
                {
                    if (FloatBooleanHashMap.this.isNonSentinelAtIndex(i))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(FloatBooleanHashMap.this.getValueAtIndex(i)));
                        first = false;
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
        public MutableBooleanIterator booleanIterator()
        {
            return FloatBooleanHashMap.this.booleanIterator();
        }

        @Override
        public boolean remove(boolean item)
        {
            int oldSize = FloatBooleanHashMap.this.size();

            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey && item == FloatBooleanHashMap.this.getSentinelValues().zeroValue)
                {
                    FloatBooleanHashMap.this.removeKey(EMPTY_KEY);
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey && item == FloatBooleanHashMap.this.getSentinelValues().oneValue)
                {
                    FloatBooleanHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < FloatBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatBooleanHashMap.this.keys[i]) && item == FloatBooleanHashMap.this.getValueAtIndex(i))
                {
                    FloatBooleanHashMap.this.removeKey(FloatBooleanHashMap.this.keys[i]);
                }
            }
            return oldSize != FloatBooleanHashMap.this.size();
        }

        @Override
        public boolean retainAll(BooleanIterable source)
        {
            int oldSize = FloatBooleanHashMap.this.size();
            final BooleanSet sourceSet = source instanceof BooleanSet ? (BooleanSet) source : source.toSet();
            FloatBooleanHashMap retained = FloatBooleanHashMap.this.select((float key, boolean value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                FloatBooleanHashMap.this.keys = retained.keys;
                FloatBooleanHashMap.this.values = retained.values;
                FloatBooleanHashMap.this.occupiedWithData = retained.occupiedWithData;
                FloatBooleanHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                FloatBooleanHashMap.this.sentinelValues = retained.sentinelValues;
                return true;
            }
            return false;
        }
    }

    private class KeySetIterator implements MutableFloatIterator
    {
        private int count;
        private int position;
        private float lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean removed = true;

        @Override
        public boolean hasNext()
        {
            return this.count < FloatBooleanHashMap.this.size();
        }

        @Override
        public float next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException("next() called, but the iterator is exhausted");
            }
            this.count++;
            this.removed = false;

            if (!this.handledZero)
            {
                this.handledZero = true;
                if (FloatBooleanHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (FloatBooleanHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            float[] keys = FloatBooleanHashMap.this.keys;
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
            if (this.removed)
            {
                throw new IllegalStateException();
            }
            FloatBooleanHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.removed = true;
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<FloatBooleanPair>
    {
        @Override
        public void each(Procedure<? super FloatBooleanPair> procedure)
        {
            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, FloatBooleanHashMap.this.getSentinelValues().zeroValue));
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, FloatBooleanHashMap.this.getSentinelValues().oneValue));
                }
            }
            for (int i = 0; i < FloatBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatBooleanHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(FloatBooleanHashMap.this.keys[i], FloatBooleanHashMap.this.getValueAtIndex(i)));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super FloatBooleanPair> objectIntProcedure)
        {
            int index = 0;
            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, FloatBooleanHashMap.this.getSentinelValues().zeroValue), index);
                    index++;
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, FloatBooleanHashMap.this.getSentinelValues().oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < FloatBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatBooleanHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(FloatBooleanHashMap.this.keys[i], FloatBooleanHashMap.this.getValueAtIndex(i)), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super FloatBooleanPair, ? super P> procedure, P parameter)
        {
            if (FloatBooleanHashMap.this.sentinelValues != null)
            {
                if (FloatBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, FloatBooleanHashMap.this.getSentinelValues().zeroValue), parameter);
                }
                if (FloatBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, FloatBooleanHashMap.this.getSentinelValues().oneValue), parameter);
                }
            }
            for (int i = 0; i < FloatBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatBooleanHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(FloatBooleanHashMap.this.keys[i], FloatBooleanHashMap.this.getValueAtIndex(i)), parameter);
                }
            }
        }

        @Override
        public Iterator<FloatBooleanPair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<FloatBooleanPair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public FloatBooleanPair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (FloatBooleanHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, FloatBooleanHashMap.this.getSentinelValues().zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (FloatBooleanHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, FloatBooleanHashMap.this.getSentinelValues().oneValue);
                    }
                }

                float[] keys = FloatBooleanHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                FloatBooleanPair result = PrimitiveTuples.pair(keys[this.position], FloatBooleanHashMap.this.values.get(this.position));
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
                return this.count != FloatBooleanHashMap.this.size();
            }
        }
    }
}
