/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.immutable.primitive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ByteBag;
import org.eclipse.collections.api.bag.primitive.ImmutableByteBag;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;
import org.eclipse.collections.impl.block.procedure.checked.primitive.CheckedByteIntProcedure;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.ByteBags;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.primitive.ByteIntPair;
import org.eclipse.collections.api.set.primitive.ImmutableByteSet;
import org.eclipse.collections.impl.factory.primitive.ByteSets;

/**
 * ImmutableByteHashBag is the non-modifiable equivalent of {@link ByteHashBag}.
 * This file was automatically generated from template file immutablePrimitiveHashBag.stg.
 *
 * @since 4.0.
 */
final class ImmutableByteHashBag implements ImmutableByteBag, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableByteBag delegate;

    private ImmutableByteHashBag(byte[] newElements)
    {
        this.checkOptimizedSize(newElements.length);
        this.delegate = ByteHashBag.newBagWith(newElements);
    }

    private void checkOptimizedSize(int length)
    {
        if (length <= 1)
        {
            throw new IllegalArgumentException("Use ByteBags.immutable.with() to instantiate an optimized collection");
        }
    }

    public static ImmutableByteHashBag newBagWith(byte... elements)
    {
        return new ImmutableByteHashBag(elements);
    }

    @Override
    public ImmutableByteBag newWith(byte element)
    {
        return ByteHashBag.newBag(this.delegate).with(element).toImmutable();
    }

    @Override
    public ImmutableByteBag newWithout(byte element)
    {
        ByteHashBag hashBag = ByteHashBag.newBag(this.delegate);
        hashBag.remove(element);
        return hashBag.toImmutable();
    }

    @Override
    public ImmutableByteBag newWithAll(ByteIterable elements)
    {
        ByteHashBag bag = ByteHashBag.newBag(this.delegate);
        bag.addAll(elements);
        return bag.toImmutable();
    }

    @Override
    public ImmutableByteBag newWithoutAll(ByteIterable elements)
    {
        ByteHashBag bag = ByteHashBag.newBag(this.delegate);
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
    public boolean contains(byte value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(byte... elements)
    {
        return this.delegate.containsAll(elements);
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
    public ImmutableByteBag select(BytePredicate predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableByteBag selectByOccurrences(IntPredicate predicate)
    {
        return this.delegate.selectByOccurrences(predicate).toImmutable();
    }

    @Override
    public ImmutableByteSet selectUnique()
    {
        MutableByteSet result = ByteSets.mutable.empty();
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
    public ImmutableList<ByteIntPair> topOccurrences(int count)
    {
        return this.delegate.topOccurrences(count).toImmutable();
    }

    @Override
    public ImmutableList<ByteIntPair> bottomOccurrences(int count)
    {
        return this.delegate.bottomOccurrences(count).toImmutable();
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
    public MutableByteList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public int sizeDistinct()
    {
        return this.delegate.sizeDistinct();
    }

    @Override
    public int occurrencesOf(byte item)
    {
        return this.delegate.occurrencesOf(item);
    }

    @Override
    public void forEachWithOccurrences(ByteIntProcedure byteIntProcedure)
    {
        this.delegate.forEachWithOccurrences(byteIntProcedure);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
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
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public byte min()
    {
        return this.delegate.min();
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
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
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
            if (this.size() <= size)
            {
                result.add(this);
            }
            else
            {
                ByteIterator iterator = this.byteIterator();
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
    public ImmutableByteBag toImmutable()
    {
        return this;
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return this.delegate.asLazy();
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
    public ByteIterator byteIterator()
    {
        return new UnmodifiableByteIterator(this.delegate.byteIterator());
    }

    private Object writeReplace()
    {
        return new ImmutableByteBagSerializationProxy(this);
    }

    protected static class ImmutableByteBagSerializationProxy implements Externalizable
    {
        private static final long serialVersionUID = 1L;

        private ByteBag bag;

        public ImmutableByteBagSerializationProxy()
        {
            // Empty constructor for Externalizable class
        }

        protected ImmutableByteBagSerializationProxy(ByteBag bag)
        {
            this.bag = bag;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeInt(this.bag.sizeDistinct());
            try
            {
                this.bag.forEachWithOccurrences(new CheckedByteIntProcedure()
                {
                    @Override
                    public void safeValue(byte item, int count) throws IOException
                    {
                        out.writeByte(item);
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
            MutableByteBag deserializedBag = new ByteHashBag();

            for (int i = 0; i < size; i++)
            {
                deserializedBag.addOccurrences(in.readByte(), in.readInt());
            }

            this.bag = deserializedBag;
        }

        protected Object readResolve()
        {
            return this.bag.toImmutable();
        }
    }
}
