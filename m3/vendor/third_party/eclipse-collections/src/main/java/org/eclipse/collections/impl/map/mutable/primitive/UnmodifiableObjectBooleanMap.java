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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction0;
import org.eclipse.collections.api.block.function.primitive.BooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectBooleanPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.MutableBooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectBooleanMap;
import org.eclipse.collections.api.map.primitive.MutableObjectBooleanMap;
import org.eclipse.collections.api.map.primitive.ObjectBooleanMap;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import org.eclipse.collections.impl.factory.primitive.ObjectBooleanMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableBooleanIterator;

/**
 * This file was automatically generated from template file unmodifiableObjectPrimitiveMap.stg.
 *
 * @since 3.2
 */
public class UnmodifiableObjectBooleanMap<K>
        implements MutableObjectBooleanMap<K>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableObjectBooleanMap<K> map;

    public UnmodifiableObjectBooleanMap(MutableObjectBooleanMap<K> map)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a UnmodifiableObjectBooleanMap on a null map");
        }

        this.map = map;
    }

    private boolean isAbsent(boolean result, K key)
    {
        return result == ObjectBooleanHashMap.EMPTY_VALUE && !this.containsKey(key);
    }

    private boolean getIfAbsentThrow(K key)
    {
        boolean result = this.map.get(key);
        if (this.isAbsent(result, key))
        {
            throw new UnsupportedOperationException("Cannot add to an " + this.getClass().getSimpleName());
        }
        return result;
    }

    @Override
    public boolean getAndPut(K key, boolean defaultValue, boolean putValue)
    {
        throw new UnsupportedOperationException("Cannot call getAndPut() on " + this.getClass().getSimpleName());
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public void put(K key, boolean value)
    {
        throw new UnsupportedOperationException("Cannot call put() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putPair(ObjectBooleanPair<K> keyValuePair)
    {
        throw new UnsupportedOperationException("Cannot call putPair() on " + this.getClass().getSimpleName());
    }

    @Override
    public void putAll(ObjectBooleanMap<? extends K> map)
    {
        throw new UnsupportedOperationException("Cannot call putAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public void updateValues(ObjectBooleanToBooleanFunction<? super K> function)
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
    public boolean removeKeyIfAbsent(K key, boolean value)
    {
        throw new UnsupportedOperationException("Cannot call removeKeyIfAbsent() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean getIfAbsentPut(K key, boolean value)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public boolean getIfAbsentPut(K key, BooleanFunction0 function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public boolean getIfAbsentPutWithKey(K key, BooleanFunction<? super K> function)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public <P> boolean getIfAbsentPutWith(K key, BooleanFunction<? super P> function, P parameter)
    {
        return this.getIfAbsentThrow(key);
    }

    @Override
    public boolean updateValue(K key, boolean initialValueIfAbsent, BooleanToBooleanFunction function)
    {
        throw new UnsupportedOperationException("Cannot call updateValue() on " + this.getClass().getSimpleName());
    }

    public boolean addToValue(K key, boolean toBeAdded)
    {
        throw new UnsupportedOperationException("Cannot call addToValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean get(Object key)
    {
        return this.map.get(key);
    }

    @Override
    public boolean getOrThrow(Object key)
    {
        return this.map.getOrThrow(key);
    }

    @Override
    public boolean getIfAbsent(Object key, boolean ifAbsent)
    {
        return this.map.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(boolean value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public void forEachValue(BooleanProcedure procedure)
    {
        this.map.forEachValue(procedure);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        this.map.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ObjectBooleanProcedure<? super K> procedure)
    {
        this.map.forEachKeyValue(procedure);
    }

    @Override
    public MutableObjectBooleanMap<K> select(ObjectBooleanPredicate<? super K> predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableObjectBooleanMap<K> reject(ObjectBooleanPredicate<? super K> predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public MutableBooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(this.map.booleanIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        this.map.forEach(procedure);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return this.map.count(predicate);
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return this.map.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return this.map.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return this.map.noneSatisfy(predicate);
    }

    @Override
    public MutableBooleanCollection select(BooleanPredicate predicate)
    {
        return this.map.select(predicate);
    }

    @Override
    public MutableBooleanCollection reject(BooleanPredicate predicate)
    {
        return this.map.reject(predicate);
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return this.map.detectIfNone(predicate, ifNone);
    }

    @Override
    public <V1> MutableCollection<V1> collect(BooleanToObjectFunction<? extends V1> function)
    {
        return this.map.collect(function);
    }

    @Override
    public boolean[] toArray()
    {
        return this.map.toArray();
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        return this.map.toArray(target);
    }

    @Override
    public boolean contains(boolean value)
    {
        return this.map.contains(value);
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return this.map.containsAll(source);
    }

    @Override
    public MutableBooleanList toList()
    {
        return this.map.toList();
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return this.map.toSet();
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return this.map.toBag();
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return this.map.asLazy();
    }

    @Override
    public MutableObjectBooleanMap<K> withKeyValue(K key, boolean value)
    {
        throw new UnsupportedOperationException("Cannot call withKeyValue() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectBooleanMap<K> withoutKey(K key)
    {
        throw new UnsupportedOperationException("Cannot call withoutKey() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectBooleanMap<K> withoutAllKeys(Iterable<? extends K> keys)
    {
        throw new UnsupportedOperationException("Cannot call withoutAllKeys() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableObjectBooleanMap<K> asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableObjectBooleanMap<K> asSynchronized()
    {
        return new SynchronizedObjectBooleanMap<>(this);
    }

    @Override
    public ImmutableObjectBooleanMap<K> toImmutable()
    {
        return ObjectBooleanMaps.immutable.withAll(this);
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
    public MutableBooleanCollection values()
    {
        return UnmodifiableBooleanCollection.of(this.map.values());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return this.map.keysView();
    }

    @Override
    public RichIterable<ObjectBooleanPair<K>> keyValuesView()
    {
        return this.map.keyValuesView();
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
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return this.map.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        return this.map.chunk(size);
    }
}
