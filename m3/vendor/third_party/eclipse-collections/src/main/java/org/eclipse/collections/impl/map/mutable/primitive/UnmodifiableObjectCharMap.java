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

import java.util.Collections;
import java.io.Serializable;
import java.util.Set;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.CharFunction0;
import org.eclipse.collections.api.block.function.primitive.CharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToCharFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectCharPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectCharProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.MutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.MutableCharObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectCharMap;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.tuple.primitive.ObjectCharPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableCharCollection;
import org.eclipse.collections.impl.factory.primitive.ObjectCharMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableCharIterator;

/**
 * This file was automatically generated from template file unmodifiableObjectPrimitiveMap.stg.
 *
 * @since 3.2
 */
public class UnmodifiableObjectCharMap<K>
        implements MutableObjectCharMap<K>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableObjectCharMap<K> map;

    public UnmodifiableObjectCharMap(MutableObjectCharMap<K> map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableObjectCharMap on a null map");
        }

        this.map = map;
    }

    private boolean isAbsent(char result, K key)
    {
        return result == ObjectCharHashMap.EMPTY_VALUE && !this.containsKey(key);
    }

    private char getIfAbsentThrow(K key)
    {
        char result = this.map.get(key);
        if (this.isAbsent(result, key))
        {
            throw new UnsupportedOperationException("Cannot add to an " + this.getClass().getSimpleName());
        }
        return result;
    }

    @Override
    public char getAndPut(K key, char defaultValue, char putValue)
    {
        throw new UnsupportedOperationException("Cannot call getAndPut() on " + this.getClass().getSimpleName());
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(K key, char value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(ObjectCharPair<K> keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(ObjectCharMap<? extends K> map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(ObjectCharToCharFunction<? super K> function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public void removeKey(K key)
    {
        throw new UnsupportedOperationException("Cannot call removeKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public void remove(Object key)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    @Override
    public char removeKeyIfAbsent(K key, char value)
    {
        throw new UnsupportedOperationException("Cannot call removeKeyIfAbsent() on " + this.getClass().getSimpleName());
    }

    @Override
    public char getIfAbsentPut(K key, char value)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public char getIfAbsentPut(K key, CharFunction0 function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public char getIfAbsentPutWithKey(K key, CharFunction<? super K> function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public <P> char getIfAbsentPutWith(K key, CharFunction<? super P> function, P parameter)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public char updateValue(K key, char initialValueIfAbsent, CharToCharFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    public char addToValue(K key, char toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public char get(Object key)
    {
        return this.map.get(key);
    }

    @Override
    public char getOrThrow(Object key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public char getIfAbsent(Object key, char ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(Object key)
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
    public void forEachKey(Procedure<? super K> procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ObjectCharProcedure<? super K> procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public MutableObjectCharMap<K> select(ObjectCharPredicate<? super K> predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableObjectCharMap<K> reject(ObjectCharPredicate<? super K> predicate)
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
    public MutableCharCollection select(CharPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableCharCollection reject(CharPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public <V1> MutableCollection<V1> collect(CharToObjectFunction<? extends V1> function)
    {
        return this.map.collect(function);
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
    public MutableObjectCharMap<K> withKeyValue(K key, char value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectCharMap<K> withoutKey(K key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectCharMap<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectCharMap<K> asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableObjectCharMap<K> asSynchronized()
    {
        return new SynchronizedObjectCharMap<>(this);
    }

    @Override
    public ImmutableObjectCharMap<K> toImmutable()
    {
        return ObjectCharMaps.immutable.withAll(this);
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
    public Set<K> keySet()
    {
        return Collections.unmodifiableSet(this.map.keySet());
    }

    @Override
    public MutableCharCollection values()
    {
        return UnmodifiableCharCollection.of(this.map.values());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<ObjectCharPair<K>> keyValuesView()
    {
        return this.map.keyValuesView();
    }

    @Override
    public MutableCharObjectMap<K> flipUniqueValues()
    {
        return this.map.flipUniqueValues().asUnmodifiable();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.map.equals(obj);
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
