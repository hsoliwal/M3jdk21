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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.api.block.function.primitive.CharIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectCharIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharIntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
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
 * A synchronized view of a {@link MutableCharStack}. It is imperative that the user manually synchronize on the collection when iterating over it using the
 * {@link CharIterator}, as per {@link Collections#synchronizedCollection(Collection)}.
 * <p>
 * This file was automatically generated from template file synchronizedPrimitiveStack.stg.
 *
 * @see MutableCharStack#asSynchronized()
 * @see MutableStack#asSynchronized()
 * @since 3.1.
 */
public class SynchronizedCharStack
        implements MutableCharStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final Object lock;
    private final MutableCharStack stack;

    public SynchronizedCharStack(MutableCharStack stack)
    {
        this(stack, null);
    }

    public SynchronizedCharStack(MutableCharStack stack, Object newLock)
    {
        this.stack = stack;
        this.lock = newLock == null ? this : newLock;
    }

    @Override
    public void push(char item)
    {
        synchronized (this.lock)
        {
            this.stack.push(item);
        }
    }

    @Override
    public char pop()
    {
        synchronized (this.lock)
        {
            return this.stack.pop();
        }
    }

    @Override
    public CharList pop(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.pop(count);
        }
    }

    @Override
    public char peek()
    {
        synchronized (this.lock)
        {
            return this.stack.peek();
        }
    }

    @Override
    public CharList peek(int count)
    {
        synchronized (this.lock)
        {
            return this.stack.peek(count);
        }
    }

    @Override
    public char peekAt(int index)
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
    public boolean contains(char value)
    {
        synchronized (this.lock)
        {
            return this.stack.contains(value);
        }
    }

    @Override
    public boolean containsAll(char... source)
    {
        synchronized (this.lock)
        {
            return this.stack.containsAll(source);
        }
    }

    @Override
    public boolean containsAll(CharIterable source)
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
        synchronized (this.lock)
        {
            this.stack.forEach(procedure);
        }
    }

    @Override
    public int count(CharPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.count(predicate);
        }
    }

    @Override
    public boolean anySatisfy(CharPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.anySatisfy(predicate);
        }
    }

    @Override
    public boolean allSatisfy(CharPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.allSatisfy(predicate);
        }
    }

    @Override
    public boolean noneSatisfy(CharPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.noneSatisfy(predicate);
        }
    }

    @Override
    public char detectIfNone(CharPredicate predicate, char ifNone)
    {
        synchronized (this.lock)
        {
            return this.stack.detectIfNone(predicate, ifNone);
        }
    }

    @Override
    public MutableCharStack select(CharPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.select(predicate);
        }
    }

    @Override
    public MutableCharStack reject(CharPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.reject(predicate);
        }
    }

    @Override
    public <V> MutableStack<V> collect(CharToObjectFunction<? extends V> function)
    {
        synchronized (this.lock)
        {
            return this.stack.collect(function);
        }
    }

    @Override
    public long sum()
    {
        synchronized (this.lock)
        {
            return this.stack.sum();
        }
    }

    @Override
    public char max()
    {
        synchronized (this.lock)
        {
            return this.stack.max();
        }
    }

    @Override
    public char min()
    {
        synchronized (this.lock)
        {
            return this.stack.min();
        }
    }

    @Override
    public char minIfEmpty(char defaultValue)
    {
        synchronized (this.lock)
        {
            return this.stack.minIfEmpty(defaultValue);
        }
    }

    @Override
    public char maxIfEmpty(char defaultValue)
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
    public MutableCharList toSortedList()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedList();
        }
    }

    @Override
    public char[] toSortedArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toSortedArray();
        }
    }

    @Override
    public char[] toArray()
    {
        synchronized (this.lock)
        {
            return this.stack.toArray();
        }
    }

    @Override
    public char[] toArray(char[] target)
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
    public MutableCharList toList()
    {
        synchronized (this.lock)
        {
            return this.stack.toList();
        }
    }

    @Override
    public MutableCharSet toSet()
    {
        synchronized (this.lock)
        {
            return this.stack.toSet();
        }
    }

    @Override
    public MutableCharBag toBag()
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
    public LazyCharIterable asLazy()
    {
        synchronized (this.lock)
        {
            return new LazyCharIterableAdapter(this);
        }
    }

    @Override
    public MutableCharStack asUnmodifiable()
    {
        synchronized (this.lock)
        {
            return new UnmodifiableCharStack(this);
        }
    }

    @Override
    public MutableCharStack asSynchronized()
    {
        return this;
    }

    @Override
    public ImmutableCharStack toImmutable()
    {
        synchronized (this.lock)
        {
            return CharStacks.immutable.withAllReversed(this);
        }
    }

    /**
     * @since 9.2.
     */
    public MutableCharStack newEmpty()
    {
        synchronized (this.lock)
        {
            return this.stack.newEmpty();
        }
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectCharToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectInto(injectedValue, function);
        }
    }

    @Override
    public RichIterable<CharIterable> chunk(int size)
    {
        synchronized (this.lock)
        {
            return this.stack.chunk(size);
        }
    }

    @Override
    public char getFirst()
    {
        synchronized (this.lock)
        {
            return this.stack.getFirst();
        }
    }

    @Override
    public int indexOf(char value)
    {
        synchronized (this.lock)
        {
            return this.stack.indexOf(value);
        }
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectCharIntToObjectFunction<? super T, ? extends T> function)
    {
        synchronized (this.lock)
        {
            return this.stack.injectIntoWithIndex(injectedValue, function);
        }
    }

    @Override
    public void forEachWithIndex(CharIntProcedure procedure)
    {
        synchronized (this.lock)
        {
            this.stack.forEachWithIndex(procedure);
        }
    }

    /**
     * Returns a new MutableCharStack including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableCharStack selectWithIndex(CharIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableCharCollection including all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableCharCollection> R selectWithIndex(CharIntPredicate predicate, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.selectWithIndex(predicate, target);
        }
    }

    /**
     * Returns a new MutableCharStack excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public MutableCharStack rejectWithIndex(CharIntPredicate predicate)
    {
        synchronized (this.lock)
        {
            return this.stack.rejectWithIndex(predicate);
        }
    }

    /**
     * Returns a new MutableCharCollection excluding all elements with corresponding indexes matching the specified predicate.
     *
     * @since 11.1.
     */
    public <R extends MutableCharCollection> R rejectWithIndex(CharIntPredicate predicate, R target)
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
    public <V> MutableStack<V> collectWithIndex(CharIntToObjectFunction<? extends V> function)
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
    public <V, R extends Collection<V>> R collectWithIndex(CharIntToObjectFunction<? extends V> function, R target)
    {
        synchronized (this.lock)
        {
            return this.stack.collectWithIndex(function, target);
        }
    }
}
