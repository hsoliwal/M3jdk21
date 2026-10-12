/*
 * Copyright (c) 2022 Goldman Sachs.
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
import java.util.NoSuchElementException;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableCharBag;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatCharPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatCharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.map.primitive.FloatCharMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatCharMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharFloatMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.tuple.primitive.FloatCharPair;
import org.eclipse.collections.impl.bag.mutable.primitive.CharHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.CharBags;
import org.eclipse.collections.impl.factory.primitive.CharFloatMaps;
import org.eclipse.collections.impl.iterator.ImmutableEmptyCharIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.CharHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableFloatSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.primitive.LazyFloatIterate;

/**
 * ImmutableFloatCharEmptyMap is an optimization for {@link ImmutableFloatCharMap} of size 0.
 * This file was automatically generated from template file immutablePrimitivePrimitiveEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatCharEmptyMap implements ImmutableFloatCharMap, Serializable
{
    static final ImmutableFloatCharMap INSTANCE = new ImmutableFloatCharEmptyMap();

    private static final long serialVersionUID = 1L;
    private static final char EMPTY_VALUE = '\0';

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public char get(float key)
    {
        return EMPTY_VALUE;
    }

    @Override
    public char getIfAbsent(float key, char ifAbsent)
    {
        return ifAbsent;
    }

    @Override
    public char getOrThrow(float key)
    {
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public boolean containsKey(float key)
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
    public void forEachKey(FloatProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(FloatCharProcedure procedure)
    {
    }

    @Override
    public LazyFloatIterable keysView()
    {
        return LazyFloatIterate.empty();
    }

    @Override
    public RichIterable<FloatCharPair> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableCharFloatMap flipUniqueValues()
    {
        return CharFloatMaps.immutable.empty();
    }

    @Override
    public ImmutableFloatCharMap select(FloatCharPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableFloatCharMap reject(FloatCharPredicate predicate)
    {
        return this;
    }

    @Override
    public ImmutableFloatCharMap toImmutable()
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
    public ImmutableFloatCharMap newWithKeyValue(float key, char value)
    {
        return new ImmutableFloatCharSingletonMap(key, value);
    }

    @Override
    public ImmutableFloatCharMap newWithoutKey(float key)
    {
        return this;
    }

    @Override
    public ImmutableFloatCharMap newWithoutAllKeys(FloatIterable keys)
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
    public MutableFloatSet keySet()
    {
        return UnmodifiableFloatSet.of(new FloatHashSet());
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
        if (!(obj instanceof FloatCharMap))
        {
            return false;
        }
        FloatCharMap map = (FloatCharMap) obj;
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
