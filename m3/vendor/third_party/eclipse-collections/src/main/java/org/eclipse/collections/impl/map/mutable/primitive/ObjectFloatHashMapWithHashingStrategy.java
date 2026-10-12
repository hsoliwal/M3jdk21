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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction0;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectFloatPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.FloatBags;
import org.eclipse.collections.api.factory.primitive.FloatLists;
import org.eclipse.collections.api.factory.primitive.FloatSets;
import org.eclipse.collections.api.factory.primitive.ObjectFloatMaps;
import org.eclipse.collections.api.factory.primitive.FloatObjectMaps;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.MutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.MutableFloatObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedFloatCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file objectPrimitiveHashMapWithHashingStrategy.stg.
 *
 * @since 7.0.
 */
public class ObjectFloatHashMapWithHashingStrategy<K> implements MutableObjectFloatMap<K>, Externalizable
{
    public static final float EMPTY_VALUE = 0.0f;

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
            return "ObjectFloatHashMapWithHashingStrategy.NULL_KEY";
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
            return "ObjectFloatHashMapWithHashingStrategy.REMOVED_KEY";
        }
    };

    private Object[] keys;
    private float[] values;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    private HashingStrategy<? super K> hashingStrategy;

    /**
     * @deprecated Use ObjectFloatHashMapWithHashingStrategy(HashingStrategy) instead.
     */
    @Deprecated
    public ObjectFloatHashMapWithHashingStrategy()
    {
    }

    public ObjectFloatHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy)
    {
        this.hashingStrategy = hashingStrategy;
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ObjectFloatHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        this.hashingStrategy = hashingStrategy;
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity << 1));
        this.allocateTable(capacity);
    }

    public ObjectFloatHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy, ObjectFloatMap<? extends K> map)
    {
        this(hashingStrategy, Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }

    public static <K> ObjectFloatHashMapWithHashingStrategy<K> newMap(HashingStrategy<? super K> hashingStrategy)
    {
        return new ObjectFloatHashMapWithHashingStrategy<>(hashingStrategy);
    }

    public static <K> ObjectFloatHashMapWithHashingStrategy<K> newMap(HashingStrategy<? super K> hashingStrategy, ObjectFloatMap<K> map)
    {
        return new ObjectFloatHashMapWithHashingStrategy<>(hashingStrategy, map);
    }

    public static <K> ObjectFloatHashMapWithHashingStrategy<K> newMap(ObjectFloatHashMapWithHashingStrategy<K> map)
    {
        return new ObjectFloatHashMapWithHashingStrategy<>(map.hashingStrategy, map);
    }

    public static <K> ObjectFloatHashMapWithHashingStrategy<K> newMapWithInitialCapacity(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        return new ObjectFloatHashMapWithHashingStrategy<>(hashingStrategy, initialCapacity);
    }

    public static <K> ObjectFloatHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, float value1)
    {
        return new ObjectFloatHashMapWithHashingStrategy<K>(hashingStrategy, 1).withKeyValue(key1, value1);
    }

    public static <K> ObjectFloatHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, float value1, K key2, float value2)
    {
        return new ObjectFloatHashMapWithHashingStrategy<K>(hashingStrategy, 2).withKeysValues(key1, value1, key2, value2);
    }

    public static <K> ObjectFloatHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, float value1, K key2, float value2, K key3, float value3)
    {
        return new ObjectFloatHashMapWithHashingStrategy<K>(hashingStrategy, 3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static <K> ObjectFloatHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, float value1, K key2, float value2, K key3, float value3, K key4, float value4)
    {
        return new ObjectFloatHashMapWithHashingStrategy<K>(hashingStrategy, 4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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

        if (!(obj instanceof ObjectFloatMap))
        {
            return false;
        }

        ObjectFloatMap<K> other = (ObjectFloatMap<K>) obj;

        if (this.size() != other.size())
        {
            return false;
        }

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!other.containsKey(this.toNonSentinel(this.keys[i])) || Float.compare(this.values[i], other.getOrThrow(this.toNonSentinel(this.keys[i]))) != 0))
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
                result += this.hashingStrategy.computeHashCode(this.toNonSentinel(this.keys[i])) ^ Float.floatToIntBits(this.values[i]);
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
    public MutableFloatIterator floatIterator()
    {
        return new InternalFloatIterator();
    }

    @Override
    public float[] toArray()
    {
        float[] result = new float[this.size()];
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
    public float[] toArray(float[] target)
    {
        if (target.length < this.size())
        {
            target = new float[this.size()];
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
    public boolean contains(float value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float item : source)
        {
            if (!this.containsValue(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(FloatIterable source)
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
    public void put(K key, float value)
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
    public void putAll(ObjectFloatMap<? extends K> map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ObjectFloatToFloatFunction<? super K> function)
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
    public float removeKeyIfAbsent(K key, float value)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.keys[index] = REMOVED_KEY;
            float oldValue = this.values[index];
            this.values[index] = EMPTY_VALUE;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;

            return oldValue;
        }
        return value;
    }

    @Override
    public float getIfAbsentPut(K key, float value)
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
    public float getAndPut(K key, float putValue, float defaultValue)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            float existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public float getIfAbsentPut(K key, FloatFunction0 function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        float value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> float getIfAbsentPutWith(K key, FloatFunction<? super P> function, P parameter)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        float value = function.floatValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public float getIfAbsentPutWithKey(K key, FloatFunction<? super K> function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        float value = function.floatValueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public float updateValue(K key, float initialValueIfAbsent, FloatToFloatFunction function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        float value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(K key, float value, int index)
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
    public float addToValue(K key, float toBeAdded)
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
    public ObjectFloatHashMapWithHashingStrategy<K> withKeyValue(K key1, float value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ObjectFloatHashMapWithHashingStrategy<K> withKeysValues(K key1, float value1, K key2, float value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ObjectFloatHashMapWithHashingStrategy<K> withKeysValues(K key1, float value1, K key2, float value2, K key3, float value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ObjectFloatHashMapWithHashingStrategy<K> withKeysValues(K key1, float value1, K key2, float value2, K key3, float value3, K key4, float value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ObjectFloatHashMapWithHashingStrategy<K> withoutKey(K key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ObjectFloatHashMapWithHashingStrategy<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        for (K key : keys)
        {
            this.removeKey(key);
        }
        return this;
    }

    @Override
    public MutableObjectFloatMap<K> asUnmodifiable()
    {
        return new UnmodifiableObjectFloatMap<>(this);
    }

    @Override
    public MutableObjectFloatMap<K> asSynchronized()
    {
        return new SynchronizedObjectFloatMap<>(this);
    }

    @Override
    public ImmutableObjectFloatMap<K> toImmutable()
    {
        return ObjectFloatMaps.immutable.withAll(this);
    }

    @Override
    public float get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public float getOrThrow(Object key)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]))
        {
            return this.values[index];
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public float getIfAbsent(Object key, float ifAbsent)
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
    public boolean containsValue(float value)
    {
        for (int i = 0; i < this.values.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && Float.compare(this.values[i], value) == 0)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void each(FloatProcedure procedure)
    {
        this.forEachValue(procedure);
    }

    @Override
    public void forEachValue(FloatProcedure procedure)
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
    public void forEachKeyValue(ObjectFloatProcedure<? super K> procedure)
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
    public ObjectFloatHashMapWithHashingStrategy<K> select(ObjectFloatPredicate<? super K> predicate)
    {
        ObjectFloatHashMapWithHashingStrategy<K> result = ObjectFloatHashMapWithHashingStrategy.newMap(this.hashingStrategy);

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
    public ObjectFloatHashMapWithHashingStrategy<K> reject(ObjectFloatPredicate<? super K> predicate)
    {
        ObjectFloatHashMapWithHashingStrategy<K> result = ObjectFloatHashMapWithHashingStrategy.newMap(this.hashingStrategy);

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
    public MutableFloatCollection select(FloatPredicate predicate)
    {
        MutableFloatList result = FloatLists.mutable.empty();

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
    public MutableFloatCollection reject(FloatPredicate predicate)
    {
        MutableFloatList result = FloatLists.mutable.empty();

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
    public float detectIfNone(FloatPredicate predicate, float ifNone)
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
    public <V> MutableCollection<V> collect(FloatToObjectFunction<? extends V> function)
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
    public int count(FloatPredicate predicate)
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
    public boolean anySatisfy(FloatPredicate predicate)
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
    public boolean allSatisfy(FloatPredicate predicate)
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
    public boolean noneSatisfy(FloatPredicate predicate)
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
    public <V> V injectInto(V injectedValue, ObjectFloatToObjectFunction<? super V, ? extends V> function)
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
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<FloatIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            FloatIterator iterator = this.floatIterator();
            while (iterator.hasNext())
            {
                MutableFloatBag batch = FloatBags.mutable.empty();
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
    public double sum()
    {
        double result = 0.0;
        double compensation = 0.0;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                double adjustedValue = this.values[i] - compensation;
                double nextSum = result + adjustedValue;
                compensation = nextSum - result - adjustedValue;
                result = nextSum;
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
        float max = 0.0f;
        boolean isMaxSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMaxSet || Float.compare(max, this.values[i]) < 0))
            {
                max = this.values[i];
                isMaxSet = true;
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
        float min = 0.0f;
        boolean isMinSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMinSet || Float.compare(this.values[i], min) < 0))
            {
                min = this.values[i];
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        float max = 0.0f;
        boolean isMaxSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMaxSet || Float.compare(max, this.values[i]) < 0))
            {
                max = this.values[i];
                isMaxSet = true;
            }
        }
        return max;
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        float min = 0.0f;
        boolean isMinSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMinSet || Float.compare(this.values[i], min) < 0))
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
        return this.sum() / (double) this.size();
    }

    @Override
    public double median()
    {
        if (this.isEmpty())
        {
            throw new ArithmeticException();
        }
        float[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            float first = sortedArray[middleIndex];
            float second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public MutableFloatList toList()
    {
        MutableFloatList result = FloatLists.mutable.withInitialCapacity(this.size());

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
    public MutableFloatSet toSet()
    {
        MutableFloatSet result = FloatSets.mutable.empty();

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
    public MutableFloatBag toBag()
    {
        MutableFloatBag result = FloatBags.mutable.empty();

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
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public float[] toSortedArray()
    {
        float[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableFloatList toSortedList()
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
                out.writeFloat(this.values[i]);
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
            this.put((K) in.readObject(), in.readFloat());
        }
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ObjectFloatPair<K>> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableFloatObjectMap<K> flipUniqueValues()
    {
        MutableFloatObjectMap<K> result = FloatObjectMaps.mutable.empty();
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
        float[] oldValues = this.values;
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
        this.values = new float[sizeToAllocate];
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

    private class InternalFloatIterator implements MutableFloatIterator
    {
        private int count;
        private int position;

        @Override
        public boolean hasNext()
        {
            return this.count != ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public float next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }

            Object[] keys = ObjectFloatHashMapWithHashingStrategy.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            float result = ObjectFloatHashMapWithHashingStrategy.this.values[this.position];
            this.count++;
            this.position++;
            return result;
        }

        @Override
        public void remove()
        {
            if (this.position == 0 || !isNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[this.position - 1]))
            {
                throw new IllegalStateException();
            }
            ObjectFloatHashMapWithHashingStrategy.this.remove(ObjectFloatHashMapWithHashingStrategy.this.keys[this.position - 1]);
            this.count--;
        }
    }

    @Override
    public Set<K> keySet()
    {
        return new KeySet();
    }

    @Override
    public MutableFloatCollection values()
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
            Object[] table = ObjectFloatHashMapWithHashingStrategy.this.keys;
            for (int i = 0; i < table.length; i++)
            {
                Object key = table[i];
                if (ObjectFloatHashMapWithHashingStrategy.isNonSentinel(key))
                {
                    K nonSentinelKey = ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(key);
                    hashCode += nonSentinelKey == null ? 0 : ObjectFloatHashMapWithHashingStrategy.this.hashingStrategy.computeHashCode(nonSentinelKey);
                }
            }
            return hashCode;
        }

        @Override
        public int size()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.containsKey(o);
        }

        @Override
        public Object[] toArray()
        {
            int size = ObjectFloatHashMapWithHashingStrategy.this.size();
            Object[] result = new Object[size];
            this.copyKeys(result);
            return result;
        }

        @Override
        public <T> T[] toArray(T[] result)
        {
            int size = ObjectFloatHashMapWithHashingStrategy.this.size();
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
            int oldSize = ObjectFloatHashMapWithHashingStrategy.this.size();
            ObjectFloatHashMapWithHashingStrategy.this.removeKey((K) key);
            return oldSize != ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean containsAll(Collection<?> collection)
        {
            for (Object aCollection : collection)
            {
                if (!ObjectFloatHashMapWithHashingStrategy.this.containsKey(aCollection))
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
            int oldSize = ObjectFloatHashMapWithHashingStrategy.this.size();
            Iterator<K> iterator = this.iterator();
            while (iterator.hasNext())
            {
                K next = iterator.next();
                if (!collection.contains(next))
                {
                    iterator.remove();
                }
            }
            return oldSize != ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            int oldSize = ObjectFloatHashMapWithHashingStrategy.this.size();
            for (Object object : collection)
            {
                ObjectFloatHashMapWithHashingStrategy.this.removeKey((K) object);
            }
            return oldSize != ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public void clear()
        {
            ObjectFloatHashMapWithHashingStrategy.this.clear();
        }

        @Override
        public Iterator<K> iterator()
        {
            return new KeySetIterator();
        }

        private void copyKeys(Object[] result)
        {
            int count = 0;
            for (int i = 0; i < ObjectFloatHashMapWithHashingStrategy.this.keys.length; i++)
            {
                Object key = ObjectFloatHashMapWithHashingStrategy.this.keys[i];
                if (ObjectFloatHashMapWithHashingStrategy.isNonSentinel(key))
                {
                    result[count++] = ObjectFloatHashMapWithHashingStrategy.this.keys[i];
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
            return this.count < ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public K next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            this.count++;
            Object[] keys = ObjectFloatHashMapWithHashingStrategy.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.currentKey = (K) ObjectFloatHashMapWithHashingStrategy.this.keys[this.position];
            this.isCurrentKeySet = true;
            this.position++;
            return ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(this.currentKey);
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
                ObjectFloatHashMapWithHashingStrategy.this.removeKeyAtIndex(ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(this.currentKey), index);
            }
            else
            {
                ObjectFloatHashMapWithHashingStrategy.this.removeKey(this.currentKey);
            }
        }
    }

    private class ValuesCollection implements MutableFloatCollection
    {
        @Override
        public int size()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.notEmpty();
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

                for (int i = 0; i < ObjectFloatHashMapWithHashingStrategy.this.keys.length; i++)
                {
                    Object key = ObjectFloatHashMapWithHashingStrategy.this.keys[i];
                    if (isNonSentinel(key))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ObjectFloatHashMapWithHashingStrategy.this.values[i]));
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
        public boolean add(float element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(float... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(FloatIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean remove(float item)
        {
            int oldSize = ObjectFloatHashMapWithHashingStrategy.this.size();

            for (int i = 0; i < ObjectFloatHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (isNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]) && Float.compare(item, ObjectFloatHashMapWithHashingStrategy.this.values[i]) == 0)
                {
                    ObjectFloatHashMapWithHashingStrategy.this.removeKey((K) ObjectFloatHashMapWithHashingStrategy.this.keys[i]);
                }
            }
            return oldSize != ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(FloatIterable source)
        {
            int oldSize = ObjectFloatHashMapWithHashingStrategy.this.size();

            FloatIterator iterator = source.floatIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(float... source)
        {
            int oldSize = ObjectFloatHashMapWithHashingStrategy.this.size();

            for (float item : source)
            {
                this.remove(item);
            }
            return oldSize != ObjectFloatHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean retainAll(FloatIterable source)
        {
            int oldSize = ObjectFloatHashMapWithHashingStrategy.this.size();
            final FloatSet sourceSet = source instanceof FloatSet ? (FloatSet) source : source.toSet();
            ObjectFloatHashMapWithHashingStrategy<K> retained = ObjectFloatHashMapWithHashingStrategy.this.select((K object, float value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ObjectFloatHashMapWithHashingStrategy.this.keys = retained.keys;
                ObjectFloatHashMapWithHashingStrategy.this.values = retained.values;
                ObjectFloatHashMapWithHashingStrategy.this.occupiedWithData = retained.occupiedWithData;
                ObjectFloatHashMapWithHashingStrategy.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(float... source)
        {
            return this.retainAll(FloatSets.mutable.with(source));
        }

        @Override
        public void clear()
        {
            ObjectFloatHashMapWithHashingStrategy.this.clear();
        }

        @Override
        public MutableFloatCollection with(float element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableFloatCollection without(float element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableFloatCollection withAll(FloatIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableFloatCollection withoutAll(FloatIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableFloatCollection asUnmodifiable()
        {
            return UnmodifiableFloatCollection.of(this);
        }

        @Override
        public MutableFloatCollection asSynchronized()
        {
            return SynchronizedFloatCollection.of(this);
        }

        @Override
        public ImmutableFloatCollection toImmutable()
        {
            return FloatLists.immutable.withAll(this);
        }

        @Override
        public MutableFloatIterator floatIterator()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.floatIterator();
        }

        @Override
        public float[] toArray()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.toArray();
        }

        @Override
        public float[] toArray(float[] target)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.toArray(target);
        }

        @Override
        public boolean contains(float value)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.containsValue(value);
        }

        @Override
        public boolean containsAll(float... source)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.containsAll(source);
        }

        @Override
        public boolean containsAll(FloatIterable source)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.containsAll(source);
        }

        @Override
        public void forEach(FloatProcedure procedure)
        {
            ObjectFloatHashMapWithHashingStrategy.this.forEach(procedure);
        }

        @Override
        public void each(FloatProcedure procedure)
        {
            this.forEach(procedure);
        }

        @Override
        public MutableFloatCollection select(FloatPredicate predicate)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.select(predicate);
        }

        @Override
        public MutableFloatCollection reject(FloatPredicate predicate)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.reject(predicate);
        }

        @Override
        public <V> MutableCollection<V> collect(FloatToObjectFunction<? extends V> function)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<FloatIterable> chunk(int size)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.chunk(size);
        }

        @Override
        public float detectIfNone(FloatPredicate predicate, float ifNone)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public int count(FloatPredicate predicate)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(FloatPredicate predicate)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(FloatPredicate predicate)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(FloatPredicate predicate)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.noneSatisfy(predicate);
        }

        @Override
        public MutableFloatList toList()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.toList();
        }

        @Override
        public MutableFloatSet toSet()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.toSet();
        }

        @Override
        public MutableFloatBag toBag()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.toBag();
        }

        @Override
        public LazyFloatIterable asLazy()
        {
            return new LazyFloatIterableAdapter(this);
        }

        @Override
        public float[] toSortedArray()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.toSortedArray();
        }

        @Override
        public MutableFloatList toSortedList()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.toSortedList();
        }

        @Override
        public double sum()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.sum();
        }

        @Override
        public float max()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.max();
        }

        @Override
        public float maxIfEmpty(float defaultValue)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.maxIfEmpty(defaultValue);
        }

        @Override
        public float min()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.min();
        }

        @Override
        public float minIfEmpty(float defaultValue)
        {
            return ObjectFloatHashMapWithHashingStrategy.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.average();
        }

        @Override
        public double median()
        {
            return ObjectFloatHashMapWithHashingStrategy.this.median();
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableFloatCollection newEmpty()
        {
            return FloatBags.mutable.empty();
        }
    }

    private class KeysView extends AbstractLazyIterable<K>
    {
        @Override
        public void each(Procedure<? super K> procedure)
        {
            ObjectFloatHashMapWithHashingStrategy.this.forEachKey(procedure);
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super K> objectFloatProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectFloatHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectFloatHashMapWithHashingStrategy.isNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]))
                {
                    objectFloatProcedure.value(ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super K, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectFloatHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectFloatHashMapWithHashingStrategy.isNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]), parameter);
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

                Object[] keys = ObjectFloatHashMapWithHashingStrategy.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                K result = ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[this.position]);
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
                return this.count != ObjectFloatHashMapWithHashingStrategy.this.size();
            }
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<ObjectFloatPair<K>>
    {
        @Override
        public void each(Procedure<? super ObjectFloatPair<K>> procedure)
        {
            for (int i = 0; i < ObjectFloatHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectFloatHashMapWithHashingStrategy.isNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]), ObjectFloatHashMapWithHashingStrategy.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ObjectFloatPair<K>> objectIntProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectFloatHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectFloatHashMapWithHashingStrategy.isNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]), ObjectFloatHashMapWithHashingStrategy.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ObjectFloatPair<K>, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectFloatHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectFloatHashMapWithHashingStrategy.isNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[i]), ObjectFloatHashMapWithHashingStrategy.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<ObjectFloatPair<K>> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ObjectFloatPair<K>>
        {
            private int count;
            private int position;

            @Override
            public ObjectFloatPair<K> next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException();
                }

                Object[] keys = ObjectFloatHashMapWithHashingStrategy.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                ObjectFloatPair<K> result = PrimitiveTuples.pair(ObjectFloatHashMapWithHashingStrategy.this.toNonSentinel(ObjectFloatHashMapWithHashingStrategy.this.keys[this.position]), ObjectFloatHashMapWithHashingStrategy.this.values[this.position]);
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
                return this.count != ObjectFloatHashMapWithHashingStrategy.this.size();
            }
        }
    }
}
