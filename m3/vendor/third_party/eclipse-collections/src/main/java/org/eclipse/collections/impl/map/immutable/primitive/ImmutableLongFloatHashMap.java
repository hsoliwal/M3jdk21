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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongFloatPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.map.primitive.LongFloatMap;
import org.eclipse.collections.impl.factory.primitive.FloatLongMaps;
import org.eclipse.collections.api.map.primitive.ImmutableLongFloatMap;
import org.eclipse.collections.api.map.primitive.ImmutableFloatLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongFloatMap;
import org.eclipse.collections.api.map.primitive.MutableFloatLongMap;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.tuple.primitive.LongFloatPair;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedLongFloatProcedure;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableFloatCollection;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.impl.map.mutable.primitive.LongFloatHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableLongSet;

/**
 * ImmutableLongFloatHashMap is the non-modifiable equivalent of {@link LongFloatHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongFloatHashMap implements ImmutableLongFloatMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableLongFloatMap delegate;

    ImmutableLongFloatHashMap(LongFloatMap delegate)
    {
        this.delegate = new LongFloatHashMap(delegate);
    }

    @Override
    public float get(long key)
    {
        return this.delegate.get(key);
    }

    @Override
    public float getIfAbsent(long key, float ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public float getOrThrow(long key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(long key)
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
    public void forEachKey(LongProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(LongFloatProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    @Override
    public LazyLongIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<LongFloatPair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableFloatLongMap flipUniqueValues()
    {
        MutableFloatLongMap result = FloatLongMaps.mutable.empty();
        this.forEachKeyValue((key, value) ->
        {
            if (result.containsKey(value))
            {
                throw new IllegalStateException("Duplicate value: " + value + " found at key: " + result.get(value) + " and key: " + key);
            }
            result.put(value, key);
        });
        return result.toImmutable();
    }

    @Override
    public ImmutableLongFloatMap select(LongFloatPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableLongFloatMap reject(LongFloatPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
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
            FloatIterator iterator = this.delegate.floatIterator();
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
    public ImmutableLongFloatMap toImmutable()
    {
        return this;
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
    public ImmutableFloatBag select(FloatPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableFloatBag reject(FloatPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(FloatToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
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
    public ImmutableLongFloatMap newWithKeyValue(long key, float value)
    {
        MutableLongFloatMap map = new LongFloatHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableLongFloatMap newWithoutKey(long key)
    {
        MutableLongFloatMap map = new LongFloatHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableLongFloatMap newWithoutAllKeys(LongIterable keys)
    {
        MutableLongFloatMap map = new LongFloatHashMap(this.size());
        map.putAll(this);
        LongIterator iterator = keys.longIterator();
        while (iterator.hasNext())
        {
            map.removeKey(iterator.next());
        }
        return map.toImmutable();
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

    @Override
    public MutableLongSet keySet()
    {
        return UnmodifiableLongSet.of(this.delegate.keySet());
    }

    @Override
    public MutableFloatCollection values()
    {
        return UnmodifiableFloatCollection.of(this.delegate.values());
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

    private Object writeReplace()
    {
        return new ImmutableLongFloatMapSerializationProxy(this);
    }

    protected static class ImmutableLongFloatMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private LongFloatMap map;

        public ImmutableLongFloatMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableLongFloatMapSerializationProxy(LongFloatMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedLongFloatProcedure()
                {
                    @Override
                    public void safeValue(long key, float value) throws IOException
                    {
                        out.writeLong(key);
                        out.writeFloat(value);
                    }
                });
            }
            catch (RuntimeException e)
            {
                if (e.getCause() instanceof IOException)
                {
                    throw (IOException) e.getCause();
                }
                throw e;
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            int size = in.readInt();
            MutableLongFloatMap deserializedMap = new LongFloatHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readLong(), in.readFloat());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
