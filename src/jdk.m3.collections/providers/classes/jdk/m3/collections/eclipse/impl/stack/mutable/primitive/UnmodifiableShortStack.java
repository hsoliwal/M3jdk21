/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.stack.mutable.primitive;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import jdk.m3.collections.eclipse.api.ShortIterable;
import jdk.m3.collections.eclipse.api.LazyShortIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableShortBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectShortIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.ShortPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortProcedure;
import jdk.m3.collections.eclipse.api.iterator.ShortIterator;
import jdk.m3.collections.eclipse.api.list.primitive.ShortList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.api.stack.MutableStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableShortStack;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableShortStack;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortStacks;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableShortIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyShortIterableAdapter;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveStack.stg.
 *
 * @since 3.1.
 */
@SuppressWarnings("serial")
public class UnmodifiableShortStack
        implements MutableShortStack, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableShortStack stack;

    public UnmodifiableShortStack(MutableShortStack stack)
    {
        this.stack = Objects.requireNonNull(stack);
    }

    @Override
    public void push(short item)
    {
        throw new UnsupportedOperationException("Cannot call push() on " + this.getClass().getSimpleName());
    }

    @Override
    public short pop()
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public ShortList pop(int count)
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public short peek()
    {
        return this.stack.peek();
    }

    @Override
    public ShortList peek(int count)
    {
        return this.stack.peek(count);
    }

    @Override
    public short peekAt(int index)
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
    public boolean contains(short value)
    {
        return this.stack.contains(value);
    }

    @Override
    public boolean containsAll(short... source)
    {
        return this.stack.containsAll(source);
    }

    @Override
    public boolean containsAll(ShortIterable source)
    {
        return this.stack.containsAll(source);
    }

    @Override
    public ShortIterator shortIterator()
    {
        return new UnmodifiableShortIterator(this.stack.shortIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(ShortProcedure procedure)
    {
        this.stack.forEach(procedure);
    }

    @Override
    public int count(ShortPredicate predicate)
    {
        return this.stack.count(predicate);
    }

    @Override
    public boolean anySatisfy(ShortPredicate predicate)
    {
        return this.stack.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(ShortPredicate predicate)
    {
        return this.stack.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(ShortPredicate predicate)
    {
        return this.stack.noneSatisfy(predicate);
    }

    @Override
    public short detectIfNone(ShortPredicate predicate, short ifNone)
    {
        return this.stack.detectIfNone(predicate, ifNone);
    }

    @Override
    public MutableShortStack select(ShortPredicate predicate)
    {
        return this.stack.select(predicate);
    }

    @Override
    public MutableShortStack reject(ShortPredicate predicate)
    {
        return this.stack.reject(predicate);
    }

    @Override
    public <V> MutableStack<V> collect(ShortToObjectFunction<? extends V> function)
    {
        return this.stack.collect(function);
    }

    @Override
    public long sum()
    {
        return this.stack.sum();
    }

    @Override
    public short max()
    {
        return this.stack.max();
    }

    @Override
    public short min()
    {
        return this.stack.min();
    }

    @Override
    public short minIfEmpty(short defaultValue)
    {
        return this.stack.minIfEmpty(defaultValue);
    }

    @Override
    public short maxIfEmpty(short defaultValue)
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
    public MutableShortList toSortedList()
    {
        return this.stack.toSortedList();
    }

    @Override
    public short[] toSortedArray()
    {
        return this.stack.toSortedArray();
    }

    @Override
    public short[] toArray()
    {
        return this.stack.toArray();
    }

    @Override
    public short[] toArray(short[] target)
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
    public MutableShortList toList()
    {
        return this.stack.toList();
    }

    @Override
    public MutableShortSet toSet()
    {
        return this.stack.toSet();
    }

    @Override
    public MutableShortBag toBag()
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
    public LazyShortIterable asLazy()
    {
        return new LazyShortIterableAdapter(this);
    }

    @Override
    public MutableShortStack asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableShortStack asSynchronized()
    {
        return new SynchronizedShortStack(this);
    }

    @Override
    public ImmutableShortStack toImmutable()
    {
        return ShortStacks.immutable.withAllReversed(this);
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableShortStack newEmpty()
    {
        return this.stack.newEmpty();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectShortToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<ShortIterable> chunk(int size)
    {
        return this.stack.chunk(size);
    }

    @Override
    public short getFirst()
    {
        return this.stack.getFirst();
    }

    @Override
    public int indexOf(short value)
    {
        return this.stack.indexOf(value);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectShortIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectIntoWithIndex(injectedValue, function);
    }

    @Override
    public void forEachWithIndex(ShortIntProcedure procedure)
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
    public <V> MutableStack<V> collectWithIndex(ShortIntToObjectFunction<? extends V> function)
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
    public <V, R extends Collection<V>> R collectWithIndex(ShortIntToObjectFunction<? extends V> function, R target)
    {
        return this.stack.collectWithIndex(function, target);
    }

    @Override
    public MutableShortStack toStack()
    {
        return this.stack.toStack();
    }
}
