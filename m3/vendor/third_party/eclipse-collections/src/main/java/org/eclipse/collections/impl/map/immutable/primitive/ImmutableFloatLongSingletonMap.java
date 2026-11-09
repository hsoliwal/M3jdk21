/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatLongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.map.primitive.FloatLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongFloatMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.FloatLongPair;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.FloatLongMaps;
import org.eclipse.collections.impl.factory.primitive.LongFloatMaps;
import org.eclipse.collections.impl.factory.primitive.FloatLists;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import org.eclipse.collections.impl.iterator.UnmodifiableLongIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyLongIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.FloatLongHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableFloatSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableFloatLongSingletonMap is an optimization for {@link ImmutableFloatLongMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatLongSingletonMap implements ImmutableFloatLongMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final long EMPTY_VALUE = 0L;
    private final float key1;
    private final long value1;

    ImmutableFloatLongSingletonMap(float key1, long value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public long get(float key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public long getIfAbsent(float key, long ifAbsent)
    {
        return Float.compare(this.key1, key) == 0 ? this.value1 : ifAbsent;
    }

    @Override
    public long getOrThrow(float key)
    {
        if (Float.compare(this.key1, key) == 0)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(float key)
    {
        return Float.compare(this.key1, key) == 0;
    }

    @Override
    public boolean containsValue(long value)
    {
        return this.value1 == value;
    }

    @Override
    public void forEachValue(LongProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(FloatProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(FloatLongProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    @Override
    public LazyFloatIterable keysView()
    {
        return FloatLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<FloatLongPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableLongFloatMap flipUniqueValues()
    {
        return LongFloatMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableFloatLongMap select(FloatLongPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? FloatLongHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new FloatLongHashMap().toImmutable();
    }

    @Override
    public ImmutableFloatLongMap reject(FloatLongPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new FloatLongHashMap().toImmutable()
                : FloatLongHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.mutable.with(LongBags.immutable.with(this.value1));
    }

    @Override
    public ImmutableFloatLongMap toImmutable()
    {
        return this;
    }

    @Override
    public LongIterator longIterator()
    {
        return new UnmodifiableLongIterator(FloatLongHashMap.newWithKeysValues(this.key1, this.value1).longIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public long sum()
    {
        return this.value1;
    }

    @Override
    public long min()
    {
        return this.value1;
    }

    @Override
    public long max()
    {
        return this.value1;
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.value1;
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        return this.value1;
    }

    @Override
    public double average()
    {
        return this.value1;
    }

    @Override
    public double median()
    {
        return this.value1;
    }

    @Override
    public long[] toSortedArray()
    {
        return new long[]{this.value1};
    }

    @Override
    public MutableLongList toSortedList()
    {
        return LongArrayList.newListWith(this.value1);
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableLongBag select(LongPredicate predicate)
    {
        return predicate.accept(this.value1) ? LongHashBag.newBagWith(this.value1).toImmutable() : LongBags.immutable.empty();
    }

    @Override
    public ImmutableLongBag reject(LongPredicate predicate)
    {
        return predicate.accept(this.value1) ? LongBags.immutable.empty() : LongHashBag.newBagWith(this.value1).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(LongToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public long[] toArray()
    {
        return new long[]{this.value1};
    }

    @Override
    public long[] toArray(long[] target)
    {
        if (target.length < 1)
        {
            target = new long[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(long value)
    {
        return this.value1 == value;
    }

    @Override
    public boolean containsAll(long... source)
    {
        for (long value : source)
        {
            if (this.value1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        for (LongIterator iterator = source.longIterator(); iterator.hasNext(); )
        {
            if (this.value1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableLongList toList()
    {
        return LongArrayList.newListWith(this.value1);
    }

    @Override
    public MutableLongSet toSet()
    {
        return LongHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableLongBag toBag()
    {
        return LongHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return new LazyLongIterableAdapter(this);
    }

    @Override
    public ImmutableFloatLongMap newWithKeyValue(float key, long value)
    {
        return FloatLongMaps.immutable.withAll(FloatLongHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableFloatLongMap newWithoutKey(float key)
    {
        return Float.compare(this.key1, key) == 0 ? FloatLongMaps.immutable.with() : this;
    }

    @Override
    public ImmutableFloatLongMap newWithoutAllKeys(FloatIterable keys)
    {
        return keys.contains(this.key1) ? FloatLongMaps.immutable.with() : this;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public MutableFloatSet keySet()
    {
        return UnmodifiableFloatSet.of(FloatHashSet.newSetWith(this.key1));
    }

    @Override
    public MutableLongCollection values()
    {
        return UnmodifiableLongCollection.of(LongArrayList.newListWith(this.value1));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof FloatLongMap))
        {
            return false;
        }
        FloatLongMap map = (FloatLongMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return Float.floatToIntBits(this.key1) ^ (int) (this.value1 ^ this.value1 >>> 32);
    }

    @Override
    public String toString()
    {
        return "{" + this.key1 + "=" + this.value1 + "}";
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
            appendable.append(String.valueOf(this.value1));
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
