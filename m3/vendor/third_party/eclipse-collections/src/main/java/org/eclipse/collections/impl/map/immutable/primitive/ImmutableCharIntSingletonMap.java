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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableIntBag;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.CharIntMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharIntMap;
import org.eclipse.collections.api.map.primitive.ImmutableIntCharMap;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.primitive.CharIntPair;
import org.eclipse.collections.impl.bag.mutable.primitive.IntHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableIntCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.CharIntMaps;
import org.eclipse.collections.impl.factory.primitive.IntCharMaps;
import org.eclipse.collections.impl.factory.primitive.CharLists;
import org.eclipse.collections.impl.factory.primitive.IntBags;
import org.eclipse.collections.impl.iterator.UnmodifiableIntIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyIntIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.CharIntHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableCharSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableCharIntSingletonMap is an optimization for {@link ImmutableCharIntMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableCharIntSingletonMap implements ImmutableCharIntMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final int EMPTY_VALUE = 0;
    private final char key1;
    private final int value1;

    ImmutableCharIntSingletonMap(char key1, int value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public int get(char key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public int getIfAbsent(char key, int ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent;
    }

    @Override
    public int getOrThrow(char key)
    {
        if (this.key1 == key)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(char key)
    {
        return this.key1 == key;
    }

    @Override
    public boolean containsValue(int value)
    {
        return this.value1 == value;
    }

    @Override
    public void forEachValue(IntProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(CharProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(CharIntProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    @Override
    public LazyCharIterable keysView()
    {
        return CharLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<CharIntPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableIntCharMap flipUniqueValues()
    {
        return IntCharMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableCharIntMap select(CharIntPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? CharIntHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new CharIntHashMap().toImmutable();
    }

    @Override
    public ImmutableCharIntMap reject(CharIntPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new CharIntHashMap().toImmutable()
                : CharIntHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectIntToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<IntIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.mutable.with(IntBags.immutable.with(this.value1));
    }

    @Override
    public ImmutableCharIntMap toImmutable()
    {
        return this;
    }

    @Override
    public IntIterator intIterator()
    {
        return new UnmodifiableIntIterator(CharIntHashMap.newWithKeysValues(this.key1, this.value1).intIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(IntProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(IntPredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public long sum()
    {
        return this.value1;
    }

    @Override
    public int min()
    {
        return this.value1;
    }

    @Override
    public int max()
    {
        return this.value1;
    }

    @Override
    public int maxIfEmpty(int defaultValue)
    {
        return this.value1;
    }

    @Override
    public int minIfEmpty(int defaultValue)
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
    public int[] toSortedArray()
    {
        return new int[]{this.value1};
    }

    @Override
    public MutableIntList toSortedList()
    {
        return IntArrayList.newListWith(this.value1);
    }

    @Override
    public boolean anySatisfy(IntPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(IntPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(IntPredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableIntBag select(IntPredicate predicate)
    {
        return predicate.accept(this.value1) ? IntHashBag.newBagWith(this.value1).toImmutable() : IntBags.immutable.empty();
    }

    @Override
    public ImmutableIntBag reject(IntPredicate predicate)
    {
        return predicate.accept(this.value1) ? IntBags.immutable.empty() : IntHashBag.newBagWith(this.value1).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(IntToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public int detectIfNone(IntPredicate predicate, int ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public int[] toArray()
    {
        return new int[]{this.value1};
    }

    @Override
    public int[] toArray(int[] target)
    {
        if (target.length < 1)
        {
            target = new int[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(int value)
    {
        return this.value1 == value;
    }

    @Override
    public boolean containsAll(int... source)
    {
        for (int value : source)
        {
            if (this.value1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(IntIterable source)
    {
        for (IntIterator iterator = source.intIterator(); iterator.hasNext(); )
        {
            if (this.value1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableIntList toList()
    {
        return IntArrayList.newListWith(this.value1);
    }

    @Override
    public MutableIntSet toSet()
    {
        return IntHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableIntBag toBag()
    {
        return IntHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyIntIterable asLazy()
    {
        return new LazyIntIterableAdapter(this);
    }

    @Override
    public ImmutableCharIntMap newWithKeyValue(char key, int value)
    {
        return CharIntMaps.immutable.withAll(CharIntHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableCharIntMap newWithoutKey(char key)
    {
        return this.key1 == key ? CharIntMaps.immutable.with() : this;
    }

    @Override
    public ImmutableCharIntMap newWithoutAllKeys(CharIterable keys)
    {
        return keys.contains(this.key1) ? CharIntMaps.immutable.with() : this;
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
    public MutableCharSet keySet()
    {
        return UnmodifiableCharSet.of(CharHashSet.newSetWith(this.key1));
    }

    @Override
    public MutableIntCollection values()
    {
        return UnmodifiableIntCollection.of(IntArrayList.newListWith(this.value1));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof CharIntMap))
        {
            return false;
        }
        CharIntMap map = (CharIntMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (int) this.key1 ^ this.value1;
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
