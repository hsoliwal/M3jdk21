/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import java.io.Serializable;

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableCharBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction0;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToCharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortCharToCharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectCharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToCharFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortCharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortCharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableCharCollection;
import jdk.m3.collections.eclipse.api.iterator.MutableCharIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.ShortCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableShortCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableCharShortMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortCharPair;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortCharMaps;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableCharIterator;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableShortSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
@SuppressWarnings("serial")
public class UnmodifiableShortCharMap
        implements MutableShortCharMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableShortCharMap map;

    public UnmodifiableShortCharMap(MutableShortCharMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableShortCharMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(short key, char value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(ShortCharPair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(ShortCharMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(ShortCharToCharFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValues() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(short key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(short key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public char removeKeyIfAbsent(short key, char value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public char getIfAbsentPut(short key, char value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public char getAndPut(short key, char putValue, char defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public char getIfAbsentPut(short key, CharFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public char getIfAbsentPutWithKey(short key, ShortToCharFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> char getIfAbsentPutWith(short key, CharFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public char updateValue(short key, char initialValueIfAbsent, CharToCharFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public char get(short key)
    {
        return this.map.get(key);
    }

    @Override
    public char getIfAbsent(short key, char ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public char getOrThrow(short key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(short key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(char value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(CharProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(ShortProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ShortCharProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyShortIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<ShortCharPair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableCharShortMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableShortCharMap select(ShortCharPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableShortCharMap reject(ShortCharPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableCharIterator charIterator()
    {
        return new UnmodifiableCharIterator(this.map.charIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(CharPredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableCharBag select(CharPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableCharBag reject(CharPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(CharToObjectFunction<? extends V> function)
    {
        return this.map.collect(function);
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.map.sum();
    }

    @Override
    public char max()
    {
        return this.map.max();
    }

    @Override
    public char maxIfEmpty(char defaultValue)
    {
        return this.map.maxIfEmpty(defaultValue);
    }

    @Override
    public char min()
    {
        return this.map.min();
    }

    @Override
    public char minIfEmpty(char defaultValue)
    {
        return this.map.minIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.map.average();
    }

    @Override
    public double median()
    {
        return this.map.median();
    }

    @Override
    public char addToValue(short key, char toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public char[] toSortedArray()
    {
        return this.map.toSortedArray();
    }

    @Override
    public MutableCharList toSortedList()
    {
        return this.map.toSortedList();
    }

    @Override
    public char[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public char[] toArray(char[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(char value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(char... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableCharList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableCharSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableCharBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyCharIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableShortCharMap withKeyValue(short key, char value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortCharMap withoutKey(short key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortCharMap withoutAllKeys(ShortIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableShortCharMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableShortCharMap asSynchronized()
    {
        return new SynchronizedShortCharMap(this);
    }

    @Override
    public ImmutableShortCharMap toImmutable()
    {
        return ShortCharMaps.immutable.withAll(this);
    }

    @Override
    public int size()
    {
        return this.map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.map.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.map.notEmpty();
    }

    @Override
    public MutableShortSet keySet()
    {
        return UnmodifiableShortSet.of(this.map.keySet());
    }

    @Override
    public MutableCharCollection values()
    {
        return UnmodifiableCharCollection.of(this.map.values());
    }

    @Override
    public boolean equals(Object otherMap)
    {
        return this.map.equals(otherMap);
    }

    @Override
    public int hashCode()
    {
        return this.map.hashCode();
    }

    @Override
    public String toString()
    {
        return this.map.toString();
    }

    @Override
    public String makeString()
    {
        return this.map.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.map.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.map.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.map.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.map.appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.map.appendString(appendable, start, separator, end);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
