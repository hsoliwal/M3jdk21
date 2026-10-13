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
import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.impl.iterator.UnmodifiableDoubleIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;

/**
 * This file was automatically generated from template file abstractUnmodifiablePrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractUnmodifiableDoubleCollection
        implements MutableDoubleCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableDoubleCollection collection;

    protected AbstractUnmodifiableDoubleCollection(MutableDoubleCollection collection)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractUnmodifiableDoubleCollection on a null collection");
        }

        this.collection = collection;
    }

    protected MutableDoubleCollection getDoubleCollection()
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
    public boolean contains(double value)
    {
        return this.collection.contains(value);
    }

    @Override
    public boolean containsAll(double... source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAny(double... source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsAny(DoubleIterable source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsNone(double... source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean containsNone(DoubleIterable source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean add(double newItem)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(double... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(DoubleIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean remove(double value)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    /**
     * @since 9.1
     */
    @Override
    public boolean removeIf(DoublePredicate predicate)
    {
        throw new UnsupportedOperationException("Cannot call removeIf() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(DoubleIterable source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(double... source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(DoubleIterable source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(double... source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleIterator doubleIterator()
    {
        return new UnmodifiableDoubleIterator(this.collection.doubleIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        this.collection.forEach(procedure);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return this.collection.count(predicate);
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return this.collection.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return this.collection.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return this.collection.noneSatisfy(predicate);
    }

    @Override
    public MutableDoubleCollection select(DoublePredicate predicate)
    {
        return this.collection.select(predicate);
    }

    @Override
    public MutableDoubleCollection reject(DoublePredicate predicate)
    {
        return this.collection.reject(predicate);
    }

    @Override
    public <V> MutableCollection<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return this.collection.collect(function);
    }

    @Override
    public MutableDoubleCollection with(double element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleCollection without(double element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleCollection withAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleCollection withoutAll(DoubleIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableDoubleCollection asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableDoubleCollection asSynchronized()
    {
        return new SynchronizedDoubleCollection(this);
    }

    @Override
    public ImmutableDoubleCollection toImmutable()
    {
        return this.collection.toImmutable();
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return this.collection.detectIfNone(predicate, ifNone);
    }

    @Override
    public double sum()
    {
        return this.collection.sum();
    }

    @Override
    public double max()
    {
        return this.collection.max();
    }

    @Override
    public double min()
    {
        return this.collection.min();
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        return this.collection.minIfEmpty(defaultValue);
    }

    @Override
    public double maxIfEmpty(double defaultValue)
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
    public MutableDoubleList toSortedList()
    {
        return this.collection.toSortedList();
    }

    @Override
    public double[] toSortedArray()
    {
        return this.collection.toSortedArray();
    }

    @Override
    public double[] toArray()
    {
        return this.collection.toArray();
    }

    @Override
    public double[] toArray(double[] target)
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
    public MutableDoubleList toList()
    {
        return this.collection.toList();
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return this.collection.toSet();
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return this.collection.toBag();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return this.collection.injectInto(injectedValue, function);
    }

    @Override
    public double reduce(DoubleDoubleToDoubleFunction accumulator)
    {
        return this.collection.reduce(accumulator);
    }

    @Override
    public double reduceIfEmpty(DoubleDoubleToDoubleFunction accumulator, double defaultValue)
    {
        return this.collection.reduceIfEmpty(accumulator, defaultValue);
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        return this.collection.chunk(size);
    }
}
