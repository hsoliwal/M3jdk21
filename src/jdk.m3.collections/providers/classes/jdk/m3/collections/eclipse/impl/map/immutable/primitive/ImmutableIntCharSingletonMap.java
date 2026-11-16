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

import jdk.m3.collections.eclipse.api.IntIterable;
import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.LazyIntIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableCharBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableCharBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntCharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntCharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.IntProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableCharCollection;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.map.primitive.IntCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableIntCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableCharIntMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.IntCharPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.CharHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntCharMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharIntMaps;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableCharIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyCharIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.CharArrayList;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.IntCharHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.CharHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableIntSet;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableIntCharSingletonMap is an optimization for {@link ImmutableIntCharMap} of size 1.
 * This file was automatically generated from template file immutablePrimitivePrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableIntCharSingletonMap implements ImmutableIntCharMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private static final char EMPTY_VALUE = '\0';
    private final int key1;
    private final char value1;

    ImmutableIntCharSingletonMap(int key1, char value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public char get(int key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public char getIfAbsent(int key, char ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent;
    }

    @Override
    public char getOrThrow(int key)
    {
        if (this.key1 == key)
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(int key)
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
    public void forEachKey(IntProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(IntCharProcedure procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(IntCharPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1);
    }

    @Override
    public LazyIntIterable keysView()
    {
        return IntLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<IntCharPair> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableCharIntMap flipUniqueValues()
    {
        return CharIntMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public ImmutableIntCharMap select(IntCharPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? IntCharHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : new IntCharHashMap().toImmutable();
    }

    @Override
    public ImmutableIntCharMap reject(IntCharPredicate predicate)
    {
        return predicate.accept(this.key1, this.value1) ? new IntCharHashMap().toImmutable()
                : IntCharHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
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
    public ImmutableIntCharMap toImmutable()
    {
        return this;
    }

    @Override
    public CharIterator charIterator()
    {
        return new UnmodifiableCharIterator(IntCharHashMap.newWithKeysValues(this.key1, this.value1).charIterator());
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
    public ImmutableIntCharMap newWithKeyValue(int key, char value)
    {
        return IntCharMaps.immutable.withAll(IntCharHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableIntCharMap newWithoutKey(int key)
    {
        return this.key1 == key ? IntCharMaps.immutable.with() : this;
    }

    @Override
    public ImmutableIntCharMap newWithoutAllKeys(IntIterable keys)
    {
        return keys.contains(this.key1) ? IntCharMaps.immutable.with() : this;
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
    public MutableIntSet keySet()
    {
        return UnmodifiableIntSet.of(IntHashSet.newSetWith(this.key1));
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
        if (!(obj instanceof IntCharMap))
        {
            return false;
        }
        IntCharMap map = (IntCharMap) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && this.value1 == map.getOrThrow(this.key1);
    }

    @Override
    public int hashCode()
    {
        return this.key1 ^ (int) this.value1;
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
