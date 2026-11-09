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
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction0;
import org.eclipse.collections.api.block.function.primitive.BooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ShortBooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortBooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ShortBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.iterator.MutableBooleanIterator;
import org.eclipse.collections.api.iterator.MutableShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.map.primitive.ShortBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortBooleanMap;
import org.eclipse.collections.api.map.primitive.MutableShortBooleanMap;
import org.eclipse.collections.api.set.primitive.BooleanSet;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.tuple.primitive.ShortBooleanPair;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.impl.bag.mutable.primitive.ShortHashBag;
import org.eclipse.collections.impl.factory.primitive.ShortBooleanMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableShortIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyShortIterable;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file primitiveBooleanHashMap.stg.
 *
 * @since 3.0.
 */
public class ShortBooleanHashMap extends AbstractMutableBooleanValuesMap implements MutableShortBooleanMap, MutableShortKeysMap, Externalizable
{
    static final boolean EMPTY_VALUE = false;
    private static final long serialVersionUID = 1L;
    private static final short EMPTY_KEY = (short) 0;
    private static final short REMOVED_KEY = (short) 1;

    /**
     * @deprecated in 5.1.0.
     */
    @Deprecated
    private static final float DEFAULT_LOAD_FACTOR = 0.5f;
    private static final int OCCUPIED_DATA_RATIO = 2;
    private static final int OCCUPIED_SENTINEL_RATIO = 4;
    private static final int DEFAULT_INITIAL_CAPACITY = 8;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 2;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private short[] keys;
    private BitSet values;

    private int occupiedWithData;
    private int occupiedWithSentinels;
    private SentinelValues sentinelValues;

    public ShortBooleanHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ShortBooleanHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity * OCCUPIED_DATA_RATIO));
        this.allocateTable(capacity);
    }

    public ShortBooleanHashMap(ShortBooleanMap map)
    {
        this(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }

    /**
     * @deprecated in 5.1.0.
     */
    @Deprecated
    public ShortBooleanHashMap(int initialCapacity, float loadFactor)
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
    public MutableShortBooleanMap asUnmodifiable()
    {
        return new UnmodifiableShortBooleanMap(this);
    }

    @Override
    public MutableShortBooleanMap asSynchronized()
    {
        return new SynchronizedShortBooleanMap(this);
    }

    @Override
    public ImmutableShortBooleanMap toImmutable()
    {
        return ShortBooleanMaps.immutable.withAll(this);
    }

    public static ShortBooleanHashMap newWithKeysValues(short key1, boolean value1)
    {
        return new ShortBooleanHashMap(1).withKeyValue(key1, value1);
    }

    public static ShortBooleanHashMap newWithKeysValues(short key1, boolean value1, short key2, boolean value2)
    {
        return new ShortBooleanHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static ShortBooleanHashMap newWithKeysValues(short key1, boolean value1, short key2, boolean value2, short key3, boolean value3)
    {
        return new ShortBooleanHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static ShortBooleanHashMap newWithKeysValues(short key1, boolean value1, short key2, boolean value2, short key3, boolean value3, short key4, boolean value4)
    {
        return new ShortBooleanHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public ShortBooleanHashMap withKeyValue(short key1, boolean value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ShortBooleanHashMap withKeysValues(short key1, boolean value1, short key2, boolean value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ShortBooleanHashMap withKeysValues(short key1, boolean value1, short key2, boolean value2, short key3, boolean value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ShortBooleanHashMap withKeysValues(short key1, boolean value1, short key2, boolean value2, short key3, boolean value3, short key4, boolean value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ShortBooleanHashMap withoutKey(short key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ShortBooleanHashMap withoutAllKeys(ShortIterable keys)
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

    protected void allocateTable(int sizeToAllocate)
    {
        this.keys = new short[sizeToAllocate];
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
        short[] old = this.keys;
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
    int probe(short element)
    {
        int index = this.spreadAndMask(element);
        short keyAtIndex = this.keys[index];

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

    int probeTwo(short element, int removedIndex)
    {
        int index = this.spreadTwoAndMask(element);
        for (int i = 0; i < INITIAL_LINEAR_PROBE; i++)
        {
            int nextIndex = (index + i) & (this.keys.length - 1);
            short keyAtIndex = this.keys[nextIndex];
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
        int nextIndex = Integer.reverse(SpreadFunctions.shortSpreadOne(element));
        int spreadTwo = Integer.reverse(SpreadFunctions.shortSpreadTwo(element)) | 1;

        while (true)
        {
            nextIndex = this.mask(nextIndex + spreadTwo);
            short keyAtIndex = this.keys[nextIndex];
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
    public void put(short key, boolean value)
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

        if (this.keys[index] == key)
        {
            // key already present in map
            this.values.set(index, value);
            return;
        }

        this.addKeyValueAtIndex(key, value, index);
    }

    @Override
    public void putAll(ShortBooleanMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ShortBooleanToBooleanFunction function)
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
    public boolean containsKey(short key)
    {
        if (isEmptyKey(key))
        {
            return this.getSentinelValues() != null && this.getSentinelValues().containsZeroKey;
        }
        if (isRemovedKey(key))
        {
            return this.getSentinelValues() != null && this.getSentinelValues().containsOneKey;
        }
        return this.keys[this.probe(key)] == key;
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
    public boolean get(short key)
    {
        return this.getIfAbsent(key, this.getEmptyValue());
    }

    @Override
    public boolean getIfAbsent(short key, boolean ifAbsent)
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
    public boolean getOrThrow(short key)
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
    public boolean getIfAbsentPut(short key, boolean value)
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
        if (this.keys[index] == key)
        {
            return this.values.get(index);
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public boolean getIfAbsentPut(short key, BooleanFunction0 function)
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
        if (this.keys[index] == key)
        {
            return this.values.get(index);
        }
        boolean value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> boolean getIfAbsentPutWith(short key, BooleanFunction<? super P> function, P parameter)
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
        if (this.keys[index] == key)
        {
            return this.values.get(index);
        }
        boolean value = function.booleanValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public boolean getIfAbsentPutWithKey(short key, ShortToBooleanFunction function)
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
        if (this.keys[index] == key)
        {
            return this.values.get(index);
        }
        boolean value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public boolean updateValue(short key, boolean initialValueIfAbsent, BooleanToBooleanFunction function)
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
        if (this.keys[index] == key)
        {
            this.values.set(index, function.valueOf(this.values.get(index)));
            return this.values.get(index);
        }
        boolean value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(short key, boolean value, int index)
    {
        if (this.keys[index] == REMOVED_KEY)
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
    public void removeKey(short key)
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
        if (this.keys[index] == key)
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
    public void remove(short key)
    {
        this.removeKey(key);
    }

    @Override
    public boolean removeKeyIfAbsent(short key, boolean value)
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
        if (this.keys[index] == key)
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

        if (!(obj instanceof ShortBooleanMap))
        {
            return false;
        }

        ShortBooleanMap other = (ShortBooleanMap) obj;

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
                result += (int) EMPTY_KEY ^ (this.getSentinelValues().zeroValue ? 1231 : 1237);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                result += (int) REMOVED_KEY ^ (this.getSentinelValues().oneValue ? 1231 : 1237);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result += (int) this.keys[i] ^ (this.getValueAtIndex(i) ? 1231 : 1237);
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
    public void forEachKey(ShortProcedure procedure)
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
        for (short key : this.keys)
        {
            if (isNonSentinel(key))
            {
                procedure.value(key);
            }
        }
    }

    @Override
    public void forEachKeyValue(ShortBooleanProcedure procedure)
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
    public ShortBooleanHashMap select(ShortBooleanPredicate predicate)
    {
        ShortBooleanHashMap result = new ShortBooleanHashMap();

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
    public ShortBooleanHashMap reject(ShortBooleanPredicate predicate)
    {
        ShortBooleanHashMap result = new ShortBooleanHashMap();

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
    public LazyShortIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ShortBooleanPair> keyValuesView()
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
                out.writeShort(EMPTY_KEY);
                out.writeBoolean(this.getSentinelValues().zeroValue);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                out.writeShort(REMOVED_KEY);
                out.writeBoolean(this.getSentinelValues().oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeShort(this.keys[i]);
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
            this.put(in.readShort(), in.readBoolean());
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
        private short lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < ShortBooleanHashMap.this.size();
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
                if (ShortBooleanHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return ShortBooleanHashMap.this.getSentinelValues().zeroValue;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ShortBooleanHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return ShortBooleanHashMap.this.getSentinelValues().oneValue;
                }
            }

            short[] keys = ShortBooleanHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.lastKey = keys[this.position];
            boolean result = ShortBooleanHashMap.this.values.get(this.position);
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
            ShortBooleanHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    private class KeysView extends AbstractLazyShortIterable
    {
        @Override
        public boolean isEmpty()
        {
            return ShortBooleanHashMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return ShortBooleanHashMap.this.notEmpty();
        }

        @Override
        public int size()
        {
            return ShortBooleanHashMap.this.size();
        }

        @Override
        public boolean contains(short key)
        {
            return ShortBooleanHashMap.this.containsKey(key);
        }

        @Override
        public boolean containsAll(short... keys)
        {
            for (short key : keys)
            {
                if (!ShortBooleanHashMap.this.containsKey(key))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean containsAll(ShortIterable source)
        {
            return source.allSatisfy(ShortBooleanHashMap.this::containsKey);
        }

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
            ShortBooleanHashMap.this.forEachKey(procedure);
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            try
            {
                appendable.append(start);
                boolean first = true;
                if (ShortBooleanHashMap.this.sentinelValues != null)
                {
                    if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey)
                    {
                        appendable.append(String.valueOf(EMPTY_KEY));
                        first = false;
                    }
                    if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey)
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(REMOVED_KEY));
                        first = false;
                    }
                }
                for (short key : ShortBooleanHashMap.this.keys)
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
        public int count(ShortPredicate predicate)
        {
            int count = 0;
            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
                {
                    count++;
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
                {
                    count++;
                }
            }
            for (short key : ShortBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && predicate.accept(key))
                {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean anySatisfy(ShortPredicate predicate)
        {
            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
                {
                    return true;
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
                {
                    return true;
                }
            }
            for (short key : ShortBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && predicate.accept(key))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean allSatisfy(ShortPredicate predicate)
        {
            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey && !predicate.accept(EMPTY_KEY))
                {
                    return false;
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey && !predicate.accept(REMOVED_KEY))
                {
                    return false;
                }
            }
            for (short key : ShortBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && !predicate.accept(key))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public short detectIfNone(ShortPredicate predicate, short value)
        {
            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
                {
                    return EMPTY_KEY;
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
                {
                    return REMOVED_KEY;
                }
            }
            for (short key : ShortBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && predicate.accept(key))
                {
                    return key;
                }
            }
            return value;
        }

        @Override
        public long sum()
        {
            long result = 0L;

            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    result += EMPTY_KEY;
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    result += REMOVED_KEY;
                }
            }
            for (short key : ShortBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key))
                {
                    result += key;
                }
            }

            return result;
        }

        @Override
        public short max()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            ShortIterator iterator = this.shortIterator();
            short max = iterator.next();
            while (iterator.hasNext())
            {
                short value = iterator.next();
                if (max < value)
                {
                    max = value;
                }
            }
            return max;
        }

        @Override
        public short min()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            ShortIterator iterator = this.shortIterator();
            short min = iterator.next();
            while (iterator.hasNext())
            {
                short value = iterator.next();
                if (value < min)
                {
                    min = value;
                }
            }
            return min;
        }

        @Override
        public short[] toSortedArray()
        {
            short[] array = this.toArray();
            Arrays.sort(array);
            return array;
        }

        @Override
        public short[] toArray()
        {
            int size = ShortBooleanHashMap.this.size();
            final short[] result = new short[size];
            ShortBooleanHashMap.this.forEachKey(new ShortProcedure()
            {
                private int index;

                @Override
                public void value(short each)
                {
                    result[this.index] = each;
                    this.index++;
                }
            });
            return result;
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
        {
            T result = injectedValue;
            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    result = function.valueOf(result, EMPTY_KEY);
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    result = function.valueOf(result, REMOVED_KEY);
                }
            }
            for (int i = 0; i < ShortBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ShortBooleanHashMap.this.keys[i]))
                {
                    result = function.valueOf(result, ShortBooleanHashMap.this.keys[i]);
                }
            }
            return result;
        }

        @Override
        public MutableShortList toList()
        {
            return ShortArrayList.newList(this);
        }

        @Override
        public MutableShortSet toSet()
        {
            return ShortHashSet.newSet(this);
        }

        @Override
        public MutableShortBag toBag()
        {
            return ShortHashBag.newBag(this);
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
        protected short getKeyAtIndex(int index)
        {
            return ShortBooleanHashMap.this.keys[index];
        }

        @Override
        protected int getTableSize()
        {
            return ShortBooleanHashMap.this.keys.length;
        }

        @Override
        protected MutableShortKeysMap getOuter()
        {
            return ShortBooleanHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return ShortBooleanHashMap.this.sentinelValues;
        }

        @Override
        public MutableShortIterator shortIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(ShortIterable source)
        {
            int oldSize = ShortBooleanHashMap.this.size();
            final ShortSet sourceSet = source instanceof ShortSet ? (ShortSet) source : source.toSet();
            ShortBooleanHashMap retained = ShortBooleanHashMap.this.select((short key, boolean value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                ShortBooleanHashMap.this.keys = retained.keys;
                ShortBooleanHashMap.this.values = retained.values;
                ShortBooleanHashMap.this.occupiedWithData = retained.occupiedWithData;
                ShortBooleanHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                ShortBooleanHashMap.this.sentinelValues = retained.sentinelValues;
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
            throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".freeze() not implemented yet");
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

                if (ShortBooleanHashMap.this.sentinelValues != null)
                {
                    if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey)
                    {
                        appendable.append(String.valueOf(ShortBooleanHashMap.this.getSentinelValues().zeroValue));
                        first = false;
                    }
                    if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey)
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ShortBooleanHashMap.this.getSentinelValues().oneValue));
                        first = false;
                    }
                }
                for (int i = 0; i < ShortBooleanHashMap.this.keys.length; i++)
                {
                    if (ShortBooleanHashMap.this.isNonSentinelAtIndex(i))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ShortBooleanHashMap.this.getValueAtIndex(i)));
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
            return ShortBooleanHashMap.this.booleanIterator();
        }

        @Override
        public boolean remove(boolean item)
        {
            int oldSize = ShortBooleanHashMap.this.size();

            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey && item == ShortBooleanHashMap.this.getSentinelValues().zeroValue)
                {
                    ShortBooleanHashMap.this.removeKey(EMPTY_KEY);
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey && item == ShortBooleanHashMap.this.getSentinelValues().oneValue)
                {
                    ShortBooleanHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < ShortBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ShortBooleanHashMap.this.keys[i]) && item == ShortBooleanHashMap.this.getValueAtIndex(i))
                {
                    ShortBooleanHashMap.this.removeKey(ShortBooleanHashMap.this.keys[i]);
                }
            }
            return oldSize != ShortBooleanHashMap.this.size();
        }

        @Override
        public boolean retainAll(BooleanIterable source)
        {
            int oldSize = ShortBooleanHashMap.this.size();
            final BooleanSet sourceSet = source instanceof BooleanSet ? (BooleanSet) source : source.toSet();
            ShortBooleanHashMap retained = ShortBooleanHashMap.this.select((short key, boolean value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ShortBooleanHashMap.this.keys = retained.keys;
                ShortBooleanHashMap.this.values = retained.values;
                ShortBooleanHashMap.this.occupiedWithData = retained.occupiedWithData;
                ShortBooleanHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                ShortBooleanHashMap.this.sentinelValues = retained.sentinelValues;
                return true;
            }
            return false;
        }
    }

    private class KeySetIterator implements MutableShortIterator
    {
        private int count;
        private int position;
        private short lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean removed = true;

        @Override
        public boolean hasNext()
        {
            return this.count < ShortBooleanHashMap.this.size();
        }

        @Override
        public short next()
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
                if (ShortBooleanHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ShortBooleanHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            short[] keys = ShortBooleanHashMap.this.keys;
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
            ShortBooleanHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.removed = true;
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<ShortBooleanPair>
    {
        @Override
        public void each(Procedure<? super ShortBooleanPair> procedure)
        {
            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, ShortBooleanHashMap.this.getSentinelValues().zeroValue));
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, ShortBooleanHashMap.this.getSentinelValues().oneValue));
                }
            }
            for (int i = 0; i < ShortBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ShortBooleanHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ShortBooleanHashMap.this.keys[i], ShortBooleanHashMap.this.getValueAtIndex(i)));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ShortBooleanPair> objectIntProcedure)
        {
            int index = 0;
            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, ShortBooleanHashMap.this.getSentinelValues().zeroValue), index);
                    index++;
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, ShortBooleanHashMap.this.getSentinelValues().oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < ShortBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ShortBooleanHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ShortBooleanHashMap.this.keys[i], ShortBooleanHashMap.this.getValueAtIndex(i)), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ShortBooleanPair, ? super P> procedure, P parameter)
        {
            if (ShortBooleanHashMap.this.sentinelValues != null)
            {
                if (ShortBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, ShortBooleanHashMap.this.getSentinelValues().zeroValue), parameter);
                }
                if (ShortBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, ShortBooleanHashMap.this.getSentinelValues().oneValue), parameter);
                }
            }
            for (int i = 0; i < ShortBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ShortBooleanHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ShortBooleanHashMap.this.keys[i], ShortBooleanHashMap.this.getValueAtIndex(i)), parameter);
                }
            }
        }

        @Override
        public Iterator<ShortBooleanPair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ShortBooleanPair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public ShortBooleanPair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (ShortBooleanHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, ShortBooleanHashMap.this.getSentinelValues().zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (ShortBooleanHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, ShortBooleanHashMap.this.getSentinelValues().oneValue);
                    }
                }

                short[] keys = ShortBooleanHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                ShortBooleanPair result = PrimitiveTuples.pair(keys[this.position], ShortBooleanHashMap.this.values.get(this.position));
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
                return this.count != ShortBooleanHashMap.this.size();
            }
        }
    }
}
