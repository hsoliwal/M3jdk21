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

import jdk.m3.collections.eclipse.api.BooleanIterable;
import jdk.m3.collections.eclipse.api.LazyBooleanIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableBooleanBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectBooleanToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectBooleanIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.BooleanPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanIntProcedure;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.BooleanProcedure;
import jdk.m3.collections.eclipse.api.iterator.BooleanIterator;
import jdk.m3.collections.eclipse.api.list.primitive.BooleanList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;
import jdk.m3.collections.eclipse.api.stack.MutableStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ImmutableBooleanStack;
import jdk.m3.collections.eclipse.api.stack.primitive.MutableBooleanStack;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanStacks;
import jdk.m3.collections.eclipse.impl.iterator.UnmodifiableBooleanIterator;
import jdk.m3.collections.eclipse.impl.lazy.primitive.LazyBooleanIterableAdapter;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveStack.stg.
 *
 * @since 3.1.
 */
@SuppressWarnings("serial")
public class UnmodifiableBooleanStack
        implements MutableBooleanStack, Serializable
{
    private static final long serialVersionUID = 1L;

    private final MutableBooleanStack stack;

    public UnmodifiableBooleanStack(MutableBooleanStack stack)
    {
        this.stack = Objects.requireNonNull(stack);
    }

    @Override
    public void push(boolean item)
    {
        throw new UnsupportedOperationException("Cannot call push() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean pop()
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public BooleanList pop(int count)
    {
        throw new UnsupportedOperationException("Cannot call pop() on " + this.getClass().getSimpleName());
    }

    @Override
    public boolean peek()
    {
        return this.stack.peek();
    }

    @Override
    public BooleanList peek(int count)
    {
        return this.stack.peek(count);
    }

    @Override
    public boolean peekAt(int index)
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
    public boolean contains(boolean value)
    {
        return this.stack.contains(value);
    }

    @Override
    public boolean containsAll(boolean... source)
    {
        return this.stack.containsAll(source);
    }

    @Override
    public boolean containsAll(BooleanIterable source)
    {
        return this.stack.containsAll(source);
    }

    @Override
    public BooleanIterator booleanIterator()
    {
        return new UnmodifiableBooleanIterator(this.stack.booleanIterator());
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(BooleanProcedure procedure)
    {
        this.stack.forEach(procedure);
    }

    @Override
    public int count(BooleanPredicate predicate)
    {
        return this.stack.count(predicate);
    }

    @Override
    public boolean anySatisfy(BooleanPredicate predicate)
    {
        return this.stack.anySatisfy(predicate);
    }

    @Override
    public boolean allSatisfy(BooleanPredicate predicate)
    {
        return this.stack.allSatisfy(predicate);
    }

    @Override
    public boolean noneSatisfy(BooleanPredicate predicate)
    {
        return this.stack.noneSatisfy(predicate);
    }

    @Override
    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone)
    {
        return this.stack.detectIfNone(predicate, ifNone);
    }

    @Override
    public MutableBooleanStack select(BooleanPredicate predicate)
    {
        return this.stack.select(predicate);
    }

    @Override
    public MutableBooleanStack reject(BooleanPredicate predicate)
    {
        return this.stack.reject(predicate);
    }

    @Override
    public <V> MutableStack<V> collect(BooleanToObjectFunction<? extends V> function)
    {
        return this.stack.collect(function);
    }

    @Override
    public boolean[] toArray()
    {
        return this.stack.toArray();
    }

    @Override
    public boolean[] toArray(boolean[] target)
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
    public MutableBooleanList toList()
    {
        return this.stack.toList();
    }

    @Override
    public MutableBooleanSet toSet()
    {
        return this.stack.toSet();
    }

    @Override
    public MutableBooleanBag toBag()
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
    public LazyBooleanIterable asLazy()
    {
        return new LazyBooleanIterableAdapter(this);
    }

    @Override
    public MutableBooleanStack asUnmodifiable()
    {
        return this;
    }

    @Override
    public MutableBooleanStack asSynchronized()
    {
        return new SynchronizedBooleanStack(this);
    }

    @Override
    public ImmutableBooleanStack toImmutable()
    {
        return BooleanStacks.immutable.withAllReversed(this);
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableBooleanStack newEmpty()
    {
        return this.stack.newEmpty();
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectInto(injectedValue, function);
    }

    @Override
    public RichIterable<BooleanIterable> chunk(int size)
    {
        return this.stack.chunk(size);
    }

    @Override
    public boolean getFirst()
    {
        return this.stack.getFirst();
    }

    @Override
    public int indexOf(boolean value)
    {
        return this.stack.indexOf(value);
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectBooleanIntToObjectFunction<? super T, ? extends T> function)
    {
        return this.stack.injectIntoWithIndex(injectedValue, function);
    }

    @Override
    public void forEachWithIndex(BooleanIntProcedure procedure)
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
    public <V> MutableStack<V> collectWithIndex(BooleanIntToObjectFunction<? extends V> function)
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
    public <V, R extends Collection<V>> R collectWithIndex(BooleanIntToObjectFunction<? extends V> function, R target)
    {
        return this.stack.collectWithIndex(function, target);
    }

    @Override
    public MutableBooleanStack toStack()
    {
        return this.stack.toStack();
    }
}
