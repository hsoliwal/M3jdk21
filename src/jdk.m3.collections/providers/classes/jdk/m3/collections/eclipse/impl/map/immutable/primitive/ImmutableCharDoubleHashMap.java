/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.immutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.LazyCharIterable;
import jdk.m3.collections.eclipse.api.LazyDoubleIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableDoubleBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableDoubleBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectDoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharDoublePredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharDoubleProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableDoubleCollection;
import jdk.m3.collections.eclipse.api.iterator.CharIterator;
import jdk.m3.collections.eclipse.api.iterator.DoubleIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.api.map.primitive.CharDoubleMap;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleCharMaps;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableCharDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableDoubleCharMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableCharDoubleMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableDoubleCharMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableDoubleSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.CharDoublePair;
import jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive.CheckedCharDoubleProcedure;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableDoubleCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableDoubleIterator;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.CharDoubleHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableCharSet;

/**
 * ImmutableCharDoubleHashMap is the non-modifiable equivalent of {@link CharDoubleHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableCharDoubleHashMap implements ImmutableCharDoubleMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableCharDoubleMap delegate;

    ImmutableCharDoubleHashMap(CharDoubleMap delegate)
    {
        this.delegate = new CharDoubleHashMap(delegate);
    }

    @Override
    public double get(char key)
    {
        return this.delegate.get(key);
    }

    @Override
    public double getIfAbsent(char key, double ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public double getOrThrow(char key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(char key)
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
    public void forEachKey(CharProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(CharDoubleProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(CharDoublePredicate predicate)
    {
        return this.delegate.allSatisfyKeyValue(predicate);
    }

    @Override
    public LazyCharIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<CharDoublePair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableDoubleCharMap flipUniqueValues()
    {
        MutableDoubleCharMap result = DoubleCharMaps.mutable.empty();
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
    public ImmutableCharDoubleMap select(CharDoublePredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableCharDoubleMap reject(CharDoublePredicate predicate)
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
    public ImmutableCharDoubleMap toImmutable()
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
    public ImmutableCharDoubleMap newWithKeyValue(char key, double value)
    {
        MutableCharDoubleMap map = new CharDoubleHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableCharDoubleMap newWithoutKey(char key)
    {
        MutableCharDoubleMap map = new CharDoubleHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableCharDoubleMap newWithoutAllKeys(CharIterable keys)
    {
        MutableCharDoubleMap map = new CharDoubleHashMap(this.size());
        map.putAll(this);
        CharIterator iterator = keys.charIterator();
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
    public MutableCharSet keySet()
    {
        return UnmodifiableCharSet.of(this.delegate.keySet());
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
        return new ImmutableCharDoubleMapSerializationProxy(this);
    }

    protected static class ImmutableCharDoubleMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private CharDoubleMap map;

        public ImmutableCharDoubleMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableCharDoubleMapSerializationProxy(CharDoubleMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedCharDoubleProcedure()
                {
                    @Override
                    public void safeValue(char key, double value) throws IOException
                    {
                        out.writeChar(key);
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
            MutableCharDoubleMap deserializedMap = new CharDoubleHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readChar(), in.readDouble());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
