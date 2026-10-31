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
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableLongBag;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectLongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongLongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.map.primitive.LongLongMap;
import org.eclipse.collections.impl.factory.primitive.LongLongMaps;
import org.eclipse.collections.api.map.primitive.ImmutableLongLongMap;
import org.eclipse.collections.api.map.primitive.MutableLongLongMap;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedLongLongProcedure;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableLongCollection;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import org.eclipse.collections.impl.iterator.UnmodifiableLongIterator;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableLongSet;

/**
 * ImmutableLongLongHashMap is the non-modifiable equivalent of {@link LongLongHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongLongHashMap implements ImmutableLongLongMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableLongLongMap delegate;

    ImmutableLongLongHashMap(LongLongMap delegate)
    {
        this.delegate = new LongLongHashMap(delegate);
    }

    @Override
    public long get(long key)
    {
        return this.delegate.get(key);
    }

    @Override
    public long getIfAbsent(long key, long ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public long getOrThrow(long key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(long key)
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
    public void forEachKey(LongProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(LongLongProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    @Override
    public LazyLongIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<LongLongPair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableLongLongMap flipUniqueValues()
    {
        MutableLongLongMap result = LongLongMaps.mutable.empty();
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
    public ImmutableLongLongMap select(LongLongPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableLongLongMap reject(LongLongPredicate predicate)
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
    public ImmutableLongLongMap toImmutable()
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
    public ImmutableLongLongMap newWithKeyValue(long key, long value)
    {
        MutableLongLongMap map = new LongLongHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableLongLongMap newWithoutKey(long key)
    {
        MutableLongLongMap map = new LongLongHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableLongLongMap newWithoutAllKeys(LongIterable keys)
    {
        MutableLongLongMap map = new LongLongHashMap(this.size());
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
        return new ImmutableLongLongMapSerializationProxy(this);
    }

    protected static class ImmutableLongLongMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private LongLongMap map;

        public ImmutableLongLongMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableLongLongMapSerializationProxy(LongLongMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedLongLongProcedure()
                {
                    @Override
                    public void safeValue(long key, long value) throws IOException
                    {
                        out.writeLong(key);
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
            MutableLongLongMap deserializedMap = new LongLongHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readLong(), in.readLong());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
