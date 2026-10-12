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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.LazyIntIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntLongPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.map.primitive.IntLongMap;
import org.eclipse.collections.impl.factory.primitive.LongIntMaps;
import org.eclipse.collections.api.map.primitive.ImmutableIntLongMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongIntMap;
import org.eclipse.collections.api.map.primitive.MutableIntLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongIntMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.IntLongPair;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedIntLongProcedure;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import org.eclipse.collections.impl.iterator.UnmodifiableLongIterator;
import org.eclipse.collections.impl.map.mutable.primitive.IntLongHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableIntSet;

/**
 * ImmutableIntLongHashMap is the non-modifiable equivalent of {@link IntLongHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableIntLongHashMap implements ImmutableIntLongMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableIntLongMap delegate;

    ImmutableIntLongHashMap(IntLongMap delegate)
    {
        this.delegate = new IntLongHashMap(delegate);
    }

    @Override
    public long get(int key)
    {
        return this.delegate.get(key);
    }

    @Override
    public long getIfAbsent(int key, long ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public long getOrThrow(int key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(int key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(long value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(LongProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(IntProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(IntLongProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    @Override
    public LazyIntIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<IntLongPair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableLongIntMap flipUniqueValues()
    {
        MutableLongIntMap result = LongIntMaps.mutable.empty();
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
    public ImmutableIntLongMap select(IntLongPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableIntLongMap reject(IntLongPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<LongIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<LongIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            LongIterator iterator = this.delegate.longIterator();
            while (iterator.hasNext())
            {
                MutableLongBag batch = LongBags.mutable.empty();
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
    public ImmutableIntLongMap toImmutable()
    {
        return this;
    }

    @Override
    public LongIterator longIterator()
    {
        return new UnmodifiableLongIterator(this.delegate.longIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(LongPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableLongBag select(LongPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableLongBag reject(LongPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(LongToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public long max()
    {
        return this.delegate.max();
    }

    @Override
    public long maxIfEmpty(long defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public long min()
    {
        return this.delegate.min();
    }

    @Override
    public long minIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableLongList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public long[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public long[] toArray(long[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(long value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableLongList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableLongSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableLongBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyLongIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableIntLongMap newWithKeyValue(int key, long value)
    {
        MutableIntLongMap map = new IntLongHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableIntLongMap newWithoutKey(int key)
    {
        MutableIntLongMap map = new IntLongHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableIntLongMap newWithoutAllKeys(IntIterable keys)
    {
        MutableIntLongMap map = new IntLongHashMap(this.size());
        map.putAll(this);
        IntIterator iterator = keys.intIterator();
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
    public MutableIntSet keySet()
    {
        return UnmodifiableIntSet.of(this.delegate.keySet());
    }

    @Override
    public MutableLongCollection values()
    {
        return UnmodifiableLongCollection.of(this.delegate.values());
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
        return new ImmutableIntLongMapSerializationProxy(this);
    }

    protected static class ImmutableIntLongMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private IntLongMap map;

        public ImmutableIntLongMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableIntLongMapSerializationProxy(IntLongMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedIntLongProcedure()
                {
                    @Override
                    public void safeValue(int key, long value) throws IOException
                    {
                        out.writeInt(key);
                        out.writeLong(value);
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
            MutableIntLongMap deserializedMap = new IntLongHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readInt(), in.readLong());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
