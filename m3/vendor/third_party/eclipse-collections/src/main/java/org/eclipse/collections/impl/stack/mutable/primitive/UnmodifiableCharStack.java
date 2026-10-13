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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.primitive.CharList;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.primitive.ImmutableCharStack;
import org.eclipse.collections.api.stack.primitive.MutableCharStack;
import org.eclipse.collections.impl.factory.primitive.CharStacks;
import org.eclipse.collections.impl.iterator.UnmodifiableCharIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyCharIterableAdapter;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveStack.stg.
 *
 * @since 3.1.
 */
public class UnmodifiableCharStack
        implements MutableCharStack, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableCharStack stack;

    public UnmodifiableCharStack(MutableCharStack stack)
    {
        this.stack = Objects.requireNonNull(stack);
    }

    @Override
    public void push(char item)
    {
        throw new UnsupportedOperationException("Cannot call push() on " + this.getClass().getSimpleName());
    }

    @Override
    public char pop()
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public CharList pop(int count)
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public char peek()
    {
        return this.stack.peek();
    }

    @Override
    public CharList peek(int count)
    {
        return this.stack.peek(count);
    }

    @Override
    public char peekAt(int index)
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
    public boolean contains(char value)
    {
        return this.stack.contains(value);
    }

    @Override
    public boolean containsAll(char... source)
    {
        return this.stack.containsAll(source);
    }

    @Override
    public boolean containsAll(CharIterable source)
    {
        return this.stack.containsAll(source);
    }

    @Override
    public CharIterator charIterator()
    {
        return new UnmodifiableCharIterator(this.stack.charIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(CharProcedure procedure)
    {
        this.stack.forEach(procedure);
    }

    @Override
    public int count(CharPredicate predicate)
    {
        return this.stack.count(predicate);
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        return this.stack.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        return this.stack.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        return this.stack.noneSatisfy(predicate);
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        return this.stack.detectIfNone(predicate, ifNone);
    }

    @Override
    public MutableCharStack select(CharPredicate predicate)
    {
        return this.stack.select(predicate);
    }

    @Override
    public MutableCharStack reject(CharPredicate predicate)
    {
        return this.stack.reject(predicate);
    }

    @Override
    public <V> MutableStack<V> collect(CharToObjectFunction<? extends V> function)
    {
        return this.stack.collect(function);
    }

    @Override
    public long sum()
    {
        return this.stack.sum();
    }

    @Override
    public char max()
    {
        return this.stack.max();
    }

    @Override
    public char min()
    {
        return this.stack.min();
    }

    @Override
    public char minIfEmpty(char defaultValue)
    {
        return this.stack.minIfEmpty(defaultValue);
    }

    @Override
    public char maxIfEmpty(char defaultValue)
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
    public MutableCharList toSortedList()
    {
        return this.stack.toSortedList();
    }

    @Override
    public char[] toSortedArray()
    {
        return this.stack.toSortedArray();
    }

    @Override
    public char[] toArray()
    {
        return this.stack.toArray();
    }

    @Override
    public char[] toArray(char[] target)
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
    public MutableCharList toList()
    {
        return this.stack.toList();
    }

    @Override
    public MutableCharSet toSet()
    {
        return this.stack.toSet();
    }

    @Override
    public MutableCharBag toBag()
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
    public LazyCharIterable asLazy()
    {
        return new LazyCharIterableAdapter(this);
    }

    @Override
    public MutableCharStack asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableCharStack asSynchronized()
    {
        return new SynchronizedCharStack(this);
    }

    @Override
    public ImmutableCharStack toImmutable()
    {
        return CharStacks.immutable.withAllReversed(this);
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableCharStack newEmpty()
    {
        return this.stack.newEmpty();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        return this.stack.chunk(size);
    }

    @Override
    public char getFirst()
    {
        return this.stack.getFirst();
    }

    @Override
    public int indexOf(char value)
    {
        return this.stack.indexOf(value);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectCharIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectIntoWithIndex(injectedValue, function);
    }

    @Override
    public void forEachWithIndex(CharIntProcedure procedure)
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
    public <V> MutableStack<V> collectWithIndex(CharIntToObjectFunction<? extends V> function)
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
    public <V, R extends Collection<V>> R collectWithIndex(CharIntToObjectFunction<? extends V> function, R target)
    {
        return this.stack.collectWithIndex(function, target);
    }
}
