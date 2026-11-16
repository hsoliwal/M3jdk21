/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.bag.immutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.ImmutableBag;
import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ShortBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableShortBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableShortBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.ShortHashBag;
import jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive.CheckedShortIntProcedure;
import jdk.m3.collections.eclipse.impl.factory.Lists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortBags;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableShortIterator;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortIntPair;
import jdk.m3.collections.eclipse.api.set.primitive.ImmutableShortSet;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortSets;

/**
 * ImmutableShortHashBag is the non-modifiable equivalent of {@link ShortHashBag}.
 * This file was automatically generated from template file immutablePrimitiveHashBag.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("serial")
final class ImmutableShortHashBag implements ImmutableShortBag, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableShortBag delegate;

    private ImmutableShortHashBag(short[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = ShortHashBag.newBagWith(newElements);
    }

    private ImmutableShortHashBag(MutableShortBag delegate)
    {
        MutableShortBag defensiveCopy = new ShortHashBag(delegate);
        this.checkOptimizedSize(defensiveCopy.size());
        this.delegate = defensiveCopy;
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use ShortBags.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableShortHashBag newBagWith(short... elements)
    {
        return new ImmutableShortHashBag(elements);
    }

    public static ImmutableShortHashBag newBagWith(MutableShortBag elements)
    {
        return new ImmutableShortHashBag(elements);
    }

    @Override
    public ImmutableShortBag newWith(short element)
    {
        return ShortHashBag.newBag(this.delegate).with(element).toImmutable();
    }

    @Override
    public ImmutableShortBag newWithout(short element)
    {
        ShortHashBag hashBag = ShortHashBag.newBag(this.delegate);
        hashBag.remove(element);
        return hashBag.toImmutable();
    }

    @Override
    public ImmutableShortBag newWithAll(ShortIterable elements)
    {
        ShortHashBag bag = ShortHashBag.newBag(this.delegate);
        bag.addAll(elements);
        return bag.toImmutable();
    }

    @Override
    public ImmutableShortBag newWithoutAll(ShortIterable elements)
    {
        ShortHashBag bag = ShortHashBag.newBag(this.delegate);
        bag.removeAll(elements);
        return bag.toImmutable();
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
    public boolean contains(short value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(short... elements)
    {
        return this.delegate.containsAll(elements);
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
    public ImmutableShortBag select(ShortPredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableShortBag selectByOccurrences(IntPredicate predicate)
    {
        return this.delegate.selectByOccurrences(predicate).toImmutable();
    }

    @Override
    public ImmutableShortSet selectUnique()
    {
        MutableShortSet result = ShortSets.mutable.empty();
        this.forEachWithOccurrences((each, occurrences) ->
        {
            if (occurrences == 1)
            {
                result.add(each);
            }
        });
        return result.toImmutable();
    }

    @Override
    public ImmutableList<ShortIntPair> topOccurrences(int count)
    {
        return this.delegate.topOccurrences(count).toImmutable();
    }

    @Override
    public ImmutableList<ShortIntPair> bottomOccurrences(int count)
    {
        return this.delegate.bottomOccurrences(count).toImmutable();
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
    public MutableShortList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public int sizeDistinct()
    {
        return this.delegate.sizeDistinct();
    }

    @Override
    public int occurrencesOf(short item)
    {
        return this.delegate.occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(ShortIntProcedure shortIntProcedure)
    {
        this.delegate.forEachWithOccurrences(shortIntProcedure);
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
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
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public short min()
    {
        return this.delegate.min();
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
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
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
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                ShortIterator iterator = this.shortIterator();
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
        }
        return result.toImmutable();
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
    public ImmutableShortBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyShortIterable asLazy()
    {
        return this.delegate.asLazy();
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
    public String toString()
    {
        return this.delegate.toString();
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
    public ShortIterator shortIterator()
    {
        return new UnmodifiableShortIterator(this.delegate.shortIterator());
    }

    private Object writeReplace()
    {
        return new ImmutableShortBagSerializationProxy(this);
    }

    protected static class ImmutableShortBagSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ShortBag bag;

        public ImmutableShortBagSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableShortBagSerializationProxy(ShortBag bag)
        {
            this.bag = bag;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeInt(this.bag.sizeDistinct());
            try
            {
                this.bag.forEachWithOccurrences(new CheckedShortIntProcedure()
                {
                    @Override
                    public void safeValue(short item, int count) throws IOException
                    {
                        out.writeShort(item);
                        out.writeInt(count);
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
            MutableShortBag deserializedBag = new ShortHashBag();

            for (int i = 0; i < size; i++)
            {
                deserializedBag.addOccurrences(in.readShort(), in.readInt());
            }

            this.bag = deserializedBag;
        }

        protected Object readResolve()
        {
            return this.bag.toImmutable();
        }
    }
}
