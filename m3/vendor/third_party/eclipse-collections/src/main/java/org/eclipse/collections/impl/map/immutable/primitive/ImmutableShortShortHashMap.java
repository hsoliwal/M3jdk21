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

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableShortBag;
import org.eclipse.collections.api.bag.primitive.ImmutableShortBag;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortShortPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.list.primitive.MutableShortList;
import org.eclipse.collections.api.map.primitive.ShortShortMap;
import org.eclipse.collections.impl.factory.primitive.ShortShortMaps;
import org.eclipse.collections.api.map.primitive.ImmutableShortShortMap;
import org.eclipse.collections.api.map.primitive.MutableShortShortMap;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.tuple.primitive.ShortShortPair;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedShortShortProcedure;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableShortCollection;
import org.eclipse.collections.impl.factory.primitive.ShortBags;
import org.eclipse.collections.impl.iterator.UnmodifiableShortIterator;
import org.eclipse.collections.impl.map.mutable.primitive.ShortShortHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableShortSet;

/**
 * ImmutableShortShortHashMap is the non-modifiable equivalent of {@link ShortShortHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableShortShortHashMap implements ImmutableShortShortMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableShortShortMap delegate;

    ImmutableShortShortHashMap(ShortShortMap delegate)
    {
        this.delegate = new ShortShortHashMap(delegate);
    }

    @Override
    public short get(short key)
    {
        return this.delegate.get(key);
    }

    @Override
    public short getIfAbsent(short key, short ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public short getOrThrow(short key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(short key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(short value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(ShortProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(ShortProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ShortShortProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    @Override
    public LazyShortIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<ShortShortPair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableShortShortMap flipUniqueValues()
    {
        MutableShortShortMap result = ShortShortMaps.mutable.empty();
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
    public ImmutableShortShortMap select(ShortShortPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableShortShortMap reject(ShortShortPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ShortIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            ShortIterator iterator = this.delegate.shortIterator();
            while (iterator.hasNext())
            {
                MutableShortBag batch = ShortBags.mutable.empty();
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
    public ImmutableShortShortMap toImmutable()
    {
        return this;
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new UnmodifiableShortIterator(this.delegate.shortIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableShortBag select(ShortPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableShortBag reject(ShortPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(ShortToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public short max()
    {
        return this.delegate.max();
    }

    @Override
    public short maxIfEmpty(short defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public short min()
    {
        return this.delegate.min();
    }

    @Override
    public short minIfEmpty(short defaultValue)
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
    public short[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableShortList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public short[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public short[] toArray(short[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(short value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(short... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableShortList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableShortSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableShortBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableShortShortMap newWithKeyValue(short key, short value)
    {
        MutableShortShortMap map = new ShortShortHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableShortShortMap newWithoutKey(short key)
    {
        MutableShortShortMap map = new ShortShortHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableShortShortMap newWithoutAllKeys(ShortIterable keys)
    {
        MutableShortShortMap map = new ShortShortHashMap(this.size());
        map.putAll(this);
        ShortIterator iterator = keys.shortIterator();
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
    public MutableShortSet keySet()
    {
        return UnmodifiableShortSet.of(this.delegate.keySet());
    }

    @Override
    public MutableShortCollection values()
    {
        return UnmodifiableShortCollection.of(this.delegate.values());
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
        return new ImmutableShortShortMapSerializationProxy(this);
    }

    protected static class ImmutableShortShortMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ShortShortMap map;

        public ImmutableShortShortMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableShortShortMapSerializationProxy(ShortShortMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedShortShortProcedure()
                {
                    @Override
                    public void safeValue(short key, short value) throws IOException
                    {
                        out.writeShort(key);
                        out.writeShort(value);
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
            MutableShortShortMap deserializedMap = new ShortShortHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readShort(), in.readShort());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
