/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectFloatPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.MutableFloatObjectMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.eclipse.collections.impl.factory.primitive.FloatObjectMaps;
import org.eclipse.collections.impl.set.mutable.UnmodifiableMutableSet;

/**
 * ImmutableObjectFloatHashMap is the non-modifiable equivalent of {@link ObjectFloatHashMap}.
 * This file was automatically generated from template file immutableObjectPrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableObjectFloatHashMap<K> extends AbstractImmutableObjectFloatMap<K> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableObjectFloatMap<K> delegate;

    ImmutableObjectFloatHashMap(ObjectFloatMap<? extends K> delegate)
    {
        this.delegate = new ObjectFloatHashMap<>(delegate);
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new UnmodifiableFloatIterator(this.delegate.floatIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableFloatCollection select(FloatPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableFloatCollection reject(FloatPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<FloatIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            FloatIterator iterator = this.floatIterator();
            while (iterator.hasNext())
            {
                MutableFloatBag batch = FloatBags.mutable.empty();
                for (int i = 0; i < size && iterator.hasNext(); i++)
                {
                    batch.add(iterator.next());
                }
                result.add(batch.toImmutable());
            }
        }
        return result.toImmutable();
    }

    @Override
    public <V> ImmutableCollection<V> collect(FloatToObjectFunction<? extends V> function)
    {
        MutableCollection<V> collection = this.delegate.collect(function);
        return collection.toImmutable();
    }

    @Override
    public double sum()
    {
        return this.delegate.sum();
    }

    @Override
    public float max()
    {
        return this.delegate.max();
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public float min()
    {
        return this.delegate.min();
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        return this.delegate.minIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.delegate.average();
    }

    @Override
    public double median()
    {
        return this.delegate.median();
    }

    @Override
    public float[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public float[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public float[] toArray(float[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(float value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(float... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableFloatList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableFloatSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableFloatBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableObjectFloatMap<K> newWithKeyValue(K key, float value)
    {
        ObjectFloatHashMap<K> map = ObjectFloatHashMap.newMap();
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableObjectFloatMap<K> newWithoutKey(K key)
    {
        ObjectFloatHashMap<K> map = ObjectFloatHashMap.newMap();
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableObjectFloatMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        ObjectFloatHashMap<K> map = ObjectFloatHashMap.newMap();
        map.putAll(this);
        Iterator<? extends K> iterator = keys.iterator();
        while (iterator.hasNext())
        {
            map.removeKey(iterator.next());
        }
        return map.toImmutable();
    }

    @Override
    public float get(Object key)
    {
        return this.delegate.get(key);
    }

    @Override
    public float getOrThrow(Object key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public float getIfAbsent(Object key, float ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(float value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(FloatProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ObjectFloatProcedure<? super K> objectFloatProcedure)
    {
        this.delegate.forEachKeyValue(objectFloatProcedure);
    }

    @Override
    public ImmutableObjectFloatMap<K> select(ObjectFloatPredicate<? super K> objectFloatPredicate)
    {
        return this.delegate.select(objectFloatPredicate).toImmutable();
    }

    @Override
    public ImmutableObjectFloatMap<K> reject(ObjectFloatPredicate<? super K> objectFloatPredicate)
    {
        return this.delegate.reject(objectFloatPredicate).toImmutable();
    }

    @Override
    public ImmutableObjectFloatMap<K> toImmutable()
    {
        return this;
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.delegate.notEmpty();
    }

    @Override
    public Set<K> keySet()
    {
        return UnmodifiableMutableSet.of(this.delegate.keySet());
    }

    @Override
    public MutableFloatCollection values()
    {
        return UnmodifiableFloatCollection.of(this.delegate.values());
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<ObjectFloatPair<K>> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableFloatObjectMap<K> flipUniqueValues()
    {
        MutableFloatObjectMap<K> result = FloatObjectMaps.mutable.empty();
        this.forEachKeyValue((key, value) ->
        {
            K oldKey = result.put(value, key);
            if (oldKey != null)
            {
                throw new IllegalStateException("Duplicate value: " + value + " found at key: " + oldKey + " and key: " + key);
            }
        });
        return result.toImmutable();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.delegate.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

    @Override
    public String toString()
    {
        return this.delegate.toString();
    }

    @Override
    public String makeString()
    {
        return this.delegate.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.delegate.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.delegate.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.delegate.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.delegate.appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.delegate.appendString(appendable, start, separator, end);
    }

    private Object writeReplace()
    {
        return new ImmutableObjectFloatMapSerializationProxy<K>(this);
    }
}
