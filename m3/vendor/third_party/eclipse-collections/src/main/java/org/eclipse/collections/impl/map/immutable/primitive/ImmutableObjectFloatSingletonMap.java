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
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatObjectMap;
import org.eclipse.collections.api.map.primitive.ObjectFloatMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.FloatLists;
import org.eclipse.collections.impl.factory.primitive.ObjectFloatMaps;
import org.eclipse.collections.impl.factory.primitive.FloatObjectMaps;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ImmutableObjectFloatSingletonMap is an optimization for {@link ImmutableObjectFloatMap} of size 1.
 * This file was automatically generated from template file immutableObjectPrimitiveSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableObjectFloatSingletonMap<K> extends AbstractImmutableObjectFloatMap<K> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private static final float EMPTY_VALUE = 0.0f;
    private final K key1;
    private final float value1;

    ImmutableObjectFloatSingletonMap(K key1, float value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new UnmodifiableFloatIterator(ObjectFloatHashMap.newWithKeysValues(this.key1, this.value1).floatIterator());
    }

    @Override
    public void forEach(FloatProcedure procedure)
    {
        this.each(procedure);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public ImmutableFloatCollection select(FloatPredicate predicate)
    {
        return predicate.accept(this.value1) ? FloatLists.immutable.with(this.value1) : FloatLists.immutable.with();
    }

    @Override
    public ImmutableFloatCollection reject(FloatPredicate predicate)
    {
        return predicate.accept(this.value1) ? FloatLists.immutable.with() : FloatLists.immutable.with(this.value1);
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return predicate.accept(this.value1) ? this.value1 : ifNone;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.value1);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        return Lists.immutable.with(FloatLists.immutable.with(this.value1));
    }

    @Override
    public <V> ImmutableCollection<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Lists.immutable.of(function.valueOf(this.value1));
    }

    @Override
    public double sum()
    {
        return this.value1;
    }

    @Override
    public float min()
    {
        return this.value1;
    }

    @Override
    public float max()
    {
        return this.value1;
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.value1;
    }

    @Override
    public float minIfEmpty(float defaultValue)
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
    public float[] toSortedArray()
    {
        return new float[]{this.value1};
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return FloatArrayList.newListWith(this.value1);
    }

    @Override
    public float[] toArray()
    {
        return new float[]{this.value1};
    }

    @Override
    public float[] toArray(float[] target)
    {
        if (target.length < 1)
        {
            target = new float[]{this.value1};
        }
        else
        {
            target[0] = this.value1;
        }
        return target;
    }

    @Override
    public boolean contains(float value)
    {
        return Float.compare(this.value1, value) == 0;
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float value : source)
        {
            if (Float.compare(this.value1, value) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        for (FloatIterator iterator = source.floatIterator(); iterator.hasNext(); )
        {
            if (Float.compare(this.value1, iterator.next()) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatArrayList.newListWith(this.value1);
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatHashSet.newSetWith(this.value1);
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatHashBag.newBagWith(this.value1);
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public ImmutableObjectFloatMap<K> newWithKeyValue(K key, float value)
    {
        return ObjectFloatMaps.immutable.withAll(ObjectFloatHashMap.newWithKeysValues(this.key1, this.value1, key, value));
    }

    @Override
    public ImmutableObjectFloatMap<K> newWithoutKey(K key)
    {
        return nullSafeEquals(this.key1, key) ? (ImmutableObjectFloatMap<K>) ImmutableObjectFloatEmptyMap.INSTANCE : this;
    }

    private static boolean nullSafeEquals(Object value, Object other)
    {
        if (value == null)
        {
            if (other == null)
            {
                return true;
            }
        }
        else if (other == value || value.equals(other))
        {
            return true;
        }
        return false;
    }

    @Override
    public ImmutableObjectFloatMap<K> newWithoutAllKeys(Iterable<? extends K> keys)
    {
        ObjectFloatHashMap<K> map = new ObjectFloatHashMap<K>(this);
        for (K key : keys)
        {
            map.removeKey(key);
        }
        return map.toImmutable();
    }

    @Override
    public float get(Object key)
    {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    @Override
    public float getOrThrow(Object key)
    {
        if (nullSafeEquals(this.key1, key))
        {
            return this.value1;
        }
        throw new IllegalStateException("Key " + key + " not present.");
    }

    @Override
    public float getIfAbsent(Object key, float ifAbsent)
    {
        return nullSafeEquals(this.key1, key) ? this.value1 : ifAbsent;
    }

    @Override
    public boolean containsKey(Object key)
    {
        return nullSafeEquals(this.key1, key);
    }

    @Override
    public boolean containsValue(float value)
    {
        return Float.compare(this.value1, value) == 0;
    }

    @Override
    public void forEachValue(FloatProcedure procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(Procedure<? super K> procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(ObjectFloatProcedure<? super K> objectFloatProcedure)
    {
        objectFloatProcedure.value(this.key1, this.value1);
    }

    @Override
    public ImmutableObjectFloatMap<K> select(ObjectFloatPredicate<? super K> objectFloatPredicate)
    {
        return objectFloatPredicate.accept(this.key1, this.value1) ? ObjectFloatHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : (ImmutableObjectFloatMap<K>) ImmutableObjectFloatEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableObjectFloatMap<K> reject(ObjectFloatPredicate<? super K> objectFloatPredicate)
    {
        return objectFloatPredicate.accept(this.key1, this.value1) ? (ImmutableObjectFloatMap<K>) ImmutableObjectFloatEmptyMap.INSTANCE
                : ObjectFloatHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public ImmutableObjectFloatMap<K> toImmutable()
    {
        return this;
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
    public Set<K> keySet()
    {
        return Sets.immutable.of(this.key1).castToSet();
    }

    @Override
    public MutableFloatCollection values()
    {
        return UnmodifiableFloatCollection.of(FloatArrayList.newListWith(this.value1));
    }

    @Override
    public LazyIterable<K> keysView()
    {
        return Lists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<ObjectFloatPair<K>> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableFloatObjectMap<K> flipUniqueValues()
    {
        return FloatObjectMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof ObjectFloatMap))
        {
            return false;
        }
        ObjectFloatMap<K> map = (ObjectFloatMap<K>) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && Float.compare(this.value1, map.getOrThrow(this.key1)) == 0;
    }

    @Override
    public int hashCode()
    {
        return (this.key1 == null ? 0 : this.key1.hashCode()) ^ Float.floatToIntBits(this.value1);
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

    private Object writeReplace()
    {
        return new ImmutableObjectFloatMapSerializationProxy<K>(this);
    }
}
