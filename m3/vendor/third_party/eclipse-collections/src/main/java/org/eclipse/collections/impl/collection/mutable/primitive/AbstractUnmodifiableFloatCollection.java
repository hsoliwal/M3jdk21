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
import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.DoubleFloatToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;

/**
 * This file was automatically generated from template file abstractUnmodifiablePrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractUnmodifiableFloatCollection
        implements MutableFloatCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableFloatCollection collection;

    protected AbstractUnmodifiableFloatCollection(MutableFloatCollection collection)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractUnmodifiableFloatCollection on a null collection");
        }

        this.collection = collection;
    }

    protected MutableFloatCollection getFloatCollection()
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
    public boolean contains(float value)
    {
        return this.collection.contains(value);
    }

    @Override
    public boolean containsAll(float... source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAny(float... source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsAny(FloatIterable source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsNone(float... source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean containsNone(FloatIterable source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean add(float newItem)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(float... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(FloatIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean remove(float value)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    /**
     * @since 9.1
     */
    @Override
    public boolean removeIf(FloatPredicate predicate)
    {
        throw new UnsupportedOperationException("Cannot call removeIf() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(FloatIterable source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(float... source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(FloatIterable source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(float... source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatIterator floatIterator()
    {
        return new UnmodifiableFloatIterator(this.collection.floatIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        this.collection.forEach(procedure);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return this.collection.count(predicate);
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return this.collection.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return this.collection.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return this.collection.noneSatisfy(predicate);
    }

    @Override
    public MutableFloatCollection select(FloatPredicate predicate)
    {
        return this.collection.select(predicate);
    }

    @Override
    public MutableFloatCollection reject(FloatPredicate predicate)
    {
        return this.collection.reject(predicate);
    }

    @Override
    public <V> MutableCollection<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return this.collection.collect(function);
    }

    @Override
    public MutableFloatCollection with(float element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatCollection without(float element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatCollection withAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatCollection withoutAll(FloatIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableFloatCollection asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableFloatCollection asSynchronized()
    {
        return new SynchronizedFloatCollection(this);
    }

    @Override
    public ImmutableFloatCollection toImmutable()
    {
        return this.collection.toImmutable();
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return this.collection.detectIfNone(predicate, ifNone);
    }

    @Override
    public double sum()
    {
        return this.collection.sum();
    }

    @Override
    public float max()
    {
        return this.collection.max();
    }

    @Override
    public float min()
    {
        return this.collection.min();
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        return this.collection.minIfEmpty(defaultValue);
    }

    @Override
    public float maxIfEmpty(float defaultValue)
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
    public MutableFloatList toSortedList()
    {
        return this.collection.toSortedList();
    }

    @Override
    public float[] toSortedArray()
    {
        return this.collection.toSortedArray();
    }

    @Override
    public float[] toArray()
    {
        return this.collection.toArray();
    }

    @Override
    public float[] toArray(float[] target)
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
    public MutableFloatList toList()
    {
        return this.collection.toList();
    }

    @Override
    public MutableFloatSet toSet()
    {
        return this.collection.toSet();
    }

    @Override
    public MutableFloatBag toBag()
    {
        return this.collection.toBag();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return this.collection.injectInto(injectedValue, function);
    }

    @Override
    public double reduce(DoubleFloatToDoubleFunction accumulator)
    {
        return this.collection.reduce(accumulator);
    }

    @Override
    public double reduceIfEmpty(DoubleFloatToDoubleFunction accumulator, double defaultValue)
    {
        return this.collection.reduceIfEmpty(accumulator, defaultValue);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        return this.collection.chunk(size);
    }
}
