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
import java.util.Objects;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.LazyDoubleIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableDoubleBag;
import org.eclipse.collections.api.block.function.primitive.DoubleIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectDoubleIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.DoublePredicate;
import org.eclipse.collections.api.block.procedure.primitive.DoubleIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.DoubleProcedure;
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
 * This file was automatically generated from template file unmodifiablePrimitiveStack.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableDoubleStack
        implements MutableDoubleStack, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableDoubleStack stack;

    public UnmodifiableDoubleStack(MutableDoubleStack stack)
    {
        this.stack = Objects.requireNonNull(stack);
    }

    @Override
    public void push(double item)
    {
        throw new UnsupportedOperationException("Cannot call push() on " + this.getClass().getSimpleName());
    }

    @Override
    public double pop()
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public DoubleList pop(int count)
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public double peek()
    {
        return this.stack.peek();
    }

    @Override
    public DoubleList peek(int count)
    {
        return this.stack.peek(count);
    }

    @Override
    public double peekAt(int index)
    {
        return this.stack.peekAt(index);
    }

    @Override
    public int size()
    {
        return this.stack.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.stack.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.stack.notEmpty();
    }

    @Override
    public void clear()
    {
        throw new UnsupportedOperationException("Cannot call clear() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean contains(double value)
    {
        return this.stack.contains(value);
    }

    @Override
    public boolean containsAll(double... source)
    {
        return this.stack.containsAll(source);
    }

    @Override
    public boolean containsAll(DoubleIterable source)
    {
        return this.stack.containsAll(source);
    }

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
        this.stack.forEach(procedure);
    }

    @Override
    public int count(DoublePredicate predicate)
    {
        return this.stack.count(predicate);
    }

    @Override
    public boolean anySatisfy(DoublePredicate predicate)
    {
        return this.stack.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(DoublePredicate predicate)
    {
        return this.stack.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(DoublePredicate predicate)
    {
        return this.stack.noneSatisfy(predicate);
    }

    @Override
    public double detectIfNone(DoublePredicate predicate, double ifNone)
    {
        return this.stack.detectIfNone(predicate, ifNone);
    }

    @Override
    public MutableDoubleStack select(DoublePredicate predicate)
    {
        return this.stack.select(predicate);
    }

    @Override
    public MutableDoubleStack reject(DoublePredicate predicate)
    {
        return this.stack.reject(predicate);
    }

    @Override
    public <V> MutableStack<V> collect(DoubleToObjectFunction<? extends V> function)
    {
        return this.stack.collect(function);
    }

    @Override
    public double sum()
    {
        return this.stack.sum();
    }

    @Override
    public double max()
    {
        return this.stack.max();
    }

    @Override
    public double min()
    {
        return this.stack.min();
    }

    @Override
    public double minIfEmpty(double defaultValue)
    {
        return this.stack.minIfEmpty(defaultValue);
    }

    @Override
    public double maxIfEmpty(double defaultValue)
    {
        return this.stack.maxIfEmpty(defaultValue);
    }

    @Override
    public double average()
    {
        return this.stack.average();
    }

    @Override
    public double median()
    {
        return this.stack.median();
    }

    @Override
    public MutableDoubleList toSortedList()
    {
        return this.stack.toSortedList();
    }

    @Override
    public double[] toSortedArray()
    {
        return this.stack.toSortedArray();
    }

    @Override
    public double[] toArray()
    {
        return this.stack.toArray();
    }

    @Override
    public double[] toArray(double[] target)
    {
        return this.stack.toArray(target);
    }

    @Override
    public String toString()
    {
        return this.stack.toString();
    }

    @Override
    public String makeString()
    {
        return this.stack.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.stack.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.stack.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.stack.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.stack.appendString(appendable, separator);
    }

    @Override
    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end)
    {
        this.stack.appendString(appendable, start, separator, end);
    }

    @Override
    public MutableDoubleList toList()
    {
        return this.stack.toList();
    }

    @Override
    public MutableDoubleSet toSet()
    {
        return this.stack.toSet();
    }

    @Override
    public MutableDoubleBag toBag()
    {
        return this.stack.toBag();
    }

    @Override
    public boolean equals(Object otherStack)
    {
        return this.stack.equals(otherStack);
    }

    @Override
    public int hashCode()
    {
        return this.stack.hashCode();
    }

    @Override
    public LazyDoubleIterable asLazy()
    {
        return new LazyDoubleIterableAdapter(this);
    }

    @Override
    public MutableDoubleStack asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableDoubleStack asSynchronized()
    {
        return new SynchronizedDoubleStack(this);
    }

    @Override
    public ImmutableDoubleStack toImmutable()
    {
        return DoubleStacks.immutable.withAllReversed(this);
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableDoubleStack newEmpty()
    {
        return this.stack.newEmpty();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectDoubleToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<DoubleIterable> chunk(int size)
    {
        return this.stack.chunk(size);
    }

    @Override
    public double getFirst()
    {
        return this.stack.getFirst();
    }

    @Override
    public int indexOf(double value)
    {
        return this.stack.indexOf(value);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectDoubleIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectIntoWithIndex(injectedValue, function);
    }

    @Override
    public void forEachWithIndex(DoubleIntProcedure procedure)
    {
        this.stack.forEachWithIndex(procedure);
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
        return this.stack.collectWithIndex(function);
    }

    /**
     * Adds elements to the target Collection using results obtained by applying the specified function to each element
     * and its corresponding index.
     *
     * @since 9.1.
     */
    @Override
    public <V, R extends Collection<V>> R collectWithIndex(DoubleIntToObjectFunction<? extends V> function, R target)
    {
        return this.stack.collectWithIndex(function, target);
    }
}
