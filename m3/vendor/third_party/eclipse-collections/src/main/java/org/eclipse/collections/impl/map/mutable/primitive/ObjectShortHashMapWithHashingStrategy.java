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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction0;
import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToShortFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectShortPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectShortProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableShortCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.ShortBags;
import org.eclipse.collections.api.factory.primitive.ShortLists;
import org.eclipse.collections.api.factory.primitive.ShortSets;
import org.eclipse.collections.api.factory.primitive.ObjectShortMaps;
import org.eclipse.collections.api.factory.primitive.ShortObjectMaps;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.iterator.MutableShortIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectShortMap;
import org.eclipse.collections.api.map.primitive.MutableObjectShortMap;
import org.eclipse.collections.api.map.primitive.MutableShortObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectShortMap;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.tuple.primitive.ObjectShortPair;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedShortCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file objectPrimitiveHashMapWithHashingStrategy.stg.
 *
 * @since 7.0.
 */
public class ObjectShortHashMapWithHashingStrategy<K> implements MutableObjectShortMap<K>, Externalizable
{
    public static final short EMPTY_VALUE = (short) 0;

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private static final Object NULL_KEY = new Object()
    {
        @Override
        public boolean equals(Object obj)
        {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        @Override
        public int hashCode()
        {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        @Override
        public String toString()
        {
            return "ObjectShortHashMapWithHashingStrategy.NULL_KEY";
        }
    };

    private static final Object REMOVED_KEY = new Object()
    {
        @Override
        public boolean equals(Object obj)
        {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        @Override
        public int hashCode()
        {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        @Override
        public String toString()
        {
            return "ObjectShortHashMapWithHashingStrategy.REMOVED_KEY";
        }
    };

    private Object[] keys;
    private short[] values;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    private HashingStrategy<? super K> hashingStrategy;

    /**
     * @deprecated Use ObjectShortHashMapWithHashingStrategy(HashingStrategy) instead.
     */
    @Deprecated
    public ObjectShortHashMapWithHashingStrategy()
    {
    }

    public ObjectShortHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy)
    {
        this.hashingStrategy = hashingStrategy;
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ObjectShortHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        this.hashingStrategy = hashingStrategy;
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity << 1));
        this.allocateTable(capacity);
    }

    public ObjectShortHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy, ObjectShortMap<? extends K> map)
    {
        this(hashingStrategy, Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }

    public static <K> ObjectShortHashMapWithHashingStrategy<K> newMap(HashingStrategy<? super K> hashingStrategy)
    {
        return new ObjectShortHashMapWithHashingStrategy<>(hashingStrategy);
    }

    public static <K> ObjectShortHashMapWithHashingStrategy<K> newMap(HashingStrategy<? super K> hashingStrategy, ObjectShortMap<K> map)
    {
        return new ObjectShortHashMapWithHashingStrategy<>(hashingStrategy, map);
    }

    public static <K> ObjectShortHashMapWithHashingStrategy<K> newMap(ObjectShortHashMapWithHashingStrategy<K> map)
    {
        return new ObjectShortHashMapWithHashingStrategy<>(map.hashingStrategy, map);
    }

    public static <K> ObjectShortHashMapWithHashingStrategy<K> newMapWithInitialCapacity(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        return new ObjectShortHashMapWithHashingStrategy<>(hashingStrategy, initialCapacity);
    }

    public static <K> ObjectShortHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, short value1)
    {
        return new ObjectShortHashMapWithHashingStrategy<K>(hashingStrategy, 1).withKeyValue(key1, value1);
    }

    public static <K> ObjectShortHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, short value1, K key2, short value2)
    {
        return new ObjectShortHashMapWithHashingStrategy<K>(hashingStrategy, 2).withKeysValues(key1, value1, key2, value2);
    }

    public static <K> ObjectShortHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, short value1, K key2, short value2, K key3, short value3)
    {
        return new ObjectShortHashMapWithHashingStrategy<K>(hashingStrategy, 3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static <K> ObjectShortHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, short value1, K key2, short value2, K key3, short value3, K key4, short value4)
    {
        return new ObjectShortHashMapWithHashingStrategy<K>(hashingStrategy, 4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    private int smallestPowerOfTwoGreaterThan(int n)
    {
        return n > 1 ? Integer.highestOneBit(n - 1) << 1 : 1;
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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof ObjectShortMap))
        {
            return false;
        }

        ObjectShortMap<K> other = (ObjectShortMap<K>) obj;

        if (this.size() != other.size())
        {
            return false;
        }

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!other.containsKey(this.toNonSentinel(this.keys[i])) || this.values[i] != other.getOrThrow(this.toNonSentinel(this.keys[i]))))
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

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result += this.hashingStrategy.computeHashCode(this.toNonSentinel(this.keys[i])) ^ (int) this.values[i];
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

        for (int i = 0; i < this.keys.length; i++)
        {
            Object key = this.keys[i];
            if (isNonSentinel(key))
            {
                if (!first)
                {
                    appendable.append(", ");
                }
                appendable.append(this.toNonSentinel(key)).append("=").append(this.values[i]);
                first = false;
            }
        }
        appendable.append("}");

        return appendable.toString();
    }

    @Override
    public int size()
    {
        return this.occupiedWithData;
    }

    @Override
    public boolean isEmpty()
    {
        return this.size() == 0;
    }

    @Override
    public boolean notEmpty()
    {
        return this.size() != 0;
    }

    @Override
    public String makeString()
    {
        return this.makeString(", ");
    }

    @Override
    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);

            boolean first = true;

            for (int i = 0; i < this.keys.length; i++)
            {
                Object key = this.keys[i];
                if (isNonSentinel(key))
                {
                    if (!first)
                    {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(String.valueOf(this.values[i])));
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
    public MutableShortIterator shortIterator()
    {
        return new InternalShortIterator();
    }

    @Override
    public short[] toArray()
    {
        short[] result = new short[this.size()];
        int index = 0;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result[index] = this.values[i];
                index++;
            }
        }
        return result;
    }

    @Override
    public short[] toArray(short[] target)
    {
        if (target.length < this.size())
        {
            target = new short[this.size()];
        }
        int index = 0;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                target[index] = this.values[i];
                index++;
            }
        }
        return target;
    }

    @Override
    public boolean contains(short value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(short... source)
    {
        for (short item : source)
        {
            if (!this.containsValue(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return this.containsAll(source.toArray());
    }

    @Override
    public void clear()
    {
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;
        Arrays.fill(this.keys, null);
        Arrays.fill(this.values, EMPTY_VALUE);
    }

    @Override
    public void put(K key, short value)
    {
        int index = this.probe(key);

        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            // key already present in map
            this.values[index] = value;
            return;
        }

        this.addKeyValueAtIndex(key, value, index);
    }

    @Override
    public void putAll(ObjectShortMap<? extends K> map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ObjectShortToShortFunction<? super K> function)
    {
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                this.values[i] = function.valueOf(this.toNonSentinel(this.keys[i]), this.values[i]);
            }
        }
    }

    @Override
    public void removeKey(K key)
    {
        int index = this.probe(key);
        this.removeKeyAtIndex(key, index);
    }

    public void removeKeyAtIndex(K key, int index)
    {
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.keys[index] = REMOVED_KEY;
            this.values[index] = EMPTY_VALUE;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;
        }
    }

    @Override
    public void remove(Object key)
    {
        this.removeKey((K) key);
    }

    @Override
    public short removeKeyIfAbsent(K key, short value)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.keys[index] = REMOVED_KEY;
            short oldValue = this.values[index];
            this.values[index] = EMPTY_VALUE;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;

            return oldValue;
        }
        return value;
    }

    @Override
    public short getIfAbsentPut(K key, short value)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public short getAndPut(K key, short putValue, short defaultValue)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            short existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public short getIfAbsentPut(K key, ShortFunction0 function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        short value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> short getIfAbsentPutWith(K key, ShortFunction<? super P> function, P parameter)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        short value = function.shortValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public short getIfAbsentPutWithKey(K key, ShortFunction<? super K> function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        short value = function.shortValueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public short updateValue(K key, short initialValueIfAbsent, ShortToShortFunction function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        short value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(K key, short value, int index)
    {
        if (this.keys[index] == REMOVED_KEY)
        {
            --this.occupiedWithSentinels;
        }
        this.keys[index] = toSentinelIfNull(key);
        this.values[index] = value;
        ++this.occupiedWithData;
        if (this.occupiedWithData + this.occupiedWithSentinels > this.maxOccupiedWithData())
        {
            this.rehashAndGrow();
        }
    }

    @Override
    public short addToValue(K key, short toBeAdded)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.values[index] += toBeAdded;
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, toBeAdded, index);
        return toBeAdded;
    }

    @Override
    public ObjectShortHashMapWithHashingStrategy<K> withKeyValue(K key1, short value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ObjectShortHashMapWithHashingStrategy<K> withKeysValues(K key1, short value1, K key2, short value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ObjectShortHashMapWithHashingStrategy<K> withKeysValues(K key1, short value1, K key2, short value2, K key3, short value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ObjectShortHashMapWithHashingStrategy<K> withKeysValues(K key1, short value1, K key2, short value2, K key3, short value3, K key4, short value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ObjectShortHashMapWithHashingStrategy<K> withoutKey(K key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ObjectShortHashMapWithHashingStrategy<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        for (K key : keys)
        {
            this.removeKey(key);
        }
        return this;
    }

    @Override
    public MutableObjectShortMap<K> asUnmodifiable()
    {
        return new UnmodifiableObjectShortMap<>(this);
    }

    @Override
    public MutableObjectShortMap<K> asSynchronized()
    {
        return new SynchronizedObjectShortMap<>(this);
    }

    @Override
    public ImmutableObjectShortMap<K> toImmutable()
    {
        return ObjectShortMaps.immutable.withAll(this);
    }

    @Override
    public short get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public short getOrThrow(Object key)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]))
        {
            return this.values[index];
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public short getIfAbsent(Object key, short ifAbsent)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    @Override
    public boolean containsKey(Object key)
    {
        int index = this.probe(key);
        return isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key);
    }

    @Override
    public boolean containsValue(short value)
    {
        for (int i = 0; i < this.values.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && this.values[i] == value)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void each(ShortProcedure procedure)
    {
        this.forEachValue(procedure);
    }

    @Override
    public void forEachValue(ShortProcedure procedure)
    {
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                procedure.value(this.values[i]);
            }
        }
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                procedure.value(this.toNonSentinel(this.keys[i]));
            }
        }
    }

    @Override
    public void forEachKeyValue(ObjectShortProcedure<? super K> procedure)
    {
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                procedure.value(this.toNonSentinel(this.keys[i]), this.values[i]);
            }
        }
    }

    @Override
    public ObjectShortHashMapWithHashingStrategy<K> select(ObjectShortPredicate<? super K> predicate)
    {
        ObjectShortHashMapWithHashingStrategy<K> result = ObjectShortHashMapWithHashingStrategy.newMap(this.hashingStrategy);

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.toNonSentinel(this.keys[i]), this.values[i]))
            {
                result.put(this.toNonSentinel(this.keys[i]), this.values[i]);
            }
        }
        return result;
    }

    @Override
    public ObjectShortHashMapWithHashingStrategy<K> reject(ObjectShortPredicate<? super K> predicate)
    {
        ObjectShortHashMapWithHashingStrategy<K> result = ObjectShortHashMapWithHashingStrategy.newMap(this.hashingStrategy);

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.toNonSentinel(this.keys[i]), this.values[i]))
            {
                result.put(this.toNonSentinel(this.keys[i]), this.values[i]);
            }
        }
        return result;
    }

    @Override
    public MutableShortCollection select(ShortPredicate predicate)
    {
        MutableShortList result = ShortLists.mutable.empty();

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i]))
            {
                result.add(this.values[i]);
            }
        }
        return result;
    }

    @Override
    public MutableShortCollection reject(ShortPredicate predicate)
    {
        MutableShortList result = ShortLists.mutable.empty();

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.values[i]))
            {
                result.add(this.values[i]);
            }
        }
        return result;
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i]))
            {
                return this.values[i];
            }
        }
        return ifNone;
    }

    @Override
    public <V> MutableCollection<V> collect(ShortToObjectFunction<? extends V> function)
    {
        MutableList<V> result = Lists.mutable.withInitialCapacity(this.size());
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result.add(function.valueOf(this.values[i]));
            }
        }
        return result;
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        int count = 0;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i]))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i]))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.values[i]))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i]))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public <V> V injectInto(V injectedValue, ObjectShortToObjectFunction<? super V, ? extends V> function)
    {
        V result = injectedValue;

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
    public RichIterable<ShortIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ShortIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            ShortIterator iterator = this.shortIterator();
            while (iterator.hasNext())
            {
                MutableShortBag batch = ShortBags.mutable.empty();
                for (int i = 0; i < size && iterator.hasNext(); i++)
                {
                    batch.add(iterator.next());
                }
                result.add(batch);
            }
        }
        return result;
    }

    @Override
    public long sum()
    {
        long result = 0L;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result += this.values[i];
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
        short max = (short) 0;
        boolean isMaxSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMaxSet || max < this.values[i]))
            {
                max = this.values[i];
                isMaxSet = true;
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
        short min = (short) 0;
        boolean isMinSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMinSet || this.values[i] < min))
            {
                min = this.values[i];
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        short max = (short) 0;
        boolean isMaxSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMaxSet || max < this.values[i]))
            {
                max = this.values[i];
                isMaxSet = true;
            }
        }
        return max;
    }

    @Override
    public short minIfEmpty(short defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        short min = (short) 0;
        boolean isMinSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMinSet || this.values[i] < min))
            {
                min = this.values[i];
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public double average()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        return (double) this.sum() / (double) this.size();
    }

    @Override
    public double median()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        short[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            short first = sortedArray[middleIndex];
            short second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public MutableShortList toList()
    {
        MutableShortList result = ShortLists.mutable.withInitialCapacity(this.size());

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result.add(this.values[i]);
            }
        }
        return result;
    }

    @Override
    public MutableShortSet toSet()
    {
        MutableShortSet result = ShortSets.mutable.empty();

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result.add(this.values[i]);
            }
        }
        return result;
    }

    @Override
    public MutableShortBag toBag()
    {
        MutableShortBag result = ShortBags.mutable.empty();

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result.add(this.values[i]);
            }
        }
        return result;
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public short[] toSortedArray()
    {
        short[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableShortList toSortedList()
    {
        return this.toList().sortThis();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(this.hashingStrategy);
        out.writeInt(this.size());
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeObject(this.toNonSentinel(this.keys[i]));
                out.writeShort(this.values[i]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.hashingStrategy = (HashingStrategy<? super K>) in.readObject();
        int size = in.readInt();
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(size << 1));
        this.allocateTable(capacity);

        for (int i = 0; i < size; i++)
        {
            this.put((K) in.readObject(), in.readShort());
        }
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ObjectShortPair<K>> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableShortObjectMap<K> flipUniqueValues()
    {
        MutableShortObjectMap<K> result = ShortObjectMaps.mutable.empty();
        this.forEachKeyValue((key, value) ->
        {
            K oldKey = result.put(value, key);
            if (oldKey != null)
            {
                throw new IllegalStateException("Duplicate value: " + value + " found at key: " + oldKey + " and key: " + key);
            }
        });
        return result;
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
        Object[] old = this.keys;
        short[] oldValues = this.values;
        this.allocateTable(newCapacity);
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;

        for (int i = 0; i < oldLength; i++)
        {
            if (isNonSentinel(old[i]))
            {
                this.put(this.toNonSentinel(old[i]), oldValues[i]);
            }
        }
    }

    // exposed for testing
    int probe(Object element)
    {
        int index = this.spread(element);

        int removedIndex = -1;
        if (isRemovedKey(this.keys[index]))
        {
            removedIndex = index;
        }

        else if (this.keys[index] == null || this.nullSafeEquals(this.toNonSentinel(this.keys[index]), element))
        {
            return index;
        }

        int nextIndex = index;
        int probe = 17;

        // loop until an empty slot is reached
        while (true)
        {
            // Probe algorithm: 17*n*(n+1)/2 where n = no. of collisions
            nextIndex += probe;
            probe += 17;
            nextIndex &= this.keys.length - 1;

            if (isRemovedKey(this.keys[nextIndex]))
            {
                if (removedIndex == -1)
                {
                    removedIndex = nextIndex;
                }
            }
            else if (this.nullSafeEquals(this.toNonSentinel(this.keys[nextIndex]), element))
            {
                return nextIndex;
            }
            else if (this.keys[nextIndex] == null)
            {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
        }
    }

    // exposed for testing
    int spread(Object element)
    {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        K nonSentinelKey = this.toNonSentinel(element);
        int h = nonSentinelKey == null ? 0 : this.hashingStrategy.computeHashCode(nonSentinelKey);
        h ^= h >>> 20 ^ h >>> 12;
        h ^= h >>> 7 ^ h >>> 4;
        return h & (this.keys.length - 1);
    }

    private boolean nullSafeEquals(K value, Object other)
    {
        if (value == null)
        {
            if (other == null)
            {
                return true;
            }
        }
        else if (value != NULL_KEY && other != null)
        {
            if (this.hashingStrategy.equals(value, this.toNonSentinel(other)))
            {
                return true;
            }
        }
        return false;
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.keys = new Object[sizeToAllocate];
        this.values = new short[sizeToAllocate];
    }

    private static boolean isRemovedKey(Object key)
    {
        return key == REMOVED_KEY;
    }

    private static <K> boolean isNonSentinel(K key)
    {
        return key != null && !isRemovedKey(key);
    }

    private K toNonSentinel(Object key)
    {
        return key == NULL_KEY ? null : (K) key;
    }

    private static Object toSentinelIfNull(Object key)
    {
        return key == null ? NULL_KEY : key;
    }

    private int maxOccupiedWithData()
    {
        int capacity = this.keys.length;
        // need at least one free slot for open addressing
        return Math.min(capacity - 1, capacity >> 1);
    }

    private class InternalShortIterator implements MutableShortIterator
    {
        private int count;
        private int position;

        @Override
        public boolean hasNext()
        {
            return this.count != ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public short next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }

            Object[] keys = ObjectShortHashMapWithHashingStrategy.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            short result = ObjectShortHashMapWithHashingStrategy.this.values[this.position];
            this.count++;
            this.position++;
            return result;
        }

        @Override
        public void remove()
        {
            if (this.position == 0 || !isNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[this.position - 1]))
            {
                throw new IllegalStateException();
            }
            ObjectShortHashMapWithHashingStrategy.this.remove(ObjectShortHashMapWithHashingStrategy.this.keys[this.position - 1]);
            this.count--;
        }
    }

    @Override
    public Set<K> keySet()
    {
        return new KeySet();
    }

    @Override
    public MutableShortCollection values()
    {
        return new ValuesCollection();
    }

    private class KeySet implements Set<K>
    {
        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof Set)
            {
                Set<?> other = (Set<?>) obj;
                if (other.size() == this.size())
                {
                    return this.containsAll(other);
                }
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            int hashCode = 0;
            Object[] table = ObjectShortHashMapWithHashingStrategy.this.keys;
            for (int i = 0; i < table.length; i++)
            {
                Object key = table[i];
                if (ObjectShortHashMapWithHashingStrategy.isNonSentinel(key))
                {
                    K nonSentinelKey = ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(key);
                    hashCode += nonSentinelKey == null ? 0 : ObjectShortHashMapWithHashingStrategy.this.hashingStrategy.computeHashCode(nonSentinelKey);
                }
            }
            return hashCode;
        }

        @Override
        public int size()
        {
            return ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectShortHashMapWithHashingStrategy.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return ObjectShortHashMapWithHashingStrategy.this.containsKey(o);
        }

        @Override
        public Object[] toArray()
        {
            int size = ObjectShortHashMapWithHashingStrategy.this.size();
            Object[] result = new Object[size];
            this.copyKeys(result);
            return result;
        }

        @Override
        public <T> T[] toArray(T[] result)
        {
            int size = ObjectShortHashMapWithHashingStrategy.this.size();
            if (result.length < size)
            {
                result = (T[]) Array.newInstance(result.getClass().getComponentType(), size);
            }
            this.copyKeys(result);
            if (size < result.length)
            {
                result[size] = null;
            }
            return result;
        }

        @Override
        public boolean add(K key)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean remove(Object key)
        {
            int oldSize = ObjectShortHashMapWithHashingStrategy.this.size();
            ObjectShortHashMapWithHashingStrategy.this.removeKey((K) key);
            return oldSize != ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean containsAll(Collection<?> collection)
        {
            for (Object aCollection : collection)
            {
                if (!ObjectShortHashMapWithHashingStrategy.this.containsKey(aCollection))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends K> collection)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean retainAll(Collection<?> collection)
        {
            int oldSize = ObjectShortHashMapWithHashingStrategy.this.size();
            Iterator<K> iterator = this.iterator();
            while (iterator.hasNext())
            {
                K next = iterator.next();
                if (!collection.contains(next))
                {
                    iterator.remove();
                }
            }
            return oldSize != ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            int oldSize = ObjectShortHashMapWithHashingStrategy.this.size();
            for (Object object : collection)
            {
                ObjectShortHashMapWithHashingStrategy.this.removeKey((K) object);
            }
            return oldSize != ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public void clear()
        {
            ObjectShortHashMapWithHashingStrategy.this.clear();
        }

        @Override
        public Iterator<K> iterator()
        {
            return new KeySetIterator();
        }

        private void copyKeys(Object[] result)
        {
            int count = 0;
            for (int i = 0; i < ObjectShortHashMapWithHashingStrategy.this.keys.length; i++)
            {
                Object key = ObjectShortHashMapWithHashingStrategy.this.keys[i];
                if (ObjectShortHashMapWithHashingStrategy.isNonSentinel(key))
                {
                    result[count++] = ObjectShortHashMapWithHashingStrategy.this.keys[i];
                }
            }
        }
    }

    private class KeySetIterator implements Iterator<K>
    {
        private int count;
        private int position;
        private K currentKey;
        private boolean isCurrentKeySet;

        @Override
        public boolean hasNext()
        {
            return this.count < ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public K next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            this.count++;
            Object[] keys = ObjectShortHashMapWithHashingStrategy.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.currentKey = (K) ObjectShortHashMapWithHashingStrategy.this.keys[this.position];
            this.isCurrentKeySet = true;
            this.position++;
            return ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(this.currentKey);
        }

        @Override
        public void remove()
        {
            if (!this.isCurrentKeySet)
            {
                throw new IllegalStateException();
            }

            this.isCurrentKeySet = false;
            this.count--;

            if (isNonSentinel(this.currentKey))
            {
                int index = this.position - 1;
                ObjectShortHashMapWithHashingStrategy.this.removeKeyAtIndex(ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(this.currentKey), index);
            }
            else
            {
                ObjectShortHashMapWithHashingStrategy.this.removeKey(this.currentKey);
            }
        }
    }

    private class ValuesCollection implements MutableShortCollection
    {
        @Override
        public int size()
        {
            return ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectShortHashMapWithHashingStrategy.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return ObjectShortHashMapWithHashingStrategy.this.notEmpty();
        }

        @Override
        public String makeString()
        {
            return this.makeString(", ");
        }

        @Override
        public String makeString(String separator)
        {
            return this.makeString("", separator, "");
        }

        @Override
        public String makeString(String start, String separator, String end)
        {
            Appendable stringBuilder = new StringBuilder();
            this.appendString(stringBuilder, start, separator, end);
            return stringBuilder.toString();
        }

        @Override
        public void appendString(Appendable appendable)
        {
            this.appendString(appendable, ", ");
        }

        @Override
        public void appendString(Appendable appendable, String separator)
        {
            this.appendString(appendable, "", separator, "");
        }

        @Override
        public void appendString(Appendable appendable, String start, String separator, String end)
        {
            try
            {
                appendable.append(start);

                boolean first = true;

                for (int i = 0; i < ObjectShortHashMapWithHashingStrategy.this.keys.length; i++)
                {
                    Object key = ObjectShortHashMapWithHashingStrategy.this.keys[i];
                    if (isNonSentinel(key))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ObjectShortHashMapWithHashingStrategy.this.values[i]));
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
        public boolean add(short element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(short... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(ShortIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean remove(short item)
        {
            int oldSize = ObjectShortHashMapWithHashingStrategy.this.size();

            for (int i = 0; i < ObjectShortHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (isNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]) && item == ObjectShortHashMapWithHashingStrategy.this.values[i])
                {
                    ObjectShortHashMapWithHashingStrategy.this.removeKey((K) ObjectShortHashMapWithHashingStrategy.this.keys[i]);
                }
            }
            return oldSize != ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(ShortIterable source)
        {
            int oldSize = ObjectShortHashMapWithHashingStrategy.this.size();

            ShortIterator iterator = source.shortIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(short... source)
        {
            int oldSize = ObjectShortHashMapWithHashingStrategy.this.size();

            for (short item : source)
            {
                this.remove(item);
            }
            return oldSize != ObjectShortHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean retainAll(ShortIterable source)
        {
            int oldSize = ObjectShortHashMapWithHashingStrategy.this.size();
            final ShortSet sourceSet = source instanceof ShortSet ? (ShortSet) source : source.toSet();
            ObjectShortHashMapWithHashingStrategy<K> retained = ObjectShortHashMapWithHashingStrategy.this.select((K object, short value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ObjectShortHashMapWithHashingStrategy.this.keys = retained.keys;
                ObjectShortHashMapWithHashingStrategy.this.values = retained.values;
                ObjectShortHashMapWithHashingStrategy.this.occupiedWithData = retained.occupiedWithData;
                ObjectShortHashMapWithHashingStrategy.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(short... source)
        {
            return this.retainAll(ShortSets.mutable.with(source));
        }

        @Override
        public void clear()
        {
            ObjectShortHashMapWithHashingStrategy.this.clear();
        }

        @Override
        public MutableShortCollection with(short element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableShortCollection without(short element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableShortCollection withAll(ShortIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableShortCollection withoutAll(ShortIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableShortCollection asUnmodifiable()
        {
            return UnmodifiableShortCollection.of(this);
        }

        @Override
        public MutableShortCollection asSynchronized()
        {
            return SynchronizedShortCollection.of(this);
        }

        @Override
        public ImmutableShortCollection toImmutable()
        {
            return ShortLists.immutable.withAll(this);
        }

        @Override
        public MutableShortIterator shortIterator()
        {
            return ObjectShortHashMapWithHashingStrategy.this.shortIterator();
        }

        @Override
        public short[] toArray()
        {
            return ObjectShortHashMapWithHashingStrategy.this.toArray();
        }

        @Override
        public short[] toArray(short[] target)
        {
            return ObjectShortHashMapWithHashingStrategy.this.toArray(target);
        }

        @Override
        public boolean contains(short value)
        {
            return ObjectShortHashMapWithHashingStrategy.this.containsValue(value);
        }

        @Override
        public boolean containsAll(short... source)
        {
            return ObjectShortHashMapWithHashingStrategy.this.containsAll(source);
        }

        @Override
        public boolean containsAll(ShortIterable source)
        {
            return ObjectShortHashMapWithHashingStrategy.this.containsAll(source);
        }

        @Override
        public void forEach(ShortProcedure procedure)
        {
            ObjectShortHashMapWithHashingStrategy.this.forEach(procedure);
        }

        @Override
        public void each(ShortProcedure procedure)
        {
            this.forEach(procedure);
        }

        @Override
        public MutableShortCollection select(ShortPredicate predicate)
        {
            return ObjectShortHashMapWithHashingStrategy.this.select(predicate);
        }

        @Override
        public MutableShortCollection reject(ShortPredicate predicate)
        {
            return ObjectShortHashMapWithHashingStrategy.this.reject(predicate);
        }

        @Override
        public <V> MutableCollection<V> collect(ShortToObjectFunction<? extends V> function)
        {
            return ObjectShortHashMapWithHashingStrategy.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
        {
            return ObjectShortHashMapWithHashingStrategy.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<ShortIterable> chunk(int size)
        {
            return ObjectShortHashMapWithHashingStrategy.this.chunk(size);
        }

        @Override
        public short detectIfNone(ShortPredicate predicate, short ifNone)
        {
            return ObjectShortHashMapWithHashingStrategy.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public int count(ShortPredicate predicate)
        {
            return ObjectShortHashMapWithHashingStrategy.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(ShortPredicate predicate)
        {
            return ObjectShortHashMapWithHashingStrategy.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(ShortPredicate predicate)
        {
            return ObjectShortHashMapWithHashingStrategy.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(ShortPredicate predicate)
        {
            return ObjectShortHashMapWithHashingStrategy.this.noneSatisfy(predicate);
        }

        @Override
        public MutableShortList toList()
        {
            return ObjectShortHashMapWithHashingStrategy.this.toList();
        }

        @Override
        public MutableShortSet toSet()
        {
            return ObjectShortHashMapWithHashingStrategy.this.toSet();
        }

        @Override
        public MutableShortBag toBag()
        {
            return ObjectShortHashMapWithHashingStrategy.this.toBag();
        }

        @Override
        public LazyShortIterable asLazy()
        {
            return new LazyShortIterableAdapter(this);
        }

        @Override
        public short[] toSortedArray()
        {
            return ObjectShortHashMapWithHashingStrategy.this.toSortedArray();
        }

        @Override
        public MutableShortList toSortedList()
        {
            return ObjectShortHashMapWithHashingStrategy.this.toSortedList();
        }

        @Override
        public long sum()
        {
            return ObjectShortHashMapWithHashingStrategy.this.sum();
        }

        @Override
        public short max()
        {
            return ObjectShortHashMapWithHashingStrategy.this.max();
        }

        @Override
        public short maxIfEmpty(short defaultValue)
        {
            return ObjectShortHashMapWithHashingStrategy.this.maxIfEmpty(defaultValue);
        }

        @Override
        public short min()
        {
            return ObjectShortHashMapWithHashingStrategy.this.min();
        }

        @Override
        public short minIfEmpty(short defaultValue)
        {
            return ObjectShortHashMapWithHashingStrategy.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return ObjectShortHashMapWithHashingStrategy.this.average();
        }

        @Override
        public double median()
        {
            return ObjectShortHashMapWithHashingStrategy.this.median();
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableShortCollection newEmpty()
        {
            return ShortBags.mutable.empty();
        }
    }

    private class KeysView extends AbstractLazyIterable<K>
    {
        @Override
        public void each(Procedure<? super K> procedure)
        {
            ObjectShortHashMapWithHashingStrategy.this.forEachKey(procedure);
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super K> objectShortProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectShortHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectShortHashMapWithHashingStrategy.isNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]))
                {
                    objectShortProcedure.value(ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super K, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectShortHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectShortHashMapWithHashingStrategy.isNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<K> iterator()
        {
            return new InternalKeysViewIterator();
        }

        public class InternalKeysViewIterator implements Iterator<K>
        {
            private int count;
            private int position;

            @Override
            public K next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException();
                }

                Object[] keys = ObjectShortHashMapWithHashingStrategy.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                K result = ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[this.position]);
                this.count++;
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
                return this.count != ObjectShortHashMapWithHashingStrategy.this.size();
            }
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<ObjectShortPair<K>>
    {
        @Override
        public void each(Procedure<? super ObjectShortPair<K>> procedure)
        {
            for (int i = 0; i < ObjectShortHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectShortHashMapWithHashingStrategy.isNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]), ObjectShortHashMapWithHashingStrategy.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ObjectShortPair<K>> objectIntProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectShortHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectShortHashMapWithHashingStrategy.isNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]), ObjectShortHashMapWithHashingStrategy.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ObjectShortPair<K>, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectShortHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectShortHashMapWithHashingStrategy.isNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[i]), ObjectShortHashMapWithHashingStrategy.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<ObjectShortPair<K>> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ObjectShortPair<K>>
        {
            private int count;
            private int position;

            @Override
            public ObjectShortPair<K> next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException();
                }

                Object[] keys = ObjectShortHashMapWithHashingStrategy.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                ObjectShortPair<K> result = PrimitiveTuples.pair(ObjectShortHashMapWithHashingStrategy.this.toNonSentinel(ObjectShortHashMapWithHashingStrategy.this.keys[this.position]), ObjectShortHashMapWithHashingStrategy.this.values[this.position]);
                this.count++;
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
                return this.count != ObjectShortHashMapWithHashingStrategy.this.size();
            }
        }
    }
}
