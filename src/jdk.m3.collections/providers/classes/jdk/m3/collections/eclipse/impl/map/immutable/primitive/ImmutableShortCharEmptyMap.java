/*
 * Copyright (c) 2026 Goldman Sachs.
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
import java.util.NoSuchElementException;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableCharBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableCharBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortCharPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortCharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableCharCollection;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.map.primitive.ShortCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableCharShortMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortCharPair;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.CharHashBag;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import jdk.m3.collections.eclipse.impl.factory.Bags;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharBags;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharShortMaps;
import jdk.m3.collections.eclipse.impl.iterator.ImmutableEmptyCharIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyCharIterableAdapter;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.CharArrayList;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.CharHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ShortHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableShortSet;
import jdk.m3.collections.eclipse.impl.utility.LazyIterate;
import jdk.m3.collections.eclipse.impl.utility.primitive.LazyShortIterate;

/**
 * ImmutableShortCharEmptyMap is an optimization for {@link ImmutableShortCharMap} of size 0.
 * This file was automatically generated from template file immutablePrimitivePrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableShortCharEmptyMap implements ImmutableShortCharMap, Serializable
{
    static final ImmutableShortCharMap INSTANCE = new ImmutableShortCharEmptyMap();

    private static final long serialVersionUID = 1L;
    private static final char EMPTY_VALUE = '\0';

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public char get(short key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public char getIfAbsent(short key, char ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public char getOrThrow(short key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(short key)
    {
        return false;
    }

    @Override
    public boolean containsValue(char value)
    {
        return false;
    }

    @Override
    public void forEachValue(CharProcedure procedure)
    {
    }

    @Override
    public void forEachKey(ShortProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(ShortCharProcedure procedure)
    {
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(ShortCharPredicate predicate)
    {
        return true;
    }

    @Override
    public LazyShortIterable keysView()
    {
        return LazyShortIterate.empty();
    }

    @Override
    public RichIterable<ShortCharPair> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableCharShortMap flipUniqueValues()
    {
        return CharShortMaps.immutable.empty();
    }

    @Override
    public ImmutableShortCharMap select(ShortCharPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableShortCharMap reject(ShortCharPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableShortCharMap toImmutable()
    {
        return this;
    }

    @Override
    public CharIterator charIterator()
    {
        return ImmutableEmptyCharIterator.INSTANCE;
    }

    @Override
    public void forEach(CharProcedure procedure)
    {
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
    }

    @Override
    public int count(CharPredicate predicate)
    {
        return 0;
    }

    @Override
    public long sum()
    {
        return 0L;
    }

    @Override
    public char min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public char max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public char maxIfEmpty(char defaultValue)
    {
        return defaultValue;
    }

    @Override
    public char minIfEmpty(char defaultValue)
    {
        return defaultValue;
    }

    @Override
    public double average()
    {
        throw new ArithmeticException();
    }

    @Override
    public double median()
    {
        throw new ArithmeticException();
    }

    @Override
    public char[] toSortedArray()
    {
        return new char[0];
    }

    @Override
    public MutableCharList toSortedList()
    {
        return new CharArrayList();
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return true;
    }

    @Override
    public ImmutableCharBag select(CharPredicate predicate)
    {
        return CharBags.immutable.empty();
    }

    @Override
    public ImmutableCharBag reject(CharPredicate predicate)
    {
        return CharBags.immutable.empty();
    }

    @Override
    public <V> ImmutableBag<V> collect(CharToObjectFunction<? extends V> function)
    {
        return Bags.immutable.empty();
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return ifNone;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return injectedValue;
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        return Lists.immutable.empty();
    }

    @Override
    public char[] toArray()
    {
        return new char[0];
    }

    @Override
    public char[] toArray(char[] target)
    {
        return target;
    }

    @Override
    public boolean contains(char value)
    {
        return false;
    }

    @Override
    public boolean containsAll(char... source)
    {
        return source.length == 0;
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        return source.isEmpty();
    }

    @Override
    public MutableCharList toList()
    {
        return new CharArrayList();
    }

    @Override
    public MutableCharSet toSet()
    {
        return new CharHashSet();
    }

    @Override
    public MutableCharBag toBag()
    {
        return new CharHashBag();
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
    }

    @Override
    public ImmutableShortCharMap newWithKeyValue(short key, char value)
    {
        return new ImmutableShortCharSingletonMap(key, value);
    }

    @Override
    public ImmutableShortCharMap newWithoutKey(short key)
    {
        return this;
    }

    @Override
    public ImmutableShortCharMap newWithoutAllKeys(ShortIterable keys)
    {
        return this;
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean notEmpty()
    {
        return false;
    }

    @Override
    public MutableShortSet keySet()
    {
        return UnmodifiableShortSet.of(new ShortHashSet());
    }

    @Override
    public MutableCharCollection values()
    {
        return UnmodifiableCharCollection.of(new CharArrayList());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ShortCharMap))
        {
            return false;
        }
        ShortCharMap map = (ShortCharMap) obj;
        return map.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "{}";
    }

    @Override
    public String makeString()
    {
        return "";
    }

    @Override
    public String makeString(String separator)
    {
        return "";
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return start + end;
    }

    @Override
    public void appendString(Appendable appendable)
    {
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
