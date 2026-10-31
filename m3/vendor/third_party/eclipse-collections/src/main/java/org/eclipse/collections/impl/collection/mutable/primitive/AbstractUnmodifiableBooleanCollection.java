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
import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.LazyBooleanIterable;
import org.eclipse.collections.api.bag.primitive.MutableBooleanBag;
import org.eclipse.collections.api.block.function.primitive.BooleanBooleanToBooleanFunction;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.ImmutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.iterator.MutableBooleanIterator;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.set.primitive.MutableBooleanSet;
import org.eclipse.collections.impl.iterator.UnmodifiableBooleanIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;

/**
 * This file was automatically generated from template file abstractUnmodifiablePrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractUnmodifiableBooleanCollection
        implements MutableBooleanCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableBooleanCollection collection;

    protected AbstractUnmodifiableBooleanCollection(MutableBooleanCollection collection)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractUnmodifiableBooleanCollection on a null collection");
        }

        this.collection = collection;
    }

    protected MutableBooleanCollection getBooleanCollection()
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
    public boolean contains(boolean value)
    {
        return this.collection.contains(value);
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return this.collection.containsAll(source);
    }

    @Override
    public boolean containsAny(boolean... source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsAny(BooleanIterable source)
    {
        return this.collection.containsAny(source);
    }

    @Override
    public boolean containsNone(boolean... source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean containsNone(BooleanIterable source)
    {
        return this.collection.containsNone(source);
    }

    @Override
    public boolean add(boolean newItem)
    {
        throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(boolean... source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean addAll(BooleanIterable source)
    {
        throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean remove(boolean value)
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }

    /**
     * @since 9.1
     */
    @Override
    public boolean removeIf(BooleanPredicate predicate)
    {
        throw new UnsupportedOperationException("Cannot call removeIf() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(BooleanIterable source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean removeAll(boolean... source)
    {
        throw new UnsupportedOperationException("Cannot call removeAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(BooleanIterable source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean retainAll(boolean... source)
    {
        throw new UnsupportedOperationException("Cannot call retainAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableBooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(this.collection.booleanIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        this.collection.forEach(procedure);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return this.collection.count(predicate);
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return this.collection.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return this.collection.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return this.collection.noneSatisfy(predicate);
    }

    @Override
    public MutableBooleanCollection select(BooleanPredicate predicate)
    {
        return this.collection.select(predicate);
    }

    @Override
    public MutableBooleanCollection reject(BooleanPredicate predicate)
    {
        return this.collection.reject(predicate);
    }

    @Override
    public <V> MutableCollection<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return this.collection.collect(function);
    }

    @Override
    public MutableBooleanCollection with(boolean element)
    {
        throw new UnsupportedOperationException("Cannot call with() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableBooleanCollection without(boolean element)
    {
        throw new UnsupportedOperationException("Cannot call without() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableBooleanCollection withAll(BooleanIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableBooleanCollection withoutAll(BooleanIterable elements)
    {
        throw new UnsupportedOperationException("Cannot call withoutAll() on " + this.getClass().getSimpleName());
    }

    @Override
    public MutableBooleanCollection asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableBooleanCollection asSynchronized()
    {
        return new SynchronizedBooleanCollection(this);
    }

    @Override
    public ImmutableBooleanCollection toImmutable()
    {
        return this.collection.toImmutable();
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return this.collection.detectIfNone(predicate, ifNone);
    }

    @Override
    public boolean[] toArray()
    {
        return this.collection.toArray();
    }

    @Override
    public boolean[] toArray(boolean[] target)
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
    public MutableBooleanList toList()
    {
        return this.collection.toList();
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return this.collection.toSet();
    }

    @Override
    public MutableBooleanBag toBag()
    {
        return this.collection.toBag();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return this.collection.injectInto(injectedValue, function);
    }

    @Override
    public boolean reduce(BooleanBooleanToBooleanFunction accumulator)
    {
        return this.collection.reduce(accumulator);
    }

    @Override
    public boolean reduceIfEmpty(BooleanBooleanToBooleanFunction accumulator, boolean defaultValue)
    {
        return this.collection.reduceIfEmpty(accumulator, defaultValue);
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        return this.collection.chunk(size);
    }
}
