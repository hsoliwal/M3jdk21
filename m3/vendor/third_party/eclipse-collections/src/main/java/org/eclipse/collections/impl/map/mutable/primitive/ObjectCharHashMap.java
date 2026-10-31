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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.CharFunction0;
import org.eclipse.collections.api.block.function.primitive.CharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectCharPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectCharProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.CharBags;
import org.eclipse.collections.api.factory.primitive.CharLists;
import org.eclipse.collections.api.factory.primitive.CharSets;
import org.eclipse.collections.api.factory.primitive.CharObjectMaps;
import org.eclipse.collections.api.factory.primitive.ObjectCharMaps;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.MutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.MutableCharObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectCharMap;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.tuple.primitive.ObjectCharPair;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedCharCollection;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * This file was automatically generated from template file objectPrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class ObjectCharHashMap<K> implements MutableObjectCharMap<K>, Externalizable
{
    public static final char EMPTY_VALUE = '\0';

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
            return "ObjectCharHashMap.NULL_KEY";
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
            return "ObjectCharHashMap.REMOVED_KEY";
        }
    };

    private Object[] keys;
    private char[] values;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    public ObjectCharHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public ObjectCharHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity << 1));
        this.allocateTable(capacity);
    }

    public ObjectCharHashMap(ObjectCharMap<? extends K> map)
    {
        this(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }

    public static <K> ObjectCharHashMap<K> newMap()
    {
        return new ObjectCharHashMap<>();
    }

    public static <K> ObjectCharHashMap<K> newWithKeysValues(K key1, char value1)
    {
        return new ObjectCharHashMap<K>(1).withKeyValue(key1, value1);
    }

    public static <K> ObjectCharHashMap<K> newWithKeysValues(K key1, char value1, K key2, char value2)
    {
        return new ObjectCharHashMap<K>(2).withKeysValues(key1, value1, key2, value2);
    }

    public static <K> ObjectCharHashMap<K> newWithKeysValues(K key1, char value1, K key2, char value2, K key3, char value3)
    {
        return new ObjectCharHashMap<K>(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static <K> ObjectCharHashMap<K> newWithKeysValues(K key1, char value1, K key2, char value2, K key3, char value3, K key4, char value4)
    {
        return new ObjectCharHashMap<K>(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
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

        if (!(obj instanceof ObjectCharMap))
        {
            return false;
        }

        ObjectCharMap<K> other = (ObjectCharMap<K>) obj;

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
                result += (this.toNonSentinel(this.keys[i]) == null ? 0 : this.keys[i].hashCode()) ^ (int) this.values[i];
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
    public MutableCharIterator charIterator()
    {
        return new InternalCharIterator();
    }

    @Override
    public char[] toArray()
    {
        char[] result = new char[this.size()];
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
    public char[] toArray(char[] target)
    {
        if (target.length < this.size())
        {
            target = new char[this.size()];
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
    public boolean contains(char value)
    {
        return this.containsValue(value);
    }

    @Override
    public boolean containsAll(char... source)
    {
        for (char item : source)
        {
            if (!this.containsValue(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(CharIterable source)
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
    public void put(K key, char value)
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
    public void putAll(ObjectCharMap<? extends K> map)
    {
        map.forEachKeyValue(this::put);
    }

    @Override
    public void updateValues(ObjectCharToCharFunction<? super K> function)
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
    public char removeKeyIfAbsent(K key, char value)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.keys[index] = REMOVED_KEY;
            char oldValue = this.values[index];
            this.values[index] = EMPTY_VALUE;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;

            return oldValue;
        }
        return value;
    }

    @Override
    public char getIfAbsentPut(K key, char value)
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
    public char getAndPut(K key, char putValue, char defaultValue)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            char existingValue = this.values[index];
            this.values[index] = putValue;
            return existingValue;
        }
        this.addKeyValueAtIndex(key, putValue, index);
        return defaultValue;
    }

    @Override
    public char getIfAbsentPut(K key, CharFunction0 function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        char value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> char getIfAbsentPutWith(K key, CharFunction<? super P> function, P parameter)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        char value = function.charValueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public char getIfAbsentPutWithKey(K key, CharFunction<? super K> function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            return this.values[index];
        }
        char value = function.charValueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public char updateValue(K key, char initialValueIfAbsent, CharToCharFunction function)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key))
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        char value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(K key, char value, int index)
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
    public char addToValue(K key, char toBeAdded)
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
    public ObjectCharHashMap<K> withKeyValue(K key1, char value1)
    {
        this.put(key1, value1);
        return this;
    }

    public ObjectCharHashMap<K> withKeysValues(K key1, char value1, K key2, char value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public ObjectCharHashMap<K> withKeysValues(K key1, char value1, K key2, char value2, K key3, char value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public ObjectCharHashMap<K> withKeysValues(K key1, char value1, K key2, char value2, K key3, char value3, K key4, char value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public ObjectCharHashMap<K> withoutKey(K key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public ObjectCharHashMap<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        for (K key : keys)
        {
            this.removeKey(key);
        }
        return this;
    }

    @Override
    public MutableObjectCharMap<K> asUnmodifiable()
    {
        return new UnmodifiableObjectCharMap<>(this);
    }

    @Override
    public MutableObjectCharMap<K> asSynchronized()
    {
        return new SynchronizedObjectCharMap<>(this);
    }

    @Override
    public ImmutableObjectCharMap<K> toImmutable()
    {
        return ObjectCharMaps.immutable.withAll(this);
    }

    @Override
    public char get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public char getOrThrow(Object key)
    {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]))
        {
            return this.values[index];
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public char getIfAbsent(Object key, char ifAbsent)
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
    public boolean containsValue(char value)
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
    public void each(CharProcedure procedure)
    {
        this.forEachValue(procedure);
    }

    @Override
    public void forEachValue(CharProcedure procedure)
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
    public void forEachKeyValue(ObjectCharProcedure<? super K> procedure)
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
    public ObjectCharHashMap<K> select(ObjectCharPredicate<? super K> predicate)
    {
        ObjectCharHashMap<K> result = ObjectCharHashMap.newMap();

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
    public ObjectCharHashMap<K> reject(ObjectCharPredicate<? super K> predicate)
    {
        ObjectCharHashMap<K> result = ObjectCharHashMap.newMap();

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
    public MutableCharCollection select(CharPredicate predicate)
    {
        MutableCharList result = CharLists.mutable.empty();

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
    public MutableCharCollection reject(CharPredicate predicate)
    {
        MutableCharList result = CharLists.mutable.empty();

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
    public char detectIfNone(CharPredicate predicate, char ifNone)
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
    public <V> MutableCollection<V> collect(CharToObjectFunction<? extends V> function)
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
    public int count(CharPredicate predicate)
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
    public boolean anySatisfy(CharPredicate predicate)
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
    public boolean allSatisfy(CharPredicate predicate)
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
    public boolean noneSatisfy(CharPredicate predicate)
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
    public <V> V injectInto(V injectedValue, ObjectCharToObjectFunction<? super V, ? extends V> function)
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
    public RichIterable<CharIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<CharIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            CharIterator iterator = this.charIterator();
            while (iterator.hasNext())
            {
                MutableCharBag batch = CharBags.mutable.empty();
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
    public char max()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        char max = '\0';
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
    public char min()
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }
        char min = '\0';
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
    public char maxIfEmpty(char defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        char max = '\0';
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
    public char minIfEmpty(char defaultValue)
    {
        if (this.isEmpty())
        {
            return defaultValue;
        }
        char min = '\0';
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
        char[] sortedArray = this.toSortedArray();
        int middleIndex = sortedArray.length >> 1;
        if (sortedArray.length > 1 && (sortedArray.length & 1) == 0)
        {
            char first = sortedArray[middleIndex];
            char second = sortedArray[middleIndex - 1];
            return ((double) first + (double) second) / 2.0;
        }
        return (double) sortedArray[middleIndex];
    }

    @Override
    public MutableCharList toList()
    {
        MutableCharList result = CharLists.mutable.withInitialCapacity(this.size());

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
    public MutableCharSet toSet()
    {
        MutableCharSet result = CharSets.mutable.empty();

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
    public MutableCharBag toBag()
    {
        MutableCharBag result = CharBags.mutable.empty();

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
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
    }

    @Override
    public char[] toSortedArray()
    {
        char[] array = this.toArray();
        Arrays.sort(array);
        return array;
    }

    @Override
    public MutableCharList toSortedList()
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
                out.writeChar(this.values[i]);
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
            this.put((K) in.readObject(), in.readChar());
        }
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<ObjectCharPair<K>> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableCharObjectMap<K> flipUniqueValues()
    {
        MutableCharObjectMap<K> result = CharObjectMaps.mutable.empty();
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
        char[] oldValues = this.values;
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
        this.values = new char[sizeToAllocate];
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

    private class InternalCharIterator implements MutableCharIterator
    {
        private int count;
        private int position;

        @Override
        public boolean hasNext()
        {
            return this.count != ObjectCharHashMap.this.size();
        }

        @Override
        public char next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }

            Object[] keys = ObjectCharHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            char result = ObjectCharHashMap.this.values[this.position];
            this.count++;
            this.position++;
            return result;
        }

        @Override
        public void remove()
        {
            if (this.position == 0 || !isNonSentinel(ObjectCharHashMap.this.keys[this.position - 1]))
            {
                throw new IllegalStateException();
            }
            ObjectCharHashMap.this.remove(ObjectCharHashMap.this.keys[this.position - 1]);
            this.count--;
        }
    }

    @Override
    public Set<K> keySet()
    {
        return new KeySet();
    }

    @Override
    public MutableCharCollection values()
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
            Object[] table = ObjectCharHashMap.this.keys;
            for (int i = 0; i < table.length; i++)
            {
                Object key = table[i];
                if (ObjectCharHashMap.isNonSentinel(key))
                {
                    K nonSentinelKey = ObjectCharHashMap.this.toNonSentinel(key);
                    hashCode += nonSentinelKey == null ? 0 : nonSentinelKey.hashCode();
                }
            }
            return hashCode;
        }

        @Override
        public int size()
        {
            return ObjectCharHashMap.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectCharHashMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            return ObjectCharHashMap.this.containsKey(o);
        }

        @Override
        public Object[] toArray()
        {
            int size = ObjectCharHashMap.this.size();
            Object[] result = new Object[size];
            this.copyKeys(result);
            return result;
        }

        @Override
        public <T> T[] toArray(T[] result)
        {
            int size = ObjectCharHashMap.this.size();
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
            int oldSize = ObjectCharHashMap.this.size();
            ObjectCharHashMap.this.removeKey((K) key);
            return oldSize != ObjectCharHashMap.this.size();
        }

        @Override
        public boolean containsAll(Collection<?> collection)
        {
            for (Object aCollection : collection)
            {
                if (!ObjectCharHashMap.this.containsKey(aCollection))
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
            int oldSize = ObjectCharHashMap.this.size();
            Iterator<K> iterator = this.iterator();
            while (iterator.hasNext())
            {
                K next = iterator.next();
                if (!collection.contains(next))
                {
                    iterator.remove();
                }
            }
            return oldSize != ObjectCharHashMap.this.size();
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            int oldSize = ObjectCharHashMap.this.size();
            for (Object object : collection)
            {
                ObjectCharHashMap.this.removeKey((K) object);
            }
            return oldSize != ObjectCharHashMap.this.size();
        }

        @Override
        public void clear()
        {
            ObjectCharHashMap.this.clear();
        }

        @Override
        public Iterator<K> iterator()
        {
            return new KeySetIterator();
        }

        private void copyKeys(Object[] result)
        {
            int count = 0;
            for (int i = 0; i < ObjectCharHashMap.this.keys.length; i++)
            {
                Object key = ObjectCharHashMap.this.keys[i];
                if (ObjectCharHashMap.isNonSentinel(key))
                {
                    result[count++] = ObjectCharHashMap.this.keys[i];
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
            return this.count < ObjectCharHashMap.this.size();
        }

        @Override
        public K next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            this.count++;
            Object[] keys = ObjectCharHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.currentKey = (K) ObjectCharHashMap.this.keys[this.position];
            this.isCurrentKeySet = true;
            this.position++;
            return ObjectCharHashMap.this.toNonSentinel(this.currentKey);
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
                ObjectCharHashMap.this.removeKeyAtIndex(ObjectCharHashMap.this.toNonSentinel(this.currentKey), index);
            }
            else
            {
                ObjectCharHashMap.this.removeKey(this.currentKey);
            }
        }
    }

    private class ValuesCollection implements MutableCharCollection
    {
        @Override
        public int size()
        {
            return ObjectCharHashMap.this.size();
        }

        @Override
        public boolean isEmpty()
        {
            return ObjectCharHashMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return ObjectCharHashMap.this.notEmpty();
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

                for (int i = 0; i < ObjectCharHashMap.this.keys.length; i++)
                {
                    Object key = ObjectCharHashMap.this.keys[i];
                    if (isNonSentinel(key))
                    {
                        if (!first)
                        {
                            appendable.append(separator);
                        }
                        appendable.append(String.valueOf(ObjectCharHashMap.this.values[i]));
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
        public boolean add(char element)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(char... source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(CharIterable source)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean remove(char item)
        {
            int oldSize = ObjectCharHashMap.this.size();

            for (int i = 0; i < ObjectCharHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(ObjectCharHashMap.this.keys[i]) && item == ObjectCharHashMap.this.values[i])
                {
                    ObjectCharHashMap.this.removeKey((K) ObjectCharHashMap.this.keys[i]);
                }
            }
            return oldSize != ObjectCharHashMap.this.size();
        }

        @Override
        public boolean removeAll(CharIterable source)
        {
            int oldSize = ObjectCharHashMap.this.size();

            CharIterator iterator = source.charIterator();
            while (iterator.hasNext())
            {
                this.remove(iterator.next());
            }
            return oldSize != ObjectCharHashMap.this.size();
        }

        @Override
        public boolean removeAll(char... source)
        {
            int oldSize = ObjectCharHashMap.this.size();

            for (char item : source)
            {
                this.remove(item);
            }
            return oldSize != ObjectCharHashMap.this.size();
        }

        @Override
        public boolean retainAll(CharIterable source)
        {
            int oldSize = ObjectCharHashMap.this.size();
            final CharSet sourceSet = source instanceof CharSet ? (CharSet) source : source.toSet();
            ObjectCharHashMap<K> retained = ObjectCharHashMap.this.select((K object, char value) -> sourceSet.contains(value));
            if (retained.size() != oldSize)
            {
                ObjectCharHashMap.this.keys = retained.keys;
                ObjectCharHashMap.this.values = retained.values;
                ObjectCharHashMap.this.occupiedWithData = retained.occupiedWithData;
                ObjectCharHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(char... source)
        {
            return this.retainAll(CharSets.mutable.with(source));
        }

        @Override
        public void clear()
        {
            ObjectCharHashMap.this.clear();
        }

        @Override
        public MutableCharCollection with(char element)
        {
            throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableCharCollection without(char element)
        {
            throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableCharCollection withAll(CharIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableCharCollection withoutAll(CharIterable elements)
        {
            throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public MutableCharCollection asUnmodifiable()
        {
            return UnmodifiableCharCollection.of(this);
        }

        @Override
        public MutableCharCollection asSynchronized()
        {
            return SynchronizedCharCollection.of(this);
        }

        @Override
        public ImmutableCharCollection toImmutable()
        {
            return CharLists.immutable.withAll(this);
        }

        @Override
        public MutableCharIterator charIterator()
        {
            return ObjectCharHashMap.this.charIterator();
        }

        @Override
        public char[] toArray()
        {
            return ObjectCharHashMap.this.toArray();
        }

        @Override
        public char[] toArray(char[] target)
        {
            return ObjectCharHashMap.this.toArray(target);
        }

        @Override
        public boolean contains(char value)
        {
            return ObjectCharHashMap.this.containsValue(value);
        }

        @Override
        public boolean containsAll(char... source)
        {
            return ObjectCharHashMap.this.containsAll(source);
        }

        @Override
        public boolean containsAll(CharIterable source)
        {
            return ObjectCharHashMap.this.containsAll(source);
        }

        @Override
        public void each(CharProcedure procedure)
        {
            ObjectCharHashMap.this.forEach(procedure);
        }

        @Override
        public MutableCharCollection select(CharPredicate predicate)
        {
            return ObjectCharHashMap.this.select(predicate);
        }

        @Override
        public MutableCharCollection reject(CharPredicate predicate)
        {
            return ObjectCharHashMap.this.reject(predicate);
        }

        @Override
        public <V> MutableCollection<V> collect(CharToObjectFunction<? extends V> function)
        {
            return ObjectCharHashMap.this.collect(function);
        }

        @Override
        public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
        {
            return ObjectCharHashMap.this.injectInto(injectedValue, function);
        }

        @Override
        public RichIterable<CharIterable> chunk(int size)
        {
            return ObjectCharHashMap.this.chunk(size);
        }

        @Override
        public char detectIfNone(CharPredicate predicate, char ifNone)
        {
            return ObjectCharHashMap.this.detectIfNone(predicate, ifNone);
        }

        @Override
        public int count(CharPredicate predicate)
        {
            return ObjectCharHashMap.this.count(predicate);
        }

        @Override
        public boolean anySatisfy(CharPredicate predicate)
        {
            return ObjectCharHashMap.this.anySatisfy(predicate);
        }

        @Override
        public boolean allSatisfy(CharPredicate predicate)
        {
            return ObjectCharHashMap.this.allSatisfy(predicate);
        }

        @Override
        public boolean noneSatisfy(CharPredicate predicate)
        {
            return ObjectCharHashMap.this.noneSatisfy(predicate);
        }

        @Override
        public MutableCharList toList()
        {
            return ObjectCharHashMap.this.toList();
        }

        @Override
        public MutableCharSet toSet()
        {
            return ObjectCharHashMap.this.toSet();
        }

        @Override
        public MutableCharBag toBag()
        {
            return ObjectCharHashMap.this.toBag();
        }

        @Override
        public LazyCharIterable asLazy()
        {
            return new LazyCharIterableAdapter(this);
        }

        @Override
        public char[] toSortedArray()
        {
            return ObjectCharHashMap.this.toSortedArray();
        }

        @Override
        public MutableCharList toSortedList()
        {
            return ObjectCharHashMap.this.toSortedList();
        }

        @Override
        public long sum()
        {
            return ObjectCharHashMap.this.sum();
        }

        @Override
        public char max()
        {
            return ObjectCharHashMap.this.max();
        }

        @Override
        public char maxIfEmpty(char defaultValue)
        {
            return ObjectCharHashMap.this.maxIfEmpty(defaultValue);
        }

        @Override
        public char min()
        {
            return ObjectCharHashMap.this.min();
        }

        @Override
        public char minIfEmpty(char defaultValue)
        {
            return ObjectCharHashMap.this.minIfEmpty(defaultValue);
        }

        @Override
        public double average()
        {
            return ObjectCharHashMap.this.average();
        }

        @Override
        public double median()
        {
            return ObjectCharHashMap.this.median();
        }

        /**
         * @since 9.2.
         */
        @Override
        public MutableCharCollection newEmpty()
        {
            return CharBags.mutable.empty();
        }
    }

    private class KeysView extends AbstractLazyIterable<K>
    {
        @Override
        public void each(Procedure<? super K> procedure)
        {
            ObjectCharHashMap.this.forEachKey(procedure);
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super K> objectCharProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectCharHashMap.this.keys.length; i++)
            {
                if (ObjectCharHashMap.isNonSentinel(ObjectCharHashMap.this.keys[i]))
                {
                    objectCharProcedure.value(ObjectCharHashMap.this.toNonSentinel(ObjectCharHashMap.this.keys[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super K, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectCharHashMap.this.keys.length; i++)
            {
                if (ObjectCharHashMap.isNonSentinel(ObjectCharHashMap.this.keys[i]))
                {
                    procedure.value(ObjectCharHashMap.this.toNonSentinel(ObjectCharHashMap.this.keys[i]), parameter);
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

                Object[] keys = ObjectCharHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                K result = ObjectCharHashMap.this.toNonSentinel(ObjectCharHashMap.this.keys[this.position]);
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
                return this.count != ObjectCharHashMap.this.size();
            }
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<ObjectCharPair<K>>
    {
        @Override
        public void each(Procedure<? super ObjectCharPair<K>> procedure)
        {
            for (int i = 0; i < ObjectCharHashMap.this.keys.length; i++)
            {
                if (ObjectCharHashMap.isNonSentinel(ObjectCharHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectCharHashMap.this.toNonSentinel(ObjectCharHashMap.this.keys[i]), ObjectCharHashMap.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super ObjectCharPair<K>> objectIntProcedure)
        {
            int index = 0;
            for (int i = 0; i < ObjectCharHashMap.this.keys.length; i++)
            {
                if (ObjectCharHashMap.isNonSentinel(ObjectCharHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(ObjectCharHashMap.this.toNonSentinel(ObjectCharHashMap.this.keys[i]), ObjectCharHashMap.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super ObjectCharPair<K>, ? super P> procedure, P parameter)
        {
            for (int i = 0; i < ObjectCharHashMap.this.keys.length; i++)
            {
                if (ObjectCharHashMap.isNonSentinel(ObjectCharHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(ObjectCharHashMap.this.toNonSentinel(ObjectCharHashMap.this.keys[i]), ObjectCharHashMap.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<ObjectCharPair<K>> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<ObjectCharPair<K>>
        {
            private int count;
            private int position;

            @Override
            public ObjectCharPair<K> next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException();
                }

                Object[] keys = ObjectCharHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                ObjectCharPair<K> result = PrimitiveTuples.pair(ObjectCharHashMap.this.toNonSentinel(ObjectCharHashMap.this.keys[this.position]), ObjectCharHashMap.this.values[this.position]);
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
                return this.count != ObjectCharHashMap.this.size();
            }
        }
    }
}
