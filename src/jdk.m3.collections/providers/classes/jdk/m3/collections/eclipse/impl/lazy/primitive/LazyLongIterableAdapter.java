/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.lazy.primitive;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableLongBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectLongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.iterator.LongIterator;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;

/**
 * This file was automatically generated from template file lazyPrimitiveIterableAdapter.stg.
 *
 * @since 3.0
 */
public class LazyLongIterableAdapter
        extends AbstractLazyLongIterable
{
    private final LongIterable delegate;

    public LazyLongIterableAdapter(LongIterable delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public LongIterator longIterator()
    {
        return this.delegate.longIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(LongProcedure procedure)
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
    public int count(LongPredicate predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public boolean anySatisfy(LongPredicate predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(LongPredicate predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(LongPredicate predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public long detectIfNone(LongPredicate predicate, long ifNone)
    {
        return this.delegate.detectIfNone(predicate, ifNone);
    }

    @Override
    public long sum()
    {
        return this.delegate.sum();
    }

    @Override
    public long max()
    {
        return this.delegate.max();
    }

    @Override
    public long min()
    {
        return this.delegate.min();
    }

    @Override
    public long minIfEmpty(long defaultValue)
    {
        return this.delegate.minIfEmpty(defaultValue);
    }

    @Override
    public long maxIfEmpty(long defaultValue)
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
    public long[] toSortedArray()
    {
        return this.delegate.toSortedArray();
    }

    @Override
    public MutableLongList toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public long[] toArray()
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
    public boolean contains(long value)
    {
        return this.delegate.contains(value);
    }

    @Override
    public boolean containsAll(long... source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAll(LongIterable source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public MutableLongList toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableLongSet toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableLongBag toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectLongToObjectFunction<? super T, ? extends T> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }
}
