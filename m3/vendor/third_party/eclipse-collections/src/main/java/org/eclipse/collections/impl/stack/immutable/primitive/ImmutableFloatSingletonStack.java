/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stack.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.LazyFloatIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.primitive.MutableFloatBag;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectFloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.list.primitive.FloatList;
import org.eclipse.collections.api.list.primitive.MutableFloatList;
import org.eclipse.collections.api.set.primitive.MutableFloatSet;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.primitive.FloatStack;
import org.eclipse.collections.api.stack.primitive.ImmutableFloatStack;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.factory.primitive.FloatStacks;
import org.eclipse.collections.impl.iterator.SingletonFloatIterator;
import org.eclipse.collections.impl.lazy.primitive.LazyFloatIterableAdapter;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.FloatHashSet;

/**
 * ImmutableFloatSingletonStack is an optimization for {@link ImmutableFloatStack} of size 1.
 * This file was automatically generated from template file immutablePrimitiveSingletonStack.stg.
 */
final class ImmutableFloatSingletonStack implements ImmutableFloatStack, Serializable
{
    private static final long serialVersionUID = 1L;
    private final float element1;

    ImmutableFloatSingletonStack(float element)
    {
        this.element1 = element;
    }

    @Override
    public FloatIterator floatIterator()
    {
        return new SingletonFloatIterator(element1);
    }

    /**
     * @since 7.0.
     */
    @Override
    public void each(FloatProcedure procedure)
    {
        procedure.value(this.element1);
    }

    @Override
    public int count(FloatPredicate predicate)
    {
        return predicate.accept(this.element1) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean allSatisfy(FloatPredicate predicate)
    {
        return predicate.accept(this.element1);
    }

    @Override
    public boolean noneSatisfy(FloatPredicate predicate)
    {
        return !predicate.accept(this.element1);
    }

    @Override
    public float peek()
    {
        return this.element1;
    }

    @Override
    public FloatList peek(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return new FloatArrayList(0);
        }
        if (count == 1)
        {
            return FloatArrayList.newListWith(this.element1);
        }
        throw new IllegalArgumentException("Count must be less than or equal to size: Count = " + count + " Size = 1");
    }

    @Override
    public float peekAt(int index)
    {
        this.checkNegativeCount(index);
        if (index == 0)
        {
            return this.element1;
        }
        throw new IllegalArgumentException("Index must be less than size: Index = " + index + " Size = 1");
    }

    @Override
    public ImmutableFloatStack select(FloatPredicate predicate)
    {
        return predicate.accept(this.element1) ? FloatStacks.immutable.with(this.element1)
                : FloatStacks.immutable.with();
    }

    @Override
    public ImmutableFloatStack reject(FloatPredicate predicate)
    {
        return predicate.accept(this.element1) ? FloatStacks.immutable.with()
                : FloatStacks.immutable.with(this.element1);
    }

    @Override
    public float detectIfNone(FloatPredicate predicate, float ifNone)
    {
        return predicate.accept(this.element1) ? this.element1 : ifNone;
    }

    @Override
    public <V> ImmutableStack<V> collect(FloatToObjectFunction<? extends V> function)
    {
        return Stacks.immutable.with(function.valueOf(this.element1));
    }

    @Override
    public double sum()
    {
        return this.element1;
    }

    @Override
    public float max()
    {
        return this.element1;
    }

    @Override
    public float maxIfEmpty(float defaultValue)
    {
        return this.element1;
    }

    @Override
    public float min()
    {
        return this.element1;
    }

    @Override
    public float minIfEmpty(float defaultValue)
    {
        return this.element1;
    }

    @Override
    public double average()
    {
        return this.element1;
    }

    @Override
    public double median()
    {
        return this.element1;
    }

    @Override
    public float[] toSortedArray()
    {
        return new float[]{this.element1};
    }

    @Override
    public MutableFloatList toSortedList()
    {
        return FloatArrayList.newListWith(this.element1);
    }

    @Override
    public float[] toArray()
    {
        return new float[]{this.element1};
    }

    @Override
    public float[] toArray(float[] target)
    {
        if (target.length < 1)
        {
            target = new float[]{this.element1};
        }
        else
        {
            target[0] = this.element1;
        }
        return target;
    }

    @Override
    public boolean contains(float value)
    {
        return Float.compare(this.element1, value) == 0;
    }

    @Override
    public boolean containsAll(float... source)
    {
        for (float value : source)
        {
            if (Float.compare(this.element1, value) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(FloatIterable source)
    {
        for (FloatIterator iterator = source.floatIterator(); iterator.hasNext(); )
        {
            if (Float.compare(this.element1, iterator.next()) != 0)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public MutableFloatList toList()
    {
        return FloatArrayList.newListWith(this.element1);
    }

    @Override
    public MutableFloatSet toSet()
    {
        return FloatHashSet.newSetWith(this.element1);
    }

    @Override
    public MutableFloatBag toBag()
    {
        return FloatHashBag.newBagWith(this.element1);
    }

    @Override
    public LazyFloatIterable asLazy()
    {
        return new LazyFloatIterableAdapter(this);
    }

    @Override
    public ImmutableFloatStack toImmutable()
    {
        return this;
    }

    @Override
    public ImmutableFloatStack push(float element)
    {
        return FloatStacks.immutable.with(this.element1, element);
    }

    @Override
    public ImmutableFloatStack pop()
    {
        return FloatStacks.immutable.with();
    }

    @Override
    public ImmutableFloatStack pop(int count)
    {
        this.checkNegativeCount(count);
        if (count == 0)
        {
            return this;
        }
        if (count == 1)
        {
            return FloatStacks.immutable.with();
        }
        throw new IllegalArgumentException("Count must be less than size: Count = " + count + " Size = 1");
    }

    private void checkNegativeCount(int count)
    {
        if (count < 0)
        {
            throw new IllegalArgumentException("Count must be positive but was " + count);
        }
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public <T> T injectInto(T injectedValue, ObjectFloatToObjectFunction<? super T, ? extends T> function)
    {
        return function.valueOf(injectedValue, this.element1);
    }

    @Override
    public RichIterable<FloatIterable> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }

        return Lists.immutable.with(this);
    }

    @Override
    public float getFirst()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".getFirst() not implemented yet");
    }

    @Override
    public int indexOf(float value)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".indexOf() not implemented yet");
    }

    @Override
    public <T> T injectIntoWithIndex(T injectedValue, ObjectFloatIntToObjectFunction<? super T, ? extends T> function)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".injectIntoWithIndex() not implemented yet");
    }

    @Override
    public void forEachWithIndex(FloatIntProcedure procedure)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".forEachWithIndex() not implemented yet");
    }

    @Override
    public boolean equals(Object otherStack)
    {
        if (otherStack == this)
        {
            return true;
        }
        if (!(otherStack instanceof FloatStack))
        {
            return false;
        }
        FloatStack stack = (FloatStack) otherStack;
        if (stack.size() != 1)
        {
            return false;
        }
        return Float.compare(this.element1, stack.peek()) == 0;
    }

    @Override
    public int hashCode()
    {
        return 31 + Float.floatToIntBits(this.element1);
    }

    @Override
    public String toString()
    {
        return "[" + this.element1 + ']';
    }

    @Override
    public String makeString()
    {
        return this.makeString(", ");
    }

    @Override
    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(String.valueOf(this.element1));
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
