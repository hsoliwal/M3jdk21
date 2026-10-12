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
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction0;
import org.eclipse.collections.api.block.function.primitive.BooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteBooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ByteBooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ByteBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.iterator.MutableBooleanIterator;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.map.primitive.ByteBooleanMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteBooleanMap;
import org.eclipse.collections.api.map.primitive.MutableByteBooleanMap;
import org.eclipse.collections.api.set.primitive.BooleanSet;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.ByteBooleanPair;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.factory.primitive.ByteBooleanMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyByteIterable;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file primitiveBooleanHashMap.stg.
 *
 * @since 3.0.
 */
public class ByteBooleanHashMap extends AbstractMutableBooleanValuesMap implements MutableByteBooleanMap, MutableByteKeysMap, Externalizable
{
    static final boolean EMPTY_VALUE = false;
    private static final long serialVersionUID = 1L;
    private static final byte EMPTY_KEY = (byte) 0;
    private static final byte REMOVED_KEY = (byte) 1;

    /**
     * @deprecated in 5.1.0.
     */
    @Deprecated
    private static final float DEFAULT_LOAD_FACTOR = 0.5f;
    private static final int OCCUPIED_DATA_RATIO = 2;
    private static final int OCCUPIED_SENTINEL_RATIO = 4;
    private static final int DEFAULT_INITIAL_CAPACITY = 8;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 1;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private byte[] keys;
    private BitSet values;

    private int occupiedWithData;
    private int occupiedWithSentinels;
    private SentinelValues sentinelValues;

    public ByteBooleanHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ByteBooleanHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity * OCCUPIED_DATA_RATIO));
        this.allocateTable(capacity);
    }

    public ByteBooleanHashMap(ByteBooleanMap map)
    {
        this(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }

    /**
     * @deprecated in 5.1.0.
     */
    @Deprecated
    public ByteBooleanHashMap(int initialCapacity, float loadFactor)
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
    public MutableByteBooleanMap asUnmodifiable()
    {
        return new UnmodifiableByteBooleanMap(this);
    }

    @Override
    public MutableByteBooleanMap asSynchronized()
    {
        return new SynchronizedByteBooleanMap(this);
    }

    @Override
    public ImmutableByteBooleanMap toImmutable()
    {
        return ByteBooleanMaps.immutable.withAll(this);
    }

    public static ByteBooleanHashMap newWithKeysValues(byte key1, boolean value1)
    {
        return new ByteBooleanHashMap(1).withKeyValue(key1, value1);
    }

    public static ByteBooleanHashMap newWithKeysValues(byte key1, boolean value1, byte key2, boolean value2)
    {
        return new ByteBooleanHashMap(2).withKeysValues(key1, value1, key2, value2);
    }

    public static ByteBooleanHashMap newWithKeysValues(byte key1, boolean value1, byte key2, boolean value2, byte key3, boolean value3)
    {
        return new ByteBooleanHashMap(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static ByteBooleanHashMap newWithKeysValues(byte key1, boolean value1, byte key2, boolean value2, byte key3, boolean value3, byte key4, boolean value4)
    {
        return new ByteBooleanHashMap(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public ByteBooleanHashMap withKeyValue(byte key1, boolean value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ByteBooleanHashMap withKeysValues(byte key1, boolean value1, byte key2, boolean value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ByteBooleanHashMap withKeysValues(byte key1, boolean value1, byte key2, boolean value2, byte key3, boolean value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ByteBooleanHashMap withKeysValues(byte key1, boolean value1, byte key2, boolean value2, byte key3, boolean value3, byte key4, boolean value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ByteBooleanHashMap withoutKey(byte key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ByteBooleanHashMap withoutAllKeys(ByteIterable keys)
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

    protected void allocateTable(int sizeToAllocate)
    {
        this.keys = new byte[sizeToAllocate];
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
        byte[] old = this.keys;
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
    int probe(byte element)
    {
        int index = this.spreadAndMask(element);
        byte keyAtIndex = this.keys[index];

        if (keyAtIndex == element || keyAtIndex == EMPTY_KEY)
        {
            return index;
        }

        int removedIndex = keyAtIndex == REMOVED_KEY ? index : -1;
        int nextIndex = index;
        int probe = 17;

        // loop until an empty slot is reached
        while (true)
        {
            // Probe algorithm: 17*n*(n+1)/2 where n = number of collisions
            nextIndex += probe;
            probe += 17;
            nextIndex &= this.keys.length - 1;

            if (this.keys[nextIndex] == element)
            {
                return nextIndex;
            }
            if (this.keys[nextIndex] == REMOVED_KEY)
            {
                if (removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            else if (this.keys[nextIndex] == EMPTY_KEY)
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
    public void put(byte key, boolean value)
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
    public void putAll(ByteBooleanMap map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ByteBooleanToBooleanFunction function)
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
    public boolean containsKey(byte key)
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
    public boolean get(byte key)
    {
        return this.getIfAbsent(key, this.getEmptyValue());
    }

    @Override
    public boolean getIfAbsent(byte key, boolean ifAbsent)
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
    public boolean getOrThrow(byte key)
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
    public boolean getIfAbsentPut(byte key, boolean value)
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
    public boolean getIfAbsentPut(byte key, BooleanFunction0 function)
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
    public <P> boolean getIfAbsentPutWith(byte key, BooleanFunction<? super P> function, P parameter)
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
    public boolean getIfAbsentPutWithKey(byte key, ByteToBooleanFunction function)
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
    public boolean updateValue(byte key, boolean initialValueIfAbsent, BooleanToBooleanFunction function)
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

    private void addKeyValueAtIndex(byte key, boolean value, int index)
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
    public void removeKey(byte key)
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
    public void remove(byte key)
    {
        this.removeKey(key);
    }

    @Override
    public boolean removeKeyIfAbsent(byte key, boolean value)
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

        if (!(obj instanceof ByteBooleanMap))
        {
            return false;
        }

        ByteBooleanMap other = (ByteBooleanMap) obj;

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
    public void forEachKey(ByteProcedure procedure)
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
        for (byte key : this.keys)
        {
            if (isNonSentinel(key))
            {
                procedure.value(key);
            }
        }
    }

    @Override
    public void forEachKeyValue(ByteBooleanProcedure procedure)
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
    public ByteBooleanHashMap select(ByteBooleanPredicate predicate)
    {
        ByteBooleanHashMap result = new ByteBooleanHashMap();

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
    public ByteBooleanHashMap reject(ByteBooleanPredicate predicate)
    {
        ByteBooleanHashMap result = new ByteBooleanHashMap();

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
    public LazyByteIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ByteBooleanPair> keyValuesView()
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
                out.writeByte(EMPTY_KEY);
                out.writeBoolean(this.getSentinelValues().zeroValue);
            }
            if (this.getSentinelValues().containsOneKey)
            {
                out.writeByte(REMOVED_KEY);
                out.writeBoolean(this.getSentinelValues().oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeByte(this.keys[i]);
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
            this.put(in.readByte(), in.readBoolean());
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
        private byte lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count < ByteBooleanHashMap.this.size();
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
                if (ByteBooleanHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return ByteBooleanHashMap.this.getSentinelValues().zeroValue;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ByteBooleanHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return ByteBooleanHashMap.this.getSentinelValues().oneValue;
                }
            }

            byte[] keys = ByteBooleanHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.lastKey = keys[this.position];
            boolean result = ByteBooleanHashMap.this.values.get(this.position);
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
            ByteBooleanHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    private class KeysView extends AbstractLazyByteIterable
    {
        @Override
        public boolean isEmpty()
        {
            return ByteBooleanHashMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return ByteBooleanHashMap.this.notEmpty();
        }

        @Override
        public int size()
        {
            return ByteBooleanHashMap.this.size();
        }

        @Override
        public boolean contains(byte key)
        {
            return ByteBooleanHashMap.this.containsKey(key);
        }

        @Override
        public boolean containsAll(byte... keys)
        {
            for (byte key : keys)
            {
                if (!ByteBooleanHashMap.this.containsKey(key))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean containsAll(ByteIterable source)
        {
            return source.allSatisfy(ByteBooleanHashMap.this::containsKey);
        }

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
            ByteBooleanHashMap.this.forEachKey(procedure);
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            try
            {
                appendable.append(start);
                boolean first = true;
                if (ByteBooleanHashMap.this.sentinelValues != null)
                {
                    if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey)
                    {
                        appendable.append(String.valueOf(EMPTY_KEY));
                        first = false;
                    }
                    if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey)
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(REMOVED_KEY));
                        first = false;
                    }
                }
                for (byte key : ByteBooleanHashMap.this.keys)
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
        public int count(BytePredicate predicate)
        {
            int count = 0;
            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
                {
                    count++;
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
                {
                    count++;
                }
            }
            for (byte key : ByteBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && predicate.accept(key))
                {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean anySatisfy(BytePredicate predicate)
        {
            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
                {
                    return true;
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
                {
                    return true;
                }
            }
            for (byte key : ByteBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && predicate.accept(key))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean allSatisfy(BytePredicate predicate)
        {
            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey && !predicate.accept(EMPTY_KEY))
                {
                    return false;
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey && !predicate.accept(REMOVED_KEY))
                {
                    return false;
                }
            }
            for (byte key : ByteBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key) && !predicate.accept(key))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public byte detectIfNone(BytePredicate predicate, byte value)
        {
            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey && predicate.accept(EMPTY_KEY))
                {
                    return EMPTY_KEY;
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey && predicate.accept(REMOVED_KEY))
                {
                    return REMOVED_KEY;
                }
            }
            for (byte key : ByteBooleanHashMap.this.keys)
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

            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    result += EMPTY_KEY;
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    result += REMOVED_KEY;
                }
            }
            for (byte key : ByteBooleanHashMap.this.keys)
            {
                if (isNonSentinel(key))
                {
                    result += key;
                }
            }

            return result;
        }

        @Override
        public byte max()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            ByteIterator iterator = this.byteIterator();
            byte max = iterator.next();
            while (iterator.hasNext())
            {
                byte value = iterator.next();
                if (max < value)
                {
                    max = value;
                }
            }
            return max;
        }

        @Override
        public byte min()
        {
            if (this.isEmpty())
            {
                throw new NoSuchElementException();
            }
            ByteIterator iterator = this.byteIterator();
            byte min = iterator.next();
            while (iterator.hasNext())
            {
                byte value = iterator.next();
                if (value < min)
                {
                    min = value;
                }
            }
            return min;
        }

        @Override
        public byte[] toSortedArray()
        {
            byte[] array = this.toArray();
            Arrays.sort(array);
            return array;
        }

        @Override
        public byte[] toArray()
        {
            int size = ByteBooleanHashMap.this.size();
            final byte[] result = new byte[size];
            ByteBooleanHashMap.this.forEachKey(new ByteProcedure()
            {
                private int index;

                @Override
                public void value(byte each)
                {
                    result[this.index] = each;
                    this.index++;
                }
            });
            return result;
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
        {
            T result = injectedValue;
            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    result = function.valueOf(result, EMPTY_KEY);
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    result = function.valueOf(result, REMOVED_KEY);
                }
            }
            for (int i = 0; i < ByteBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ByteBooleanHashMap.this.keys[i]))
                {
                    result = function.valueOf(result, ByteBooleanHashMap.this.keys[i]);
                }
            }
            return result;
        }

        @Override
        public MutableByteList toList()
        {
            return ByteArrayList.newList(this);
        }

        @Override
        public MutableByteSet toSet()
        {
            return ByteHashSet.newSet(this);
        }

        @Override
        public MutableByteBag toBag()
        {
            return ByteHashBag.newBag(this);
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
        protected byte getKeyAtIndex(int index)
        {
            return ByteBooleanHashMap.this.keys[index];
        }

        @Override
        protected int getTableSize()
        {
            return ByteBooleanHashMap.this.keys.length;
        }

        @Override
        protected MutableByteKeysMap getOuter()
        {
            return ByteBooleanHashMap.this;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return ByteBooleanHashMap.this.sentinelValues;
        }

        @Override
        public MutableByteIterator byteIterator()
        {
            return new KeySetIterator();
        }

        @Override
        public boolean retainAll(ByteIterable source)
        {
            int oldSize = ByteBooleanHashMap.this.size();
            final ByteSet sourceSet = source instanceof ByteSet ? (ByteSet) source : source.toSet();
            ByteBooleanHashMap retained = ByteBooleanHashMap.this.select((byte key, boolean value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                ByteBooleanHashMap.this.keys = retained.keys;
                ByteBooleanHashMap.this.values = retained.values;
                ByteBooleanHashMap.this.occupiedWithData = retained.occupiedWithData;
                ByteBooleanHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                ByteBooleanHashMap.this.sentinelValues = retained.sentinelValues;
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
            throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".freeze() not implemented yet");
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

                if (ByteBooleanHashMap.this.sentinelValues != null)
                {
                    if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey)
                    {
                        appendable.append(String.valueOf(ByteBooleanHashMap.this.getSentinelValues().zeroValue));
                        first = false;
                    }
                    if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey)
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ByteBooleanHashMap.this.getSentinelValues().oneValue));
                        first = false;
                    }
                }
                for (int i = 0; i < ByteBooleanHashMap.this.keys.length; i++)
                {
                    if (ByteBooleanHashMap.this.isNonSentinelAtIndex(i))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ByteBooleanHashMap.this.getValueAtIndex(i)));
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
            return ByteBooleanHashMap.this.booleanIterator();
        }

        @Override
        public boolean remove(boolean item)
        {
            int oldSize = ByteBooleanHashMap.this.size();

            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey && item == ByteBooleanHashMap.this.getSentinelValues().zeroValue)
                {
                    ByteBooleanHashMap.this.removeKey(EMPTY_KEY);
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey && item == ByteBooleanHashMap.this.getSentinelValues().oneValue)
                {
                    ByteBooleanHashMap.this.removeKey(REMOVED_KEY);
                }
            }
            for (int i = 0; i < ByteBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ByteBooleanHashMap.this.keys[i]) && item == ByteBooleanHashMap.this.getValueAtIndex(i))
                {
                    ByteBooleanHashMap.this.removeKey(ByteBooleanHashMap.this.keys[i]);
                }
            }
            return oldSize != ByteBooleanHashMap.this.size();
        }

        @Override
        public boolean retainAll(BooleanIterable source)
        {
            int oldSize = ByteBooleanHashMap.this.size();
            final BooleanSet sourceSet = source instanceof BooleanSet ? (BooleanSet) source : source.toSet();
            ByteBooleanHashMap retained = ByteBooleanHashMap.this.select((byte key, boolean value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ByteBooleanHashMap.this.keys = retained.keys;
                ByteBooleanHashMap.this.values = retained.values;
                ByteBooleanHashMap.this.occupiedWithData = retained.occupiedWithData;
                ByteBooleanHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                ByteBooleanHashMap.this.sentinelValues = retained.sentinelValues;
                return true;
            }
            return false;
        }
    }

    private class KeySetIterator implements MutableByteIterator
    {
        private int count;
        private int position;
        private byte lastKey;
        private boolean handledZero;
        private boolean handledOne;
        private boolean removed = true;

        @Override
        public boolean hasNext()
        {
            return this.count < ByteBooleanHashMap.this.size();
        }

        @Override
        public byte next()
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
                if (ByteBooleanHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOne)
            {
                this.handledOne = true;
                if (ByteBooleanHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }

            byte[] keys = ByteBooleanHashMap.this.keys;
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
            ByteBooleanHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.removed = true;
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<ByteBooleanPair>
    {
        @Override
        public void each(Procedure<? super ByteBooleanPair> procedure)
        {
            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, ByteBooleanHashMap.this.getSentinelValues().zeroValue));
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, ByteBooleanHashMap.this.getSentinelValues().oneValue));
                }
            }
            for (int i = 0; i < ByteBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ByteBooleanHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ByteBooleanHashMap.this.keys[i], ByteBooleanHashMap.this.getValueAtIndex(i)));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ByteBooleanPair> objectIntProcedure)
        {
            int index = 0;
            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, ByteBooleanHashMap.this.getSentinelValues().zeroValue), index);
                    index++;
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, ByteBooleanHashMap.this.getSentinelValues().oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < ByteBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ByteBooleanHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ByteBooleanHashMap.this.keys[i], ByteBooleanHashMap.this.getValueAtIndex(i)), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ByteBooleanPair, ? super P> procedure, P parameter)
        {
            if (ByteBooleanHashMap.this.sentinelValues != null)
            {
                if (ByteBooleanHashMap.this.getSentinelValues().containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, ByteBooleanHashMap.this.getSentinelValues().zeroValue), parameter);
                }
                if (ByteBooleanHashMap.this.getSentinelValues().containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, ByteBooleanHashMap.this.getSentinelValues().oneValue), parameter);
                }
            }
            for (int i = 0; i < ByteBooleanHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ByteBooleanHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ByteBooleanHashMap.this.keys[i], ByteBooleanHashMap.this.getValueAtIndex(i)), parameter);
                }
            }
        }

        @Override
        public Iterator<ByteBooleanPair> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ByteBooleanPair>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public ByteBooleanPair next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (ByteBooleanHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, ByteBooleanHashMap.this.getSentinelValues().zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (ByteBooleanHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, ByteBooleanHashMap.this.getSentinelValues().oneValue);
                    }
                }

                byte[] keys = ByteBooleanHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                ByteBooleanPair result = PrimitiveTuples.pair(keys[this.position], ByteBooleanHashMap.this.values.get(this.position));
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
                return this.count != ByteBooleanHashMap.this.size();
            }
        }
    }
}
