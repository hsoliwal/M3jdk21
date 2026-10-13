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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction0;
import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharLongToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToLongFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharLongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.map.primitive.ImmutableCharLongMap;
import org.eclipse.collections.api.map.primitive.CharLongMap;
import org.eclipse.collections.api.map.primitive.MutableCharLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongCharMap;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.CharLongPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import org.eclipse.collections.impl.factory.primitive.CharLongMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableLongIterator;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableCharSet;

/**
 * This file was automatically generated from template file unmodifiablePrimitivePrimitiveMap.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableCharLongMap
        implements MutableCharLongMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableCharLongMap map;

    public UnmodifiableCharLongMap(MutableCharLongMap map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableCharLongMap on a null map");
        }

        this.map = map;
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(char key, long value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(CharLongPair keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(CharLongMap map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(CharLongToLongFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValues() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(char key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(char key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public long removeKeyIfAbsent(char key, long value)
    {
        if (this.map.containsKey(key))
        {
            throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
        }
        return value;
    }

    @Override
    public long getIfAbsentPut(char key, long value)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public long getAndPut(char key, long putValue, long defaultValue)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public long getIfAbsentPut(char key, LongFunction0 function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public long getIfAbsentPutWithKey(char key, CharToLongFunction function)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public <P> long getIfAbsentPutWith(char key, LongFunction<? super P> function, P parameter)
    {
        return this.map.getIfAbsentPut(key, () -> { throw new UnsupportedOperationException(); });
    }

    @Override
    public long updateValue(char key, long initialValueIfAbsent, LongToLongFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public long get(char key)
    {
        return this.map.get(key);
    }

    @Override
    public long getIfAbsent(char key, long ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public long getOrThrow(char key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean containsKey(char key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(long value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(LongProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(CharProcedure procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(CharLongProcedure procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public LazyCharIterable keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<CharLongPair> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableLongCharMap flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public MutableCharLongMap select(CharLongPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableCharLongMap reject(CharLongPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableLongIterator longIterator()
    {
        return new UnmodifiableLongIterator(this.map.longIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableLongBag select(LongPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableLongBag reject(LongPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public <V> MutableBag<V> collect(LongToObjectFunction<? extends V> function)
    {
        return this.map.collect(function);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.map.sum();
    }

    @Override
    public long max()
    {
        return this.map.max();
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.map.maxIfEmpty(defaultValue);
    }

    @Override
    public long min()
    {
        return this.map.min();
    }

    @Override
    public long minIfEmpty(long defaultValue)
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
    public long addToValue(char key, long toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public long[] toSortedArray()
    {
        return this.map.toSortedArray();
    }

    @Override
    public MutableLongList toSortedList()
    {
        return this.map.toSortedList();
    }

    @Override
    public long[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public long[] toArray(long[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(long value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableLongList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableLongSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableLongBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableCharLongMap withKeyValue(char key, long value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharLongMap withoutKey(char key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharLongMap withoutAllKeys(CharIterable keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableCharLongMap asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableCharLongMap asSynchronized()
    {
        return new SynchronizedCharLongMap(this);
    }

    @Override
    public ImmutableCharLongMap toImmutable()
    {
        return CharLongMaps.immutable.withAll(this);
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
    public MutableCharSet keySet()
    {
        return UnmodifiableCharSet.of(this.map.keySet());
    }

    @Override
    public MutableLongCollection values()
    {
        return UnmodifiableLongCollection.of(this.map.values());
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
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
