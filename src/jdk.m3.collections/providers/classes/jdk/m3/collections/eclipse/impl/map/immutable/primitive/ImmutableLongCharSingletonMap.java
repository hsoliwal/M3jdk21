/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.LazyLongIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableCharBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableCharBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongCharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongCharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableCharCollection;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.map.primitive.LongCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableLongCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableCharLongMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongCharPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.CharHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongCharMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharLongMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableCharIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyCharIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.CharArrayList;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.LongCharHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.CharHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableLongSet;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableLongCharSingletonMap is an optimization for {@link ImmutableLongCharMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongCharSingletonMap implements ImmutableLongCharMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final char EMPTY_VALUE = '\0';
    private final long key1;
    private final char value1;

    ImmutableLongCharSingletonMap(long key1, char value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public char get(long key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public char getIfAbsent(long key, char ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent;
    }

    @Override
    public char getOrThrow(long key)
    {
        if (this.key1 == key)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(long key)
    {
        return this.key1 == key;
    }

    @Override
    public boolean containsValue(char value)
    {
        return this.value1 == value;
    }

    @Override
    public void forEachValue(CharProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(LongProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(LongCharProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(LongCharPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1);
    }

    @Override
    public LazyLongIterable keysView()
    {
        return LongLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<LongCharPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableCharLongMap flipUniqueValues()
    {
        return CharLongMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableLongCharMap select(LongCharPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? LongCharHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new LongCharHashMap().toImmutable();
    }

    @Override
    public ImmutableLongCharMap reject(LongCharPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new LongCharHashMap().toImmutable()
                : LongCharHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.mutable.with(CharBags.immutable.with(this.value1));
    }

    @Override
    public ImmutableLongCharMap toImmutable()
    {
        return this;
    }

    @Override
    public CharIterator charIterator()
    {
        return new UnmodifiableCharIterator(LongCharHashMap.newWithKeysValues(this.key1, this.value1).charIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(CharPredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public long sum()
    {
        return this.value1;
    }

    @Override
    public char min()
    {
        return this.value1;
    }

    @Override
    public char max()
    {
        return this.value1;
    }

    @Override
    public char maxIfEmpty(char defaultValue)
    {
        return this.value1;
    }

    @Override
    public char minIfEmpty(char defaultValue)
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
    public char[] toSortedArray()
    {
        return new char[]{this.value1};
    }

    @Override
    public MutableCharList toSortedList()
    {
        return CharArrayList.newListWith(this.value1);
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableCharBag select(CharPredicate predicate)
    {
        return predicate.accept(this.value1) ? CharHashBag.newBagWith(this.value1).toImmutable() : CharBags.immutable.empty();
    }

    @Override
    public ImmutableCharBag reject(CharPredicate predicate)
    {
        return predicate.accept(this.value1) ? CharBags.immutable.empty() : CharHashBag.newBagWith(this.value1).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(CharToObjectFunction<? extends V> function)
    {
        return Bags.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public char[] toArray()
    {
        return new char[]{this.value1};
    }

    @Override
    public char[] toArray(char[] target)
    {
        if (target.length < 1)
        {
            target = new char[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(char value)
    {
        return this.value1 == value;
    }

    @Override
    public boolean containsAll(char... source)
    {
        for (char value : source)
        {
            if (this.value1 != value)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        for (CharIterator iterator = source.charIterator(); iterator.hasNext(); )
        {
            if (this.value1 != iterator.next())
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableCharList toList()
    {
        return CharArrayList.newListWith(this.value1);
    }

    @Override
    public MutableCharSet toSet()
    {
        return CharHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableCharBag toBag()
    {
        return CharHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
    }

    @Override
    public ImmutableLongCharMap newWithKeyValue(long key, char value)
    {
        return LongCharMaps.immutable.withAll(LongCharHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableLongCharMap newWithoutKey(long key)
    {
        return this.key1 == key ? LongCharMaps.immutable.with() : this;
    }

    @Override
    public ImmutableLongCharMap newWithoutAllKeys(LongIterable keys)
    {
        return keys.contains(this.key1) ? LongCharMaps.immutable.with() : this;
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
    public MutableLongSet keySet()
    {
        return UnmodifiableLongSet.of(LongHashSet.newSetWith(this.key1));
    }

    @Override
    public MutableCharCollection values()
    {
        return UnmodifiableCharCollection.of(CharArrayList.newListWith(this.value1));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof LongCharMap))
        {
            return false;
        }
        LongCharMap map = (LongCharMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return (int) (this.key1 ^ this.key1 >>> 32) ^ (int) this.value1;
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
