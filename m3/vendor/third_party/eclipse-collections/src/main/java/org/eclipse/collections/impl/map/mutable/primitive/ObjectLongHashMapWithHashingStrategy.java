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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction0;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectLongPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.LongBags;
import org.eclipse.collections.api.factory.primitive.LongLists;
import org.eclipse.collections.api.factory.primitive.LongSets;
import org.eclipse.collections.api.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectLongMap;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedLongCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file objectPrimitiveHashMapWithHashingStrategy.stg.
 *
 * @since 7.0.
 */
public class ObjectLongHashMapWithHashingStrategy<K> implements MutableObjectLongMap<K>, Externalizable
{
    public static final long EMPTY_VALUE = 0L;

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
            return "ObjectLongHashMapWithHashingStrategy.NULL_KEY";
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
            return "ObjectLongHashMapWithHashingStrategy.REMOVED_KEY";
        }
    };

    private Object[] keys;
    private long[] values;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    private HashingStrategy<? super K> hashingStrategy;

    /**
     * @deprecated Use ObjectLongHashMapWithHashingStrategy(HashingStrategy) instead.
     */
    @Deprecated
    public ObjectLongHashMapWithHashingStrategy()
    {
    }

    public ObjectLongHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy)
    {
        this.hashingStrategy = hashingStrategy;
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ObjectLongHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        this.hashingStrategy = hashingStrategy;
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity << 1));
        this.allocateTable(capacity);
    }

    public ObjectLongHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy, ObjectLongMap<? extends K> map)
    {
        this(hashingStrategy, Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }

    public static <K> ObjectLongHashMapWithHashingStrategy<K> newMap(HashingStrategy<? super K> hashingStrategy)
    {
        return new ObjectLongHashMapWithHashingStrategy<>(hashingStrategy);
    }

    public static <K> ObjectLongHashMapWithHashingStrategy<K> newMap(HashingStrategy<? super K> hashingStrategy, ObjectLongMap<K> map)
    {
        return new ObjectLongHashMapWithHashingStrategy<>(hashingStrategy, map);
    }

    public static <K> ObjectLongHashMapWithHashingStrategy<K> newMap(ObjectLongHashMapWithHashingStrategy<K> map)
    {
        return new ObjectLongHashMapWithHashingStrategy<>(map.hashingStrategy, map);
    }

    public static <K> ObjectLongHashMapWithHashingStrategy<K> newMapWithInitialCapacity(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        return new ObjectLongHashMapWithHashingStrategy<>(hashingStrategy, initialCapacity);
    }

    public static <K> ObjectLongHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, long value1)
    {
        return new ObjectLongHashMapWithHashingStrategy<K>(hashingStrategy, 1).withKeyValue(key1, value1);
    }

    public static <K> ObjectLongHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, long value1, K key2, long value2)
    {
        return new ObjectLongHashMapWithHashingStrategy<K>(hashingStrategy, 2).withKeysValues(key1, value1, key2, value2);
    }

    public static <K> ObjectLongHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, long value1, K key2, long value2, K key3, long value3)
    {
        return new ObjectLongHashMapWithHashingStrategy<K>(hashingStrategy, 3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static <K> ObjectLongHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, long value1, K key2, long value2, K key3, long value3, K key4, long value4)
    {
        return new ObjectLongHashMapWithHashingStrategy<K>(hashingStrategy, 4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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

        if (!(obj instanceof ObjectLongMap))
        {
            return false;
        }

        ObjectLongMap<K> other = (ObjectLongMap<K>) obj;

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
                result += this.hashingStrategy.computeHashCode(this.toNonSentinel(this.keys[i])) ^ (int) (this.values[i] ^ this.values[i] >>> 32);
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
    public MutableLongIterator longIterator()
    {
        return new InternalLongIterator();
    }

    @Override
    public long[] toArray()
    {
        long[] result = new long[this.size()];
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
    public long[] toArray(long[] target)
    {
        if (target.length < this.size())
        {
            target = new long[this.size()];
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
    public boolean contains(long value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        for (long item : source)
        {
            if (!this.containsValue(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(LongIterable source)
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
    public void put(K key, long value)
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
    public void putAll(ObjectLongMap<? extends K> map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ObjectLongToLongFunction<? super K> function)
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
    public long removeKeyIfAbsent(K key, long value)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.keys[index] = REMOVED_KEY;
            long oldValue = this.values[index];
            this.values[index] = EMPTY_VALUE;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;

            return oldValue;
        }
        return value;
    }

    @Override
    public long getIfAbsentPut(K key, long value)
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
    public long getAndPut(K key, long putValue, long defaultValue)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            long existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public long getIfAbsentPut(K key, LongFunction0 function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        long value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> long getIfAbsentPutWith(K key, LongFunction<? super P> function, P parameter)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        long value = function.longValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public long getIfAbsentPutWithKey(K key, LongFunction<? super K> function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        long value = function.longValueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public long updateValue(K key, long initialValueIfAbsent, LongToLongFunction function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        long value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(K key, long value, int index)
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
    public long addToValue(K key, long toBeAdded)
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
    public ObjectLongHashMapWithHashingStrategy<K> withKeyValue(K key1, long value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ObjectLongHashMapWithHashingStrategy<K> withKeysValues(K key1, long value1, K key2, long value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ObjectLongHashMapWithHashingStrategy<K> withKeysValues(K key1, long value1, K key2, long value2, K key3, long value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ObjectLongHashMapWithHashingStrategy<K> withKeysValues(K key1, long value1, K key2, long value2, K key3, long value3, K key4, long value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ObjectLongHashMapWithHashingStrategy<K> withoutKey(K key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ObjectLongHashMapWithHashingStrategy<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        for (K key : keys)
        {
            this.removeKey(key);
        }
        return this;
    }

    @Override
    public MutableObjectLongMap<K> asUnmodifiable()
    {
        return new UnmodifiableObjectLongMap<>(this);
    }

    @Override
    public MutableObjectLongMap<K> asSynchronized()
    {
        return new SynchronizedObjectLongMap<>(this);
    }

    @Override
    public ImmutableObjectLongMap<K> toImmutable()
    {
        return ObjectLongMaps.immutable.withAll(this);
    }

    @Override
    public long get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public long getOrThrow(Object key)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]))
        {
            return this.values[index];
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public long getIfAbsent(Object key, long ifAbsent)
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
    public boolean containsValue(long value)
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
    public void each(LongProcedure procedure)
    {
        this.forEachValue(procedure);
    }

    @Override
    public void forEachValue(LongProcedure procedure)
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
    public void forEachKeyValue(ObjectLongProcedure<? super K> procedure)
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
    public ObjectLongHashMapWithHashingStrategy<K> select(ObjectLongPredicate<? super K> predicate)
    {
        ObjectLongHashMapWithHashingStrategy<K> result = ObjectLongHashMapWithHashingStrategy.newMap(this.hashingStrategy);

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
    public ObjectLongHashMapWithHashingStrategy<K> reject(ObjectLongPredicate<? super K> predicate)
    {
        ObjectLongHashMapWithHashingStrategy<K> result = ObjectLongHashMapWithHashingStrategy.newMap(this.hashingStrategy);

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
    public MutableLongCollection select(LongPredicate predicate)
    {
        MutableLongList result = LongLists.mutable.empty();

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
    public MutableLongCollection reject(LongPredicate predicate)
    {
        MutableLongList result = LongLists.mutable.empty();

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
    public long detectIfNone(LongPredicate predicate, long ifNone)
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
    public <V> MutableCollection<V> collect(LongToObjectFunction<? extends V> function)
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
    public int count(LongPredicate predicate)
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
    public boolean anySatisfy(LongPredicate predicate)
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
    public boolean allSatisfy(LongPredicate predicate)
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
    public boolean noneSatisfy(LongPredicate predicate)
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
    public <V> V injectInto(V injectedValue, ObjectLongToObjectFunction<? super V, ? extends V> function)
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
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<LongIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            LongIterator iterator = this.longIterator();
            while (iterator.hasNext())
            {
                MutableLongBag batch = LongBags.mutable.empty();
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
    public long max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        long max = 0L;
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
    public long min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        long min = 0L;
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
    public long maxIfEmpty(long defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        long max = 0L;
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
    public long minIfEmpty(long defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        long min = 0L;
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
        long[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            long first = sortedArray[middleIndex];
            long second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public MutableLongList toList()
    {
        MutableLongList result = LongLists.mutable.withInitialCapacity(this.size());

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
    public MutableLongSet toSet()
    {
        MutableLongSet result = LongSets.mutable.empty();

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
    public MutableLongBag toBag()
    {
        MutableLongBag result = LongBags.mutable.empty();

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
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
    }

    @Override
    public long[] toSortedArray()
    {
        long[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableLongList toSortedList()
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
                out.writeLong(this.values[i]);
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
            this.put((K) in.readObject(), in.readLong());
        }
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ObjectLongPair<K>> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableLongObjectMap<K> flipUniqueValues()
    {
        MutableLongObjectMap<K> result = LongObjectMaps.mutable.empty();
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
        long[] oldValues = this.values;
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
        this.values = new long[sizeToAllocate];
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

    private class InternalLongIterator implements MutableLongIterator
    {
        private int count;
        private int position;

        @Override
        public boolean hasNext()
        {
            return this.count != ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public long next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }

            Object[] keys = ObjectLongHashMapWithHashingStrategy.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            long result = ObjectLongHashMapWithHashingStrategy.this.values[this.position];
            this.count++;
            this.position++;
            return result;
        }

        @Override
        public void remove()
        {
            if (this.position == 0 || !isNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[this.position - 1]))
            {
                throw new IllegalStateException();
            }
            ObjectLongHashMapWithHashingStrategy.this.remove(ObjectLongHashMapWithHashingStrategy.this.keys[this.position - 1]);
            this.count--;
        }
    }

    @Override
    public Set<K> keySet()
    {
        return new KeySet();
    }

    @Override
    public MutableLongCollection values()
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
            Object[] table = ObjectLongHashMapWithHashingStrategy.this.keys;
            for (int i = 0; i < table.length; i++)
            {
                Object key = table[i];
                if (ObjectLongHashMapWithHashingStrategy.isNonSentinel(key))
                {
                    K nonSentinelKey = ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(key);
                    hashCode += nonSentinelKey == null ? 0 : ObjectLongHashMapWithHashingStrategy.this.hashingStrategy.computeHashCode(nonSentinelKey);
                }
            }
            return hashCode;
        }

        @Override
        public int size()
        {
            return ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectLongHashMapWithHashingStrategy.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return ObjectLongHashMapWithHashingStrategy.this.containsKey(o);
        }

        @Override
        public Object[] toArray()
        {
            int size = ObjectLongHashMapWithHashingStrategy.this.size();
            Object[] result = new Object[size];
            this.copyKeys(result);
            return result;
        }

        @Override
        public <T> T[] toArray(T[] result)
        {
            int size = ObjectLongHashMapWithHashingStrategy.this.size();
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
            int oldSize = ObjectLongHashMapWithHashingStrategy.this.size();
            ObjectLongHashMapWithHashingStrategy.this.removeKey((K) key);
            return oldSize != ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean containsAll(Collection<?> collection)
        {
            for (Object aCollection : collection)
            {
                if (!ObjectLongHashMapWithHashingStrategy.this.containsKey(aCollection))
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
            int oldSize = ObjectLongHashMapWithHashingStrategy.this.size();
            Iterator<K> iterator = this.iterator();
            while (iterator.hasNext())
            {
                K next = iterator.next();
                if (!collection.contains(next))
                {
                    iterator.remove();
                }
            }
            return oldSize != ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            int oldSize = ObjectLongHashMapWithHashingStrategy.this.size();
            for (Object object : collection)
            {
                ObjectLongHashMapWithHashingStrategy.this.removeKey((K) object);
            }
            return oldSize != ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public void clear()
        {
            ObjectLongHashMapWithHashingStrategy.this.clear();
        }

        @Override
        public Iterator<K> iterator()
        {
            return new KeySetIterator();
        }

        private void copyKeys(Object[] result)
        {
            int count = 0;
            for (int i = 0; i < ObjectLongHashMapWithHashingStrategy.this.keys.length; i++)
            {
                Object key = ObjectLongHashMapWithHashingStrategy.this.keys[i];
                if (ObjectLongHashMapWithHashingStrategy.isNonSentinel(key))
                {
                    result[count++] = ObjectLongHashMapWithHashingStrategy.this.keys[i];
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
            return this.count < ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public K next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            this.count++;
            Object[] keys = ObjectLongHashMapWithHashingStrategy.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.currentKey = (K) ObjectLongHashMapWithHashingStrategy.this.keys[this.position];
            this.isCurrentKeySet = true;
            this.position++;
            return ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(this.currentKey);
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
                ObjectLongHashMapWithHashingStrategy.this.removeKeyAtIndex(ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(this.currentKey), index);
            }
            else
            {
                ObjectLongHashMapWithHashingStrategy.this.removeKey(this.currentKey);
            }
        }
    }

    private class ValuesCollection implements MutableLongCollection
    {
        @Override
        public int size()
        {
            return ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectLongHashMapWithHashingStrategy.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return ObjectLongHashMapWithHashingStrategy.this.notEmpty();
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

                for (int i = 0; i < ObjectLongHashMapWithHashingStrategy.this.keys.length; i++)
                {
                    Object key = ObjectLongHashMapWithHashingStrategy.this.keys[i];
                    if (isNonSentinel(key))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ObjectLongHashMapWithHashingStrategy.this.values[i]));
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
        public boolean add(long element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(long... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(LongIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean remove(long item)
        {
            int oldSize = ObjectLongHashMapWithHashingStrategy.this.size();

            for (int i = 0; i < ObjectLongHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (isNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]) && item == ObjectLongHashMapWithHashingStrategy.this.values[i])
                {
                    ObjectLongHashMapWithHashingStrategy.this.removeKey((K) ObjectLongHashMapWithHashingStrategy.this.keys[i]);
                }
            }
            return oldSize != ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(LongIterable source)
        {
            int oldSize = ObjectLongHashMapWithHashingStrategy.this.size();

            LongIterator iterator = source.longIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(long... source)
        {
            int oldSize = ObjectLongHashMapWithHashingStrategy.this.size();

            for (long item : source)
            {
                this.remove(item);
            }
            return oldSize != ObjectLongHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean retainAll(LongIterable source)
        {
            int oldSize = ObjectLongHashMapWithHashingStrategy.this.size();
            final LongSet sourceSet = source instanceof LongSet ? (LongSet) source : source.toSet();
            ObjectLongHashMapWithHashingStrategy<K> retained = ObjectLongHashMapWithHashingStrategy.this.select((K object, long value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ObjectLongHashMapWithHashingStrategy.this.keys = retained.keys;
                ObjectLongHashMapWithHashingStrategy.this.values = retained.values;
                ObjectLongHashMapWithHashingStrategy.this.occupiedWithData = retained.occupiedWithData;
                ObjectLongHashMapWithHashingStrategy.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(long... source)
        {
            return this.retainAll(LongSets.mutable.with(source));
        }

        @Override
        public void clear()
        {
            ObjectLongHashMapWithHashingStrategy.this.clear();
        }

        @Override
        public MutableLongCollection with(long element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableLongCollection without(long element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableLongCollection withAll(LongIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableLongCollection withoutAll(LongIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableLongCollection asUnmodifiable()
        {
            return UnmodifiableLongCollection.of(this);
        }

        @Override
        public MutableLongCollection asSynchronized()
        {
            return SynchronizedLongCollection.of(this);
        }

        @Override
        public ImmutableLongCollection toImmutable()
        {
            return LongLists.immutable.withAll(this);
        }

        @Override
        public MutableLongIterator longIterator()
        {
            return ObjectLongHashMapWithHashingStrategy.this.longIterator();
        }

        @Override
        public long[] toArray()
        {
            return ObjectLongHashMapWithHashingStrategy.this.toArray();
        }

        @Override
        public long[] toArray(long[] target)
        {
            return ObjectLongHashMapWithHashingStrategy.this.toArray(target);
        }

        @Override
        public boolean contains(long value)
        {
            return ObjectLongHashMapWithHashingStrategy.this.containsValue(value);
        }

        @Override
        public boolean containsAll(long... source)
        {
            return ObjectLongHashMapWithHashingStrategy.this.containsAll(source);
        }

        @Override
        public boolean containsAll(LongIterable source)
        {
            return ObjectLongHashMapWithHashingStrategy.this.containsAll(source);
        }

        @Override
        public void forEach(LongProcedure procedure)
        {
            ObjectLongHashMapWithHashingStrategy.this.forEach(procedure);
        }

        @Override
        public void each(LongProcedure procedure)
        {
            this.forEach(procedure);
        }

        @Override
        public MutableLongCollection select(LongPredicate predicate)
        {
            return ObjectLongHashMapWithHashingStrategy.this.select(predicate);
        }

        @Override
        public MutableLongCollection reject(LongPredicate predicate)
        {
            return ObjectLongHashMapWithHashingStrategy.this.reject(predicate);
        }

        @Override
        public <V> MutableCollection<V> collect(LongToObjectFunction<? extends V> function)
        {
            return ObjectLongHashMapWithHashingStrategy.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
        {
            return ObjectLongHashMapWithHashingStrategy.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<LongIterable> chunk(int size)
        {
            return ObjectLongHashMapWithHashingStrategy.this.chunk(size);
        }

        @Override
        public long detectIfNone(LongPredicate predicate, long ifNone)
        {
            return ObjectLongHashMapWithHashingStrategy.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public int count(LongPredicate predicate)
        {
            return ObjectLongHashMapWithHashingStrategy.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(LongPredicate predicate)
        {
            return ObjectLongHashMapWithHashingStrategy.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(LongPredicate predicate)
        {
            return ObjectLongHashMapWithHashingStrategy.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(LongPredicate predicate)
        {
            return ObjectLongHashMapWithHashingStrategy.this.noneSatisfy(predicate);
        }

        @Override
        public MutableLongList toList()
        {
            return ObjectLongHashMapWithHashingStrategy.this.toList();
        }

        @Override
        public MutableLongSet toSet()
        {
            return ObjectLongHashMapWithHashingStrategy.this.toSet();
        }

        @Override
        public MutableLongBag toBag()
        {
            return ObjectLongHashMapWithHashingStrategy.this.toBag();
        }

        @Override
        public LazyLongIterable asLazy()
        {
            return new LazyLongIterableAdapter(this);
        }

        @Override
        public long[] toSortedArray()
        {
            return ObjectLongHashMapWithHashingStrategy.this.toSortedArray();
        }

        @Override
        public MutableLongList toSortedList()
        {
            return ObjectLongHashMapWithHashingStrategy.this.toSortedList();
        }

        @Override
        public long sum()
        {
            return ObjectLongHashMapWithHashingStrategy.this.sum();
        }

        @Override
        public long max()
        {
            return ObjectLongHashMapWithHashingStrategy.this.max();
        }

        @Override
        public long maxIfEmpty(long defaultValue)
        {
            return ObjectLongHashMapWithHashingStrategy.this.maxIfEmpty(defaultValue);
        }

        @Override
        public long min()
        {
            return ObjectLongHashMapWithHashingStrategy.this.min();
        }

        @Override
        public long minIfEmpty(long defaultValue)
        {
            return ObjectLongHashMapWithHashingStrategy.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return ObjectLongHashMapWithHashingStrategy.this.average();
        }

        @Override
        public double median()
        {
            return ObjectLongHashMapWithHashingStrategy.this.median();
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableLongCollection newEmpty()
        {
            return LongBags.mutable.empty();
        }
    }

    private class KeysView extends AbstractLazyIterable<K>
    {
        @Override
        public void each(Procedure<? super K> procedure)
        {
            ObjectLongHashMapWithHashingStrategy.this.forEachKey(procedure);
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super K> objectLongProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectLongHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectLongHashMapWithHashingStrategy.isNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]))
                {
                    objectLongProcedure.value(ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super K, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectLongHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectLongHashMapWithHashingStrategy.isNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]), parameter);
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

                Object[] keys = ObjectLongHashMapWithHashingStrategy.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                K result = ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[this.position]);
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
                return this.count != ObjectLongHashMapWithHashingStrategy.this.size();
            }
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<ObjectLongPair<K>>
    {
        @Override
        public void each(Procedure<? super ObjectLongPair<K>> procedure)
        {
            for (int i = 0; i < ObjectLongHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectLongHashMapWithHashingStrategy.isNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]), ObjectLongHashMapWithHashingStrategy.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ObjectLongPair<K>> objectIntProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectLongHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectLongHashMapWithHashingStrategy.isNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]), ObjectLongHashMapWithHashingStrategy.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ObjectLongPair<K>, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectLongHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectLongHashMapWithHashingStrategy.isNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[i]), ObjectLongHashMapWithHashingStrategy.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<ObjectLongPair<K>> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ObjectLongPair<K>>
        {
            private int count;
            private int position;

            @Override
            public ObjectLongPair<K> next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException();
                }

                Object[] keys = ObjectLongHashMapWithHashingStrategy.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                ObjectLongPair<K> result = PrimitiveTuples.pair(ObjectLongHashMapWithHashingStrategy.this.toNonSentinel(ObjectLongHashMapWithHashingStrategy.this.keys[this.position]), ObjectLongHashMapWithHashingStrategy.this.values[this.position]);
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
                return this.count != ObjectLongHashMapWithHashingStrategy.this.size();
            }
        }
    }
}
