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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.bag.primitive.ImmutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleBytePredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleByteProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.map.primitive.DoubleByteMap;
import org.eclipse.collections.impl.factory.primitive.ByteDoubleMaps;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleByteMap;
import org.eclipse.collections.api.map.primitive.ImmutableByteDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleByteMap;
import org.eclipse.collections.api.map.primitive.MutableByteDoubleMap;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.api.tuple.primitive.DoubleBytePair;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedDoubleByteProcedure;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableByteCollection;
import org.eclipse.collections.impl.factory.primitive.ByteBags;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.map.mutable.primitive.DoubleByteHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableDoubleSet;

/**
 * ImmutableDoubleByteHashMap is the non-modifiable equivalent of {@link DoubleByteHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableDoubleByteHashMap implements ImmutableDoubleByteMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableDoubleByteMap delegate;

    ImmutableDoubleByteHashMap(DoubleByteMap delegate)
    {
        this.delegate = new DoubleByteHashMap(delegate);
    }

    @Override
    public byte get(double key)
    {
        return this.delegate.get(key);
    }

    @Override
    public byte getIfAbsent(double key, byte ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public byte getOrThrow(double key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(double key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(byte value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(ByteProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(DoubleProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(DoubleByteProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    @Override
    public LazyDoubleIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<DoubleBytePair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableByteDoubleMap flipUniqueValues()
    {
        MutableByteDoubleMap result = ByteDoubleMaps.mutable.empty();
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
    public ImmutableDoubleByteMap select(DoubleBytePredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableDoubleByteMap reject(DoubleBytePredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<ByteIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            ByteIterator iterator = this.delegate.byteIterator();
            while (iterator.hasNext())
            {
                MutableByteBag batch = ByteBags.mutable.empty();
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
    public ImmutableDoubleByteMap toImmutable()
    {
        return this;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return new UnmodifiableByteIterator(this.delegate.byteIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableByteBag select(BytePredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableByteBag reject(BytePredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(ByteToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public byte max()
    {
        return this.delegate.max();
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public byte min()
    {
        return this.delegate.min();
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
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
    public byte[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableByteList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public byte[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(byte value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableByteList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableByteSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableByteBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableDoubleByteMap newWithKeyValue(double key, byte value)
    {
        MutableDoubleByteMap map = new DoubleByteHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableDoubleByteMap newWithoutKey(double key)
    {
        MutableDoubleByteMap map = new DoubleByteHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableDoubleByteMap newWithoutAllKeys(DoubleIterable keys)
    {
        MutableDoubleByteMap map = new DoubleByteHashMap(this.size());
        map.putAll(this);
        DoubleIterator iterator = keys.doubleIterator();
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
    public MutableDoubleSet keySet()
    {
        return UnmodifiableDoubleSet.of(this.delegate.keySet());
    }

    @Override
    public MutableByteCollection values()
    {
        return UnmodifiableByteCollection.of(this.delegate.values());
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
        return new ImmutableDoubleByteMapSerializationProxy(this);
    }

    protected static class ImmutableDoubleByteMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private DoubleByteMap map;

        public ImmutableDoubleByteMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableDoubleByteMapSerializationProxy(DoubleByteMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedDoubleByteProcedure()
                {
                    @Override
                    public void safeValue(double key, byte value) throws IOException
                    {
                        out.writeDouble(key);
                        out.writeByte(value);
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
            MutableDoubleByteMap deserializedMap = new DoubleByteHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readDouble(), in.readByte());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
