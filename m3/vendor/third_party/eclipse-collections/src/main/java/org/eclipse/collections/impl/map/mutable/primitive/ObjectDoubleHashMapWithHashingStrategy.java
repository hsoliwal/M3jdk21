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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction0;
import org.eclipse.collections.api.block.function.primitive.DoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectDoublePredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectDoubleProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.DoubleBags;
import org.eclipse.collections.api.factory.primitive.DoubleLists;
import org.eclipse.collections.api.factory.primitive.DoubleSets;
import org.eclipse.collections.api.factory.primitive.ObjectDoubleMaps;
import org.eclipse.collections.api.factory.primitive.DoubleObjectMaps;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectDoubleMap;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.ObjectDoublePair;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedDoubleCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableDoubleCollection;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file objectPrimitiveHashMapWithHashingStrategy.stg.
 *
 * @since 7.0.
 */
public class ObjectDoubleHashMapWithHashingStrategy<K> implements MutableObjectDoubleMap<K>, Externalizable
{
    public static final double EMPTY_VALUE = 0.0;

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
            return "ObjectDoubleHashMapWithHashingStrategy.NULL_KEY";
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
            return "ObjectDoubleHashMapWithHashingStrategy.REMOVED_KEY";
        }
    };

    private Object[] keys;
    private double[] values;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    private HashingStrategy<? super K> hashingStrategy;

    /**
     * @deprecated Use ObjectDoubleHashMapWithHashingStrategy(HashingStrategy) instead.
     */
    @Deprecated
    public ObjectDoubleHashMapWithHashingStrategy()
    {
    }

    public ObjectDoubleHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy)
    {
        this.hashingStrategy = hashingStrategy;
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ObjectDoubleHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        this.hashingStrategy = hashingStrategy;
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity << 1));
        this.allocateTable(capacity);
    }

    public ObjectDoubleHashMapWithHashingStrategy(HashingStrategy<? super K> hashingStrategy, ObjectDoubleMap<? extends K> map)
    {
        this(hashingStrategy, Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }

    public static <K> ObjectDoubleHashMapWithHashingStrategy<K> newMap(HashingStrategy<? super K> hashingStrategy)
    {
        return new ObjectDoubleHashMapWithHashingStrategy<>(hashingStrategy);
    }

    public static <K> ObjectDoubleHashMapWithHashingStrategy<K> newMap(HashingStrategy<? super K> hashingStrategy, ObjectDoubleMap<K> map)
    {
        return new ObjectDoubleHashMapWithHashingStrategy<>(hashingStrategy, map);
    }

    public static <K> ObjectDoubleHashMapWithHashingStrategy<K> newMap(ObjectDoubleHashMapWithHashingStrategy<K> map)
    {
        return new ObjectDoubleHashMapWithHashingStrategy<>(map.hashingStrategy, map);
    }

    public static <K> ObjectDoubleHashMapWithHashingStrategy<K> newMapWithInitialCapacity(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        return new ObjectDoubleHashMapWithHashingStrategy<>(hashingStrategy, initialCapacity);
    }

    public static <K> ObjectDoubleHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, double value1)
    {
        return new ObjectDoubleHashMapWithHashingStrategy<K>(hashingStrategy, 1).withKeyValue(key1, value1);
    }

    public static <K> ObjectDoubleHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, double value1, K key2, double value2)
    {
        return new ObjectDoubleHashMapWithHashingStrategy<K>(hashingStrategy, 2).withKeysValues(key1, value1, key2, value2);
    }

    public static <K> ObjectDoubleHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, double value1, K key2, double value2, K key3, double value3)
    {
        return new ObjectDoubleHashMapWithHashingStrategy<K>(hashingStrategy, 3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static <K> ObjectDoubleHashMapWithHashingStrategy<K> newWithKeysValues(HashingStrategy<? super K> hashingStrategy, K key1, double value1, K key2, double value2, K key3, double value3, K key4, double value4)
    {
        return new ObjectDoubleHashMapWithHashingStrategy<K>(hashingStrategy, 4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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

        if (!(obj instanceof ObjectDoubleMap))
        {
            return false;
        }

        ObjectDoubleMap<K> other = (ObjectDoubleMap<K>) obj;

        if (this.size() != other.size())
        {
            return false;
        }

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!other.containsKey(this.toNonSentinel(this.keys[i])) || Double.compare(this.values[i], other.getOrThrow(this.toNonSentinel(this.keys[i]))) != 0))
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
                result += this.hashingStrategy.computeHashCode(this.toNonSentinel(this.keys[i])) ^ (int) (Double.doubleToLongBits(this.values[i]) ^ Double.doubleToLongBits(this.values[i]) >>> 32);
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
    public MutableDoubleIterator doubleIterator()
    {
        return new InternalDoubleIterator();
    }

    @Override
    public double[] toArray()
    {
        double[] result = new double[this.size()];
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
    public double[] toArray(double[] target)
    {
        if (target.length < this.size())
        {
            target = new double[this.size()];
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
    public boolean contains(double value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(double... source)
    {
        for (double item : source)
        {
            if (!this.containsValue(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(DoubleIterable source)
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
    public void put(K key, double value)
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
    public void putAll(ObjectDoubleMap<? extends K> map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ObjectDoubleToDoubleFunction<? super K> function)
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
    public double removeKeyIfAbsent(K key, double value)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.keys[index] = REMOVED_KEY;
            double oldValue = this.values[index];
            this.values[index] = EMPTY_VALUE;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;

            return oldValue;
        }
        return value;
    }

    @Override
    public double getIfAbsentPut(K key, double value)
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
    public double getAndPut(K key, double putValue, double defaultValue)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            double existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public double getIfAbsentPut(K key, DoubleFunction0 function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        double value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> double getIfAbsentPutWith(K key, DoubleFunction<? super P> function, P parameter)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        double value = function.doubleValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public double getIfAbsentPutWithKey(K key, DoubleFunction<? super K> function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        double value = function.doubleValueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public double updateValue(K key, double initialValueIfAbsent, DoubleToDoubleFunction function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && this.nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        double value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(K key, double value, int index)
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
    public double addToValue(K key, double toBeAdded)
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
    public ObjectDoubleHashMapWithHashingStrategy<K> withKeyValue(K key1, double value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ObjectDoubleHashMapWithHashingStrategy<K> withKeysValues(K key1, double value1, K key2, double value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ObjectDoubleHashMapWithHashingStrategy<K> withKeysValues(K key1, double value1, K key2, double value2, K key3, double value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ObjectDoubleHashMapWithHashingStrategy<K> withKeysValues(K key1, double value1, K key2, double value2, K key3, double value3, K key4, double value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ObjectDoubleHashMapWithHashingStrategy<K> withoutKey(K key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ObjectDoubleHashMapWithHashingStrategy<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        for (K key : keys)
        {
            this.removeKey(key);
        }
        return this;
    }

    @Override
    public MutableObjectDoubleMap<K> asUnmodifiable()
    {
        return new UnmodifiableObjectDoubleMap<>(this);
    }

    @Override
    public MutableObjectDoubleMap<K> asSynchronized()
    {
        return new SynchronizedObjectDoubleMap<>(this);
    }

    @Override
    public ImmutableObjectDoubleMap<K> toImmutable()
    {
        return ObjectDoubleMaps.immutable.withAll(this);
    }

    @Override
    public double get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public double getOrThrow(Object key)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]))
        {
            return this.values[index];
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public double getIfAbsent(Object key, double ifAbsent)
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
    public boolean containsValue(double value)
    {
        for (int i = 0; i < this.values.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && Double.compare(this.values[i], value) == 0)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void each(DoubleProcedure procedure)
    {
        this.forEachValue(procedure);
    }

    @Override
    public void forEachValue(DoubleProcedure procedure)
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
    public void forEachKeyValue(ObjectDoubleProcedure<? super K> procedure)
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
    public ObjectDoubleHashMapWithHashingStrategy<K> select(ObjectDoublePredicate<? super K> predicate)
    {
        ObjectDoubleHashMapWithHashingStrategy<K> result = ObjectDoubleHashMapWithHashingStrategy.newMap(this.hashingStrategy);

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
    public ObjectDoubleHashMapWithHashingStrategy<K> reject(ObjectDoublePredicate<? super K> predicate)
    {
        ObjectDoubleHashMapWithHashingStrategy<K> result = ObjectDoubleHashMapWithHashingStrategy.newMap(this.hashingStrategy);

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
    public MutableDoubleCollection select(DoublePredicate predicate)
    {
        MutableDoubleList result = DoubleLists.mutable.empty();

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
    public MutableDoubleCollection reject(DoublePredicate predicate)
    {
        MutableDoubleList result = DoubleLists.mutable.empty();

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
    public double detectIfNone(DoublePredicate predicate, double ifNone)
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
    public <V> MutableCollection<V> collect(DoubleToObjectFunction<? extends V> function)
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
    public int count(DoublePredicate predicate)
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
    public boolean anySatisfy(DoublePredicate predicate)
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
    public boolean allSatisfy(DoublePredicate predicate)
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
    public boolean noneSatisfy(DoublePredicate predicate)
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
    public <V> V injectInto(V injectedValue, ObjectDoubleToObjectFunction<? super V, ? extends V> function)
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
    public RichIterable<DoubleIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<DoubleIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            DoubleIterator iterator = this.doubleIterator();
            while (iterator.hasNext())
            {
                MutableDoubleBag batch = DoubleBags.mutable.empty();
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
    public double max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        double max = 0.0;
        boolean isMaxSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMaxSet || Double.compare(max, this.values[i]) < 0))
            {
                max = this.values[i];
                isMaxSet = true;
            }
        }
        return max;
    }

    @Override
    public double min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        double min = 0.0;
        boolean isMinSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMinSet || Double.compare(this.values[i], min) < 0))
            {
                min = this.values[i];
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        double max = 0.0;
        boolean isMaxSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMaxSet || Double.compare(max, this.values[i]) < 0))
            {
                max = this.values[i];
                isMaxSet = true;
            }
        }
        return max;
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        double min = 0.0;
        boolean isMinSet = false;

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMinSet || Double.compare(this.values[i], min) < 0))
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
        double[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            double first = sortedArray[middleIndex];
            double second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public MutableDoubleList toList()
    {
        MutableDoubleList result = DoubleLists.mutable.withInitialCapacity(this.size());

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
    public MutableDoubleSet toSet()
    {
        MutableDoubleSet result = DoubleSets.mutable.empty();

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
    public MutableDoubleBag toBag()
    {
        MutableDoubleBag result = DoubleBags.mutable.empty();

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
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public double[] toSortedArray()
    {
        double[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableDoubleList toSortedList()
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
                out.writeDouble(this.values[i]);
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
            this.put((K) in.readObject(), in.readDouble());
        }
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ObjectDoublePair<K>> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableDoubleObjectMap<K> flipUniqueValues()
    {
        MutableDoubleObjectMap<K> result = DoubleObjectMaps.mutable.empty();
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
        double[] oldValues = this.values;
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
        this.values = new double[sizeToAllocate];
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

    private class InternalDoubleIterator implements MutableDoubleIterator
    {
        private int count;
        private int position;

        @Override
        public boolean hasNext()
        {
            return this.count != ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public double next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }

            Object[] keys = ObjectDoubleHashMapWithHashingStrategy.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            double result = ObjectDoubleHashMapWithHashingStrategy.this.values[this.position];
            this.count++;
            this.position++;
            return result;
        }

        @Override
        public void remove()
        {
            if (this.position == 0 || !isNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[this.position - 1]))
            {
                throw new IllegalStateException();
            }
            ObjectDoubleHashMapWithHashingStrategy.this.remove(ObjectDoubleHashMapWithHashingStrategy.this.keys[this.position - 1]);
            this.count--;
        }
    }

    @Override
    public Set<K> keySet()
    {
        return new KeySet();
    }

    @Override
    public MutableDoubleCollection values()
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
            Object[] table = ObjectDoubleHashMapWithHashingStrategy.this.keys;
            for (int i = 0; i < table.length; i++)
            {
                Object key = table[i];
                if (ObjectDoubleHashMapWithHashingStrategy.isNonSentinel(key))
                {
                    K nonSentinelKey = ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(key);
                    hashCode += nonSentinelKey == null ? 0 : ObjectDoubleHashMapWithHashingStrategy.this.hashingStrategy.computeHashCode(nonSentinelKey);
                }
            }
            return hashCode;
        }

        @Override
        public int size()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.containsKey(o);
        }

        @Override
        public Object[] toArray()
        {
            int size = ObjectDoubleHashMapWithHashingStrategy.this.size();
            Object[] result = new Object[size];
            this.copyKeys(result);
            return result;
        }

        @Override
        public <T> T[] toArray(T[] result)
        {
            int size = ObjectDoubleHashMapWithHashingStrategy.this.size();
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
            int oldSize = ObjectDoubleHashMapWithHashingStrategy.this.size();
            ObjectDoubleHashMapWithHashingStrategy.this.removeKey((K) key);
            return oldSize != ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean containsAll(Collection<?> collection)
        {
            for (Object aCollection : collection)
            {
                if (!ObjectDoubleHashMapWithHashingStrategy.this.containsKey(aCollection))
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
            int oldSize = ObjectDoubleHashMapWithHashingStrategy.this.size();
            Iterator<K> iterator = this.iterator();
            while (iterator.hasNext())
            {
                K next = iterator.next();
                if (!collection.contains(next))
                {
                    iterator.remove();
                }
            }
            return oldSize != ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            int oldSize = ObjectDoubleHashMapWithHashingStrategy.this.size();
            for (Object object : collection)
            {
                ObjectDoubleHashMapWithHashingStrategy.this.removeKey((K) object);
            }
            return oldSize != ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public void clear()
        {
            ObjectDoubleHashMapWithHashingStrategy.this.clear();
        }

        @Override
        public Iterator<K> iterator()
        {
            return new KeySetIterator();
        }

        private void copyKeys(Object[] result)
        {
            int count = 0;
            for (int i = 0; i < ObjectDoubleHashMapWithHashingStrategy.this.keys.length; i++)
            {
                Object key = ObjectDoubleHashMapWithHashingStrategy.this.keys[i];
                if (ObjectDoubleHashMapWithHashingStrategy.isNonSentinel(key))
                {
                    result[count++] = ObjectDoubleHashMapWithHashingStrategy.this.keys[i];
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
            return this.count < ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public K next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            this.count++;
            Object[] keys = ObjectDoubleHashMapWithHashingStrategy.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.currentKey = (K) ObjectDoubleHashMapWithHashingStrategy.this.keys[this.position];
            this.isCurrentKeySet = true;
            this.position++;
            return ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(this.currentKey);
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
                ObjectDoubleHashMapWithHashingStrategy.this.removeKeyAtIndex(ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(this.currentKey), index);
            }
            else
            {
                ObjectDoubleHashMapWithHashingStrategy.this.removeKey(this.currentKey);
            }
        }
    }

    private class ValuesCollection implements MutableDoubleCollection
    {
        @Override
        public int size()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.notEmpty();
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

                for (int i = 0; i < ObjectDoubleHashMapWithHashingStrategy.this.keys.length; i++)
                {
                    Object key = ObjectDoubleHashMapWithHashingStrategy.this.keys[i];
                    if (isNonSentinel(key))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ObjectDoubleHashMapWithHashingStrategy.this.values[i]));
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
        public boolean add(double element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(double... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(DoubleIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean remove(double item)
        {
            int oldSize = ObjectDoubleHashMapWithHashingStrategy.this.size();

            for (int i = 0; i < ObjectDoubleHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (isNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]) && Double.compare(item, ObjectDoubleHashMapWithHashingStrategy.this.values[i]) == 0)
                {
                    ObjectDoubleHashMapWithHashingStrategy.this.removeKey((K) ObjectDoubleHashMapWithHashingStrategy.this.keys[i]);
                }
            }
            return oldSize != ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(DoubleIterable source)
        {
            int oldSize = ObjectDoubleHashMapWithHashingStrategy.this.size();

            DoubleIterator iterator = source.doubleIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean removeAll(double... source)
        {
            int oldSize = ObjectDoubleHashMapWithHashingStrategy.this.size();

            for (double item : source)
            {
                this.remove(item);
            }
            return oldSize != ObjectDoubleHashMapWithHashingStrategy.this.size();
        }

        @Override
        public boolean retainAll(DoubleIterable source)
        {
            int oldSize = ObjectDoubleHashMapWithHashingStrategy.this.size();
            final DoubleSet sourceSet = source instanceof DoubleSet ? (DoubleSet) source : source.toSet();
            ObjectDoubleHashMapWithHashingStrategy<K> retained = ObjectDoubleHashMapWithHashingStrategy.this.select((K object, double value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ObjectDoubleHashMapWithHashingStrategy.this.keys = retained.keys;
                ObjectDoubleHashMapWithHashingStrategy.this.values = retained.values;
                ObjectDoubleHashMapWithHashingStrategy.this.occupiedWithData = retained.occupiedWithData;
                ObjectDoubleHashMapWithHashingStrategy.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(double... source)
        {
            return this.retainAll(DoubleSets.mutable.with(source));
        }

        @Override
        public void clear()
        {
            ObjectDoubleHashMapWithHashingStrategy.this.clear();
        }

        @Override
        public MutableDoubleCollection with(double element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableDoubleCollection without(double element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableDoubleCollection withAll(DoubleIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableDoubleCollection withoutAll(DoubleIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableDoubleCollection asUnmodifiable()
        {
            return UnmodifiableDoubleCollection.of(this);
        }

        @Override
        public MutableDoubleCollection asSynchronized()
        {
            return SynchronizedDoubleCollection.of(this);
        }

        @Override
        public ImmutableDoubleCollection toImmutable()
        {
            return DoubleLists.immutable.withAll(this);
        }

        @Override
        public MutableDoubleIterator doubleIterator()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.doubleIterator();
        }

        @Override
        public double[] toArray()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.toArray();
        }

        @Override
        public double[] toArray(double[] target)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.toArray(target);
        }

        @Override
        public boolean contains(double value)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.containsValue(value);
        }

        @Override
        public boolean containsAll(double... source)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.containsAll(source);
        }

        @Override
        public boolean containsAll(DoubleIterable source)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.containsAll(source);
        }

        @Override
        public void forEach(DoubleProcedure procedure)
        {
            ObjectDoubleHashMapWithHashingStrategy.this.forEach(procedure);
        }

        @Override
        public void each(DoubleProcedure procedure)
        {
            this.forEach(procedure);
        }

        @Override
        public MutableDoubleCollection select(DoublePredicate predicate)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.select(predicate);
        }

        @Override
        public MutableDoubleCollection reject(DoublePredicate predicate)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.reject(predicate);
        }

        @Override
        public <V> MutableCollection<V> collect(DoubleToObjectFunction<? extends V> function)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<DoubleIterable> chunk(int size)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.chunk(size);
        }

        @Override
        public double detectIfNone(DoublePredicate predicate, double ifNone)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public int count(DoublePredicate predicate)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(DoublePredicate predicate)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(DoublePredicate predicate)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(DoublePredicate predicate)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.noneSatisfy(predicate);
        }

        @Override
        public MutableDoubleList toList()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.toList();
        }

        @Override
        public MutableDoubleSet toSet()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.toSet();
        }

        @Override
        public MutableDoubleBag toBag()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.toBag();
        }

        @Override
        public LazyDoubleIterable asLazy()
        {
            return new LazyDoubleIterableAdapter(this);
        }

        @Override
        public double[] toSortedArray()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.toSortedArray();
        }

        @Override
        public MutableDoubleList toSortedList()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.toSortedList();
        }

        @Override
        public double sum()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.sum();
        }

        @Override
        public double max()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.max();
        }

        @Override
        public double maxIfEmpty(double defaultValue)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.maxIfEmpty(defaultValue);
        }

        @Override
        public double min()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.min();
        }

        @Override
        public double minIfEmpty(double defaultValue)
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.average();
        }

        @Override
        public double median()
        {
            return ObjectDoubleHashMapWithHashingStrategy.this.median();
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableDoubleCollection newEmpty()
        {
            return DoubleBags.mutable.empty();
        }
    }

    private class KeysView extends AbstractLazyIterable<K>
    {
        @Override
        public void each(Procedure<? super K> procedure)
        {
            ObjectDoubleHashMapWithHashingStrategy.this.forEachKey(procedure);
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super K> objectDoubleProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectDoubleHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectDoubleHashMapWithHashingStrategy.isNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]))
                {
                    objectDoubleProcedure.value(ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super K, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectDoubleHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectDoubleHashMapWithHashingStrategy.isNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]), parameter);
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

                Object[] keys = ObjectDoubleHashMapWithHashingStrategy.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                K result = ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[this.position]);
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
                return this.count != ObjectDoubleHashMapWithHashingStrategy.this.size();
            }
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<ObjectDoublePair<K>>
    {
        @Override
        public void each(Procedure<? super ObjectDoublePair<K>> procedure)
        {
            for (int i = 0; i < ObjectDoubleHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectDoubleHashMapWithHashingStrategy.isNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]), ObjectDoubleHashMapWithHashingStrategy.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ObjectDoublePair<K>> objectIntProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectDoubleHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectDoubleHashMapWithHashingStrategy.isNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]), ObjectDoubleHashMapWithHashingStrategy.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ObjectDoublePair<K>, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectDoubleHashMapWithHashingStrategy.this.keys.length; i++)
            {
                if (ObjectDoubleHashMapWithHashingStrategy.isNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[i]), ObjectDoubleHashMapWithHashingStrategy.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<ObjectDoublePair<K>> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ObjectDoublePair<K>>
        {
            private int count;
            private int position;

            @Override
            public ObjectDoublePair<K> next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException();
                }

                Object[] keys = ObjectDoubleHashMapWithHashingStrategy.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                ObjectDoublePair<K> result = PrimitiveTuples.pair(ObjectDoubleHashMapWithHashingStrategy.this.toNonSentinel(ObjectDoubleHashMapWithHashingStrategy.this.keys[this.position]), ObjectDoubleHashMapWithHashingStrategy.this.values[this.position]);
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
                return this.count != ObjectDoubleHashMapWithHashingStrategy.this.size();
            }
        }
    }
}
