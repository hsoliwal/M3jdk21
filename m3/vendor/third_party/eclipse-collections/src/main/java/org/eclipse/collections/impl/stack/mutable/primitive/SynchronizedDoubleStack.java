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

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoubleIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.api.set.primitive.MutableDoubleSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.ImmutableDoubleStack;
import org.eclipse.collections.api.stack.primitive.MutableDoubleStack;
import org.eclipse.collections.impl.factory.primitive.DoubleStacks;
import org.eclipse.collections.impl.iterator.UnmodifiableDoubleIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyDoubleIterableAdapter;

/**
 * A synchronized view of a {@link MutableDoubleStack}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link DoubleIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveStack.stg.
 *
 * @see MutableDoubleStack#asSynchronized()
 * @see MutableStack#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedDoubleStack
        implements MutableDoubleStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Object lock;
    private final MutableDoubleStack stack;

    public SynchronizedDoubleStack(MutableDoubleStack stack)
    {
        this(stack, null);
    }

    public SynchronizedDoubleStack(MutableDoubleStack stack, Object newLock)
    {
        this.stack = stack;
        this.lock = newLock == null ? this : newLock;
    }

    @Override
    public void push(double item)
    {
        synchronized (this.lock)
        {
            this.stack.push(item);
        }
    }

    @Override
    public double pop()
    {
        synchronized (this.lock)
        {
            return this.stack.pop();
        }
    }

    @Override
    public DoubleList pop(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.pop(count);
        }
    }

    @Override
    public double peek()
    {
        synchronized (this.lock)
        {
            return this.stack.peek();
        }
    }

    @Override
    public DoubleList peek(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.peek(count);
        }
    }

    @Override
    public double peekAt(int index)
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
    public boolean contains(double value)
    {
        synchronized (this.lock)
        {
            return this.stack.contains(value);
        }
    }

    @Override
    public boolean containsAll(double... source)
    {
        synchronized (this.lock)
        {
            return this.stack.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(DoubleIterable source)
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
    public DoubleIterator doubleIterator()
    {
        return new UnmodifiableDoubleIterator(this.stack.doubleIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(DoubleProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.stack.forEach(procedure);
        }
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.noneSatisfy(predicate);
        }
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        synchronized (this.lock)
        {
            return this.stack.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public MutableDoubleStack select(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.select(predicate);
        }
    }

    @Override
    public MutableDoubleStack reject(DoublePredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.reject(predicate);
        }
    }

    @Override
    public <V> MutableStack<V> collect(DoubleToObjectFunction<? extends V> function)
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
    public double max()
    {
        synchronized (this.lock)
        {
            return this.stack.max();
        }
    }

    @Override
    public double min()
    {
        synchronized (this.lock)
        {
            return this.stack.min();
        }
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        synchronized (this.lock)
        {
            return this.stack.minIfEmpty(defaultValue);
        }
    }

    @Override
    public double maxIfEmpty(double defaultValue)
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
    public MutableDoubleList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedList();
        }
    }

    @Override
    public double[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedArray();
        }
    }

    @Override
    public double[] toArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toArray();
        }
    }

    @Override
    public double[] toArray(double[] target)
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
    public MutableDoubleList toList()
    {
        synchronized (this.lock)
        {
            return this.stack.toList();
        }
    }

    @Override
    public MutableDoubleSet toSet()
    {
        synchronized (this.lock)
        {
            return this.stack.toSet();
        }
    }

    @Override
    public MutableDoubleBag toBag()
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
    public LazyDoubleIterable asLazy()
    {
        synchronized (this.lock)
        {
            return new LazyDoubleIterableAdapter(this);
        }
    }

    @Override
    public MutableDoubleStack asUnmodifiable()
    {
        synchronized (this.lock)
        {
            return new UnmodifiableDoubleStack(this);
        }
    }

    @Override
    public MutableDoubleStack asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableDoubleStack toImmutable()
    {
        synchronized (this.lock)
        {
            return DoubleStacks.immutable.withAllReversed(this);
        }
    }

    /**
     * @since 9.2.
     */
    public MutableDoubleStack newEmpty()
    {
        synchronized (this.lock)
        {
            return this.stack.newEmpty();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectInto(injectedValue, function);
        }
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.stack.chunk(size);
        }
    }

    @Override
    public double getFirst()
    {
        synchronized (this.lock)
        {
            return this.stack.getFirst();
        }
    }

    @Override
    public int indexOf(double value)
    {
        synchronized (this.lock)
        {
            return this.stack.indexOf(value);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectIntoWithIndex(injectedValue, function);
        }
    }

    @Override
    public void forEachWithIndex(DoubleIntProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.stack.forEachWithIndex(procedure);
        }
    }

    /**
     * Returns a new MutableDoubleStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableDoubleStack selectWithIndex(DoubleIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableDoubleCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableDoubleCollection> R selectWithIndex(DoubleIntPredicate predicate, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableDoubleStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableDoubleStack rejectWithIndex(DoubleIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableDoubleCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableDoubleCollection> R rejectWithIndex(DoubleIntPredicate predicate, R target)
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
    public <V> MutableStack<V> collectWithIndex(DoubleIntToObjectFunction<? extends V> function)
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
    public <V, R extends Collection<V>> R collectWithIndex(DoubleIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.collectWithIndex(function, target);
        }
    }
}
