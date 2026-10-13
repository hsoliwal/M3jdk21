/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.mutable.primitive;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.ImmutableFloatStack;
import org.eclipse.collections.api.stack.primitive.MutableFloatStack;
import org.eclipse.collections.impl.factory.primitive.FloatStacks;
import org.eclipse.collections.impl.iterator.UnmodifiableFloatIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;

/**
 * A synchronized view of a {@link MutableFloatStack}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link FloatIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveStack.stg.
 *
 * @see MutableFloatStack#asSynchronized()
 * @see MutableStack#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedFloatStack
        implements MutableFloatStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Object lock;
    private final MutableFloatStack stack;

    public SynchronizedFloatStack(MutableFloatStack stack)
    {
        this(stack, null);
    }

    public SynchronizedFloatStack(MutableFloatStack stack, Object newLock)
    {
        this.stack = stack;
        this.lock = newLock == null ? this : newLock;
    }

    @Override
    public void push(float item)
    {
        synchronized (this.lock)
        {
            this.stack.push(item);
        }
    }

    @Override
    public float pop()
    {
        synchronized (this.lock)
        {
            return this.stack.pop();
        }
    }

    @Override
    public FloatList pop(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.pop(count);
        }
    }

    @Override
    public float peek()
    {
        synchronized (this.lock)
        {
            return this.stack.peek();
        }
    }

    @Override
    public FloatList peek(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.peek(count);
        }
    }

    @Override
    public float peekAt(int index)
    {
        synchronized (this.lock)
        {
            return this.stack.peekAt(index);
        }
    }

    @Override
    public int size()
    {
        synchronized (this.lock)
        {
            return this.stack.size();
        }
    }

    @Override
    public boolean isEmpty()
    {
        synchronized (this.lock)
        {
            return this.stack.isEmpty();
        }
    }

    @Override
    public boolean notEmpty()
    {
        synchronized (this.lock)
        {
            return this.stack.notEmpty();
        }
    }

    @Override
    public void clear()
    {
        synchronized (this.lock)
        {
            this.stack.clear();
        }
    }

    @Override
    public boolean contains(float value)
    {
        synchronized (this.lock)
        {
            return this.stack.contains(value);
        }
    }

    @Override
    public boolean containsAll(float... source)
    {
        synchronized (this.lock)
        {
            return this.stack.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        synchronized (this.lock)
        {
            return this.stack.containsAll(source);
        }
    }

    /**
     * Must be called in a synchronized block.
     */
    @Override
    public FloatIterator floatIterator()
    {
        return new UnmodifiableFloatIterator(this.stack.floatIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.stack.forEach(procedure);
        }
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.noneSatisfy(predicate);
        }
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        synchronized (this.lock)
        {
            return this.stack.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public MutableFloatStack select(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.select(predicate);
        }
    }

    @Override
    public MutableFloatStack reject(FloatPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.reject(predicate);
        }
    }

    @Override
    public <V> MutableStack<V> collect(FloatToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.stack.collect(function);
        }
    }

    @Override
    public double sum()
    {
        synchronized (this.lock)
        {
            return this.stack.sum();
        }
    }

    @Override
    public float max()
    {
        synchronized (this.lock)
        {
            return this.stack.max();
        }
    }

    @Override
    public float min()
    {
        synchronized (this.lock)
        {
            return this.stack.min();
        }
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        synchronized (this.lock)
        {
            return this.stack.minIfEmpty(defaultValue);
        }
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        synchronized (this.lock)
        {
            return this.stack.maxIfEmpty(defaultValue);
        }
    }

    @Override
    public double average()
    {
        synchronized (this.lock)
        {
            return this.stack.average();
        }
    }

    @Override
    public double median()
    {
        synchronized (this.lock)
        {
            return this.stack.median();
        }
    }

    @Override
    public MutableFloatList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedList();
        }
    }

    @Override
    public float[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedArray();
        }
    }

    @Override
    public float[] toArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toArray();
        }
    }

    @Override
    public float[] toArray(float[] target)
    {
        synchronized (this.lock)
        {
            return this.stack.toArray(target);
        }
    }

    @Override
    public String toString()
    {
        synchronized (this.lock)
        {
            return this.stack.toString();
        }
    }

    @Override
    public String makeString()
    {
        synchronized (this.lock)
        {
            return this.stack.makeString();
        }
    }

    @Override
    public String makeString(String separator)
    {
        synchronized (this.lock)
        {
            return this.stack.makeString(separator);
        }
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        synchronized (this.lock)
        {
            return this.stack.makeString(start, separator, end);
        }
    }

    @Override
    public void appendString(Appendable appendable)
    {
        synchronized (this.lock)
        {
            this.stack.appendString(appendable);
        }
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        synchronized (this.lock)
        {
            this.stack.appendString(appendable, separator);
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
            this.stack.appendString(appendable, start, separator, end);
        }
    }

    @Override
    public MutableFloatList toList()
    {
        synchronized (this.lock)
        {
            return this.stack.toList();
        }
    }

    @Override
    public MutableFloatSet toSet()
    {
        synchronized (this.lock)
        {
            return this.stack.toSet();
        }
    }

    @Override
    public MutableFloatBag toBag()
    {
        synchronized (this.lock)
        {
            return this.stack.toBag();
        }
    }

    @Override
    public boolean equals(Object otherStack)
    {
        synchronized (this.lock)
        {
            return this.stack.equals(otherStack);
        }
    }

    @Override
    public int hashCode()
    {
        synchronized (this.lock)
        {
            return this.stack.hashCode();
        }
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        synchronized (this.lock)
        {
            return new LazyFloatIterableAdapter(this);
        }
    }

    @Override
    public MutableFloatStack asUnmodifiable()
    {
        synchronized (this.lock)
        {
            return new UnmodifiableFloatStack(this);
        }
    }

    @Override
    public MutableFloatStack asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableFloatStack toImmutable()
    {
        synchronized (this.lock)
        {
            return FloatStacks.immutable.withAllReversed(this);
        }
    }

    /**
     * @since 9.2.
     */
    public MutableFloatStack newEmpty()
    {
        synchronized (this.lock)
        {
            return this.stack.newEmpty();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectInto(injectedValue, function);
        }
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.stack.chunk(size);
        }
    }

    @Override
    public float getFirst()
    {
        synchronized (this.lock)
        {
            return this.stack.getFirst();
        }
    }

    @Override
    public int indexOf(float value)
    {
        synchronized (this.lock)
        {
            return this.stack.indexOf(value);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectFloatIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectIntoWithIndex(injectedValue, function);
        }
    }

    @Override
    public void forEachWithIndex(FloatIntProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.stack.forEachWithIndex(procedure);
        }
    }

    /**
     * Returns a new MutableFloatStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableFloatStack selectWithIndex(FloatIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableFloatCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableFloatCollection> R selectWithIndex(FloatIntPredicate predicate, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableFloatStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableFloatStack rejectWithIndex(FloatIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableFloatCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableFloatCollection> R rejectWithIndex(FloatIntPredicate predicate, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.rejectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableStack using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V> MutableStack<V> collectWithIndex(FloatIntToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.stack.collectWithIndex(function);
        }
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    public <V, R extends Collection<V>> R collectWithIndex(FloatIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.collectWithIndex(function, target);
        }
    }
}
