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
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.bag.sorted.MutableSortedBag;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleObjectToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatObjectToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.LongObjectToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.FloatObjectPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.FloatObjectProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.factory.BiMaps;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.FloatObjectMaps;
import org.eclipse.collections.api.factory.primitive.ObjectFloatMaps;
import org.eclipse.collections.api.factory.primitive.ObjectDoubleMaps;
import org.eclipse.collections.api.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.api.map.primitive.FloatObjectMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableObjectLongMap;
import org.eclipse.collections.api.map.primitive.MutableFloatObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectFloatMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.api.partition.bag.PartitionMutableBag;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.FloatSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.FloatObjectPair;
import org.eclipse.collections.impl.SpreadFunctions;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.bag.mutable.primitive.BooleanHashBag;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.bag.mutable.primitive.CharHashBag;
import org.eclipse.collections.impl.bag.mutable.primitive.DoubleHashBag;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.bag.mutable.primitive.IntHashBag;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.bag.mutable.primitive.ShortHashBag;
import org.eclipse.collections.impl.bag.sorted.mutable.TreeBag;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Functions0;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.PrimitiveFunctions;
import org.eclipse.collections.impl.block.factory.Procedures2;
import org.eclipse.collections.impl.block.procedure.BiMapCollectProcedure;
import org.eclipse.collections.impl.block.procedure.MapCollectProcedure;
import org.eclipse.collections.impl.block.procedure.MutatingAggregationProcedure;
import org.eclipse.collections.impl.block.procedure.PartitionProcedure;
import org.eclipse.collections.impl.block.procedure.SelectInstancesOfProcedure;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.lazy.primitive.AbstractLazyFloatIterable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.sorted.mutable.TreeSortedMap;
import org.eclipse.collections.impl.multimap.bag.HashBagMultimap;
import org.eclipse.collections.impl.partition.bag.PartitionHashBag;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.internal.IterableIterate;

/**
 * This file was automatically generated from template file primitiveObjectHashMap.stg.
 *
 * @since 3.0.
 */
public class FloatObjectHashMap<V> implements MutableFloatObjectMap<V>, Externalizable
{
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_INITIAL_CAPACITY = 8;
    private static final float EMPTY_KEY = 0.0f;
    private static final float REMOVED_KEY = 1.0f;
    private static final int CACHE_LINE_SIZE = 64;
    private static final int KEY_SIZE = 4;
    private static final int INITIAL_LINEAR_PROBE = CACHE_LINE_SIZE / KEY_SIZE / 2; /* half a cache line */

    private float[] keys;
    private V[] values;
    private int occupiedWithData;
    private int occupiedWithSentinels;

    private SentinelValues<V> sentinelValues;

    private boolean copyKeysOnWrite = false;

    public FloatObjectHashMap()
    {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public FloatObjectHashMap(int initialCapacity)
    {
        if (initialCapacity < 0)
        {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity << 1));
        this.allocateTable(capacity);
    }

    public FloatObjectHashMap(FloatObjectMap<? extends V> map)
    {
        this(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
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

    public static <V> FloatObjectHashMap<V> newMap()
    {
        return new FloatObjectHashMap<>();
    }

    public static <V> FloatObjectHashMap<V> newMap(FloatObjectMap<? extends V> map)
    {
        return new FloatObjectHashMap<>(map);
    }

    public static <V> FloatObjectHashMap<V> newWithKeysValues(float key, V value)
    {
        return new FloatObjectHashMap<V>(1).withKeyValue(key, value);
    }

    public static <V> FloatObjectHashMap<V> newWithKeysValues(float key1, V value1, float key2, V value2)
    {
        return new FloatObjectHashMap<V>(2).withKeysValues(key1, value1, key2, value2);
    }

    public static <V> FloatObjectHashMap<V> newWithKeysValues(float key1, V value1, float key2, V value2, float key3, V value3)
    {
        return new FloatObjectHashMap<V>(3).withKeysValues(key1, value1, key2, value2, key3, value3);
    }

    public static <V> FloatObjectHashMap<V> newWithKeysValues(float key1, V value1, float key2, V value2, float key3, V value3, float key4, V value4)
    {
        return new FloatObjectHashMap<V>(4).withKeysValues(key1, value1, key2, value2, key3, value3, key4, value4);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof FloatObjectMap))
        {
            return false;
        }

        FloatObjectMap<V> other = (FloatObjectMap<V>) obj;

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
            if (this.sentinelValues.containsZeroKey && (!other.containsKey(EMPTY_KEY) || !nullSafeEquals(this.sentinelValues.zeroValue, other.get(EMPTY_KEY))))
            {
                return false;
            }

            if (this.sentinelValues.containsOneKey && (!other.containsKey(REMOVED_KEY) || !nullSafeEquals(this.sentinelValues.oneValue, other.get(REMOVED_KEY))))
            {
                return false;
            }
        }

        for (int i = 0; i < this.keys.length; i++)
        {
            float key = this.keys[i];
            if (isNonSentinel(key) && (!other.containsKey(key) || !nullSafeEquals(this.values[i], other.get(key))))
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
                result += Float.floatToIntBits(EMPTY_KEY) ^ (this.sentinelValues.zeroValue == null ? 0 : this.sentinelValues.zeroValue.hashCode());
            }

            if (this.sentinelValues.containsOneKey)
            {
                result += Float.floatToIntBits(REMOVED_KEY) ^ (this.sentinelValues.oneValue == null ? 0 : this.sentinelValues.oneValue.hashCode());
            }
        }

        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result += Float.floatToIntBits(this.keys[i]) ^ (this.values[i] == null ? 0 : this.values[i].hashCode());
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
            float key = this.keys[i];
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
    public int size()
    {
        return this.occupiedWithData + (this.sentinelValues == null ? 0 : this.sentinelValues.size());
    }

    @Override
    public boolean isEmpty()
    {
        return this.occupiedWithData == 0 && (this.sentinelValues == null || this.sentinelValues.size() == 0);
    }

    @Override
    public boolean notEmpty()
    {
        return this.occupiedWithData != 0 || (this.sentinelValues != null && this.sentinelValues.size() != 0);
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

            if (this.sentinelValues != null)
            {
                if (this.sentinelValues.containsZeroKey)
                {
                    appendable.append(String.valueOf(this.sentinelValues.zeroValue));
                    first = false;
                }
                if (this.sentinelValues.containsOneKey)
                {
                    if (!first)
                    {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(this.sentinelValues.oneValue));
                    first = false;
                }
            }
            for (int i = 0; i < this.keys.length; i++)
            {
                float key = this.keys[i];
                if (isNonSentinel(key))
                {
                    if (!first)
                    {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(this.values[i]));
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
    public Iterator<V> iterator()
    {
        return new InternalIterator();
    }

    @Override
    public Object[] toArray()
    {
        Object[] result = new Object[this.size()];
        int index = 0;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                result[index++] = this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey)
            {
                result[index++] = this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result[index++] = this.values[i];
            }
        }
        return result;
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        int size = this.size();

        final T[] result = a.length < size
                ? (T[]) Array.newInstance(a.getClass().getComponentType(), size)
                : a;

        this.forEachWithIndex((Object each, int index) -> result[index] = (T) each);
        if (result.length > size)
        {
            result[size] = null;
        }
        return result;
    }

    @Override
    public boolean contains(Object object)
    {
        return this.containsValue(object);
    }

    @Override
    public boolean containsAllIterable(Iterable<?> source)
    {
        for (Object item : source)
        {
            if (!this.contains(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> source)
    {
        return this.containsAllIterable(source);
    }

    @Override
    public boolean containsAllArguments(Object... elements)
    {
        for (Object item : elements)
        {
            if (!this.contains(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public FloatObjectHashMap<V> tap(Procedure<? super V> procedure)
    {
        this.forEachValue(procedure);
        return this;
    }

    @Override
    public void each(Procedure<? super V> procedure)
    {
        this.forEachValue(procedure);
    }

    @Override
    public void forEachWithIndex(ObjectIntProcedure<? super V> objectIntProcedure)
    {
        int index = 0;
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                objectIntProcedure.value(this.sentinelValues.zeroValue, index++);
            }
            if (this.sentinelValues.containsOneKey)
            {
                objectIntProcedure.value(this.sentinelValues.oneValue, index++);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                objectIntProcedure.value(this.values[i], index++);
            }
        }
    }

    @Override
    public <P> void forEachWith(Procedure2<? super V, ? super P> procedure, P parameter)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                procedure.value(this.sentinelValues.zeroValue, parameter);
            }
            if (this.sentinelValues.containsOneKey)
            {
                procedure.value(this.sentinelValues.oneValue, parameter);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                procedure.value(this.values[i], parameter);
            }
        }
    }

    @Override
    public void forEachValue(Procedure<? super V> procedure)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                procedure.value(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                procedure.value(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                procedure.value(this.values[i]);
            }
        }
    }

    @Override
    public void forEachKey(FloatProcedure procedure)
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
    public void forEachKeyValue(FloatObjectProcedure<? super V> procedure)
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
    public FloatObjectHashMap<V> select(FloatObjectPredicate<? super V> predicate)
    {
        FloatObjectHashMap<V> result = FloatObjectHashMap.newMap();

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
    public FloatObjectHashMap<V> reject(FloatObjectPredicate<? super V> predicate)
    {
        FloatObjectHashMap<V> result = FloatObjectHashMap.newMap();

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
    public MutableBag<V> select(Predicate<? super V> predicate)
    {
        MutableBag<V> result = new HashBag<>();
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue))
            {
                result.add(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue))
            {
                result.add(this.sentinelValues.oneValue);
            }
        }
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
    public <R extends Collection<V>> R select(Predicate<? super V> predicate, R target)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue))
            {
                target.add(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue))
            {
                target.add(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i]))
            {
                target.add(this.values[i]);
            }
        }
        return target;
    }

    @Override
    public <P> MutableBag<V> selectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.select(Predicates.bind(predicate, parameter));
    }

    @Override
    public <P, R extends Collection<V>> R selectWith(Predicate2<? super V, ? super P> predicate, P parameter, R targetCollection)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue, parameter))
            {
                targetCollection.add(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue, parameter))
            {
                targetCollection.add(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i], parameter))
            {
                targetCollection.add(this.values[i]);
            }
        }
        return targetCollection;
    }

    @Override
    public MutableBag<V> reject(Predicate<? super V> predicate)
    {
        MutableBag<V> result = new HashBag<>();
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && !predicate.accept(this.sentinelValues.zeroValue))
            {
                result.add(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey && !predicate.accept(this.sentinelValues.oneValue))
            {
                result.add(this.sentinelValues.oneValue);
            }
        }
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
    public <R extends Collection<V>> R reject(Predicate<? super V> predicate, R target)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && !predicate.accept(this.sentinelValues.zeroValue))
            {
                target.add(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey && !predicate.accept(this.sentinelValues.oneValue))
            {
                target.add(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.values[i]))
            {
                target.add(this.values[i]);
            }
        }
        return target;
    }

    @Override
    public <P> MutableBag<V> rejectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.reject(Predicates.bind(predicate, parameter));
    }

    @Override
    public <P, R extends Collection<V>> R rejectWith(Predicate2<? super V, ? super P> predicate, P parameter, R targetCollection)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && !predicate.accept(this.sentinelValues.zeroValue, parameter))
            {
                targetCollection.add(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey && !predicate.accept(this.sentinelValues.oneValue, parameter))
            {
                targetCollection.add(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.values[i], parameter))
            {
                targetCollection.add(this.values[i]);
            }
        }
        return targetCollection;
    }

    @Override
    public PartitionMutableBag<V> partition(Predicate<? super V> predicate)
    {
        PartitionMutableBag<V> partitionMutableBag = new PartitionHashBag<>();
        this.forEach(new PartitionProcedure<V>(predicate, partitionMutableBag));
        return partitionMutableBag;
    }

    @Override
    public <P> PartitionMutableBag<V> partitionWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        PartitionMutableBag<V> partitionMutableBag = new PartitionHashBag<>();
        this.forEach(new PartitionProcedure<V>(Predicates.bind(predicate, parameter), partitionMutableBag));
        return partitionMutableBag;
    }

    @Override
    public <S> MutableBag<S> selectInstancesOf(Class<S> clazz)
    {
        MutableBag<S> result = Bags.mutable.withInitialCapacity(this.size());
        this.forEach(new SelectInstancesOfProcedure<S>(clazz, result));
        return result;
    }

    @Override
    public <VV> MutableBag<VV> collect(Function<? super V, ? extends VV> function)
    {
        return this.collect(function, new HashBag<VV>());
    }

    @Override
    public MutableBooleanBag collectBoolean(BooleanFunction<? super V> booleanFunction)
    {
        return this.collectBoolean(booleanFunction, new BooleanHashBag());
    }

    @Override
    public MutableByteBag collectByte(ByteFunction<? super V> byteFunction)
    {
        return this.collectByte(byteFunction, new ByteHashBag());
    }

    @Override
    public MutableCharBag collectChar(CharFunction<? super V> charFunction)
    {
        return this.collectChar(charFunction, new CharHashBag());
    }

    @Override
    public MutableDoubleBag collectDouble(DoubleFunction<? super V> doubleFunction)
    {
        return this.collectDouble(doubleFunction, new DoubleHashBag());
    }

    @Override
    public MutableFloatBag collectFloat(FloatFunction<? super V> floatFunction)
    {
        return this.collectFloat(floatFunction, new FloatHashBag());
    }

    @Override
    public MutableIntBag collectInt(IntFunction<? super V> intFunction)
    {
        return this.collectInt(intFunction, new IntHashBag());
    }

    @Override
    public MutableLongBag collectLong(LongFunction<? super V> longFunction)
    {
        return this.collectLong(longFunction, new LongHashBag());
    }

    @Override
    public MutableShortBag collectShort(ShortFunction<? super V> shortFunction)
    {
        return this.collectShort(shortFunction, new ShortHashBag());
    }

    @Override
    public <P, VV> MutableBag<VV> collectWith(Function2<? super V, ? super P, ? extends VV> function, P parameter)
    {
        return this.collect(Functions.bind(function, parameter));
    }

    @Override
    public <P, VV, R extends Collection<VV>> R collectWith(Function2<? super V, ? super P, ? extends VV> function, P parameter, R targetCollection)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                targetCollection.add(function.value(this.sentinelValues.zeroValue, parameter));
            }
            if (this.sentinelValues.containsOneKey)
            {
                targetCollection.add(function.value(this.sentinelValues.oneValue, parameter));
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                targetCollection.add(function.value(this.values[i], parameter));
            }
        }
        return targetCollection;
    }

    @Override
    public <VV, R extends Collection<VV>> R collect(Function<? super V, ? extends VV> function, R target)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                target.add(function.valueOf(this.sentinelValues.zeroValue));
            }
            if (this.sentinelValues.containsOneKey)
            {
                target.add(function.valueOf(this.sentinelValues.oneValue));
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                target.add(function.valueOf(this.values[i]));
            }
        }
        return target;
    }

    @Override
    public <VV> MutableBag<VV> collectIf(Predicate<? super V> predicate, Function<? super V, ? extends VV> function)
    {
        return this.collectIf(predicate, function, HashBag.<VV>newBag());
    }

    @Override
    public <VV, R extends Collection<VV>> R collectIf(Predicate<? super V> predicate, Function<? super V, ? extends VV> function, R target)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue))
            {
                target.add(function.valueOf(this.sentinelValues.zeroValue));
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue))
            {
                target.add(function.valueOf(this.sentinelValues.oneValue));
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i]))
            {
                target.add(function.valueOf(this.values[i]));
            }
        }
        return target;
    }

    @Override
    public <VV> MutableBag<VV> flatCollect(Function<? super V, ? extends Iterable<VV>> function)
    {
        return this.flatCollect(function, new HashBag<VV>());
    }

    @Override
    public <VV, R extends Collection<VV>> R flatCollect(Function<? super V, ? extends Iterable<VV>> function, R target)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                Iterate.addAllTo(function.valueOf(this.sentinelValues.zeroValue), target);
            }
            if (this.sentinelValues.containsOneKey)
            {
                Iterate.addAllTo(function.valueOf(this.sentinelValues.oneValue), target);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                Iterate.addAllTo(function.valueOf(this.values[i]), target);
            }
        }
        return target;
    }

    @Override
    public V detect(Predicate<? super V> predicate)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue))
            {
                return this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue))
            {
                return this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i]))
            {
                return this.values[i];
            }
        }
        return null;
    }

    @Override
    public <P> V detectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue, parameter))
            {
                return this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue, parameter))
            {
                return this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i], parameter))
            {
                return this.values[i];
            }
        }
        return null;
    }

    @Override
    public Optional<V> detectOptional(Predicate<? super V> predicate)
    {
        return Optional.ofNullable(this.detect(predicate));
    }

    @Override
    public <P> Optional<V> detectWithOptional(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return Optional.ofNullable(this.detectWith(predicate, parameter));
    }

    @Override
    public V detectIfNone(Predicate<? super V> predicate, Function0<? extends V> function)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue))
            {
                return this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue))
            {
                return this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i]))
            {
                return this.values[i];
            }
        }
        return function.value();
    }

    @Override
    public <P> V detectWithIfNone(Predicate2<? super V, ? super P> predicate, P parameter, Function0<? extends V> function)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue, parameter))
            {
                return this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue, parameter))
            {
                return this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i], parameter))
            {
                return this.values[i];
            }
        }
        return function.value();
    }

    @Override
    public int count(Predicate<? super V> predicate)
    {
        int count = 0;
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue))
            {
                count++;
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue))
            {
                count++;
            }
        }
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
    public <P> int countWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        int count = 0;
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue, parameter))
            {
                count++;
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue, parameter))
            {
                count++;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i], parameter))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean anySatisfy(Predicate<? super V> predicate)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue))
            {
                return true;
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue))
            {
                return true;
            }
        }
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
    public <P> boolean anySatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && predicate.accept(this.sentinelValues.zeroValue, parameter))
            {
                return true;
            }
            if (this.sentinelValues.containsOneKey && predicate.accept(this.sentinelValues.oneValue, parameter))
            {
                return true;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && predicate.accept(this.values[i], parameter))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allSatisfy(Predicate<? super V> predicate)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && !predicate.accept(this.sentinelValues.zeroValue))
            {
                return false;
            }
            if (this.sentinelValues.containsOneKey && !predicate.accept(this.sentinelValues.oneValue))
            {
                return false;
            }
        }
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
    public <P> boolean allSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey && !predicate.accept(this.sentinelValues.zeroValue, parameter))
            {
                return false;
            }
            if (this.sentinelValues.containsOneKey && !predicate.accept(this.sentinelValues.oneValue, parameter))
            {
                return false;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && !predicate.accept(this.values[i], parameter))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean noneSatisfy(Predicate<? super V> predicate)
    {
        return !this.anySatisfy(predicate);
    }

    @Override
    public <P> boolean noneSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return !this.anySatisfyWith(predicate, parameter);
    }

    @Override
    public <IV> IV injectInto(IV injectedValue, Function2<? super IV, ? super V, ? extends IV> function)
    {
        IV result = injectedValue;
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                result = function.value(result, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result = function.value(result, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result = function.value(result, this.values[i]);
            }
        }
        return result;
    }

    @Override
    public int injectInto(int injectedValue, IntObjectToIntFunction<? super V> function)
    {
        int result = injectedValue;
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                result = function.intValueOf(result, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result = function.intValueOf(result, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result = function.intValueOf(result, this.values[i]);
            }
        }
        return result;
    }

    @Override
    public long injectInto(long injectedValue, LongObjectToLongFunction<? super V> function)
    {
        long result = injectedValue;
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                result = function.longValueOf(result, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result = function.longValueOf(result, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result = function.longValueOf(result, this.values[i]);
            }
        }
        return result;
    }

    @Override
    public float injectInto(float injectedValue, FloatObjectToFloatFunction<? super V> function)
    {
        float result = injectedValue;
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                result = function.floatValueOf(result, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result = function.floatValueOf(result, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result = function.floatValueOf(result, this.values[i]);
            }
        }
        return result;
    }

    @Override
    public double injectInto(double injectedValue, DoubleObjectToDoubleFunction<? super V> function)
    {
        double result = injectedValue;
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                result = function.doubleValueOf(result, this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                result = function.doubleValueOf(result, this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                result = function.doubleValueOf(result, this.values[i]);
            }
        }
        return result;
    }

    /**
     * @deprecated in 7.0. Use {@link org.eclipse.collections.api.ordered.OrderedIterable#zip(Iterable)} instead.
     */
    @Deprecated
    @Override
    public <S> MutableBag<Pair<V, S>> zip(Iterable<S> that)
    {
        if (that instanceof Collection || that instanceof RichIterable)
        {
            int thatSize = Iterate.sizeOf(that);
            HashBag<Pair<V, S>> target = HashBag.newBag(Math.min(this.size(), thatSize));
            return this.zip(that, target);
        }
        return this.zip(that, HashBag.newBag());
    }

    @Override
    public <S, R extends Collection<Pair<V, S>>> R zip(Iterable<S> that, R target)
    {
        return IterableIterate.zip(this, that, target);
    }

    /**
     * @deprecated in 7.0. Use {@link org.eclipse.collections.api.ordered.OrderedIterable#zipWithIndex()} instead.
     */
    @Deprecated
    @Override
    public MutableSet<Pair<V, Integer>> zipWithIndex()
    {
        return this.zipWithIndex(UnifiedSet.<Pair<V, Integer>>newSet(this.size()));
    }

    @Override
    public <R extends Collection<Pair<V, Integer>>> R zipWithIndex(R target)
    {
        return IterableIterate.zipWithIndex(this, target);
    }

    @Override
    public RichIterable<RichIterable<V>> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<RichIterable<V>> result = Lists.mutable.of();
        if (this.notEmpty())
        {
            Iterator<V> iterator = this.iterator();
            while (iterator.hasNext())
            {
                MutableList<V> batch = Lists.mutable.empty();
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
    public <K, VV> MutableMap<K, VV> aggregateInPlaceBy(Function<? super V, ? extends K> groupBy, Function0<? extends VV> zeroValueFactory, Procedure2<? super VV, ? super V> mutatingAggregator)
    {
        MutableMap<K, VV> map = UnifiedMap.newMap();
        this.forEach(new MutatingAggregationProcedure<V, K, VV>(map, groupBy, zeroValueFactory, mutatingAggregator));
        return map;
    }

    @Override
    public <VV> MutableBagMultimap<VV, V> groupBy(Function<? super V, ? extends VV> function)
    {
        return this.groupBy(function, HashBagMultimap.<VV, V>newMultimap());
    }

    @Override
    public <VV, R extends MutableMultimap<VV, V>> R groupBy(Function<? super V, ? extends VV> function, R target)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                target.put(function.valueOf(this.sentinelValues.zeroValue), this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                target.put(function.valueOf(this.sentinelValues.oneValue), this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                target.put(function.valueOf(this.values[i]), this.values[i]);
            }
        }
        return target;
    }

    @Override
    public <VV> MutableBagMultimap<VV, V> groupByEach(Function<? super V, ? extends Iterable<VV>> function)
    {
        return this.groupByEach(function, HashBagMultimap.<VV, V>newMultimap());
    }

    @Override
    public <VV, R extends MutableMultimap<VV, V>> R groupByEach(
            Function<? super V, ? extends Iterable<VV>> function,
            R target)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                Iterable<VV> iterable = function.valueOf(this.sentinelValues.zeroValue);
                for (VV key : iterable)
                {
                    target.put(key, this.sentinelValues.zeroValue);
                }
            }
            if (this.sentinelValues.containsOneKey)
            {
                Iterable<VV> iterable = function.valueOf(this.sentinelValues.oneValue);
                for (VV key : iterable)
                {
                    target.put(key, this.sentinelValues.oneValue);
                }
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                Iterable<VV> iterable = function.valueOf(this.values[i]);
                for (VV key : iterable)
                {
                    target.put(key, this.values[i]);
                }
            }
        }
        return target;
    }

    @Override
    public <VV> MutableMap<VV, V> groupByUniqueKey(Function<? super V, ? extends VV> function)
    {
        return this.groupByUniqueKey(function, UnifiedMap.<VV, V>newMap(this.size()));
    }

    @Override
    public <VV, R extends MutableMapIterable<VV, V>> R groupByUniqueKey(
                Function<? super V, ? extends VV> function,
                R target)
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                if (target.put(function.valueOf(this.sentinelValues.zeroValue), this.sentinelValues.zeroValue) != null)
                {
                    throw new IllegalStateException("Key " + function.valueOf(this.sentinelValues.zeroValue) + " already exists in map!");
                }
            }
            if (this.sentinelValues.containsOneKey)
            {
                if (target.put(function.valueOf(this.sentinelValues.oneValue), this.sentinelValues.oneValue) != null)
                {
                    throw new IllegalStateException("Key " + function.valueOf(this.sentinelValues.oneValue) + " already exists in map!");
                }
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                if (target.put(function.valueOf(this.values[i]), this.values[i]) != null)
                {
                    throw new IllegalStateException("Key " + function.valueOf(this.values[i]) + " already exists in map!");
                }
            }
        }
        return target;
    }

    @Override
    public V getFirst()
    {
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                return this.values[i];
            }
        }
        return null;
    }

    @Override
    public V getLast()
    {
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                return this.values[i];
            }
        }
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
        }
        return null;
    }

    @Override
    public V getOnly()
    {
        if (this.size() != 1)
        {
            throw new IllegalStateException("Size must be 1 but was " + this.size());
        }

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                return this.values[i];
            }
        }

        return null;
    }

    @Override
    public MutableList<V> toList()
    {
        MutableList<V> list = Lists.mutable.of();
        this.forEachWith(Procedures2.<V>addToCollection(), list);
        return list;
    }

    @Override
    public <R extends Collection<V>> R into(R target)
    {
        return Iterate.addAllTo(this, target);
    }

    @Override
    public MutableSet<V> toSet()
    {
        MutableSet<V> set = UnifiedSet.newSet();
        this.forEachWith(Procedures2.<V>addToCollection(), set);
        return set;
    }

    @Override
    public MutableSortedSet<V> toSortedSet()
    {
        MutableSortedSet<V> set = TreeSortedSet.newSet();
        this.forEachWith(Procedures2.<V>addToCollection(), set);
        return set;
    }

    @Override
    public MutableSortedSet<V> toSortedSet(Comparator<? super V> comparator)
    {
        MutableSortedSet<V> set = TreeSortedSet.newSet(comparator);
        this.forEachWith(Procedures2.<V>addToCollection(), set);
        return set;
    }

    @Override
    public MutableBag<V> toBag()
    {
        MutableBag<V> bag = Bags.mutable.of();
        this.forEachWith(Procedures2.<V>addToCollection(), bag);
        return bag;
    }

    @Override
    public MutableSortedBag<V> toSortedBag()
    {
        MutableSortedBag<V> sortedBag = TreeBag.newBag();
        this.forEachWith(Procedures2.<V>addToCollection(), sortedBag);
        return sortedBag;
    }

    @Override
    public MutableSortedBag<V> toSortedBag(Comparator<? super V> comparator)
    {
        MutableSortedBag<V> sortedBag = TreeBag.newBag(comparator);
        this.forEachWith(Procedures2.<V>addToCollection(), sortedBag);
        return sortedBag;
    }

    @Override
    public <NK, NV> MutableMap<NK, NV> toMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        UnifiedMap<NK, NV> map = UnifiedMap.newMap();
        this.forEach(new MapCollectProcedure<V, NK, NV>(map, keyFunction, valueFunction));
        return map;
    }

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return this.toSortedMap(Comparators.naturalOrder(), keyFunction, valueFunction);
    }

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Comparator<? super NK> comparator, Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        TreeSortedMap<NK, NV> sortedMap = TreeSortedMap.newMap(comparator);
        this.forEach(new MapCollectProcedure<V, NK, NV>(sortedMap, keyFunction, valueFunction));
        return sortedMap;
    }

    @Override
    public <NK, NV> MutableBiMap<NK, NV> toBiMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        MutableBiMap<NK, NV> biMap = BiMaps.mutable.empty();
        this.forEach(new BiMapCollectProcedure<>(biMap, keyFunction, valueFunction));
        return biMap;
    }

    @Override
    public LazyIterable<V> asLazy()
    {
        return LazyIterate.adapt(this);
    }

    @Override
    public V min(Comparator<? super V> comparator)
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }

        V min = null;
        boolean isMinSet = false;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                min = this.sentinelValues.zeroValue;
                isMinSet = true;
            }
            if (this.sentinelValues.containsOneKey && (!isMinSet || comparator.compare(min, this.sentinelValues.oneValue) > 0))
            {
                min = this.sentinelValues.oneValue;
                isMinSet = true;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMinSet || comparator.compare(min, this.values[i]) > 0))
            {
                min = this.values[i];
                isMinSet = true;
            }
        }
        return min;
    }

    @Override
    public V max(Comparator<? super V> comparator)
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }

        V max = null;
        boolean isMaxSet = false;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                max = this.sentinelValues.zeroValue;
                isMaxSet = true;
            }
            if (this.sentinelValues.containsOneKey && (!isMaxSet || comparator.compare(max, this.sentinelValues.oneValue) < 0))
            {
                max = this.sentinelValues.oneValue;
                isMaxSet = true;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && (!isMaxSet || comparator.compare(max, this.values[i]) < 0))
            {
                max = this.values[i];
                isMaxSet = true;
            }
        }
        return max;
    }

    @Override
    public V min()
    {
        return this.min(Comparators.naturalOrder());
    }

    @Override
    public V max()
    {
        return this.max(Comparators.naturalOrder());
    }

    @Override
    public <VV extends Comparable<? super VV>> V maxBy(Function<? super V, ? extends VV> function)
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }

        V max = null;
        boolean isMaxSet = false;
        VV maxValue = null;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                max = this.sentinelValues.zeroValue;
                isMaxSet = true;
                maxValue = function.valueOf(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                VV nextValue = function.valueOf(this.sentinelValues.oneValue);
                if (!isMaxSet || nextValue.compareTo(maxValue) > 0)
                {
                    max = this.sentinelValues.oneValue;
                    isMaxSet = true;
                    maxValue = nextValue;
                }
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                V next = this.values[i];
                VV nextValue = function.valueOf(next);
                if (!isMaxSet || nextValue.compareTo(maxValue) > 0)
                {
                    max = next;
                    isMaxSet = true;
                    maxValue = nextValue;
                }
            }
        }
        return max;
    }

    @Override
    public <VV extends Comparable<? super VV>> V minBy(Function<? super V, ? extends VV> function)
    {
        if (this.isEmpty())
        {
            throw new NoSuchElementException();
        }

        V min = null;
        boolean isMinSet = false;
        VV minValue = null;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                min = this.sentinelValues.zeroValue;
                isMinSet = true;
                minValue = function.valueOf(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                VV nextValue = function.valueOf(this.sentinelValues.oneValue);
                if (!isMinSet || nextValue.compareTo(minValue) < 0)
                {
                    min = this.sentinelValues.oneValue;
                    isMinSet = true;
                    minValue = nextValue;
                }
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                V next = this.values[i];
                VV nextValue = function.valueOf(next);
                if (!isMinSet || nextValue.compareTo(minValue) < 0)
                {
                    min = next;
                    isMinSet = true;
                    minValue = nextValue;
                }
            }
        }
        return min;
    }

    @Override
    public long sumOfInt(IntFunction<? super V> function)
    {
        long sum = 0L;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                sum += function.intValueOf(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                sum += function.intValueOf(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                sum += function.intValueOf(this.values[i]);
            }
        }
        return sum;
    }

    @Override
    public double sumOfFloat(FloatFunction<? super V> function)
    {
        double sum = 0.0;
        double compensation = 0.0;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                double adjustedValue = function.floatValueOf(this.sentinelValues.zeroValue) - compensation;
                double nextSum = sum + adjustedValue;
                compensation = nextSum - sum - adjustedValue;
                sum = nextSum;
            }
            if (this.sentinelValues.containsOneKey)
            {
                double adjustedValue = function.floatValueOf(this.sentinelValues.oneValue) - compensation;
                double nextSum = sum + adjustedValue;
                compensation = nextSum - sum - adjustedValue;
                sum = nextSum;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                double adjustedValue = function.floatValueOf(this.values[i]) - compensation;
                double nextSum = sum + adjustedValue;
                compensation = nextSum - sum - adjustedValue;
                sum = nextSum;
            }
        }
        return sum;
    }

    @Override
    public long sumOfLong(LongFunction<? super V> function)
    {
        long sum = 0L;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                sum += function.longValueOf(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                sum += function.longValueOf(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                sum += function.longValueOf(this.values[i]);
            }
        }
        return sum;
    }

    @Override
    public double sumOfDouble(DoubleFunction<? super V> function)
    {
        double sum = 0.0;
        double compensation = 0.0;

        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                double adjustedValue = function.doubleValueOf(this.sentinelValues.zeroValue) - compensation;
                double nextSum = sum + adjustedValue;
                compensation = nextSum - sum - adjustedValue;
                sum = nextSum;
            }
            if (this.sentinelValues.containsOneKey)
            {
                double adjustedValue = function.doubleValueOf(this.sentinelValues.oneValue) - compensation;
                double nextSum = sum + adjustedValue;
                compensation = nextSum - sum - adjustedValue;
                sum = nextSum;
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                double adjustedValue = function.doubleValueOf(this.values[i]) - compensation;
                double nextSum = sum + adjustedValue;
                compensation = nextSum - sum - adjustedValue;
                sum = nextSum;
            }
        }
        return sum;
    }

    @Override
    public <V1> MutableObjectLongMap<V1> sumByInt(Function<? super V, ? extends V1> groupBy, IntFunction<? super V> function)
    {
        MutableObjectLongMap<V1> result = ObjectLongMaps.mutable.empty();
        return this.injectInto(result, PrimitiveFunctions.sumByIntFunction(groupBy, function));
    }

    @Override
    public <V1> MutableObjectDoubleMap<V1> sumByFloat(Function<? super V, ? extends V1> groupBy, FloatFunction<? super V> function)
    {
        MutableObjectDoubleMap<V1> result = ObjectDoubleMaps.mutable.empty();
        return this.injectInto(result, PrimitiveFunctions.sumByFloatFunction(groupBy, function));
    }

    @Override
    public <V1> MutableObjectLongMap<V1> sumByLong(Function<? super V, ? extends V1> groupBy, LongFunction<? super V> function)
    {
        MutableObjectLongMap<V1> result = ObjectLongMaps.mutable.empty();
        return this.injectInto(result, PrimitiveFunctions.sumByLongFunction(groupBy, function));
    }

    @Override
    public <V1> MutableObjectDoubleMap<V1> sumByDouble(Function<? super V, ? extends V1> groupBy, DoubleFunction<? super V> function)
    {
        MutableObjectDoubleMap<V1> result = ObjectDoubleMaps.mutable.empty();
        return this.injectInto(result, PrimitiveFunctions.sumByDoubleFunction(groupBy, function));
    }

    @Override
    public void clear()
    {
        this.sentinelValues = null;
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;
        if (this.copyKeysOnWrite)
        {
            this.keys = new float[this.keys.length];
            this.copyKeysOnWrite = false;
        }
        Arrays.fill(this.keys, EMPTY_KEY);
        Arrays.fill(this.values, null);
    }

    @Override
    public V put(float key, V value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues<>();
            }
            V oldValue = this.sentinelValues.zeroValue;
            this.sentinelValues.containsZeroKey = true;
            this.sentinelValues.zeroValue = value;
            return oldValue;
        }

        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues<>();
            }
            V oldValue = this.sentinelValues.oneValue;
            this.sentinelValues.containsOneKey = true;
            this.sentinelValues.oneValue = value;
            return oldValue;
        }

        int index = this.probe(key);

        if (Float.compare(this.keys[index], key) == 0)
        {
            // key already present in map
            V oldValue = this.values[index];
            this.values[index] = value;
            return oldValue;
        }

        this.addKeyValueAtIndex(key, value, index);
        return null;
    }

    @Override
    public void putAll(FloatObjectMap<? extends V> map)
    {
        map.forEachKeyValue((float key, V value) -> FloatObjectHashMap.this.put(key, value));
    }

    @Override
    public boolean containsKey(float key)
    {
        if (isEmptyKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsZeroKey;
        }
        if (isRemovedKey(key))
        {
            return this.sentinelValues != null && this.sentinelValues.containsOneKey;
        }
        return Float.compare(this.keys[this.probe(key)], key) == 0;
    }

    @Override
    public boolean containsValue(Object value)
    {
        if (this.sentinelValues != null && this.sentinelValues.containsValue((V) value))
        {
            return true;
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]) && nullSafeEquals(this.values[i], value))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(float key)
    {
        return this.getIfAbsent(key, Functions0.<V>nullValue());
    }

    @Override
    public V getIfAbsent(float key, Function0<? extends V> ifAbsent)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return ifAbsent.value();
            }
            return this.sentinelValues.zeroValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                return ifAbsent.value();
            }
            return this.sentinelValues.oneValue;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        return ifAbsent.value();
    }

    @Override
    public V getIfAbsentPut(float key, V value)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsZeroKey = true;
                this.sentinelValues.zeroValue = value;
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            this.sentinelValues.containsZeroKey = true;
            this.sentinelValues.zeroValue = value;
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsOneKey = true;
                this.sentinelValues.oneValue = value;
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            this.sentinelValues.containsOneKey = true;
            this.sentinelValues.oneValue = value;
            return value;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public V getIfAbsentPut(float key, Function0<? extends V> function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                V value = function.value();
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsZeroKey = true;
                this.sentinelValues.zeroValue = value;
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            V value = function.value();
            this.sentinelValues.containsZeroKey = true;
            this.sentinelValues.zeroValue = value;
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                V value = function.value();
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsOneKey = true;
                this.sentinelValues.oneValue = value;
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            V value = function.value();
            this.sentinelValues.containsOneKey = true;
            this.sentinelValues.oneValue = value;
            return value;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        V value = function.value();
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> V getIfAbsentPutWith(float key, Function<? super P, ? extends V> function, P parameter)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                V value = function.valueOf(parameter);
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsZeroKey = true;
                this.sentinelValues.zeroValue = value;
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            V value = function.valueOf(parameter);
            this.sentinelValues.containsZeroKey = true;
            this.sentinelValues.zeroValue = value;
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                V value = function.valueOf(parameter);
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsOneKey = true;
                this.sentinelValues.oneValue = value;
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            V value = function.valueOf(parameter);
            this.sentinelValues.containsOneKey = true;
            this.sentinelValues.oneValue = value;
            return value;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        V value = function.valueOf(parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public V getIfAbsentPutWithKey(float key, FloatToObjectFunction<? extends V> function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                V value = function.valueOf(key);
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsZeroKey = true;
                this.sentinelValues.zeroValue = value;
                return value;
            }
            if (this.sentinelValues.containsZeroKey)
            {
                return this.sentinelValues.zeroValue;
            }
            V value = function.valueOf(key);
            this.sentinelValues.containsZeroKey = true;
            this.sentinelValues.zeroValue = value;
            return value;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                V value = function.valueOf(key);
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsOneKey = true;
                this.sentinelValues.oneValue = value;
                return value;
            }
            if (this.sentinelValues.containsOneKey)
            {
                return this.sentinelValues.oneValue;
            }
            V value = function.valueOf(key);
            this.sentinelValues.containsOneKey = true;
            this.sentinelValues.oneValue = value;
            return value;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            return this.values[index];
        }
        V value = function.valueOf(key);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public V updateValue(float key, Function0<? extends V> factory, Function<? super V, ? extends V> function)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsZeroKey = true;
                this.sentinelValues.zeroValue = function.valueOf(factory.value());
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                this.sentinelValues.zeroValue = function.valueOf(this.sentinelValues.zeroValue);
            }
            else
            {
                this.sentinelValues.containsZeroKey = true;
                this.sentinelValues.zeroValue = function.valueOf(factory.value());
            }
            return this.sentinelValues.zeroValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsOneKey = true;
                this.sentinelValues.oneValue = function.valueOf(factory.value());
            }
            else if (this.sentinelValues.containsOneKey)
            {
                this.sentinelValues.oneValue = function.valueOf(this.sentinelValues.oneValue);
            }
            else
            {
                this.sentinelValues.containsOneKey = true;
                this.sentinelValues.oneValue = function.valueOf(factory.value());
            }
            return this.sentinelValues.oneValue;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        V value = function.valueOf(factory.value());
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public <P> V updateValueWith(float key, Function0<? extends V> factory, Function2<? super V, ? super P, ? extends V> function, P parameter)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsZeroKey = true;
                this.sentinelValues.zeroValue = function.value(factory.value(), parameter);
            }
            else if (this.sentinelValues.containsZeroKey)
            {
                this.sentinelValues.zeroValue = function.value(this.sentinelValues.zeroValue, parameter);
            }
            else
            {
                this.sentinelValues.containsZeroKey = true;
                this.sentinelValues.zeroValue = function.value(factory.value(), parameter);
            }
            return this.sentinelValues.zeroValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null)
            {
                this.sentinelValues = new SentinelValues<>();
                this.sentinelValues.containsOneKey = true;
                this.sentinelValues.oneValue = function.value(factory.value(), parameter);
            }
            else if (this.sentinelValues.containsOneKey)
            {
                this.sentinelValues.oneValue = function.value(this.sentinelValues.oneValue, parameter);
            }
            else
            {
                this.sentinelValues.containsOneKey = true;
                this.sentinelValues.oneValue = function.value(factory.value(), parameter);
            }
            return this.sentinelValues.oneValue;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            this.values[index] = function.value(this.values[index], parameter);
            return this.values[index];
        }
        V value = function.value(factory.value(), parameter);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    @Override
    public V removeKey(float key)
    {
        if (isEmptyKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsZeroKey)
            {
                return null;
            }
            V oldValue = this.sentinelValues.zeroValue;
            if (this.sentinelValues.containsOneKey)
            {
                this.sentinelValues.containsZeroKey = false;
                this.sentinelValues.zeroValue = null;
            }
            else
            {
                this.sentinelValues = null;
            }
            return oldValue;
        }
        if (isRemovedKey(key))
        {
            if (this.sentinelValues == null || !this.sentinelValues.containsOneKey)
            {
                return null;
            }
            V oldValue = this.sentinelValues.oneValue;
            if (this.sentinelValues.containsZeroKey)
            {
                this.sentinelValues.containsOneKey = false;
                this.sentinelValues.oneValue = null;
            }
            else
            {
                this.sentinelValues = null;
            }
            return oldValue;
        }
        int index = this.probe(key);
        if (Float.compare(this.keys[index], key) == 0)
        {
            V oldValue = this.values[index];
            this.removeKeyAtIndex(index);
            return oldValue;
        }
        return null;
    }

    @Override
    public V remove(float key)
    {
        return this.removeKey(key);
    }

    @Override
    public FloatObjectHashMap<V> withKeyValue(float key, V value)
    {
        this.put(key, value);
        return this;
    }

    @Override
    public MutableFloatObjectMap<V> withoutKey(float key)
    {
        this.removeKey(key);
        return this;
    }

    @Override
    public MutableFloatObjectMap<V> withoutAllKeys(FloatIterable keys)
    {
        FloatIterator iterator = keys.floatIterator();
        while (iterator.hasNext())
        {
            float item = iterator.next();
            this.removeKey(item);
        }
        return this;
    }

    public FloatObjectHashMap<V> withKeysValues(float key1, V value1, float key2, V value2)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        return this;
    }

    public FloatObjectHashMap<V> withKeysValues(float key1, V value1, float key2, V value2, float key3, V value3)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        return this;
    }

    public FloatObjectHashMap<V> withKeysValues(float key1, V value1, float key2, V value2, float key3, V value3, float key4, V value4)
    {
        this.put(key1, value1);
        this.put(key2, value2);
        this.put(key3, value3);
        this.put(key4, value4);
        return this;
    }

    @Override
    public MutableFloatObjectMap<V> asUnmodifiable()
    {
        return new UnmodifiableFloatObjectMap<>(this);
    }

    @Override
    public MutableFloatObjectMap<V> asSynchronized()
    {
        return new SynchronizedFloatObjectMap<>(this);
    }

    @Override
    public ImmutableFloatObjectMap<V> toImmutable()
    {
        return FloatObjectMaps.immutable.withAll(this);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(this.size());
        if (this.sentinelValues != null)
        {
            if (this.sentinelValues.containsZeroKey)
            {
                out.writeFloat(EMPTY_KEY);
                out.writeObject(this.sentinelValues.zeroValue);
            }
            if (this.sentinelValues.containsOneKey)
            {
                out.writeFloat(REMOVED_KEY);
                out.writeObject(this.sentinelValues.oneValue);
            }
        }
        for (int i = 0; i < this.keys.length; i++)
        {
            if (isNonSentinel(this.keys[i]))
            {
                out.writeFloat(this.keys[i]);
                out.writeObject(this.values[i]);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        int size = in.readInt();
        for (int i = 0; i < size; i++)
        {
            this.put(in.readFloat(), (V) in.readObject());
        }
    }

    private void addKeyValueAtIndex(float key, V value, int index)
    {
        if (Float.compare(this.keys[index], REMOVED_KEY) == 0)
        {
            this.occupiedWithSentinels--;
        }
        if (this.copyKeysOnWrite)
        {
            this.copyKeys();
        }
        this.keys[index] = key;
        this.values[index] = value;
        ++this.occupiedWithData;
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
        this.values[index] = null;
        this.occupiedWithData--;
        this.occupiedWithSentinels++;
    }

    private void copyKeys()
    {
        float[] copy = new float[this.keys.length];
        System.arraycopy(this.keys, 0, copy, 0, this.keys.length);
        this.keys = copy;
        this.copyKeysOnWrite = false;
    }

    private static final class SentinelValues<V> extends AbstractSentinelValues
    {
        private V zeroValue;
        private V oneValue;

        public int size()
        {
            return (this.containsZeroKey ? 1 : 0) + (this.containsOneKey ? 1 : 0);
        }

        public boolean containsValue(V value)
        {
            boolean valueEqualsZeroValue = this.containsZeroKey && nullSafeEquals(this.zeroValue, value);
            boolean valueEqualsOneValue = this.containsOneKey && nullSafeEquals(this.oneValue, value);
            return valueEqualsZeroValue || valueEqualsOneValue;
        }
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

    private class InternalIterator implements Iterator<V>
    {
        private int count;
        private int position;
        private float currentKey;
        private boolean isCurrentKeySet;
        private boolean handledZeroKey;
        private boolean handledOneKey;

        @Override
        public boolean hasNext()
        {
            return this.count != FloatObjectHashMap.this.size();
        }

        @Override
        public V next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            this.count++;
            if (!this.handledZeroKey)
            {
                this.handledZeroKey = true;
                if (FloatObjectHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.currentKey = FloatObjectHashMap.EMPTY_KEY;
                    this.isCurrentKeySet = true;
                    return FloatObjectHashMap.this.sentinelValues.zeroValue;
                }
            }
            if (!this.handledOneKey)
            {
                this.handledOneKey = true;
                if (FloatObjectHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.currentKey = FloatObjectHashMap.REMOVED_KEY;
                    this.isCurrentKeySet = true;
                    return FloatObjectHashMap.this.sentinelValues.oneValue;
                }
            }
            float[] keys = FloatObjectHashMap.this.keys;
            while (!isNonSentinel(keys[this.position]))
            {
                this.position++;
            }
            this.currentKey = FloatObjectHashMap.this.keys[this.position];
            this.isCurrentKeySet = true;
            V result = FloatObjectHashMap.this.values[this.position];
            this.position++;
            return result;
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
                FloatObjectHashMap.this.removeKeyAtIndex(index);
            }
            else
            {
                FloatObjectHashMap.this.removeKey(this.currentKey);
            }
        }
    }

    @Override
    public MutableFloatSet keySet()
    {
        return new KeySet();
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
        float[] old = this.keys;
        V[] oldValues = this.values;
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
        this.copyKeysOnWrite = false;
    }

    // exposed for testing
    int probe(float element)
    {
        int index = this.mask((int) element);
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
        int nextIndex = (int) SpreadFunctions.floatSpreadOne(element);
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

    protected void allocateTable(int sizeToAllocate)
    {
        this.keys = new float[sizeToAllocate];
        this.values = (V[]) new Object[sizeToAllocate];
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

    private int maxOccupiedWithData()
    {
        int capacity = this.keys.length;
        // need at least one free slot for open addressing
        return Math.min(capacity - 1, capacity >> 1);
    }

    private class KeysMapWrapper implements MutableFloatKeysMap
    {
        @Override
        public int size()
        {
            return FloatObjectHashMap.this.size();
        }

        @Override
        public boolean containsKey(float key)
        {
            return FloatObjectHashMap.this.containsKey(key);
        }

        @Override
        public void forEachKey(FloatProcedure procedure)
        {
            FloatObjectHashMap.this.forEachKey(procedure);
        }

        @Override
        public boolean isEmpty()
        {
            return FloatObjectHashMap.this.isEmpty();
        }

        @Override
        public boolean notEmpty()
        {
            return FloatObjectHashMap.this.notEmpty();
        }

        @Override
        public void removeKey(float key)
        {
            FloatObjectHashMap.this.removeKey(key);
        }

        @Override
        public void clear()
        {
            FloatObjectHashMap.this.clear();
        }
    }

    private class KeySet extends AbstractMutableFloatKeySet
    {
        private KeysMapWrapper outer = new KeysMapWrapper();

        @Override
        protected MutableFloatKeysMap getOuter()
        {
            return outer;
        }

        @Override
        protected SentinelValues getSentinelValues()
        {
            return FloatObjectHashMap.this.sentinelValues;
        }

        @Override
        protected float getKeyAtIndex(int index)
        {
            return FloatObjectHashMap.this.keys[index];
        }

        @Override
        protected int getTableSize()
        {
            return FloatObjectHashMap.this.keys.length;
        }

        @Override
        public MutableFloatIterator floatIterator()
        {
            return new KeysSetIterator();
        }

        @Override
        public boolean retainAll(FloatIterable source)
        {
            int oldSize = FloatObjectHashMap.this.size();
            final FloatSet sourceSet = source instanceof FloatSet ? (FloatSet) source : source.toSet();
            FloatObjectHashMap<V> retained = FloatObjectHashMap.this.select((float key, V value) -> sourceSet.contains(key));
            if (retained.size() != oldSize)
            {
                FloatObjectHashMap.this.keys = retained.keys;
                FloatObjectHashMap.this.values = retained.values;
                FloatObjectHashMap.this.sentinelValues = retained.sentinelValues;
                FloatObjectHashMap.this.occupiedWithData = retained.occupiedWithData;
                FloatObjectHashMap.this.occupiedWithSentinels = retained.occupiedWithSentinels;
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
            FloatObjectHashMap.this.copyKeysOnWrite = true;
            boolean containsZeroKey = false;
            boolean containsOneKey = false;
            if (FloatObjectHashMap.this.sentinelValues != null)
            {
                containsZeroKey = FloatObjectHashMap.this.sentinelValues.containsZeroKey;
                containsOneKey = FloatObjectHashMap.this.sentinelValues.containsOneKey;
            }
            return new ImmutableFloatMapKeySet(FloatObjectHashMap.this.keys, FloatObjectHashMap.this.occupiedWithData, containsZeroKey, containsOneKey);
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

    private class KeysSetIterator implements MutableFloatIterator
    {
        private int count;
        private int position;
        private float lastKey;
        private boolean handledZeroKey;
        private boolean handledOneKey;
        private boolean canRemove;

        @Override
        public boolean hasNext()
        {
            return this.count != FloatObjectHashMap.this.size();
        }

        @Override
        public float next()
        {
            if (!this.hasNext())
            {
                throw new NoSuchElementException();
            }
            this.count++;
            this.canRemove = true;

            if (!this.handledZeroKey)
            {
                this.handledZeroKey = true;
                if (FloatObjectHashMap.this.containsKey(EMPTY_KEY))
                {
                    this.lastKey = EMPTY_KEY;
                    return this.lastKey;
                }
            }
            if (!this.handledOneKey)
            {
                this.handledOneKey = true;
                if (FloatObjectHashMap.this.containsKey(REMOVED_KEY))
                {
                    this.lastKey = REMOVED_KEY;
                    return this.lastKey;
                }
            }
            float[] keys = FloatObjectHashMap.this.keys;
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
            FloatObjectHashMap.this.removeKey(this.lastKey);
            this.count--;
            this.canRemove = false;
        }
    }

    @Override
    public Collection<V> values()
    {
        return new ValuesCollection();
    }

    @Override
    public LazyFloatIterable keysView()
    {
        return new KeysView();
    }

    @Override
    public RichIterable<FloatObjectPair<V>> keyValuesView()
    {
        return new KeyValuesView();
    }

    @Override
    public MutableObjectFloatMap<V> flipUniqueValues()
    {
        MutableObjectFloatMap<V> result = ObjectFloatMaps.mutable.empty();
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

    protected class ValuesCollection implements Collection<V>
    {
        @Override
        public boolean add(V v)
        {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        @Override
        public boolean addAll(Collection<? extends V> collection)
        {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        @Override
        public void clear()
        {
            FloatObjectHashMap.this.clear();
        }

        @Override
        public boolean contains(Object o)
        {
            return FloatObjectHashMap.this.containsValue(o);
        }

        @Override
        public boolean containsAll(Collection<?> collection)
        {
            // todo: this is N^2. if c is large, we should copy the values to a set.
            return Iterate.allSatisfy(collection, Predicates.in(this));
        }

        @Override
        public boolean isEmpty()
        {
            return FloatObjectHashMap.this.isEmpty();
        }

        @Override
        public Iterator<V> iterator()
        {
            return FloatObjectHashMap.this.iterator();
        }

        @Override
        public boolean remove(Object o)
        {
            // this is so slow that the extra overhead of the iterator won't be noticeable
            if (o == null)
            {
                for (Iterator<V> it = this.iterator(); it.hasNext(); )
                {
                    if (it.next() == null)
                    {
                        it.remove();
                        return true;
                    }
                }
            }
            else
            {
                for (Iterator<V> it = this.iterator(); it.hasNext(); )
                {
                    V o2 = it.next();
                    if (o == o2 || o2.equals(o))
                    {
                        it.remove();
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> collection)
        {
            // todo: this is N^2. if c is large, we should copy the values to a set.
            boolean changed = false;

            for (Object obj : collection)
            {
                if (this.remove(obj))
                {
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean retainAll(Collection<?> collection)
        {
            boolean modified = false;
            Iterator<V> e = this.iterator();
            while (e.hasNext())
            {
                if (!collection.contains(e.next()))
                {
                    e.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public int size()
        {
            return FloatObjectHashMap.this.size();
        }

        @Override
        public Object[] toArray()
        {
            return FloatObjectHashMap.this.toArray();
        }

        @Override
        public <T> T[] toArray(T[] result)
        {
            return FloatObjectHashMap.this.toArray(result);
        }
    }

    private class KeysView extends AbstractLazyFloatIterable
    {
        @Override
        public FloatIterator floatIterator()
        {
            return new UnmodifiableFloatIterator(new KeysSetIterator());
        }

        @Override
        public void each(FloatProcedure procedure)
        {
            FloatObjectHashMap.this.forEachKey(procedure);
        }
    }

    private class KeyValuesView extends AbstractLazyIterable<FloatObjectPair<V>>
    {
        @Override
        public void each(Procedure<? super FloatObjectPair<V>> procedure)
        {
            if (FloatObjectHashMap.this.sentinelValues != null)
            {
                if (FloatObjectHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, FloatObjectHashMap.this.sentinelValues.zeroValue));
                }
                if (FloatObjectHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, FloatObjectHashMap.this.sentinelValues.oneValue));
                }
            }
            for (int i = 0; i < FloatObjectHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatObjectHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(FloatObjectHashMap.this.keys[i], FloatObjectHashMap.this.values[i]));
                }
            }
        }

        @Override
        public void forEachWithIndex(ObjectIntProcedure<? super FloatObjectPair<V>> objectIntProcedure)
        {
            int index = 0;
            if (FloatObjectHashMap.this.sentinelValues != null)
            {
                if (FloatObjectHashMap.this.sentinelValues.containsZeroKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(EMPTY_KEY, FloatObjectHashMap.this.sentinelValues.zeroValue), index);
                    index++;
                }
                if (FloatObjectHashMap.this.sentinelValues.containsOneKey)
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(REMOVED_KEY, FloatObjectHashMap.this.sentinelValues.oneValue), index);
                    index++;
                }
            }
            for (int i = 0; i < FloatObjectHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatObjectHashMap.this.keys[i]))
                {
                    objectIntProcedure.value(PrimitiveTuples.pair(FloatObjectHashMap.this.keys[i], FloatObjectHashMap.this.values[i]), index);
                    index++;
                }
            }
        }

        @Override
        public <P> void forEachWith(Procedure2<? super FloatObjectPair<V>, ? super P> procedure, P parameter)
        {
            if (FloatObjectHashMap.this.sentinelValues != null)
            {
                if (FloatObjectHashMap.this.sentinelValues.containsZeroKey)
                {
                    procedure.value(PrimitiveTuples.pair(EMPTY_KEY, FloatObjectHashMap.this.sentinelValues.zeroValue), parameter);
                }
                if (FloatObjectHashMap.this.sentinelValues.containsOneKey)
                {
                    procedure.value(PrimitiveTuples.pair(REMOVED_KEY, FloatObjectHashMap.this.sentinelValues.oneValue), parameter);
                }
            }
            for (int i = 0; i < FloatObjectHashMap.this.keys.length; i++)
            {
                if (isNonSentinel(FloatObjectHashMap.this.keys[i]))
                {
                    procedure.value(PrimitiveTuples.pair(FloatObjectHashMap.this.keys[i], FloatObjectHashMap.this.values[i]), parameter);
                }
            }
        }

        @Override
        public Iterator<FloatObjectPair<V>> iterator()
        {
            return new InternalKeyValuesIterator();
        }

        public class InternalKeyValuesIterator implements Iterator<FloatObjectPair<V>>
        {
            private int count;
            private int position;
            private boolean handledZero;
            private boolean handledOne;

            @Override
            public FloatObjectPair<V> next()
            {
                if (!this.hasNext())
                {
                    throw new NoSuchElementException("next() called, but the iterator is exhausted");
                }
                this.count++;

                if (!this.handledZero)
                {
                    this.handledZero = true;
                    if (FloatObjectHashMap.this.containsKey(EMPTY_KEY))
                    {
                        return PrimitiveTuples.pair(EMPTY_KEY, FloatObjectHashMap.this.sentinelValues.zeroValue);
                    }
                }
                if (!this.handledOne)
                {
                    this.handledOne = true;
                    if (FloatObjectHashMap.this.containsKey(REMOVED_KEY))
                    {
                        return PrimitiveTuples.pair(REMOVED_KEY, FloatObjectHashMap.this.sentinelValues.oneValue);
                    }
                }

                float[] keys = FloatObjectHashMap.this.keys;
                while (!isNonSentinel(keys[this.position]))
                {
                    this.position++;
                }
                FloatObjectPair<V> result = PrimitiveTuples.pair(keys[this.position], FloatObjectHashMap.this.values[this.position]);
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
                return this.count != FloatObjectHashMap.this.size();
            }
        }
    }
}
