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

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableBooleanBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableBooleanBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectBooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortBooleanPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortBooleanProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableBooleanCollection;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.iterator.BooleanIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.map.primitive.ShortBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.ImmutableShortBooleanMap;
import jdk.m3.collections.eclipse.api.map.primitive.MutableShortBooleanMap;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortBooleanPair;
import jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive.CheckedShortBooleanProcedure;
import jdk.m3.collections.eclipse.impl.collection.mutable.primitive.UnmodifiableBooleanCollection;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableBooleanIterator;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.ShortBooleanHashMap;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.UnmodifiableShortSet;

/**
 * ImmutableShortBooleanHashMap is the non-modifiable equivalent of {@link ShortBooleanHashMap}.
 * This file was automatically generated from template file immutablePrimitivePrimitiveHashMap.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableShortBooleanHashMap implements ImmutableShortBooleanMap, Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableShortBooleanMap delegate;

    ImmutableShortBooleanHashMap(ShortBooleanMap delegate)
    {
        this.delegate = new ShortBooleanHashMap(delegate);
    }

    @Override
    public boolean get(short key)
    {
        return this.delegate.get(key);
    }

    @Override
    public boolean getIfAbsent(short key, boolean ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean getOrThrow(short key)
    {
        return this.delegate.getOrThrow(key);
    }

    @Override
    public boolean containsKey(short key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(boolean value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public void forEachValue(BooleanProcedure procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(ShortProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(ShortBooleanProcedure procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    /**
     * @since 12.0
     */
    public boolean allSatisfyKeyValue(ShortBooleanPredicate predicate)
    {
        return this.delegate.allSatisfyKeyValue(predicate);
    }

    @Override
    public LazyShortIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<ShortBooleanPair> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableShortBooleanMap select(ShortBooleanPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableShortBooleanMap reject(ShortBooleanPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<BooleanIterable> result = Lists.mutable.empty();
        if (this.notEmpty())
        {
            BooleanIterator iterator = this.delegate.booleanIterator();
            while (iterator.hasNext())
            {
                MutableBooleanBag batch = BooleanBags.mutable.empty();
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
    public ImmutableShortBooleanMap toImmutable()
    {
        return this;
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(this.delegate.booleanIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public ImmutableBooleanBag select(BooleanPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableBooleanBag reject(BooleanPredicate predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <V> ImmutableBag<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        MutableBag<V> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public boolean[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        return this.delegate.toArray(target);
    }

    @Override
    public boolean contains(boolean value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableBooleanList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public ImmutableShortBooleanMap newWithKeyValue(short key, boolean value)
    {
        MutableShortBooleanMap map = new ShortBooleanHashMap(this.size() + 1);
        map.putAll(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableShortBooleanMap newWithoutKey(short key)
    {
        MutableShortBooleanMap map = new ShortBooleanHashMap(this.size());
        map.putAll(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableShortBooleanMap newWithoutAllKeys(ShortIterable keys)
    {
        MutableShortBooleanMap map = new ShortBooleanHashMap(this.size());
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
    public MutableBooleanCollection values()
    {
        return UnmodifiableBooleanCollection.of(this.delegate.values());
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
        return new ImmutableShortBooleanMapSerializationProxy(this);
    }

    protected static class ImmutableShortBooleanMapSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ShortBooleanMap map;

        public ImmutableShortBooleanMapSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableShortBooleanMapSerializationProxy(ShortBooleanMap map)
        {
            this.map = map;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException
        {
            out.writeInt(this.map.size());
            try
            {
                this.map.forEachKeyValue(new CheckedShortBooleanProcedure()
                {
                    @Override
                    public void safeValue(short key, boolean value) throws IOException
                    {
                        out.writeShort(key);
                        out.writeBoolean(value);
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
            MutableShortBooleanMap deserializedMap = new ShortBooleanHashMap();

            for (int i = 0; i < size; i++)
            {
                deserializedMap.put(in.readShort(), in.readBoolean());
            }

            this.map = deserializedMap;
        }

        protected Object readResolve()
        {
            return this.map.toImmutable();
        }
    }
}
