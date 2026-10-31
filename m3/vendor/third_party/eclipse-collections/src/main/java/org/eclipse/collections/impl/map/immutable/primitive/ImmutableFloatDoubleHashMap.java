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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.bag.primitive.ImmutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatDoublePredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatDoubleProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.map.primitive.FloatDoubleMap;
import org.eclipse.collections.impl.factory.primitive.DoubleFloatMaps;
import org.eclipse.collections.api.map.primitive.ImmutableFloatDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableDoubleFloatMap;
import org.eclipse.collections.api.map.primitive.MutableFloatDoubleMap;
import org.eclipse.collections.api.map.primitive.MutableDoubleFloatMap;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.tuple.primitive.FloatDoublePair;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedFloatDoubleProcedure;
import org.eclipse.collections.impl.collection.mutable.primitive.UnmodifiableDoubleCollection;
import org.eclipse.collections.impl.factory.primitive.DoubleBags;
import org.eclipse.collections.impl.iterator.UnmodifiableDoubleIterator;
import org.eclipse.collections.impl.map.mutable.primitive.FloatDoubleHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableFloatSet;

/**
 * ImmutableFloatDoubleHashMap is the non-modifiable equivalent of {@link FloatDoubleHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableFloatDoubleHashMap implements ImmutableFloatDoubleMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableFloatDoubleMap delegate;

    ImmutableFloatDoubleHashMap(FloatDoubleMap delegate)
    {
        this.delegate = new FloatDoubleHashMap(delegate);
    }

    @Override
    public double get(float key)
    {
        return this.delegate.get(key);
    }

    @Override
    public double getIfAbsent(float key, double ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public double getOrThrow(float key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(float key)
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
    public void forEachKey(FloatProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(FloatDoubleProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    @Override
    public LazyFloatIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<FloatDoublePair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableDoubleFloatMap flipUniqueValues()
    {
        MutableDoubleFloatMap result = DoubleFloatMaps.mutable.empty();
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
    public ImmutableFloatDoubleMap select(FloatDoublePredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableFloatDoubleMap reject(FloatDoublePredicate predicate)
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
    public ImmutableFloatDoubleMap toImmutable()
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
    public ImmutableFloatDoubleMap newWithKeyValue(float key, double value)
    {
        MutableFloatDoubleMap map = new FloatDoubleHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableFloatDoubleMap newWithoutKey(float key)
    {
        MutableFloatDoubleMap map = new FloatDoubleHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableFloatDoubleMap newWithoutAllKeys(FloatIterable keys)
    {
        MutableFloatDoubleMap map = new FloatDoubleHashMap(this.size());
        map.putAll(this);
        FloatIterator iterator = keys.floatIterator();
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
    public MutableFloatSet keySet()
    {
        return UnmodifiableFloatSet.of(this.delegate.keySet());
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
        return new ImmutableFloatDoubleMapSerializationProxy(this);
    }

    protected static class ImmutableFloatDoubleMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private FloatDoubleMap map;

        public ImmutableFloatDoubleMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableFloatDoubleMapSerializationProxy(FloatDoubleMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedFloatDoubleProcedure()
                {
                    @Override
                    public void safeValue(float key, double value) throws IOException
                    {
                        out.writeFloat(key);
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
            MutableFloatDoubleMap deserializedMap = new FloatDoubleHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readFloat(), in.readDouble());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
