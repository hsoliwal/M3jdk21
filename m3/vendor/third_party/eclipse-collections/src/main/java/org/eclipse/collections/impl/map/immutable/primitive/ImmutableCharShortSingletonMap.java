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
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableShortBag;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.map.primitive.CharShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharShortMap;
import org.eclipse.collections.api.map.primitive.ImmutableShortCharMap;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.tuple.primitive.CharShortPair;
import org.eclipse.collections.impl.bag.mutable.primitive.ShortHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.CharShortMaps;
import org.eclipse.collections.impl.factory.primitive.ShortCharMaps;
import org.eclipse.collections.impl.factory.primitive.CharLists;
import org.eclipse.collections.impl.factory.primitive.ShortBags;
import org.eclipse.collections.impl.iterator.UnmodifiableShortIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyShortIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.CharShortHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableCharSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableCharShortSingletonMap is an optimization for {@link ImmutableCharShortMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableCharShortSingletonMap implements ImmutableCharShortMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final short EMPTY_VALUE = (short) 0;
    private final char key1;
    private final short value1;

    ImmutableCharShortSingletonMap(char key1, short value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public short get(char key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public short getIfAbsent(char key, short ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent;
    }

    @Override
    public short getOrThrow(char key)
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
    public boolean containsValue(short value)
    {
        return this.value1 == value;
    }

    @Override
    public void forEachValue(ShortProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(CharProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(CharShortProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    @Override
    public LazyCharIterable keysView()
    {
        return CharLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<CharShortPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableShortCharMap flipUniqueValues()
    {
        return ShortCharMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableCharShortMap select(CharShortPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? CharShortHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new CharShortHashMap().toImmutable();
    }

    @Override
    public ImmutableCharShortMap reject(CharShortPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new CharShortHashMap().toImmutable()
                : CharShortHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.mutable.with(ShortBags.immutable.with(this.value1));
    }

    @Override
    public ImmutableCharShortMap toImmutable()
    {
        return this;
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new UnmodifiableShortIterator(CharShortHashMap.newWithKeysValues(this.key1, this.value1).shortIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public long sum()
    {
        return this.value1;
    }

    @Override
    public short min()
    {
        return this.value1;
    }

    @Override
    public short max()
    {
        return this.value1;
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        return this.value1;
    }

    @Override
    public short minIfEmpty(short defaultValue)
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
    public short[] toSortedArray()
    {
        return new short[]{this.value1};
    }

    @Override
    public MutableShortList toSortedList()
    {
        return ShortArrayList.newListWith(this.value1);
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableShortBag select(ShortPredicate predicate)
    {
        return predicate.accept(this.value1) ? ShortHashBag.newBagWith(this.value1).toImmutable() : ShortBags.immutable.empty();
    }

    @Override
    public ImmutableShortBag reject(ShortPredicate predicate)
    {
        return predicate.accept(this.value1) ? ShortBags.immutable.empty() : ShortHashBag.newBagWith(this.value1).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public short[] toArray()
    {
        return new short[]{this.value1};
    }

    @Override
    public short[] toArray(short[] target)
    {
        if (target.length < 1)
        {
            target = new short[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(short value)
    {
        return this.value1 == value;
    }

    @Override
    public boolean containsAll(short... source)
    {
        for (short value : source)
        {
            if (this.value1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        for (ShortIterator iterator = source.shortIterator(); iterator.hasNext(); )
        {
            if (this.value1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableShortList toList()
    {
        return ShortArrayList.newListWith(this.value1);
    }

    @Override
    public MutableShortSet toSet()
    {
        return ShortHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableShortBag toBag()
    {
        return ShortHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public ImmutableCharShortMap newWithKeyValue(char key, short value)
    {
        return CharShortMaps.immutable.withAll(CharShortHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableCharShortMap newWithoutKey(char key)
    {
        return this.key1 == key ? CharShortMaps.immutable.with() : this;
    }

    @Override
    public ImmutableCharShortMap newWithoutAllKeys(CharIterable keys)
    {
        return keys.contains(this.key1) ? CharShortMaps.immutable.with() : this;
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
    public MutableShortCollection values()
    {
        return UnmodifiableShortCollection.of(ShortArrayList.newListWith(this.value1));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof CharShortMap))
        {
            return false;
        }
        CharShortMap map = (CharShortMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (int) this.key1 ^ (int) this.value1;
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
