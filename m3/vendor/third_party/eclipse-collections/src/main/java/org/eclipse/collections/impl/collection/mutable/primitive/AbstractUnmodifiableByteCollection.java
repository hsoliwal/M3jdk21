/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.collection.mutable.primitive;

import java.io.Serializable;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.LazyByteIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.LongByteToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;
import org.eclipse.collections.impl.iterator.UnmodifiableByteIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyByteIterableAdapter;

/**
 * This file was automatically generated from template file abstractUnmodifiablePrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractUnmodifiableByteCollection
        implements MutableByteCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableByteCollection collection;

    protected AbstractUnmodifiableByteCollection(MutableByteCollection collection)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractUnmodifiableByteCollection on a null collection");
        }

        this.collection = collection;
    }

    protected MutableByteCollection getByteCollection()
    {
        return this.collection;
    }

    @Override
    public int size()
    {
        return this.collection.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.collection.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.collection.notEmpty();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean contains(byte value)
    {
        return this.collection.contains(value);
    }

    @Override
    public boolean containsAll(byte... source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAll(ByteIterable source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAny(byte... source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsAny(ByteIterable source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsNone(byte... source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean containsNone(ByteIterable source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean add(byte newItem)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(byte... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(ByteIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean remove(byte value)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    /**
     * @since 9.1
     */
    @Override
    public boolean removeIf(BytePredicate predicate)
    {
        throw new UnsupportedOperationException("Cannot call removeIf() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(ByteIterable source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(byte... source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(ByteIterable source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(byte... source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteIterator byteIterator()
    {
        return new UnmodifiableByteIterator(this.collection.byteIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ByteProcedure procedure)
    {
        this.collection.forEach(procedure);
    }

    @Override
    public int count(BytePredicate predicate)
    {
        return this.collection.count(predicate);
    }

    @Override
    public boolean anySatisfy(BytePredicate predicate)
    {
        return this.collection.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BytePredicate predicate)
    {
        return this.collection.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BytePredicate predicate)
    {
        return this.collection.noneSatisfy(predicate);
    }

    @Override
    public MutableByteCollection select(BytePredicate predicate)
    {
        return this.collection.select(predicate);
    }

    @Override
    public MutableByteCollection reject(BytePredicate predicate)
    {
        return this.collection.reject(predicate);
    }

    @Override
    public <V> MutableCollection<V> collect(ByteToObjectFunction<? extends V> function)
    {
        return this.collection.collect(function);
    }

    @Override
    public MutableByteCollection with(byte element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteCollection without(byte element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteCollection withAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteCollection withoutAll(ByteIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableByteCollection asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableByteCollection asSynchronized()
    {
        return new SynchronizedByteCollection(this);
    }

    @Override
    public ImmutableByteCollection toImmutable()
    {
        return this.collection.toImmutable();
    }

    @Override
    public LazyByteIterable asLazy()
    {
        return new LazyByteIterableAdapter(this);
    }

    @Override
    public byte detectIfNone(BytePredicate predicate, byte ifNone)
    {
        return this.collection.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.collection.sum();
    }

    @Override
    public byte max()
    {
        return this.collection.max();
    }

    @Override
    public byte min()
    {
        return this.collection.min();
    }

    @Override
    public byte minIfEmpty(byte defaultValue)
    {
        return this.collection.minIfEmpty(defaultValue);
    }

    @Override
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.collection.maxIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.collection.average();
    }

    @Override
    public double median()
    {
        return this.collection.median();
    }

    @Override
    public MutableByteList toSortedList()
    {
        return this.collection.toSortedList();
    }

    @Override
    public byte[] toSortedArray()
    {
        return this.collection.toSortedArray();
    }

    @Override
    public byte[] toArray()
    {
        return this.collection.toArray();
    }

    @Override
    public byte[] toArray(byte[] target)
    {
        return this.collection.toArray(target);
    }

    @Override
    public String toString()
    {
        return this.collection.toString();
    }

    @Override
    public String makeString()
    {
        return this.collection.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.collection.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.collection.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.collection.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.collection.appendString(appendable, separator);
    }

    @Override
    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        this.collection.appendString(appendable, start, separator, end);
    }

    @Override
    public MutableByteList toList()
    {
        return this.collection.toList();
    }

    @Override
    public MutableByteSet toSet()
    {
        return this.collection.toSet();
    }

    @Override
    public MutableByteBag toBag()
    {
        return this.collection.toBag();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return this.collection.injectInto(injectedValue, function);
    }

    @Override
    public long reduce(LongByteToLongFunction accumulator)
    {
        return this.collection.reduce(accumulator);
    }

    @Override
    public long reduceIfEmpty(LongByteToLongFunction accumulator, long defaultValue)
    {
        return this.collection.reduceIfEmpty(accumulator, defaultValue);
    }

    @Override
    public RichIterable<ByteIterable> chunk(int size)
    {
        return this.collection.chunk(size);
    }
}
