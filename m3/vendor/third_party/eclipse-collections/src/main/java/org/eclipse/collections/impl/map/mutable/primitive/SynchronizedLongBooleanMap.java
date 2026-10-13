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
import java.util.Collection;
import java.util.Collections;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction0;
import org.eclipse.collections.api.block.function.primitive.BooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongBooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.LongToBooleanFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongBooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongBooleanProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.MutableBooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongBooleanMap;
import org.eclipse.collections.api.map.primitive.LongBooleanMap;
import org.eclipse.collections.api.map.primitive.MutableLongBooleanMap;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.api.tuple.primitive.LongBooleanPair;
import org.eclipse.collections.impl.SynchronizedRichIterable;
import org.eclipse.collections.impl.factory.primitive.LongBooleanMaps;
import org.eclipse.collections.impl.set.mutable.primitive.SynchronizedLongSet;
import org.eclipse.collections.impl.collection.mutable.primitive.SynchronizedBooleanCollection;

/**
 * A synchronized view of a {@link MutableLongBooleanMap}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link MutableBooleanIterator} as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitivePrimitiveMap.stg.
 *
 * @see MutableLongBooleanMap#asSynchronized()
 * @see MutableMap#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedLongBooleanMap
        implements MutableLongBooleanMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Object lock;
    private final MutableLongBooleanMap map;

    public SynchronizedLongBooleanMap(MutableLongBooleanMap map)
    {
        this(map, null);
    }

    public SynchronizedLongBooleanMap(MutableLongBooleanMap map, Object newLock)
    {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot create a SynchronizedLongBooleanMap on a null map");
        }

        this.map = map;
        this.lock = newLock == null ? this : newLock;
    }

    @Override
    public void clear()
    {
        synchronized (this.lock)
        {
            this.map.clear();
        }
    }

    @Override
    public void put(long key, boolean value)
    {
        synchronized (this.lock)
        {
            this.map.put(key, value);
        }
    }

    @Override
    public void putPair(LongBooleanPair keyValuePair)
    {
        synchronized (this.lock)
        {
            this.map.put(keyValuePair.getOne(), keyValuePair.getTwo());
        }
    }

    @Override
    public void putAll(LongBooleanMap map)
    {
        synchronized (this.lock)
        {
            this.map.putAll(map);
        }
    }

    @Override
    public void updateValues(LongBooleanToBooleanFunction function)
    {
        synchronized (this.lock)
        {
            this.map.updateValues(function);
        }
    }

    @Override
    public void removeKey(long key)
    {
        synchronized (this.lock)
        {
            this.map.removeKey(key);
        }
    }

    @Override
    public void remove(long key)
    {
        synchronized (this.lock)
        {
            this.map.remove(key);
        }
    }

    @Override
    public boolean removeKeyIfAbsent(long key, boolean value)
    {
        synchronized (this.lock)
        {
            return this.map.removeKeyIfAbsent(key, value);
        }
    }

    @Override
    public boolean getIfAbsentPut(long key, boolean value)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPut(key, value);
        }
    }

    @Override
    public boolean getAndPut(long key, boolean putValue, boolean defaultValue)
    {
        synchronized (this.lock)
        {
            return this.map.getAndPut(key, putValue, defaultValue);
        }
    }

    @Override
    public boolean getIfAbsentPut(long key, BooleanFunction0 function)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPut(key, function);
        }
    }

    @Override
    public boolean getIfAbsentPutWithKey(long key, LongToBooleanFunction function)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPutWithKey(key, function);
        }
    }

    @Override
    public <P> boolean getIfAbsentPutWith(long key, BooleanFunction<? super P> function, P parameter)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsentPutWith(key, function, parameter);
        }
    }

    @Override
    public boolean updateValue(long key, boolean initialValueIfAbsent, BooleanToBooleanFunction function)
    {
        synchronized (this.lock)
        {
            return this.map.updateValue(key, initialValueIfAbsent, function);
        }
    }

    @Override
    public boolean get(long key)
    {
        synchronized (this.lock)
        {
            return this.map.get(key);
        }
    }

    @Override
    public boolean getIfAbsent(long key, boolean ifAbsent)
    {
        synchronized (this.lock)
        {
            return this.map.getIfAbsent(key, ifAbsent);
        }
    }

    @Override
    public boolean getOrThrow(long key)
    {
        synchronized (this.lock)
        {
            return this.map.getOrThrow(key);
        }
    }

    @Override
    public boolean containsKey(long key)
    {
        synchronized (this.lock)
        {
            return this.map.containsKey(key);
        }
    }

    @Override
    public boolean containsValue(boolean value)
    {
        synchronized (this.lock)
        {
            return this.map.containsValue(value);
        }
    }

    @Override
    public void forEachValue(BooleanProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEachValue(procedure);
        }
    }

    @Override
    public void forEachKey(LongProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEachKey(procedure);
        }
    }

    @Override
    public void forEachKeyValue(LongBooleanProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEachKeyValue(procedure);
        }
    }

    @Override
    public LazyLongIterable keysView()
    {
        synchronized (this.lock)
        {
            return this.map.keysView();
        }
    }

    @Override
    public RichIterable<LongBooleanPair> keyValuesView()
    {
        synchronized (this.lock)
        {
            return SynchronizedRichIterable.of(this.map.keyValuesView(), this.lock).asLazy();
        }
    }

    @Override
    public MutableLongBooleanMap select(LongBooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.select(predicate);
        }
    }

    @Override
    public MutableLongBooleanMap reject(LongBooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.reject(predicate);
        }
    }

    /**
     * This must be manually synchronized by the developer.
     */
    @Override
    public MutableBooleanIterator booleanIterator()
    {
        return this.map.booleanIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.map.forEach(procedure);
        }
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.noneSatisfy(predicate);
        }
    }

    @Override
    public MutableBooleanBag select(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.select(predicate);
        }
    }

    @Override
    public MutableBooleanBag reject(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.map.reject(predicate);
        }
    }

    @Override
    public <V> MutableBag<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.map.collect(function);
        }
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        synchronized (this.lock)
        {
            return this.map.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public boolean[] toArray()
    {
        synchronized (this.lock)
        {
            return this.map.toArray();
        }
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        synchronized (this.lock)
        {
            return this.map.toArray(target);
        }
    }

    @Override
    public boolean contains(boolean value)
    {
        synchronized (this.lock)
        {
            return this.map.contains(value);
        }
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        synchronized (this.lock)
        {
            return this.map.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        synchronized (this.lock)
        {
            return this.map.containsAll(source);
        }
    }

    @Override
    public MutableBooleanList toList()
    {
        synchronized (this.lock)
        {
            return this.map.toList();
        }
    }

    @Override
    public MutableBooleanSet toSet()
    {
        synchronized (this.lock)
        {
            return this.map.toSet();
        }
    }

    @Override
    public MutableBooleanBag toBag()
    {
        synchronized (this.lock)
        {
            return this.map.toBag();
        }
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        synchronized (this.lock)
        {
            return this.map.asLazy();
        }
    }

    @Override
    public MutableLongBooleanMap withKeyValue(long key, boolean value)
    {
        synchronized (this.lock)
        {
            this.map.withKeyValue(key, value);
        }
        return this;
    }

    @Override
    public MutableLongBooleanMap withoutKey(long key)
    {
        synchronized (this.lock)
        {
            this.map.withoutKey(key);
        }
        return this;
    }

    @Override
    public MutableLongBooleanMap withoutAllKeys(LongIterable keys)
    {
        synchronized (this.lock)
        {
            this.map.withoutAllKeys(keys);
        }
        return this;
    }

    @Override
    public MutableLongBooleanMap asUnmodifiable()
    {
        synchronized (this.lock)
        {
            return new UnmodifiableLongBooleanMap(this);
        }
    }

    @Override
    public MutableLongBooleanMap asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableLongBooleanMap toImmutable()
    {
        synchronized (this.lock)
        {
            return LongBooleanMaps.immutable.withAll(this);
        }
    }

    @Override
    public int size()
    {
        synchronized (this.lock)
        {
            return this.map.size();
        }
    }

    @Override
    public boolean isEmpty()
    {
        synchronized (this.lock)
        {
            return this.map.isEmpty();
        }
    }

    @Override
    public boolean notEmpty()
    {
        synchronized (this.lock)
        {
            return this.map.notEmpty();
        }
    }

    @Override
    public MutableLongSet keySet()
    {
        synchronized (this.lock)
        {
            return SynchronizedLongSet.of(this.map.keySet(), this.lock);
        }
    }

    @Override
    public MutableBooleanCollection values()
    {
        synchronized (this.lock)
        {
            return SynchronizedBooleanCollection.of(this.map.values(), this.lock);
        }
    }

    @Override
    public boolean equals(Object otherMap)
    {
        synchronized (this.lock)
        {
            return this.map.equals(otherMap);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.lock)
        {
            return this.map.hashCode();
        }
    }

    @Override
    public String toString()
    {
        synchronized (this.lock)
        {
            return this.map.toString();
        }
    }

    @Override
    public String makeString()
    {
        synchronized (this.lock)
        {
            return this.map.makeString();
        }
    }

    @Override
    public String makeString(String separator)
    {
        synchronized (this.lock)
        {
            return this.map.makeString(separator);
        }
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            return this.map.makeString(start, separator, end);
        }
    }

    @Override
    public void appendString(Appendable appendable)
    {
        synchronized (this.lock)
        {
            this.map.appendString(appendable);
        }
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        synchronized (this.lock)
        {
            this.map.appendString(appendable, separator);
        }
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            this.map.appendString(appendable, start, separator, end);
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.map.injectInto(injectedValue, function);
        }
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.map.chunk(size);
        }
    }
}
