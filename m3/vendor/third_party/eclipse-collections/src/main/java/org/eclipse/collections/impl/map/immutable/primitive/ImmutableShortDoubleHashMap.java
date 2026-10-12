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
import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyShortIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.bag.primitive.ImmutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortDoublePredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortDoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.ShortIterator;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.map.primitive.ShortDoubleMap;
import org.eclipse.collections.impl.factory.primitive.DoubleShortMaps;
import org.eclipse.collections.api.map.primitive.ImmutableShortDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleShortMap;
import org.eclipse.collections.api.map.primitive.MutableShortDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleShortMap;
import org.eclipse.collections.api.set.primitive.MutableShortSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.ShortDoublePair;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedShortDoubleProcedure;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableDoubleCollection;
import org.eclipse.collections.impl.factory.primitive.DoubleBags;
import org.eclipse.collections.impl.iterator.UnmodifiableDoubleIterator;
import org.eclipse.collections.impl.map.mutable.primitive.ShortDoubleHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableShortSet;

/**
 * ImmutableShortDoubleHashMap is the non-modifiable equivalent of {@link ShortDoubleHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableShortDoubleHashMap implements ImmutableShortDoubleMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableShortDoubleMap delegate;

    ImmutableShortDoubleHashMap(ShortDoubleMap delegate)
    {
        this.delegate = new ShortDoubleHashMap(delegate);
    }

    @Override
    public double get(short key)
    {
        return this.delegate.get(key);
    }

    @Override
    public double getIfAbsent(short key, double ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public double getOrThrow(short key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(short key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(double value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(DoubleProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(ShortProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ShortDoubleProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    @Override
    public LazyShortIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<ShortDoublePair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableDoubleShortMap flipUniqueValues()
    {
        MutableDoubleShortMap result = DoubleShortMaps.mutable.empty();
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
    public ImmutableShortDoubleMap select(ShortDoublePredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableShortDoubleMap reject(ShortDoublePredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<DoubleIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            DoubleIterator iterator = this.delegate.doubleIterator();
            while (iterator.hasNext())
            {
                MutableDoubleBag batch = DoubleBags.mutable.empty();
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
    public ImmutableShortDoubleMap toImmutable()
    {
        return this;
    }

    @Override
    public DoubleIterator doubleIterator()
    {
        return new UnmodifiableDoubleIterator(this.delegate.doubleIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableDoubleBag select(DoublePredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableDoubleBag reject(DoublePredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public double sum()
    {
        return this.delegate.sum();
    }

    @Override
    public double max()
    {
        return this.delegate.max();
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
    }

    @Override
    public double min()
    {
        return this.delegate.min();
    }

    @Override
    public double minIfEmpty(double defaultValue)
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
    public double[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public double[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public double[] toArray(double[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(double value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(double... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableDoubleList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableShortDoubleMap newWithKeyValue(short key, double value)
    {
        MutableShortDoubleMap map = new ShortDoubleHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableShortDoubleMap newWithoutKey(short key)
    {
        MutableShortDoubleMap map = new ShortDoubleHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableShortDoubleMap newWithoutAllKeys(ShortIterable keys)
    {
        MutableShortDoubleMap map = new ShortDoubleHashMap(this.size());
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
    public MutableDoubleCollection values()
    {
        return UnmodifiableDoubleCollection.of(this.delegate.values());
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
        return new ImmutableShortDoubleMapSerializationProxy(this);
    }

    protected static class ImmutableShortDoubleMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ShortDoubleMap map;

        public ImmutableShortDoubleMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableShortDoubleMapSerializationProxy(ShortDoubleMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedShortDoubleProcedure()
                {
                    @Override
                    public void safeValue(short key, double value) throws IOException
                    {
                        out.writeShort(key);
                        out.writeDouble(value);
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
            MutableShortDoubleMap deserializedMap = new ShortDoubleHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readShort(), in.readDouble());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
