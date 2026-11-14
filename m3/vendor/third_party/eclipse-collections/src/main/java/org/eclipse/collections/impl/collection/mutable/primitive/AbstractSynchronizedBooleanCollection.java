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
import org.eclipse.collections.impl.lazy.primitive.LazyBooleanIterableAdapter;

/**
 * This file was automatically generated from template file abstractSynchronizedPrimitiveCollection.stg.
 *
 * @since 3.1.
 */
public abstract class AbstractSynchronizedBooleanCollection
        implements MutableBooleanCollection, Serializable
{
    private static final long serialVersionUID = 1L;

    private final Object lock;
    private final MutableBooleanCollection collection;

    protected AbstractSynchronizedBooleanCollection(MutableBooleanCollection collection)
    {
        this(collection, null);
    }

    protected AbstractSynchronizedBooleanCollection(MutableBooleanCollection collection, Object newLock)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("Cannot create a AbstractSynchronizedBooleanCollection on a null collection");
        }
        this.collection = collection;
        this.lock = newLock == null ? this : newLock;
    }

    protected Object getLock()
    {
        return this.lock;
    }

    protected MutableBooleanCollection getBooleanCollection()
    {
        return this.collection;
    }

    @Override
    public int size()
    {
        synchronized (this.lock)
        {
            return this.collection.size();
        }
    }

    @Override
    public boolean isEmpty()
    {
        synchronized (this.lock)
        {
            return this.collection.isEmpty();
        }
    }

    @Override
    public boolean notEmpty()
    {
        synchronized (this.lock)
        {
            return this.collection.notEmpty();
        }
    }

    @Override
    public void clear()
    {
        synchronized (this.lock)
        {
            this.collection.clear();
        }
    }

    @Override
    public MutableBooleanCollection select(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.select(predicate);
        }
    }

    @Override
    public MutableBooleanCollection reject(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.reject(predicate);
        }
    }

    @Override
    public <V> MutableCollection<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.collection.collect(function);
        }
    }

    @Override
    public MutableBooleanCollection with(boolean element)
    {
        synchronized (this.lock)
        {
            this.add(element);
        }
        return this;
    }

    @Override
    public MutableBooleanCollection without(boolean element)
    {
        synchronized (this.lock)
        {
            this.remove(element);
        }
        return this;
    }

    @Override
    public MutableBooleanCollection withAll(BooleanIterable elements)
    {
        synchronized (this.lock)
        {
            this.addAll(elements);
        }
        return this;
    }

    @Override
    public MutableBooleanCollection withoutAll(BooleanIterable elements)
    {
        synchronized (this.lock)
        {
            this.removeAll(elements);
        }
        return this;
    }

    @Override
    public MutableBooleanCollection asUnmodifiable()
    {
        return new UnmodifiableBooleanCollection(this);
    }

    @Override
    public MutableBooleanCollection asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableBooleanCollection toImmutable()
    {
        synchronized (this.lock)
        {
            return this.collection.toImmutable();
        }
    }

    @Override
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public boolean contains(boolean value)
    {
        synchronized (this.lock)
        {
            return this.collection.contains(value);
        }
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAll(source);
        }
    }

    @Override
    public boolean containsAny(boolean... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsAny(BooleanIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsAny(source);
        }
    }

    @Override
    public boolean containsNone(boolean... source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean containsNone(BooleanIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.containsNone(source);
        }
    }

    @Override
    public boolean add(boolean newItem)
    {
        synchronized (this.lock)
        {
            return this.collection.add(newItem);
        }
    }

    @Override
    public boolean addAll(boolean... source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean addAll(BooleanIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.addAll(source);
        }
    }

    @Override
    public boolean remove(boolean value)
    {
        synchronized (this.lock)
        {
            return this.collection.remove(value);
        }
    }

    @Override
    public boolean removeIf(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.removeIf(predicate);
        }
    }

    @Override
    public boolean removeAll(BooleanIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean removeAll(boolean... source)
    {
        synchronized (this.lock)
        {
            return this.collection.removeAll(source);
        }
    }

    @Override
    public boolean retainAll(BooleanIterable source)
    {
        synchronized (this.lock)
        {
            return this.collection.retainAll(source);
        }
    }

    @Override
    public boolean retainAll(boolean... source)
    {
        synchronized (this.lock)
        {
            return this.collection.retainAll(source);
        }
    }

    /**
     * Must be called in a synchronized block.
     */
    @Override
    public MutableBooleanIterator booleanIterator()
    {
        return this.collection.booleanIterator();
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.collection.forEach(procedure);
        }
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.collection.noneSatisfy(predicate);
        }
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        synchronized (this.lock)
        {
            return this.collection.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public boolean[] toArray()
    {
        synchronized (this.lock)
        {
            return this.collection.toArray();
        }
    }

    @Override
    public boolean[] toArray(boolean[] target)
    {
        synchronized (this.lock)
        {
            return this.collection.toArray(target);
        }
    }

    @Override
    public String toString()
    {
        synchronized (this.lock)
        {
            return this.collection.toString();
        }
    }

    @Override
    public String makeString()
    {
        synchronized (this.lock)
        {
            return this.collection.makeString();
        }
    }

    @Override
    public String makeString(String separator)
    {
        synchronized (this.lock)
        {
            return this.collection.makeString(separator);
        }
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            return this.collection.makeString(start, separator, end);
        }
    }

    @Override
    public void appendString(Appendable appendable)
    {
        synchronized (this.lock)
        {
            this.collection.appendString(appendable);
        }
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        synchronized (this.lock)
        {
            this.collection.appendString(appendable, separator);
        }
    }

    @Override
    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        synchronized (this.lock)
        {
            this.collection.appendString(appendable, start, separator, end);
        }
    }

    @Override
    public MutableBooleanList toList()
    {
        synchronized (this.lock)
        {
            return this.collection.toList();
        }
    }

    @Override
    public MutableBooleanSet toSet()
    {
        synchronized (this.lock)
        {
            return this.collection.toSet();
        }
    }

    @Override
    public MutableBooleanBag toBag()
    {
        synchronized (this.lock)
        {
            return this.collection.toBag();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.collection.injectInto(injectedValue, function);
        }
    }

    @Override
    public boolean reduce(BooleanBooleanToBooleanFunction accumulator)
    {
        synchronized (this.lock)
        {
            return this.collection.reduce(accumulator);
        }
    }

    @Override
    public boolean reduceIfEmpty(BooleanBooleanToBooleanFunction accumulator, boolean defaultValue)
    {
        synchronized (this.lock)
        {
            return this.collection.reduceIfEmpty(accumulator, defaultValue);
        }
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.collection.chunk(size);
        }
    }
}
