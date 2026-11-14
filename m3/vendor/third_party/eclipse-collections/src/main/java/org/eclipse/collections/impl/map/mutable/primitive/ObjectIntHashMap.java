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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction0;
import org.eclipse.collections.api.block.function.primitive.IntToIntFunction;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToIntFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.IntBags;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedIntCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableIntCollection;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file objectPrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class ObjectIntHashMap<K> implements MutableObjectIntMap<K>, Externalizable
{
    public static final int EMPTY_VALUE = 0;

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
            return "ObjectIntHashMap.NULL_KEY";
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
            return "ObjectIntHashMap.REMOVED_KEY";
        }
    };

    private Object[] keys;
    private int[] values;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    public ObjectIntHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ObjectIntHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity << 1));
        this.allocateTable(capacity);
    }

    public ObjectIntHashMap(ObjectIntMap<? extends K> map)
    {
        this(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }

    public static <K> ObjectIntHashMap<K> newMap()
    {
        return new ObjectIntHashMap<>();
    }

    public static <K> ObjectIntHashMap<K> newWithKeysValues(K key1, int value1)
    {
        return new ObjectIntHashMap<K>(1).withKeyValue(key1, value1);
    }

    public static <K> ObjectIntHashMap<K> newWithKeysValues(K key1, int value1, K key2, int value2)
    {
        return new ObjectIntHashMap<K>(2).withKeysValues(key1, value1, key2, value2);
    }

    public static <K> ObjectIntHashMap<K> newWithKeysValues(K key1, int value1, K key2, int value2, K key3, int value3)
    {
        return new ObjectIntHashMap<K>(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static <K> ObjectIntHashMap<K> newWithKeysValues(K key1, int value1, K key2, int value2, K key3, int value3, K key4, int value4)
    {
        return new ObjectIntHashMap<K>(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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

        if (!(obj instanceof ObjectIntMap))
        {
            return false;
        }

        ObjectIntMap<K> other = (ObjectIntMap<K>) obj;

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
                result += (this.toNonSentinel(this.keys[i]) == null ? 0 : this.keys[i].hashCode()) ^ this.values[i];
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
    public MutableIntIterator intIterator()
    {
        return new InternalIntIterator();
    }

    @Override
    public int[] toArray()
    {
        int[] result = new int[this.size()];
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
    public int[] toArray(int[] target)
    {
        if (target.length < this.size())
        {
            target = new int[this.size()];
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
    public boolean contains(int value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(int... source)
    {
        for (int item : source)
        {
            if (!this.containsValue(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(IntIterable source)
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
    public void put(K key, int value)
    {
        int index = this.probe(key);

        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            // key already present in map
            this.values[index] = value;
            return;
        }

        this.addKeyValueAtIndex(key, value, index);
    }

    @Override
    public void putAll(ObjectIntMap<? extends K> map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ObjectIntToIntFunction<? super K> function)
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

    private void removeKeyAtIndex(K key, int index)
    {
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
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
    public int removeKeyIfAbsent(K key, int value)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.keys[index] = REMOVED_KEY;
            int oldValue = this.values[index];
            this.values[index] = EMPTY_VALUE;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;

            return oldValue;
        }
        return value;
    }

    @Override
    public int getIfAbsentPut(K key, int value)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public int getAndPut(K key, int putValue, int defaultValue)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            int existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public int getIfAbsentPut(K key, IntFunction0 function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        int value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> int getIfAbsentPutWith(K key, IntFunction<? super P> function, P parameter)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        int value = function.intValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public int getIfAbsentPutWithKey(K key, IntFunction<? super K> function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        int value = function.intValueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public int updateValue(K key, int initialValueIfAbsent, IntToIntFunction function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        int value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(K key, int value, int index)
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
    public int addToValue(K key, int toBeAdded)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.values[index] += toBeAdded;
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, toBeAdded, index);
        return toBeAdded;
    }

    @Override
    public ObjectIntHashMap<K> withKeyValue(K key1, int value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ObjectIntHashMap<K> withKeysValues(K key1, int value1, K key2, int value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ObjectIntHashMap<K> withKeysValues(K key1, int value1, K key2, int value2, K key3, int value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ObjectIntHashMap<K> withKeysValues(K key1, int value1, K key2, int value2, K key3, int value3, K key4, int value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ObjectIntHashMap<K> withoutKey(K key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ObjectIntHashMap<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        for (K key : keys)
        {
            this.removeKey(key);
        }
        return this;
    }

    @Override
    public MutableObjectIntMap<K> asUnmodifiable()
    {
        return new UnmodifiableObjectIntMap<>(this);
    }

    @Override
    public MutableObjectIntMap<K> asSynchronized()
    {
        return new SynchronizedObjectIntMap<>(this);
    }

    @Override
    public ImmutableObjectIntMap<K> toImmutable()
    {
        return ObjectIntMaps.immutable.withAll(this);
    }

    @Override
    public int get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public int getOrThrow(Object key)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]))
        {
            return this.values[index];
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public int getIfAbsent(Object key, int ifAbsent)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        return ifAbsent;
    }

    @Override
    public boolean containsKey(Object key)
    {
        int index = this.probe(key);
        return isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key);
    }

    @Override
    public boolean containsValue(int value)
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

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        this.forEachValue(procedure);
    }

    @Override
    public void forEachValue(IntProcedure procedure)
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
    public void forEachKeyValue(ObjectIntProcedure<? super K> procedure)
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
    public ObjectIntHashMap<K> select(ObjectIntPredicate<? super K> predicate)
    {
        ObjectIntHashMap<K> result = ObjectIntHashMap.newMap();

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
    public ObjectIntHashMap<K> reject(ObjectIntPredicate<? super K> predicate)
    {
        ObjectIntHashMap<K> result = ObjectIntHashMap.newMap();

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
    public MutableIntCollection select(IntPredicate predicate)
    {
        MutableIntList result = IntLists.mutable.empty();

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
    public MutableIntCollection reject(IntPredicate predicate)
    {
        MutableIntList result = IntLists.mutable.empty();

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
    public int detectIfNone(IntPredicate predicate, int ifNone)
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
    public <V> MutableCollection<V> collect(IntToObjectFunction<? extends V> function)
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
    public int count(IntPredicate predicate)
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
    public boolean anySatisfy(IntPredicate predicate)
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
    public boolean allSatisfy(IntPredicate predicate)
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
    public boolean noneSatisfy(IntPredicate predicate)
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
    public <V> V injectInto(V injectedValue, ObjectIntToObjectFunction<? super V, ? extends V> function)
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
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<IntIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            IntIterator iterator = this.intIterator();
            while (iterator.hasNext())
            {
                MutableIntBag batch = IntBags.mutable.empty();
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
    public int max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        int max = 0;
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
    public int min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        int min = 0;
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
    public int maxIfEmpty(int defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        int max = 0;
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
    public int minIfEmpty(int defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        int min = 0;
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
        int[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            int first = sortedArray[middleIndex];
            int second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public MutableIntList toList()
    {
        MutableIntList result = IntLists.mutable.withInitialCapacity(this.size());

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
    public MutableIntSet toSet()
    {
        MutableIntSet result = IntSets.mutable.empty();

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
    public MutableIntBag toBag()
    {
        MutableIntBag result = IntBags.mutable.empty();

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
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
    }

    @Override
    public int[] toSortedArray()
    {
        int[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableIntList toSortedList()
    {
        return this.toList().sortThis();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeObject(this.toNonSentinel(this.keys[i]));
                out.writeInt(this.values[i]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(size << 1));
        this.allocateTable(capacity);

        for (int i = 0; i < size; i++)
        {
            this.put((K) in.readObject(), in.readInt());
        }
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ObjectIntPair<K>> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableIntObjectMap<K> flipUniqueValues()
    {
        MutableIntObjectMap<K> result = IntObjectMaps.mutable.empty();
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
        int[] oldValues = this.values;
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

        else if (this.keys[index] == null || nullSafeEquals(this.toNonSentinel(this.keys[index]), element))
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
            else if (nullSafeEquals(this.toNonSentinel(this.keys[nextIndex]), element))
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
        int h = element == null ? 0 : element.hashCode();
        h ^= h >>> 20 ^ h >>> 12;
        h ^= h >>> 7 ^ h >>> 4;
        return h & (this.keys.length - 1);
    }

    private static boolean nullSafeEquals(Object value, Object other)
    {
        if (value == null)
        {
            if (other == null)
            {
                return true;
            }
        }
        else if (other == value || value.equals(other))
        {
            return true;
        }
        return false;
    }

    protected void allocateTable(int sizeToAllocate)
    {
        this.keys = new Object[sizeToAllocate];
        this.values = new int[sizeToAllocate];
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

    private class InternalIntIterator implements MutableIntIterator
    {
        private int count;
        private int position;

        @Override
        public boolean hasNext()
        {
            return this.count != ObjectIntHashMap.this.size();
        }

        @Override
        public int next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }

            Object[] keys = ObjectIntHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            int result = ObjectIntHashMap.this.values[this.position];
            this.count++;
            this.position++;
            return result;
        }

        @Override
        public void remove()
        {
            if (this.position == 0 || !isNonSentinel(ObjectIntHashMap.this.keys[this.position - 1]))
            {
                throw new IllegalStateException();
            }
            ObjectIntHashMap.this.remove(ObjectIntHashMap.this.keys[this.position - 1]);
            this.count--;
        }
    }

    @Override
    public Set<K> keySet()
    {
        return new KeySet();
    }

    @Override
    public MutableIntCollection values()
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
            Object[] table = ObjectIntHashMap.this.keys;
            for (int i = 0; i < table.length; i++)
            {
                Object key = table[i];
                if (ObjectIntHashMap.isNonSentinel(key))
                {
                    K nonSentinelKey = ObjectIntHashMap.this.toNonSentinel(key);
                    hashCode += nonSentinelKey == null ? 0 : nonSentinelKey.hashCode();
                }
            }
            return hashCode;
        }

        @Override
        public int size()
        {
            return ObjectIntHashMap.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectIntHashMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return ObjectIntHashMap.this.containsKey(o);
        }

        @Override
        public Object[] toArray()
        {
            int size = ObjectIntHashMap.this.size();
            Object[] result = new Object[size];
            this.copyKeys(result);
            return result;
        }

        @Override
        public <T> T[] toArray(T[] result)
        {
            int size = ObjectIntHashMap.this.size();
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
            int oldSize = ObjectIntHashMap.this.size();
            ObjectIntHashMap.this.removeKey((K) key);
            return oldSize != ObjectIntHashMap.this.size();
        }

        @Override
        public boolean containsAll(Collection<?> collection)
        {
            for (Object aCollection : collection)
            {
                if (!ObjectIntHashMap.this.containsKey(aCollection))
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
            int oldSize = ObjectIntHashMap.this.size();
            Iterator<K> iterator = this.iterator();
            while (iterator.hasNext())
            {
                K next = iterator.next();
                if (!collection.contains(next))
                {
                    iterator.remove();
                }
            }
            return oldSize != ObjectIntHashMap.this.size();
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            int oldSize = ObjectIntHashMap.this.size();
            for (Object object : collection)
            {
                ObjectIntHashMap.this.removeKey((K) object);
            }
            return oldSize != ObjectIntHashMap.this.size();
        }

        @Override
        public void clear()
        {
            ObjectIntHashMap.this.clear();
        }

        @Override
        public Iterator<K> iterator()
        {
            return new KeySetIterator();
        }

        private void copyKeys(Object[] result)
        {
            int count = 0;
            for (int i = 0; i < ObjectIntHashMap.this.keys.length; i++)
            {
                Object key = ObjectIntHashMap.this.keys[i];
                if (ObjectIntHashMap.isNonSentinel(key))
                {
                    result[count++] = ObjectIntHashMap.this.keys[i];
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
            return this.count < ObjectIntHashMap.this.size();
        }

        @Override
        public K next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            this.count++;
            Object[] keys = ObjectIntHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.currentKey = (K) ObjectIntHashMap.this.keys[this.position];
            this.isCurrentKeySet = true;
            this.position++;
            return ObjectIntHashMap.this.toNonSentinel(this.currentKey);
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
                ObjectIntHashMap.this.removeKeyAtIndex(ObjectIntHashMap.this.toNonSentinel(this.currentKey), index);
            }
            else
            {
                ObjectIntHashMap.this.removeKey(this.currentKey);
            }
        }
    }

    private class ValuesCollection implements MutableIntCollection
    {
        @Override
        public int size()
        {
            return ObjectIntHashMap.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectIntHashMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return ObjectIntHashMap.this.notEmpty();
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

                for (int i = 0; i < ObjectIntHashMap.this.keys.length; i++)
                {
                    Object key = ObjectIntHashMap.this.keys[i];
                    if (isNonSentinel(key))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ObjectIntHashMap.this.values[i]));
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
        public boolean add(int element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(int... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(IntIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean remove(int item)
        {
            int oldSize = ObjectIntHashMap.this.size();

            for (int i = 0; i < ObjectIntHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ObjectIntHashMap.this.keys[i]) && item == ObjectIntHashMap.this.values[i])
                {
                    ObjectIntHashMap.this.removeKey((K) ObjectIntHashMap.this.keys[i]);
                }
            }
            return oldSize != ObjectIntHashMap.this.size();
        }

        @Override
        public boolean removeAll(IntIterable source)
        {
            int oldSize = ObjectIntHashMap.this.size();

            IntIterator iterator = source.intIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != ObjectIntHashMap.this.size();
        }

        @Override
        public boolean removeAll(int... source)
        {
            int oldSize = ObjectIntHashMap.this.size();

            for (int item : source)
            {
                this.remove(item);
            }
            return oldSize != ObjectIntHashMap.this.size();
        }

        @Override
        public boolean retainAll(IntIterable source)
        {
            int oldSize = ObjectIntHashMap.this.size();
            final IntSet sourceSet = source instanceof IntSet ? (IntSet) source : source.toSet();
            ObjectIntHashMap<K> retained = ObjectIntHashMap.this.select((K object, int value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ObjectIntHashMap.this.keys = retained.keys;
                ObjectIntHashMap.this.values = retained.values;
                ObjectIntHashMap.this.occupiedWithData = retained.occupiedWithData;
                ObjectIntHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(int... source)
        {
            return this.retainAll(IntSets.mutable.with(source));
        }

        @Override
        public void clear()
        {
            ObjectIntHashMap.this.clear();
        }

        @Override
        public MutableIntCollection with(int element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableIntCollection without(int element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableIntCollection withAll(IntIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableIntCollection withoutAll(IntIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableIntCollection asUnmodifiable()
        {
            return UnmodifiableIntCollection.of(this);
        }

        @Override
        public MutableIntCollection asSynchronized()
        {
            return SynchronizedIntCollection.of(this);
        }

        @Override
        public ImmutableIntCollection toImmutable()
        {
            return IntLists.immutable.withAll(this);
        }

        @Override
        public MutableIntIterator intIterator()
        {
            return ObjectIntHashMap.this.intIterator();
        }

        @Override
        public int[] toArray()
        {
            return ObjectIntHashMap.this.toArray();
        }

        @Override
        public int[] toArray(int[] target)
        {
            return ObjectIntHashMap.this.toArray(target);
        }

        @Override
        public boolean contains(int value)
        {
            return ObjectIntHashMap.this.containsValue(value);
        }

        @Override
        public boolean containsAll(int... source)
        {
            return ObjectIntHashMap.this.containsAll(source);
        }

        @Override
        public boolean containsAll(IntIterable source)
        {
            return ObjectIntHashMap.this.containsAll(source);
        }

        @Override
        public void each(IntProcedure procedure)
        {
            ObjectIntHashMap.this.forEach(procedure);
        }

        @Override
        public MutableIntCollection select(IntPredicate predicate)
        {
            return ObjectIntHashMap.this.select(predicate);
        }

        @Override
        public MutableIntCollection reject(IntPredicate predicate)
        {
            return ObjectIntHashMap.this.reject(predicate);
        }

        @Override
        public <V> MutableCollection<V> collect(IntToObjectFunction<? extends V> function)
        {
            return ObjectIntHashMap.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
        {
            return ObjectIntHashMap.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<IntIterable> chunk(int size)
        {
            return ObjectIntHashMap.this.chunk(size);
        }

        @Override
        public int detectIfNone(IntPredicate predicate, int ifNone)
        {
            return ObjectIntHashMap.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public int count(IntPredicate predicate)
        {
            return ObjectIntHashMap.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(IntPredicate predicate)
        {
            return ObjectIntHashMap.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(IntPredicate predicate)
        {
            return ObjectIntHashMap.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(IntPredicate predicate)
        {
            return ObjectIntHashMap.this.noneSatisfy(predicate);
        }

        @Override
        public MutableIntList toList()
        {
            return ObjectIntHashMap.this.toList();
        }

        @Override
        public MutableIntSet toSet()
        {
            return ObjectIntHashMap.this.toSet();
        }

        @Override
        public MutableIntBag toBag()
        {
            return ObjectIntHashMap.this.toBag();
        }

        @Override
        public LazyIntIterable asLazy()
        {
            return new LazyIntIterableAdapter(this);
        }

        @Override
        public int[] toSortedArray()
        {
            return ObjectIntHashMap.this.toSortedArray();
        }

        @Override
        public MutableIntList toSortedList()
        {
            return ObjectIntHashMap.this.toSortedList();
        }

        @Override
        public long sum()
        {
            return ObjectIntHashMap.this.sum();
        }

        @Override
        public int max()
        {
            return ObjectIntHashMap.this.max();
        }

        @Override
        public int maxIfEmpty(int defaultValue)
        {
            return ObjectIntHashMap.this.maxIfEmpty(defaultValue);
        }

        @Override
        public int min()
        {
            return ObjectIntHashMap.this.min();
        }

        @Override
        public int minIfEmpty(int defaultValue)
        {
            return ObjectIntHashMap.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return ObjectIntHashMap.this.average();
        }

        @Override
        public double median()
        {
            return ObjectIntHashMap.this.median();
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableIntCollection newEmpty()
        {
            return IntBags.mutable.empty();
        }
    }

    private class KeysView extends AbstractLazyIterable<K>
    {
        @Override
        public void each(Procedure<? super K> procedure)
        {
            ObjectIntHashMap.this.forEachKey(procedure);
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super K> objectIntProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectIntHashMap.this.keys.length; i++)
            {
                if (ObjectIntHashMap.isNonSentinel(ObjectIntHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(ObjectIntHashMap.this.toNonSentinel(ObjectIntHashMap.this.keys[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super K, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectIntHashMap.this.keys.length; i++)
            {
                if (ObjectIntHashMap.isNonSentinel(ObjectIntHashMap.this.keys[i]))
                {
                    procedure.value(ObjectIntHashMap.this.toNonSentinel(ObjectIntHashMap.this.keys[i]), parameter);
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

                Object[] keys = ObjectIntHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                K result = ObjectIntHashMap.this.toNonSentinel(ObjectIntHashMap.this.keys[this.position]);
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
                return this.count != ObjectIntHashMap.this.size();
            }
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<ObjectIntPair<K>>
    {
        @Override
        public void each(Procedure<? super ObjectIntPair<K>> procedure)
        {
            for (int i = 0; i < ObjectIntHashMap.this.keys.length; i++)
            {
                if (ObjectIntHashMap.isNonSentinel(ObjectIntHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectIntHashMap.this.toNonSentinel(ObjectIntHashMap.this.keys[i]), ObjectIntHashMap.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ObjectIntPair<K>> objectIntProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectIntHashMap.this.keys.length; i++)
            {
                if (ObjectIntHashMap.isNonSentinel(ObjectIntHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ObjectIntHashMap.this.toNonSentinel(ObjectIntHashMap.this.keys[i]), ObjectIntHashMap.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ObjectIntPair<K>, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectIntHashMap.this.keys.length; i++)
            {
                if (ObjectIntHashMap.isNonSentinel(ObjectIntHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectIntHashMap.this.toNonSentinel(ObjectIntHashMap.this.keys[i]), ObjectIntHashMap.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<ObjectIntPair<K>> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ObjectIntPair<K>>
        {
            private int count;
            private int position;

            @Override
            public ObjectIntPair<K> next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException();
                }

                Object[] keys = ObjectIntHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                ObjectIntPair<K> result = PrimitiveTuples.pair(ObjectIntHashMap.this.toNonSentinel(ObjectIntHashMap.this.keys[this.position]), ObjectIntHashMap.this.values[this.position]);
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
                return this.count != ObjectIntHashMap.this.size();
            }
        }
    }
}
