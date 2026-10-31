/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.lazy.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.api.block.function.primitive.ObjectByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.api.set.primitive.MutableByteSet;

/**
 * This file was automatically generated from template file lazyPrimitiveIterableAdapter.stg.
 *
 * @since 3.0
 */
public class LazyByteIterableAdapter
        extends AbstractLazyByteIterable
{
    private final ByteIterable delegate;

    public LazyByteIterableAdapter(ByteIterable delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public ByteIterator byteIterator()
    {
        return this.delegate.byteIterator();
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
    public byte maxIfEmpty(byte defaultValue)
    {
        return this.delegate.maxIfEmpty(defaultValue);
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
    public <T> T injectInto(T injectedValue, ObjectByteToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }
}
