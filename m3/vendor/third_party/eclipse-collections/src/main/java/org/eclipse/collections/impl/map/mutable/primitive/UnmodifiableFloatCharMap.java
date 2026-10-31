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

import java.io.Serializable;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.CharFunction0;
import org.eclipse.collections.api.block.function.primitive.CharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatCharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToCharFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatCharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatCharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.map.primitive.ImmutableFloatCharMap;
import org.eclipse.collections.api.map.primitive.FloatCharMap;
import org.eclipse.collections.api.map.primitive.MutableFloatCharMap;
import org.eclipse.collections.api.map.primitive.MutableCharFloatMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.tuple.primitive.FloatCharPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import org.eclipse.collections.impl.factory.primitive.FloatCharMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableCharIterator;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableFloatSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableFloatCharMap
        implements MutableFloatCharMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableFloatCharMap map;

    public UnmodifiableFloatCharMap(MutableFloatCharMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableFloatCharMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(float key, char value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(FloatCharPair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(FloatCharMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(FloatCharToCharFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValues() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(float key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(float key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public char removeKeyIfAbsent(float key, char value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public char getIfAbsentPut(float key, char value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public char getAndPut(float key, char putValue, char defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public char getIfAbsentPut(float key, CharFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public char getIfAbsentPutWithKey(float key, FloatToCharFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> char getIfAbsentPutWith(float key, CharFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public char updateValue(float key, char initialValueIfAbsent, CharToCharFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public char get(float key)
    {
        return this.map.get(key);
    }

    @Override
    public char getIfAbsent(float key, char ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public char getOrThrow(float key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(float key)
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
    public void forEachKey(FloatProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(FloatCharProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyFloatIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<FloatCharPair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableCharFloatMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableFloatCharMap select(FloatCharPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableFloatCharMap reject(FloatCharPredicate predicate)
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
    public char addToValue(float key, char toBeAdded)
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
    public MutableFloatCharMap withKeyValue(float key, char value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatCharMap withoutKey(float key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatCharMap withoutAllKeys(FloatIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatCharMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableFloatCharMap asSynchronized()
    {
        return new SynchronizedFloatCharMap(this);
    }

    @Override
    public ImmutableFloatCharMap toImmutable()
    {
        return FloatCharMaps.immutable.withAll(this);
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
    public MutableFloatSet keySet()
    {
        return UnmodifiableFloatSet.of(this.map.keySet());
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
